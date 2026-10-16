#version 150
// Factory Ascension: the cutout terrain fragment shader with parallax depth on opaque block texels.
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
    float shade = 1.0;
    if (tex.a > 0.99) {
        vec3 r = faParallax(Sampler0, texCoord0, faPos, dp1, dp2, duv1, duv2, 0.16, true);
        // Sampled with the original derivatives, so the mip level doesn't jump at the wrapped edges
        vec4 shifted = textureGrad(Sampler0, r.xy, duv1, duv2);
        if (shifted.a > 0.99) { tex = shifted; shade = r.z; }   // never step onto a see-through texel
    }
    vec4 color = tex * vertexColor * ColorModulator;
    color.rgb *= shade;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
