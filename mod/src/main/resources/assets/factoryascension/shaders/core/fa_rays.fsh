#version 150
// Ascension Lighting, pass 3: light shafts. Bright sky is smeared toward the sun's (or moon's) spot on screen.
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 SunPos;                // the sun on screen, 0..1 (may be off screen)
uniform float RayStrength;          // 0 = none (sun behind you or night)
uniform int Steps;
in vec2 texCoord;
out vec4 fragColor;

vec3 sky(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return vec3(0.0);
    if (texture(DepthSampler, uv).r < 0.99999) return vec3(0.0);   // only open sky lets light through
    vec3 c = texture(DiffuseSampler, uv).rgb;
    return c * smoothstep(0.55, 1.0, max(c.r, max(c.g, c.b)));
}

void main() {
    if (RayStrength <= 0.0) { fragColor = vec4(0.0); return; }
    vec2 dir = (SunPos - texCoord) / float(Steps);
    vec2 uv = texCoord;
    vec3 sum = vec3(0.0);
    float decay = 1.0;
    for (int i = 0; i < 64; i++) {
        if (i >= Steps) break;
        uv += dir;
        sum += sky(uv) * decay;
        decay *= 0.965;
    }
    fragColor = vec4(sum / float(Steps) * RayStrength, 1.0);
}
