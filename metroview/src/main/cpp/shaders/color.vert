#version 450
// Colored-geometry vertex shader (metro lines, transfer links, label bubbles).
// Map space -> screen space -> Vulkan NDC.
layout(location = 0) in vec2 inPos;
layout(location = 1) in vec4 inColor;
layout(location = 0) out vec4 fragColor;

layout(push_constant) uniform Push {
    vec2 scroll;    // pan offset in map units
    float scale;    // zoom factor
    vec2 viewSize;  // surface size in px
    vec2 pivot;     // zoom/rotation pivot in px (view center)
    float angle;    // map rotation, radians, clockwise-positive on screen
} pc;

void main() {
    vec2 p = (inPos + pc.scroll - pc.pivot) * pc.scale;
    float c = cos(pc.angle);
    float s = sin(pc.angle);
    vec2 screen = vec2(c * p.x - s * p.y, s * p.x + c * p.y) + pc.pivot;
    // NOTE: in Vulkan NDC (-1,-1) is the TOP-LEFT, so Y is not flipped here.
    vec2 ndc = vec2(
        screen.x / (pc.viewSize.x * 0.5) - 1.0,
        screen.y / (pc.viewSize.y * 0.5) - 1.0
    );
    gl_Position = vec4(ndc, 0.0, 1.0);
    fragColor = inColor;
}
