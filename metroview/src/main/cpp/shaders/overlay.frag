#version 450
// Debug-overlay fragment shader: raw RGBA sample. The panel (background,
// text, frame-time graph) is composited on the CPU into the overlay texture,
// so no tinting — unlike the glyph-atlas text path.
layout(location = 0) in vec2 fragUV;
layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 0) uniform sampler2D overlay;

void main() {
    outColor = texture(overlay, fragUV);
}
