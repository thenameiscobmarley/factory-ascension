#version 150
// Ascension Lighting, pass 4: back onto the frame. Mode 4 copies the frame; mode 0 writes the final picture
// (frame x shadow + bounced light + rays); 2 and 3 are developer views.
uniform sampler2D DiffuseSampler;   // blurred pass 1
uniform sampler2D RaySampler;       // pass 3
uniform sampler2D SceneSampler;     // a copy of the frame (mode 4 makes it)
uniform sampler2D ReflSampler;      // the reflections (fa_ssr): rgb, a = how much
uniform sampler2D BloomSampler;     // fa_glow: what glows, blurred wide
uniform sampler2D BlurSampler;      // fa_glow: the scene, blurred (depth of field)
uniform sampler2D DepthSampler;
uniform mat4 InvProjMat;
uniform int Mode;
uniform float RaysOn;
uniform float Grade;                // 1 = the filmic grade (tone curve, bloom)
uniform float BloomAmount;
uniform float Haze;                 // aerial perspective density (0 off)
uniform vec3 HazeColor, SunDir, SunColor;
uniform float SkyOpen;
uniform float Aim;                  // 0..1 while aiming down sights: depth of field
uniform vec3 SunScreen;             // the sun on screen (x, y) and how visible it is (0 = behind you or hidden)
uniform float Aspect;

vec3 viewPos(vec2 uv) {
    float d = texture(DepthSampler, uv).r;
    vec4 p = InvProjMat * vec4(uv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// The ACES filmic curve (Narkowicz fit), on linear light
vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 l = texture(DiffuseSampler, texCoord);
    // The frame's own alpha is kept everywhere: the game reads it later, so writing 1 would change the picture
    vec4 sceneTex = texture(SceneSampler, texCoord);
    if (Mode == 4) { fragColor = sceneTex; return; }   // copy the frame
    if (Mode == 0) {
        // The frame, shadowed, plus the bounced light and the rays, in one opaque pass (no blending to go wrong)
        vec3 lit = sceneTex.rgb * l.a + l.rgb;
        vec4 refl = texture(ReflSampler, texCoord);
        lit = mix(lit, refl.rgb * mix(1.0, l.a, 0.5), refl.a);
        lit += texture(RaySampler, texCoord).rgb * RaysOn;
        float depth = texture(DepthSampler, texCoord).r;
        bool sky = depth >= 0.99999;
        vec3 p = viewPos(texCoord);
        float dist = length(p);
        // Depth of field while aiming: what's well behind (or in front of) what you're looking at goes soft
        if (Aim > 0.01 && !sky) {
            float focus = length(viewPos(vec2(0.5)));
            float coc = smoothstep(0.15, 0.6, abs(dist - focus) / max(focus, 1.0));
            lit = mix(lit, texture(BlurSampler, texCoord).rgb, coc * Aim * 0.9);
        } else if (Aim > 0.01) {
            lit = mix(lit, texture(BlurSampler, texCoord).rgb, Aim * 0.6);
        }
        // Aerial perspective: distance fades into a haze lit by the sun (only out under the sky)
        if (Haze > 0.0 && !sky) {
            vec3 v = p / max(dist, 1e-3);
            float f = (1.0 - exp(-max(0.0, dist - 20.0) * Haze)) * SkyOpen;
            vec3 hc = mix(HazeColor, SunColor, pow(max(dot(v, SunDir), 0.0), 6.0) * 0.55);
            lit = mix(lit, hc, clamp(f, 0.0, 0.85));
        }
        if (Grade > 0.5) {
            // Bloom, then a film-like tone curve: highlights roll off instead of clipping, shadows keep detail
            lit += texture(BloomSampler, texCoord).rgb * BloomAmount;
            // The sun: a soft glare round it and faint lens ghosts along the line through the middle of the screen
            if (SunScreen.z > 0.01) {
                vec2 d = (texCoord - SunScreen.xy) * vec2(Aspect, 1.0);
                float glare = exp(-length(d) * 5.0) * 0.35 + exp(-length(d) * 1.4) * 0.08;
                vec3 sunc = vec3(1.0, 0.86, 0.62);
                vec3 ghosts = vec3(0.0);
                vec2 axis = vec2(0.5) - SunScreen.xy;
                for (int i = 1; i <= 4; i++) {
                    float k = float(i) * 0.55;
                    vec2 gp = SunScreen.xy + axis * k * 1.1;
                    float r = 0.025 + 0.02 * float(i);
                    float g = smoothstep(r, r * 0.6, length((texCoord - gp) * vec2(Aspect, 1.0)));
                    ghosts += g * (i % 2 == 0 ? vec3(0.35, 0.55, 1.0) : vec3(1.0, 0.6, 0.3)) * 0.05;
                }
                lit += (sunc * glare + ghosts) * SunScreen.z;
            }
            vec3 linc = pow(max(lit, 0.0), vec3(2.2));
            linc = aces(linc * 1.65);
            lit = pow(linc, vec3(1.0 / 2.2));
            float lum = dot(lit, vec3(0.299, 0.587, 0.114));
            lit = mix(vec3(lum), lit, 1.06);
        }
        fragColor = vec4(lit, sceneTex.a);
    }
    else if (Mode == 1) fragColor = vec4(l.rgb + texture(RaySampler, texCoord).rgb * RaysOn, 1.0);
    else if (Mode == 2) fragColor = vec4(vec3(l.a), 1.0);              // debug: the shadow term alone
    else if (Mode == 5) fragColor = sceneTex;   // debug: the copied frame, unchanged
    else if (Mode == 6) fragColor = vec4(texture(ReflSampler, texCoord).rgb * texture(ReflSampler, texCoord).a, 1.0);   // debug: reflections
    else fragColor = vec4(l.rgb * 4.0, 1.0);                           // debug: the bounced light, brightened
}
