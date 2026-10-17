#version 150
// Factory Ascension water: the top of the water is shaded from the waves' own surface: the sky reflected more the
// flatter you look (Fresnel), a sun glint, and foam on the wave crests. Everything else translucent is as the game's.
#moj_import <fog.glsl>
#moj_import <fa_waves.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec4 FaWater;
uniform vec3 FaSun;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightColor;
in vec2 texCoord0;
in vec3 faPos;
in vec2 faWorld;
in float faWaterTop;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (faWaterTop > 0.5) {
        vec3 w = fa_wave(faWorld, FaWater.w);
        vec2 slope = (w.yz + fa_ripple_slope(faWorld, FaWater.w)) * 4.0;   // the light bends more than the surface moves, as in a render
        vec3 n = normalize(vec3(-slope.x, 1.0, -slope.y));
        vec3 v = normalize(-faPos);
        float facing = max(dot(n, v), 0.0);
        float fresnel = 0.02 + 0.98 * pow(1.0 - facing, 5.0);
        // How much sky reaches here (dark in caves, dim at night): the lightmap's brightness
        float sky = clamp(max(lightColor.r, max(lightColor.g, lightColor.b)), 0.0, 1.0);
        vec3 skyCol = mix(FogColor.rgb, vec3(0.62, 0.78, 1.0), 0.35) * sky;
        // Deep water darker and bluer, seen from above
        vec3 body = color.rgb * mix(0.75, 1.0, fresnel);
        vec3 col = mix(body, skyCol, clamp(fresnel * 0.85, 0.0, 1.0));
        // The sun's glint
        float sunUp = clamp(FaSun.y * 4.0, 0.0, 1.0);
        float glint = pow(max(dot(reflect(-v, n), normalize(FaSun)), 0.0), 220.0) * sunUp * sky;
        col += vec3(1.0, 0.95, 0.85) * glint * 2.5;
        // Foam where the crests are highest, broken up by the water texture's own pattern
        float crest = smoothstep(0.040, 0.062, w.x);
        float grain = texture(Sampler0, texCoord0).g;
        float foam = crest * smoothstep(0.35, 0.75, grain + crest * 0.3);
        col = mix(col, vec3(0.92, 0.95, 0.97) * max(sky, 0.15), foam * 0.8);
        color = vec4(col, clamp(mix(color.a, 1.0, fresnel * 0.6) + foam * 0.5 + glint, 0.0, 1.0));
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
