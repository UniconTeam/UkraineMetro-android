#include "geometry.h"

#include <cmath>

namespace metro {

namespace {

constexpr float kPi = 3.14159265358979323846f;
constexpr int kDiscSlices = 10;

}  // namespace

void appendDisc(std::vector<ColorVertex>& out, float cx, float cy, float radius,
                float r, float g, float b, float a) {
    if (radius <= 0.0f) {
        return;
    }
    for (int i = 0; i < kDiscSlices; ++i) {
        const float a0 = (static_cast<float>(i) / kDiscSlices) * 2.0f * kPi;
        const float a1 = (static_cast<float>(i + 1) / kDiscSlices) * 2.0f * kPi;
        out.push_back({cx, cy, r, g, b, a});
        out.push_back({cx + std::cos(a0) * radius, cy + std::sin(a0) * radius, r, g, b, a});
        out.push_back({cx + std::cos(a1) * radius, cy + std::sin(a1) * radius, r, g, b, a});
    }
}

void appendQuad(std::vector<ColorVertex>& out, float x0, float y0, float x1, float y1,
                float nx, float ny, float half, float r, float g, float b, float a) {
    const float ax = x0 + nx * half;
    const float ay = y0 + ny * half;
    const float bx = x0 - nx * half;
    const float by = y0 - ny * half;
    const float cx = x1 + nx * half;
    const float cy = y1 + ny * half;
    const float dx = x1 - nx * half;
    const float dy = y1 - ny * half;
    out.push_back({ax, ay, r, g, b, a});
    out.push_back({bx, by, r, g, b, a});
    out.push_back({cx, cy, r, g, b, a});
    out.push_back({bx, by, r, g, b, a});
    out.push_back({dx, dy, r, g, b, a});
    out.push_back({cx, cy, r, g, b, a});
}

void emitThickSegment(std::vector<ColorVertex>& out, float x0, float y0, float x1, float y1,
                      float half, float r, float g, float b, float a) {
    const float dx = x1 - x0;
    const float dy = y1 - y0;
    const float len = std::sqrt(dx * dx + dy * dy);
    if (len < 1e-6f) {
        appendDisc(out, x0, y0, half, r, g, b, a);
        return;
    }
    appendQuad(out, x0, y0, x1, y1, -dy / len, dx / len, half, r, g, b, a);
}

void catmullRomPoint(const float* p0, const float* p1, const float* p2, const float* p3,
                     float t, float* x, float* y) {
    // Centripetal Catmull-Rom (Barry-Goldman formulation): knot spacing
    // follows sqrt(chord length), so wildly uneven control points (a tiny
    // segment between long ones, as in real map data) cannot form cusps
    // or loops, unlike uniform Catmull-Rom. Still interpolates p1..p2.
    auto dist = [](const float* a, const float* b) {
        const float dx = b[0] - a[0];
        const float dy = b[1] - a[1];
        return std::sqrt(dx * dx + dy * dy);
    };
    const float eps = 1e-4f;
    float d0 = dist(p0, p1);
    float d1 = dist(p1, p2);
    float d2 = dist(p2, p3);
    if (d0 < eps) {
        d0 = eps;
    }
    if (d1 < eps) {
        *x = p1[0];
        *y = p1[1];
        return;
    }
    if (d2 < eps) {
        d2 = eps;
    }
    const float t0 = 0.0f;
    const float t1 = t0 + std::sqrt(d0);
    const float t2 = t1 + std::sqrt(d1);
    const float t3 = t2 + std::sqrt(d2);
    const float tt = t1 + (t2 - t1) * t;
    const float ax = ((t1 - tt) * p0[0] + (tt - t0) * p1[0]) / (t1 - t0);
    const float ay = ((t1 - tt) * p0[1] + (tt - t0) * p1[1]) / (t1 - t0);
    const float bx = ((t2 - tt) * p1[0] + (tt - t1) * p2[0]) / (t2 - t1);
    const float by = ((t2 - tt) * p1[1] + (tt - t1) * p2[1]) / (t2 - t1);
    const float cx = ((t3 - tt) * p2[0] + (tt - t2) * p3[0]) / (t3 - t2);
    const float cy = ((t3 - tt) * p2[1] + (tt - t2) * p3[1]) / (t3 - t2);
    const float dx = ((t2 - tt) * ax + (tt - t0) * bx) / (t2 - t0);
    const float dy = ((t2 - tt) * ay + (tt - t0) * by) / (t2 - t0);
    const float ex = ((t3 - tt) * bx + (tt - t1) * cx) / (t3 - t1);
    const float ey = ((t3 - tt) * by + (tt - t1) * cy) / (t3 - t1);
    *x = ((t2 - tt) * dx + (tt - t1) * ex) / (t2 - t1);
    *y = ((t2 - tt) * dy + (tt - t1) * ey) / (t2 - t1);
}

void appendSegment(std::vector<ColorVertex>& out, float x0, float y0, float x1, float y1,
                   float r, float g, float b, float a, float width) {
    const float half = width * 0.5f;
    emitThickSegment(out, x0, y0, x1, y1, half, r, g, b, a);
    const float dx = x1 - x0;
    const float dy = y1 - y0;
    if (std::sqrt(dx * dx + dy * dy) >= 1e-6f) {
        appendDisc(out, x0, y0, half, r, g, b, a);
        appendDisc(out, x1, y1, half, r, g, b, a);
    }
}

void appendPolyline(std::vector<ColorVertex>& out, const float* pts, int pointCount,
                    float r, float g, float b, float a, float width) {
    if (pts == nullptr || pointCount <= 0) {
        return;
    }
    const float half = width * 0.5f;
    if (pointCount == 1) {
        appendDisc(out, pts[0], pts[1], half, r, g, b, a);
        return;
    }
    for (int i = 0; i < pointCount - 1; ++i) {
        appendSegment(out, pts[2 * i], pts[2 * i + 1], pts[2 * i + 2], pts[2 * i + 3],
                      r, g, b, a, width);
    }
}

void appendSmoothPolyline(std::vector<ColorVertex>& out, const float* pts, int pointCount,
                          float r, float g, float b, float a, float width, int subdiv) {
    if (pts == nullptr || pointCount <= 0) {
        return;
    }
    const float half = width * 0.5f;
    if (pointCount == 1) {
        appendDisc(out, pts[0], pts[1], half, r, g, b, a);
        return;
    }
    const int steps = subdiv > 0 ? subdiv : 8;
    float px = 0.0f;
    float py = 0.0f;
    bool havePrev = false;
    for (int i = 0; i < pointCount - 1; ++i) {
        const float* p0 = pts + 2 * (i > 0 ? i - 1 : 0);
        const float* p1 = pts + 2 * i;
        const float* p2 = pts + 2 * (i + 1);
        const float* p3 = pts + 2 * (i + 2 < pointCount ? i + 2 : pointCount - 1);
        for (int s = (i == 0 ? 0 : 1); s <= steps; ++s) {
            float x = 0.0f;
            float y = 0.0f;
            catmullRomPoint(p0, p1, p2, p3, static_cast<float>(s) / steps, &x, &y);
            if (havePrev) {
                emitThickSegment(out, px, py, x, y, half, r, g, b, a);
            }
            px = x;
            py = y;
            havePrev = true;
        }
    }
    // Round caps only at the two ends: the spline itself is C1-continuous,
    // so interior joints need no discs (they would only lump the silhouette).
    appendDisc(out, pts[0], pts[1], half, r, g, b, a);
    appendDisc(out, pts[2 * (pointCount - 1)], pts[2 * (pointCount - 1) + 1], half, r, g, b, a);
}

void appendRect(std::vector<ColorVertex>& out, float x0, float y0, float x1, float y1,
                float r, float g, float b, float a) {
    out.push_back({x0, y0, r, g, b, a});
    out.push_back({x0, y1, r, g, b, a});
    out.push_back({x1, y0, r, g, b, a});
    out.push_back({x0, y1, r, g, b, a});
    out.push_back({x1, y1, r, g, b, a});
    out.push_back({x1, y0, r, g, b, a});
}

ShelfPacker::ShelfPacker(int width, int height)
    : width_(width), height_(height) {}

bool ShelfPacker::pack(int w, int h, AtlasRect* out) {
    if (w <= 0 || h <= 0 || w > width_) {
        return false;
    }
    if (cursorX_ + w > width_) {
        cursorX_ = 0;
        cursorY_ += rowH_;
        rowH_ = 0;
    }
    if (cursorY_ + h > height_) {
        return false;
    }
    if (out != nullptr) {
        out->x = cursorX_;
        out->y = cursorY_;
        out->w = w;
        out->h = h;
    }
    cursorX_ += w;
    if (h > rowH_) {
        rowH_ = h;
    }
    usedArea_ += w * h;
    return true;
}

}  // namespace metro
