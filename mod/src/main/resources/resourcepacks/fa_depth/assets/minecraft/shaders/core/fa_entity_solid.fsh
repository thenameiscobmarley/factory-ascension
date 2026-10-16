#version 150
// Factory Ascension: the entity fragment shader (as the game's, no cut-out) with lit relief (fa_parallax.glsl) on the mob.
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
    vec4 color = texture(Sampler0, texCoord0);
    float shade = 1.0, spec = 0.0;
    if (faPerspective > 0.5 && color.a > 0.99) faEntity(Sampler0, texCoord0, faPos, dp1, dp2, duv1, duv2, shade, spec);
    color.rgb = color.rgb * shade + spec;
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
