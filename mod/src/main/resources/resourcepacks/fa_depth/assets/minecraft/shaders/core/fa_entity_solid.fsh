#version 150
// Factory Ascension: the entity fragment shader (as the game's, no cut-out) with parallax depth on the mob's texture.
#moj_import <fog.glsl>
#moj_import <fa_parallax.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 faPos;
flat in float faPerspective;

out vec4 fragColor;

void main() {
    vec3 dp1 = dFdx(faPos), dp2 = dFdy(faPos);
    vec2 duv1 = dFdx(texCoord0), duv2 = dFdy(texCoord0);
    vec2 uv = texCoord0;
    float shade = 1.0;
    vec4 color = texture(Sampler0, texCoord0);
    if (faPerspective > 0.5 && color.a > 0.99) {
        vec3 r = faParallax(Sampler0, texCoord0, faPos, dp1, dp2, duv1, duv2, 0.06, false);
        vec4 shifted = textureGrad(Sampler0, r.xy, duv1, duv2);
        if (shifted.a > 0.99) { color = shifted; shade = r.z; }   // never step onto a see-through texel
    }
    color.rgb *= shade;
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
