#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // The exact texel (the atlas may be filtered smooth; the depth pack's terrain picks texels too, so they match)
    ivec2 size = textureSize(Sampler0, 0);
    vec4 color = texelFetch(Sampler0, clamp(ivec2(texCoord0 * vec2(size)), ivec2(0), size - 1), 0);
    if (color.a < 0.1) discard;
    // Opaque: the depth pack keeps block heights in the alpha, which isn't see-through
    color = vec4(color.rgb, 1.0) * vertexColor * ColorModulator;
    fragColor = linear_fog(vec4(color.rgb, 1.0), vertexDistance, FogStart, FogEnd, FogColor);
}
