#version 150
// Factory Ascension's realism camera: the picture as a real (cheap, frightened) camera sees it.
// Mode 1 copies the frame; mode 0 draws it back through the lens:
//  - barrel distortion (a slight fisheye that bulges the middle), fit so the corners stay filled
//  - colour fringing toward the edges, as a real lens does
//  - less colour and a cold, slightly green grade, a gentle S-curve for contrast
//  - film grain that moves every frame, stronger in the shadows
//  - a vignette that tightens and pulses with your heartbeat when you're stressed, red at the edges when you're hurt
//  - after a blast close by: everything goes soft and pale for a few seconds (your ears are ringing)
uniform sampler2D SceneSampler;
uniform vec2 OutSize;
uniform float Time, Fisheye, Grain, Desat, Vignette, Stress, Ring, Hurt, Dread;
uniform int Mode;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 sampleBlur(vec2 uv, float r) {
    vec2 px = r / OutSize;
    vec3 c = texture(SceneSampler, uv).rgb * 0.4;
    c += texture(SceneSampler, uv + vec2(px.x, 0)).rgb * 0.15;
    c += texture(SceneSampler, uv - vec2(px.x, 0)).rgb * 0.15;
    c += texture(SceneSampler, uv + vec2(0, px.y)).rgb * 0.15;
    c += texture(SceneSampler, uv - vec2(0, px.y)).rgb * 0.15;
    return c;
}

void main() {
    if (Mode == 1) {
        fragColor = vec4(texture(SceneSampler, texCoord).rgb, 1.0);
        return;
    }
    float aspect = OutSize.x / OutSize.y;
    vec2 c = texCoord - 0.5;
    vec2 ca = vec2(c.x * aspect, c.y);
    float r2 = dot(ca, ca);
    float corner = 0.25 * aspect * aspect + 0.25;
    float k = 0.16 * Fisheye;
    // Barrel: points further out move further out; divided so the corners land on the corners
    vec2 d = c * (1.0 + k * r2) / (1.0 + k * corner);
    float ab = 0.0035 * Fisheye * r2 / corner + Ring * 0.004;
    vec2 uvR = 0.5 + d * (1.0 + ab), uvG = 0.5 + d, uvB = 0.5 + d * (1.0 - ab);
    vec3 col;
    float blur = Ring * 2.5;
    if (blur > 0.05) {
        col = vec3(sampleBlur(uvR, blur).r, sampleBlur(uvG, blur).g, sampleBlur(uvB, blur).b);
    } else {
        col = vec3(texture(SceneSampler, uvR).r, texture(SceneSampler, uvG).g, texture(SceneSampler, uvB).b);
    }
    // Grade: less colour (more when hurt or deafened), cold shadows, a soft S-curve
    float l = dot(col, vec3(0.299, 0.587, 0.114));
    float ds = clamp(Desat + Hurt * 0.35 + Ring * 0.4 + Dread * 0.1, 0.0, 1.0);
    col = mix(col, vec3(l), ds);
    col *= mix(vec3(1.0), vec3(0.94, 1.0, 1.03), 0.6 + 0.4 * Dread);
    col = mix(col, col * col * (3.0 - 2.0 * col), 0.22);
    // Vignette: tighter and pulsing with the heart when stressed
    float beat = pow(0.5 + 0.5 * sin(Time * (5.0 + Stress * 5.0)), 6.0);
    float tight = 0.9 - 0.18 * Stress - 0.06 * beat * Stress - 0.05 * Dread;
    // (smoothstep needs its edges in order: 1 in the middle, falling to 0 toward the corners)
    float v = 1.0 - smoothstep(tight - 0.6, tight, length(ca) * (0.9 + 0.1 * Vignette));
    col *= mix(1.0, v, clamp(0.55 * Vignette + 0.25 * Stress, 0.0, 1.0));
    col = mix(col, vec3(0.45, 0.02, 0.0), (1.0 - v) * Hurt * (0.6 + 0.4 * beat));
    // Grain: a fresh pattern every frame, stronger in the dark
    float g = hash(floor(texCoord * OutSize) + fract(Time * 13.17) * 917.0) - 0.5;
    col += g * Grain * (0.05 + 0.04 * Dread) * (1.25 - l);
    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
