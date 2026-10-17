#version 150
// Ascension Lighting, reflections (half resolution): screen-space ray-traced reflections. Each pixel's reflected ray is
// marched through the depth buffer; where it meets something on screen, that's what the surface reflects, otherwise the
// sky (when the ray goes up, and only when you're under open sky). How much a surface reflects: water (blue, flat) is a
// near mirror; anything flat and facing up is wet and reflective in the rain; flat ground has a faint sky sheen. All of it
// stronger at grazing angles (Fresnel). Output: rgb = the reflected colour, a = how much of it to show.
uniform sampler2D DiffuseSampler;   // the frame's colour
uniform sampler2D DepthSampler;     // the frame's depth
uniform mat4 InvProjMat;
uniform mat4 ProjMat;
uniform vec2 OutSize;
uniform vec3 ViewUp;                // world up, in view space
uniform vec3 SkyColor;
uniform float SkyOpen;              // 0 underground .. 1 under open sky
uniform float Rain;                 // 0..1
uniform int Steps;
uniform float Strength;
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

float ign(vec2 px) {
    return fract(52.9829189 * fract(dot(px, vec2(0.06711056, 0.00583715))));
}

void main() {
    float d = texture(DepthSampler, texCoord).r;
    if (d >= 0.99999) { fragColor = vec4(0.0); return; }
    vec3 p = viewPos(texCoord);
    vec2 texel = 1.0 / OutSize;
    // The normal from whichever neighbours are on the same surface (smaller depth step), so edges don't smear
    vec3 a1 = viewPos(texCoord + vec2(texel.x, 0.0)) - p, a2 = p - viewPos(texCoord - vec2(texel.x, 0.0));
    vec3 b1 = viewPos(texCoord + vec2(0.0, texel.y)) - p, b2 = p - viewPos(texCoord - vec2(0.0, texel.y));
    vec3 dx = abs(a1.z) < abs(a2.z) ? a1 : a2;
    vec3 dy = abs(b1.z) < abs(b2.z) ? b1 : b2;
    vec3 n = normalize(cross(dx, dy));
    if (dot(n, p) > 0.0) n = -n;
    vec3 v = normalize(p);
    vec3 albedo = texture(DiffuseSampler, texCoord).rgb;
    float up = dot(n, ViewUp);
    float flatUp = smoothstep(0.82, 0.97, up);
    float water = smoothstep(0.03, 0.16, albedo.b - max(albedo.r, albedo.g) * 0.9) * flatUp;
    float wet = Rain * SkyOpen * flatUp * 0.6;
    // Dry ground doesn't mirror the scene (that read as a blurry film over every floor); water and wet ground do
    float gloss = max(water * 0.9, wet);
    float fres = 0.04 + 0.96 * pow(1.0 - max(dot(-v, n), 0.0), 5.0);
    float w = gloss * mix(0.3, 1.0, fres) * Strength;
    if (w < 0.01) { fragColor = vec4(0.0); return; }
    vec3 r = normalize(reflect(v, n));
    float dist = length(p);
    float stepLen = max(0.12, dist * 0.03);
    vec3 q = p + n * (0.03 + dist * 0.002) + r * stepLen * ign(gl_FragCoord.xy);
    bool found = false;
    vec2 huv = vec2(0.0);
    for (int i = 0; i < 64; i++) {
        if (i >= Steps) break;
        vec3 prev = q;
        q += r * stepLen;
        stepLen *= 1.1;
        if (q.z > -0.05) break;                              // came back behind the camera
        vec2 uv = toScreen(q);
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) break;
        if (texture(DepthSampler, uv).r >= 0.99999) continue;
        float diff = viewPos(uv).z - q.z;                   // > 0: the ray went behind what's drawn there
        if (diff > 0.0 && diff < stepLen * 2.5 + 0.25) {
            vec3 lo = prev, hi = q;
            for (int k = 0; k < 5; k++) {
                vec3 m = (lo + hi) * 0.5;
                if (viewPos(toScreen(m)).z - m.z > 0.0) hi = m; else lo = m;
            }
            huv = toScreen(hi);
            found = true;
            break;
        }
    }
    float skyAmt = smoothstep(-0.05, 0.35, dot(r, ViewUp)) * SkyOpen;
    vec3 sky = SkyColor * mix(1.0, 0.55, Rain);
    vec3 refl = sky;
    float have = skyAmt;
    if (found) {
        vec2 e = min(huv, 1.0 - huv);
        float conf = smoothstep(0.0, 0.07, min(e.x, e.y));
        refl = mix(sky, texture(DiffuseSampler, huv).rgb, conf);
        have = max(conf, skyAmt);
    }
    // Water tints what it reflects a little
    refl *= mix(vec3(1.0), albedo * 1.6 + 0.35, 0.3 * water);
    fragColor = vec4(refl, clamp(w * have, 0.0, 0.92));
}
