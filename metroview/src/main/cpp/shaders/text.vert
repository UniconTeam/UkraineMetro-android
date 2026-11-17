#version 450
// Textured-quad vertex shader for FreeType station labels.
layout(location = 0) in vec2 inPos;
layout(location = 1) in vec2 inUV;
layout(location = 0) out vec2 fragUV;

layout(push_constant) uniform Push {
    vec2 scroll;
    float scale;
    vec2 viewSize;
    vec2 pivot;
    vec4 textColor;
} pc;

void main() {
    vec2 screen = (inPos + pc.scroll - pc.pivot) * pc.scale + pc.pivot;
    // NOTE: in Vulkan NDC (-1,-1) is the TOP-LEFT, so Y is not flipped here.
    vec2 ndc = vec2(
        screen.x / (pc.viewSize.x * 0.5) - 1.0,
        screen.y / (pc.viewSize.y * 0.5) - 1.0
    );
    gl_Position = vec4(ndc, 0.0, 1.0);
    fragUV = inUV;
}
