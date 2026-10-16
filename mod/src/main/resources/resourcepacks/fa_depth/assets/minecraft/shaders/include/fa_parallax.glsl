// Factory Ascension parallax: real depth on block and mob textures without a shader pack.
// Each texel's depth comes from how much darker it is than its texture's average (dark = deep; the average is the
// texture's smallest mip level, so every texture uses its full depth range), read from a slightly blurred mip; the view ray is
// marched through that height field inside the block's own texture (atlas sprites are wrapped, so it never reads a
// neighbour). Derivatives are taken by the caller in uniform control flow and passed in.
// Returns (u, v, shade): the shifted texture coordinate and a darkening for deep spots.

float faLum(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec2 faWrap(vec2 c, vec2 org, vec2 spr, bool atlas) {
    if (atlas) return org + mod(c - org, spr);
    return clamp(c, vec2(0.0), vec2(1.0));
}

vec3 faParallax(sampler2D tex, vec2 uv, vec3 p, vec3 dp1, vec3 dp2, vec2 duv1, vec2 duv2, float maxDepth, bool atlas) {
    float dist = length(p);
    float fade = 1.0 - smoothstep(10.0, 22.0, dist);    // only nearby surfaces: costs nothing far away
    if (fade <= 0.0) return vec3(uv, 1.0);
    vec3 n = cross(dp1, dp2);
    float nlen = length(n);
    if (nlen < 1e-10) return vec3(uv, 1.0);
    n /= nlen;
    vec3 v = p / max(dist, 1e-4);                        // camera -> fragment
    if (dot(v, n) > 0.0) n = -n;                         // the normal faces the camera
    // How texture coordinates change across the surface (gradient of u and of v)
    vec3 dp2perp = cross(dp2, n);
    vec3 dp1perp = cross(n, dp1);
    float det = dot(dp1, dp2perp);
    if (abs(det) < 1e-12) return vec3(uv, 1.0);
    vec3 gu = (dp2perp * duv1.x + dp1perp * duv2.x) / det;
    vec3 gv = (dp2perp * duv1.y + dp1perp * duv2.y) / det;
    // One block of surface is one texture (sprite); its corner in the atlas. Sprites sit on a grid of their own size
    // (16, 32, 64 texels), so the measured size is snapped to that exact power of two, or the edges drift onto neighbours.
    vec2 spr = vec2(length(gu), length(gv));
    if (spr.x < 1e-6 || spr.y < 1e-6) return vec3(uv, 1.0);
    if (atlas) {
        vec2 size = vec2(textureSize(tex, 0));
        spr = exp2(floor(log2(spr * size) + 0.5)) / size;
    }
    vec2 org = floor(uv / spr) * spr;
    float cosT = max(0.15, -dot(v, n));
    vec3 lateral = v + n * cosT;                         // sideways part of the ray per unit length
    float sMax = maxDepth * fade / cosT;
    vec2 uvRun = vec2(dot(gu, lateral), dot(gv, lateral)) * sMax;
    // The texture's average brightness: its smallest mip is one texel (blocks); mobs have no mips, so mid-grey
    float avg = atlas ? faLum(textureLod(tex, org + spr * 0.5, 4.0).rgb) : 0.5;
    const int STEPS = 12;
    float stepFrac = 1.0 / float(STEPS);
    vec2 cur = uv, prev = uv;
    float depth = 0.0, prevDepth = 0.0, h = 0.0, prevH = 0.0;
    for (int i = 0; i < STEPS; i++) {
        h = clamp(0.5 + (avg - faLum(textureLod(tex, faWrap(cur, org, spr, atlas), 1.0).rgb)) * 2.4, 0.0, 1.0);
        if (depth >= h) break;
        prev = cur; prevDepth = depth; prevH = h;
        cur += uvRun * stepFrac;
        depth += stepFrac;
    }
    // Between the last step above the surface and the first below it
    float a = h - depth, b = prevH - prevDepth;
    float t = clamp(b / max(b - a, 1e-4), 0.0, 1.0);
    vec2 hit = faWrap(mix(prev, cur, t), org, spr, atlas);
    float hitDepth = mix(prevDepth, depth, t);
    return vec3(hit, 1.0 - hitDepth * 0.3 * fade);
}
