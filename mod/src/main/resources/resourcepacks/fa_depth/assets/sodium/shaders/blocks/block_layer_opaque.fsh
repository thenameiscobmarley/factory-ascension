#version 330 core
// Factory Ascension depth, Embeddium's block shader: as Embeddium's own, with lit relief (fa_parallax.glsl) on opaque texels.

#import <sodium:include/fog.glsl>
#import <sodium:include/fa_parallax.glsl>

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec3 v_FaPos; // Relative to the camera
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
    if (diffuseColor.a > 0.99) {
        vec2 uv;
        faBlock(u_BlockTex, v_TexCoord, v_FaPos, dp1, dp2, duv1, duv2, 0.14, uv, shade, spec);
        // The original derivatives (and Embeddium's mip bias: no mips on cut-out blocks) so the mip level doesn't jump
        float bias = exp2(v_MaterialMipBias);
        vec4 shifted = textureGrad(u_BlockTex, uv, duv1 * bias, duv2 * bias);
        if (shifted.a > 0.99) diffuseColor = shifted; else { shade = 1.0; spec = 0.0; }
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

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}
