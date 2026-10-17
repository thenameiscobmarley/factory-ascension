#version 330 core
// Factory Ascension depth, Embeddium's block shader: as Embeddium's own, with lit relief (fa_parallax.glsl) on opaque texels.

#import <sodium:include/fog.glsl>
#import <sodium:include/fa_parallax.glsl>
#import <sodium:include/fa_waves.glsl>

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec3 v_FaPos; // Relative to the camera
in vec2 v_FaWorld;
in float v_FaWaterTop;
uniform vec4 u_FaWater;
uniform vec3 u_FaSun;
in float v_FragDistance; // The fragment's distance from the camera

in float v_MaterialMipBias;
in float v_MaterialAlphaCutoff;

uniform sampler2D u_BlockTex; // The block texture

uniform vec4 u_FogColor; // The color of the shader fog
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog

out vec4 fragColor; // The output fragment for the color framebuffer

void main() {
    // Derivatives first, while every pixel runs the same code
    vec3 dp1 = dFdx(v_FaPos), dp2 = dFdy(v_FaPos);
    vec2 duv1 = dFdx(v_TexCoord), duv2 = dFdy(v_TexCoord);
    vec4 diffuseColor = texture(u_BlockTex, v_TexCoord, v_MaterialMipBias);
    float shade = 1.0, spec = 0.0;
    if (diffuseColor.a > 0.74) {
        vec2 uv;
        faBlock(u_BlockTex, v_TexCoord, v_FaPos, dp1, dp2, duv1, duv2, 0.14, uv, shade, spec);
        // The original derivatives (and Embeddium's mip bias: no mips on cut-out blocks) so the mip level doesn't jump
        float bias = exp2(v_MaterialMipBias);
        vec4 shifted = textureGrad(u_BlockTex, uv, duv1 * bias, duv2 * bias);
        if (shifted.a > 0.74) diffuseColor = shifted; else { shade = 1.0; spec = 0.0; }
    }

#ifdef USE_FRAGMENT_DISCARD
    if (diffuseColor.a < v_MaterialAlphaCutoff) {
        discard;
    }
#endif

#ifdef USE_VANILLA_COLOR_FORMAT
    diffuseColor *= v_Color;
    diffuseColor.rgb = diffuseColor.rgb * shade + spec * v_Color.rgb;
#else
    diffuseColor.rgb *= v_Color.rgb;
    diffuseColor.rgb *= v_Color.a;
    diffuseColor.rgb = diffuseColor.rgb * shade + spec * v_Color.rgb * v_Color.a;
#endif

    // Water's top: sky reflection (Fresnel), sun glint and crest foam, as the vanilla pack's fa_water.fsh
    if (v_FaWaterTop > 0.5) {
        vec3 w = fa_wave(v_FaWorld, u_FaWater.w);
        vec2 slope = (w.yz + fa_ripple_slope(v_FaWorld, u_FaWater.w)) * 4.0;
        vec3 n = normalize(vec3(-slope.x, 1.0, -slope.y));
        vec3 v = normalize(-v_FaPos);
        float fresnel = 0.02 + 0.98 * pow(1.0 - max(dot(n, v), 0.0), 5.0);
        float sky = clamp(max(v_Color.r, max(v_Color.g, v_Color.b)), 0.0, 1.0);
        vec3 skyCol = mix(u_FogColor.rgb, vec3(0.62, 0.78, 1.0), 0.35) * sky;
        vec3 col = mix(diffuseColor.rgb * mix(0.75, 1.0, fresnel), skyCol, clamp(fresnel * 0.85, 0.0, 1.0));
        float glint = pow(max(dot(reflect(-v, n), normalize(u_FaSun)), 0.0), 220.0) * clamp(u_FaSun.y * 4.0, 0.0, 1.0) * sky;
        col += vec3(1.0, 0.95, 0.85) * glint * 2.5;
        float crest = smoothstep(0.040, 0.062, w.x);
        float foam = crest * smoothstep(0.35, 0.75, texture(u_BlockTex, v_TexCoord).g + crest * 0.3);
        col = mix(col, vec3(0.92, 0.95, 0.97) * max(sky, 0.15), foam * 0.8);
        diffuseColor = vec4(col, clamp(mix(diffuseColor.a, 1.0, fresnel * 0.6) + foam * 0.5 + glint, 0.0, 1.0));
    }

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}
