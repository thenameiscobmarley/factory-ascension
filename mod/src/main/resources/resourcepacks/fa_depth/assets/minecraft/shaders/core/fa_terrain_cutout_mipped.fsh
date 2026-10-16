#version 150
// Factory Ascension: the cutout_mipped terrain fragment shader with lit relief (fa_parallax.glsl) on opaque block texels.
#moj_import <fog.glsl>
#moj_import <fa_parallax.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 faPos;

out vec4 fragColor;

void main() {
    // Derivatives first, while every pixel runs the same code
    vec3 dp1 = dFdx(faPos), dp2 = dFdy(faPos);
    vec2 duv1 = dFdx(texCoord0), duv2 = dFdy(texCoord0);
    vec4 tex = texture(Sampler0, texCoord0);
    float shade = 1.0, spec = 0.0;
    if (tex.a > 0.99) {
        vec2 uv;
        faBlock(Sampler0, texCoord0, faPos, dp1, dp2, duv1, duv2, 0.14, uv, shade, spec);
        // Sampled with the original derivatives, so the mip level doesn't jump
        vec4 shifted = textureGrad(Sampler0, uv, duv1, duv2);
        if (shifted.a > 0.99) tex = shifted; else { shade = 1.0; spec = 0.0; }
    }
    vec4 color = tex * vertexColor * ColorModulator;
    color.rgb = color.rgb * shade + spec * vertexColor.rgb;
    if (color.a < 0.5) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
