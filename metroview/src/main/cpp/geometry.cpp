#include "geometry.h"

#include <cmath>

namespace metro {

namespace {

constexpr float kPi = 3.14159265358979323846f;
constexpr int kDiscSlices = 10;

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

}  // namespace

void appendSegment(std::vector<ColorVertex>& out, float x0, float y0, float x1, float y1,
                   float r, float g, float b, float a, float width) {
    const float half = width * 0.5f;
    const float dx = x1 - x0;
    const float dy = y1 - y0;
    const float len = std::sqrt(dx * dx + dy * dy);
    if (len < 1e-6f) {
        appendDisc(out, x0, y0, half, r, g, b, a);
        return;
    }
    const float nx = -dy / len;
    const float ny = dx / len;
    appendQuad(out, x0, y0, x1, y1, nx, ny, half, r, g, b, a);
    appendDisc(out, x0, y0, half, r, g, b, a);
    appendDisc(out, x1, y1, half, r, g, b, a);
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
