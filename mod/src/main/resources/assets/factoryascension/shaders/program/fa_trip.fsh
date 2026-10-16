#version 150
// What the drugs do to your eyes (drugs/Drugs, client/TripClient). Each strength is 0..1, faded in and out.
//  Stoned: a soft warm haze, slow drift, things a little dreamy and saturated, a gentle trail.
//  Wired (cocaine): wide pupils - brighter, sharper, harder contrast, colder.
//  Tweak (meth): the picture won't sit still - fine jitter, chromatic shimmer, tunnel vision, crushed blacks.
//  Trip (mushrooms): the world breathes - slow warping waves, colours rolling through the hues, long trails.
//  Psych (psychosis): colour drains, the edges darken and crawl in, a faint pulse, now and then a flicker.
//  Crash: grey, dim and slow, a slight blur.  Numb (morphine): soft focus, muted, warm.
//  Rolling (ecstasy): warm, glowing, colour pulsing with your heartbeat.  Sedated (xanax): heavy eyelids that droop shut
//  now and then.  Dissoc (ketamine): the world stretches away down a tunnel, smeared trails.  Drunk: a slow sway and
//  double vision that drifts apart and back.
uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;
uniform vec2 OutSize;
uniform float Clock, Stoned, Wired, Tweak, Trip, Psych, Crash, Numb, Rolling, Sedated, Dissoc, Drunk;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float lum(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec3 hue(vec3 c, float a) {
    const vec3 k = vec3(0.57735);
    float ca = cos(a);
    return c * ca + cross(k, c) * sin(a) + k * dot(k, c) * (1.0 - ca);
}

vec3 blur(vec2 uv, float r) {
    vec2 px = r / OutSize;
    vec3 c = texture(DiffuseSampler, uv).rgb * 0.36;
    c += texture(DiffuseSampler, uv + vec2(px.x, 0.0)).rgb * 0.16;
    c += texture(DiffuseSampler, uv - vec2(px.x, 0.0)).rgb * 0.16;
    c += texture(DiffuseSampler, uv + vec2(0.0, px.y)).rgb * 0.16;
    c += texture(DiffuseSampler, uv - vec2(0.0, px.y)).rgb * 0.16;
    return c;
}

void main() {
    vec2 uv = texCoord;
    vec2 c = uv - 0.5;
    float t = Clock;
    // ---- Where each pixel comes from ----
    // Trip: slow breathing waves
    uv += Trip * 0.012 * vec2(sin(uv.y * 9.0 + t * 1.3) + sin(uv.y * 23.0 - t * 0.7) * 0.4,
                              cos(uv.x * 8.0 + t * 1.1) + cos(uv.x * 19.0 + t * 0.9) * 0.4);
    uv = 0.5 + (uv - 0.5) * (1.0 - Trip * 0.02 * sin(t * 0.8));
    // Stoned: a slow drift
    uv += Stoned * 0.003 * vec2(sin(t * 0.37), cos(t * 0.29));
    // Drunk: the room sways
    uv += Drunk * vec2(sin(t * 0.9) * 0.012, sin(t * 0.7 + 1.0) * 0.006);
    // Ketamine: everything pulled away from you, down a long tunnel
    uv = 0.5 + (uv - 0.5) * (1.0 + Dissoc * (0.18 + 0.06 * sin(t * 0.4)) * smoothstep(0.0, 0.7, length(c)));
    // Tweak: jitter, a few times a second
    vec2 j = vec2(hash(vec2(floor(t * 14.0), 3.1)), hash(vec2(floor(t * 14.0), 7.7))) - 0.5;
    uv += Tweak * j * 0.004;
    uv = clamp(uv, 0.001, 0.999);
    // ---- The picture ----
    float soft = Stoned * 1.2 + Crash * 1.0 + Numb * 1.3;
    vec3 col = soft > 0.01 ? mix(texture(DiffuseSampler, uv).rgb, blur(uv, 1.5 + soft), clamp(soft * 0.6, 0.0, 0.8)) : texture(DiffuseSampler, uv).rgb;
    // Drunk: double vision drifting apart and back together
    if (Drunk > 0.0) {
        vec2 o = vec2(0.012 + 0.008 * sin(t * 0.6), 0.003 * sin(t * 0.43)) * Drunk;
        col = mix(col, texture(DiffuseSampler, clamp(uv + o, 0.001, 0.999)).rgb, 0.4 * Drunk);
    }
    // Chromatic shimmer (meth, mushrooms)
    float ca = Tweak * 0.003 * (0.6 + 0.4 * sin(t * 31.0)) + Trip * 0.004;
    if (ca > 0.0) {
        col.r = texture(DiffuseSampler, clamp(uv + c * ca * 2.0, 0.001, 0.999)).r;
        col.b = texture(DiffuseSampler, clamp(uv - c * ca * 2.0, 0.001, 0.999)).b;
    }
    // Wired: sharpen and contrast
    if (Wired > 0.0) {
        vec3 b = blur(uv, 1.0);
        col += (col - b) * 1.6 * Wired;
        col = mix(col, (col - 0.5) * 1.25 + 0.55, Wired * 0.6);
        col *= vec3(0.96, 1.0, 1.06 * (1.0 + 0.05 * Wired));
    }
    // Stoned: warm, saturated, a little bright
    float l = lum(col);
    col = mix(col, mix(vec3(l), col, 1.35) * vec3(1.06, 1.02, 0.9) + 0.03, Stoned * 0.7);
    // Trip: hues rolling, more colour
    if (Trip > 0.0) {
        float a = Trip * (1.2 * sin(t * 0.25) + 0.6 * sin(length(c) * 8.0 - t * 1.6));
        col = mix(col, hue(col, a), Trip);
        l = lum(col);
        col = mix(vec3(l), col, 1.0 + Trip * 0.7);
    }
    // Ecstasy: warm and glowing, pulsing with the heartbeat
    if (Rolling > 0.0) {
        float beat = pow(0.5 + 0.5 * sin(t * 7.5), 6.0);
        l = lum(col);
        col = mix(col, mix(vec3(l), col, 1.5) * vec3(1.08, 1.0, 1.04) + 0.05 + beat * 0.05, Rolling * 0.8);
    }
    // Tweak: crushed, hard, tunnel
    col = mix(col, max(vec3(0.0), (col - 0.06) * 1.15), Tweak * 0.7);
    // Numb: muted and warm
    l = lum(col);
    col = mix(col, mix(vec3(l), col, 0.6) * vec3(1.05, 1.0, 0.92), Numb * 0.6);
    // Crash: grey and dim
    l = lum(col);
    col = mix(col, vec3(l) * 0.72, Crash * 0.75);
    // Psychosis: drained, the dark crawling in, a pulse, a flicker
    l = lum(col);
    col = mix(col, vec3(l) * vec3(0.92, 1.0, 0.95), Psych * 0.65);
    float pulse = 0.5 + 0.5 * sin(t * 1.9);
    float flick = step(0.985, hash(vec2(floor(t * 9.0), 1.3))) * Psych;
    col *= 1.0 - flick * 0.45;
    // ---- Edges: tunnel (meth), crawl (psychosis) ----
    float r = length(c * vec2(OutSize.x / OutSize.y, 1.0));
    float crawl = 0.04 * sin(atan(c.y, c.x) * 7.0 + t * 0.6) + 0.03 * sin(atan(c.y, c.x) * 13.0 - t * 1.1);
    float edge = Tweak * smoothstep(0.42, 0.95, r) * 0.7 + Psych * smoothstep(0.5 - 0.08 * pulse + crawl, 0.95, r) * 0.9;
    col *= 1.0 - clamp(edge, 0.0, 0.95);
    // Xanax: heavy eyelids, drooping shut now and then
    if (Sedated > 0.0) {
        float droop = Sedated * (0.12 + 0.35 * pow(0.5 + 0.5 * sin(t * 0.35), 8.0));
        float lid = smoothstep(0.5 - droop - 0.08, 0.5 - droop, abs(texCoord.y - 0.5));
        col *= 1.0 - lid;
        l = lum(col);
        col = mix(col, vec3(l), Sedated * 0.25);
    }
    // Ketamine: drained, with a cold cast
    l = lum(col);
    col = mix(col, vec3(l) * vec3(0.9, 1.0, 1.08), Dissoc * 0.5);
    // ---- Trails: the last frame lingers ----
    float trail = clamp(Trip * 0.72 + Stoned * 0.35 + Tweak * 0.15 + Dissoc * 0.8 + Drunk * 0.3, 0.0, 0.88);
    if (trail > 0.0) {
        vec3 prev = texture(PrevSampler, texCoord).rgb;
        col = max(col, mix(col, prev, trail));
    }
    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
