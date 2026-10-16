#version 150
// Ascension Lighting, pass 1 (half resolution): screen-space traced ambient occlusion and one bounce of light.
// Around each pixel, rays are marched through the depth buffer in a rotated spiral; samples that rise above the
// surface block the sky (shadow) and pass on their own colour as bounced light. Output: rgb = bounced light already
// tinted by this pixel's colour, a = how much open sky it sees (1 = none blocked).
uniform sampler2D DiffuseSampler;   // the frame's colour
uniform sampler2D DepthSampler;     // the frame's depth
uniform mat4 InvProjMat;
uniform vec2 OutSize;
uniform float Radius;               // in blocks
uniform int Samples;
uniform float Strength;
uniform mat4 ProjMat;
uniform vec3 SunDir;                // towards the sun (or moon), in view space
uniform float SunLight;             // how strong its shadows are (0 at night, in rain, underground)
uniform int ShadowSteps;
in vec2 texCoord;
out vec4 fragColor;

vec3 viewPos(vec2 uv) {
    float d = texture(DepthSampler, uv).r;
    vec4 p = InvProjMat * vec4(uv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec2 toScreen(vec3 v) {
    vec4 c = ProjMat * vec4(v, 1.0);
    return c.xy / c.w * 0.5 + 0.5;
}

float ign(vec2 px) {   // interleaved gradient noise: cheap, even per-pixel rotation
    return fract(52.9829189 * fract(dot(px, vec2(0.06711056, 0.00583715))));
}

void main() {
    float d = texture(DepthSampler, texCoord).r;
    if (d >= 0.99999) { fragColor = vec4(0.0, 0.0, 0.0, 1.0); return; }   // sky
    vec3 p = viewPos(texCoord);
    // The surface normal from neighbouring depths
    vec2 texel = 1.0 / OutSize;
    vec3 n = normalize(cross(viewPos(texCoord + vec2(texel.x, 0.0)) - p, viewPos(texCoord + vec2(0.0, texel.y)) - p));
    if (dot(n, p) > 0.0) n = -n;
    float dist = max(0.5, -p.z);
    float screenRadius = Radius / dist * 0.9;          // a fixed size in the world, smaller far away
    float rot = ign(gl_FragCoord.xy) * 6.2831853;
    float occl = 0.0;
    vec3 bounce = vec3(0.0);
    float total = 0.0;
    for (int i = 0; i < 32; i++) {
        if (i >= Samples) break;
        float t = (float(i) + 0.5) / float(Samples);
        float a = rot + float(i) * 2.39996323;          // golden-angle spiral
        vec2 uv = texCoord + vec2(cos(a), sin(a)) * screenRadius * t;
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) continue;
        vec3 q = viewPos(uv);
        vec3 v = q - p;
        float len = length(v);
        float facing = max(0.0, dot(n, v / max(len, 1e-4)) - 0.05);
        float range = clamp(1.0 - len / (Radius * 2.0), 0.0, 1.0);
        float w = facing * range;
        occl += w;
        bounce += texture(DiffuseSampler, uv).rgb * w;
        total += 1.0;
    }
    if (total < 1.0) { fragColor = vec4(0.0, 0.0, 0.0, 1.0); return; }
    // Shadow: how much of the hemisphere nearby geometry blocks, curved so corners darken and open ground doesn't
    float blocked = clamp(occl / total * 4.5 * Strength, 0.0, 1.0);
    float ao = clamp(1.0 - blocked * 0.75, 0.25, 1.0);
    // Bounce: the average colour of what blocks the sky, lighting this surface, tinted by its own colour
    vec3 albedo = texture(DiffuseSampler, texCoord).rgb;
    vec3 gi = (occl > 0.0 ? bounce / occl : vec3(0.0)) * albedo * blocked * 1.4 * Strength;
    // Sun shadows: march toward the sun through the depth buffer; anything on screen in the way shadows this pixel
    float sun = 1.0;
    if (SunLight > 0.0 && dot(n, SunDir) > 0.02 && dist < 64.0) {
        vec3 q = p + n * (0.03 + dist * 0.003);
        float stepLen = 0.08 + dist * 0.004;
        float j = ign(gl_FragCoord.xy + 17.0);
        q += SunDir * stepLen * j;
        for (int i = 0; i < 48; i++) {
            if (i >= ShadowSteps) break;
            q += SunDir * stepLen;
            stepLen *= 1.16;
            vec2 uv = toScreen(q);
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0 || q.z > -0.05) break;
            if (texture(DepthSampler, uv).r >= 0.99999) continue;
            float diff = viewPos(uv).z - q.z;
            if (diff > 0.03 && diff < 1.2 + stepLen) { sun = 1.0 - 0.45 * SunLight * (1.0 - smoothstep(40.0, 64.0, dist)); break; }
        }
    }
    fragColor = vec4(gi, ao * sun);
}
