#version 450
// Glyph atlas sampling. Atlas stores white glyphs; tint with uniform text color.
layout(location = 0) in vec2 fragUV;
layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 0) uniform sampler2D atlas;

layout(push_constant) uniform Push {
    vec2 scroll;
    float scale;
    vec2 viewSize;
    vec2 pivot;
    vec4 textColor;
} pc;

void main() {
    vec4 texel = texture(atlas, fragUV);
    outColor = vec4(pc.textColor.rgb, pc.textColor.a * texel.a);
}
