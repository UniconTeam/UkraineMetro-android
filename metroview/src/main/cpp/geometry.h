#pragma once

#include <cstdint>
#include <vector>

namespace metro {

// Colored vertex in map space. Transformed to NDC by the vertex shader
// via push constants (scroll / scale / pivot / view size).
struct ColorVertex {
    float x;
    float y;
    float r;
    float g;
    float b;
    float a;
};

// Textured vertex in map space with atlas UVs.
struct TextVertex {
    float x;
    float y;
    float u;
    float v;
};

// Packed rectangle inside the glyph atlas (pixel space).
struct AtlasRect {
    int x = 0;
    int y = 0;
    int w = 0;
    int h = 0;
};

// Expands a polyline into triangles: a quad per segment plus a filled disc
// (triangle fan) at every vertex for round joints and round caps.
void appendPolyline(std::vector<ColorVertex>& out, const float* pts, int pointCount,
                    float r, float g, float b, float a, float width);

// Expands a single segment with round caps (discs at both ends).
void appendSegment(std::vector<ColorVertex>& out, float x0, float y0, float x1, float y1,
                   float r, float g, float b, float a, float width);

// Appends an axis-aligned filled rect (two triangles).
void appendRect(std::vector<ColorVertex>& out, float x0, float y0, float x1, float y1,
                float r, float g, float b, float a);

// Simple shelf packer for the glyph atlas. Pure logic, no dependencies.
class ShelfPacker {
public:
    ShelfPacker(int width, int height);
    bool pack(int w, int h, AtlasRect* out);
    int width() const { return width_; }
    int height() const { return height_; }
    int usedArea() const { return usedArea_; }

private:
    int width_;
    int height_;
    int cursorX_ = 0;
    int cursorY_ = 0;
    int rowH_ = 0;
    int usedArea_ = 0;
};

// Map space -> screen space (px), zooming around a pivot (usually view center).
inline void mapToScreen(float mx, float my, float scrollX, float scrollY, float scale,
                        float pivotX, float pivotY, float* sx, float* sy) {
    *sx = (mx + scrollX - pivotX) * scale + pivotX;
    *sy = (my + scrollY - pivotY) * scale + pivotY;
}

// Screen space (px) -> Vulkan NDC. In Vulkan (-1,-1) is the TOP-LEFT,
// so unlike OpenGL no Y flip is applied here.
inline void screenToNdc(float sx, float sy, float viewW, float viewH, float* nx, float* ny) {
    *nx = sx / (viewW * 0.5f) - 1.0f;
    *ny = sy / (viewH * 0.5f) - 1.0f;
}

}  // namespace metro
