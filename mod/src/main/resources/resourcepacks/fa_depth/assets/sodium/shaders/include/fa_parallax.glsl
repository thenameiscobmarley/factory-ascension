// Factory Ascension depth: real relief on block and mob textures without a shader pack (vanilla renderer and Embeddium).
// A texel's depth comes from how much darker it is than its texture's average (dark = deep). Depth is read *between*
// texels (bilinear), from a softer mip the more "smoothing" is set, so surfaces are smooth slopes instead of stacked
// layers. The texture's average brightness sits at the surface (only darker detail sinks), and every read stays inside the
// block's own sprite, so there are no outlines and no bowl: faces are as flat as the texture says, edge to edge. The relief is lit (a sun-like light from above, plus a highlight that moves with your
// view), so you can see the depth even face-on. Mobs get the lit relief only (their textures are made of many small
// islands; shifting them would bleed one into the next).
// Real heights: client/render/DepthHeights writes each solid block texture's own height map into its alpha (1 = the
// surface, 0.75 = the deepest), knowing what the block is (boards, bricks, stones, ore, grass, a table...). A texture
// it hasn't done (still exactly opaque) falls back to the darkness guess above.
// DepthShaders puts the settings in front of this file: FA_SMOOTH (0..1), FA_DEPTH (0..2), FA_MIPS (the atlas' mip levels).
#ifndef FA_SMOOTH
#define FA_SMOOTH 0.5
#endif
#ifndef FA_DEPTH
#define FA_DEPTH 1.0
#endif
#ifndef FA_MIPS
#define FA_MIPS 4
#endif

const vec3 FA_LIGHT = vec3(0.3487, 0.8466, 0.3985);   // normalize(0.35, 0.85, 0.4)

float faLum(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

float faTexel(sampler2D tex, ivec2 p, ivec2 lo, ivec2 hi, int lod, float avg) {
    vec4 c = texelFetch(tex, clamp(p, lo, hi), lod);
    if (avg < 0.0) return clamp((1.0 - c.a) * 4.0, 0.0, 1.0);   // a real height map (avg < 0 marks it)
    // The texture's average sits just under the surface; only what's darker than that sinks (mortar, cracks, gaps)
    return clamp(0.1 + (avg - faLum(c.rgb)) * 2.4, 0.0, 1.0);
}

// Depth at uv from one mip level: bilinear between its texels, never reading outside the sprite
float faDepthLod(sampler2D tex, vec2 uv, vec2 org, vec2 spr, int lod, float avg) {
    vec2 size = vec2(textureSize(tex, lod));
    vec2 t = uv * size - 0.5;
    vec2 fl = floor(t);
    ivec2 i = ivec2(fl);
    vec2 f = t - fl;
    ivec2 lo = ivec2(floor(org * size + 0.5));
    ivec2 hi = max(lo, ivec2(floor((org + spr) * size + 0.5)) - 1);
    float a = faTexel(tex, i, lo, hi, lod, avg);
    float b = faTexel(tex, i + ivec2(1, 0), lo, hi, lod, avg);
    float c = faTexel(tex, i + ivec2(0, 1), lo, hi, lod, avg);
    float d = faTexel(tex, i + ivec2(1, 1), lo, hi, lod, avg);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float faDepth(sampler2D tex, vec2 uv, vec2 org, vec2 spr, float avg) {
    float L = min(FA_SMOOTH * 2.0, float(FA_MIPS));
    int l0 = int(L);
    float h = faDepthLod(tex, uv, org, spr, l0, avg);
    float lf = L - float(l0);
    if (lf > 0.02 && l0 < FA_MIPS) h = mix(h, faDepthLod(tex, uv, org, spr, l0 + 1, avg), lf);
    // No pulling the edges up to the surface: that bowed every block. The texture's own detail runs right to the edge,
    // so tiling textures carry on smoothly into the next block.
    return h;
}

// The surface's frame: its normal (towards the camera) and the world-space gradients of u and v. False if degenerate.
bool faFrame(vec3 p, vec3 dp1, vec3 dp2, vec2 duv1, vec2 duv2, out vec3 n, out vec3 gu, out vec3 gv) {
    n = cross(dp1, dp2);
    gu = vec3(0.0);
    gv = vec3(0.0);
    float nlen = length(n);
    if (nlen < 1e-10) return false;
    n /= nlen;
    if (dot(p, n) > 0.0) n = -n;
    vec3 dp2perp = cross(dp2, n);
    vec3 dp1perp = cross(n, dp1);
    float det = dot(dp1, dp2perp);
    if (abs(det) < 1e-12) return false;
    gu = (dp2perp * duv1.x + dp1perp * duv2.x) / det;
    gv = (dp2perp * duv1.y + dp1perp * duv2.y) / det;
    return length(gu) > 1e-6 && length(gv) > 1e-6;
}

// Lighting of a tilted bit of surface relative to the flat surface: (shade multiplier, highlight)
vec2 faLight(vec3 n, vec3 gu, vec3 gv, vec3 v, float slopeU, float slopeV, float gloss, float fade) {
    vec3 nw = normalize(n + normalize(gu) * slopeU + normalize(gv) * slopeV);
    float flat0 = 0.55 + 0.45 * max(dot(n, FA_LIGHT), 0.0);
    float lit = 0.55 + 0.45 * max(dot(nw, FA_LIGHT), 0.0);
    float shade = clamp(mix(1.0, lit / flat0, fade), 0.55, 1.45);
    // A highlight from the light and one from the sky above you, both moving as you move: wet stone, polished metal
    vec3 h1 = normalize(FA_LIGHT - v);
    vec3 h2 = normalize(vec3(0.0, 1.0, 0.0) - v);
    float spec = pow(max(dot(nw, h1), 0.0), 36.0) * 0.5 + pow(max(dot(nw, h2), 0.0), 24.0) * 0.08;
    return vec2(shade, spec * gloss * fade);
}

// Blocks: the parallax-shifted coordinate, a shade and a highlight
void faBlock(sampler2D tex, vec2 uv, vec3 p, vec3 dp1, vec3 dp2, vec2 duv1, vec2 duv2, float maxDepth,
             out vec2 outUv, out float shade, out float spec) {
    outUv = uv;
    shade = 1.0;
    spec = 0.0;
    float dist = length(p);
    float fade = 1.0 - smoothstep(12.0, 26.0, dist);
    if (fade <= 0.0) return;
    vec3 n, gu, gv;
    if (!faFrame(p, dp1, dp2, duv1, duv2, n, gu, gv)) return;
    vec3 v = p / max(dist, 1e-4);
    // One block face is one sprite; sprites sit on a grid of their own (power of two) size
    vec2 size = vec2(textureSize(tex, 0));
    vec2 spr = exp2(floor(log2(vec2(length(gu), length(gv)) * size) + 0.5)) / size;
    vec2 org = floor(uv / spr) * spr;
    vec2 texel = 1.0 / size;
    vec4 avgC = textureLod(tex, org + spr * 0.5, float(FA_MIPS));
    float avg = avgC.a < 0.999 ? -1.0 : faLum(avgC.rgb);   // a height map in its alpha, or guess from darkness
    float cosT = max(0.2, -dot(v, n));
    vec3 lateral = v + n * cosT;
    float depthWorld = maxDepth * FA_DEPTH * fade;
    vec2 uvRun = vec2(dot(gu, lateral), dot(gv, lateral)) * depthWorld / cosT;
    vec2 lo = org + texel * 0.5, hi = org + spr - texel * 0.5;
    // March until below the surface, then home in on where it crossed
    const int STEPS = 14;
    float st = 1.0 / float(STEPS);
    vec2 cur = uv, prev = uv;
    float d = 0.0, prevD = 0.0;
    float h = faDepth(tex, cur, org, spr, avg);
    for (int i = 0; i < STEPS; i++) {
        if (d >= h) break;
        prev = cur;
        prevD = d;
        cur = clamp(cur + uvRun * st, lo, hi);
        d += st;
        h = faDepth(tex, cur, org, spr, avg);
    }
    for (int i = 0; i < 5; i++) {
        vec2 mid = (prev + cur) * 0.5;
        float md = (prevD + d) * 0.5;
        if (md < faDepth(tex, mid, org, spr, avg)) { prev = mid; prevD = md; } else { cur = mid; d = md; }
    }
    outUv = clamp(cur, lo, hi);
    // The relief's slope at the hit, in world units, for the lighting
    float hx = faDepth(tex, clamp(outUv + vec2(texel.x, 0.0), lo, hi), org, spr, avg) - faDepth(tex, clamp(outUv - vec2(texel.x, 0.0), lo, hi), org, spr, avg);
    float hy = faDepth(tex, clamp(outUv + vec2(0.0, texel.y), lo, hi), org, spr, avg) - faDepth(tex, clamp(outUv - vec2(0.0, texel.y), lo, hi), org, spr, avg);
    float su = hx * 0.5 * depthWorld * length(gu) / texel.x;
    float sv = hy * 0.5 * depthWorld * length(gv) / texel.y;
    float gloss = 1.0 - d * 0.7;                        // the raised parts catch the light, the cracks don't
    vec2 l = faLight(n, gu, gv, v, su, sv, gloss, fade);
    shade = l.x * (1.0 - max(0.0, d - 0.1) * 0.3 * fade);   // deep spots (cracks, mortar) are darker
    spec = l.y;
}

// Mobs: lit relief only (brighter = raised), no shifting
void faEntity(sampler2D tex, vec2 uv, vec3 p, vec3 dp1, vec3 dp2, vec2 duv1, vec2 duv2, out float shade, out float spec) {
    shade = 1.0;
    spec = 0.0;
    float dist = length(p);
    float fade = 1.0 - smoothstep(10.0, 22.0, dist);
    if (fade <= 0.0) return;
    vec3 n, gu, gv;
    if (!faFrame(p, dp1, dp2, duv1, duv2, n, gu, gv)) return;
    vec3 v = p / max(dist, 1e-4);
    vec2 texel = 1.0 / vec2(textureSize(tex, 0));
    float r = 1.0 + FA_SMOOTH * 1.5;
    vec4 c0 = texture(tex, uv);
    float h0 = faLum(c0.rgb);
    vec4 cr = texture(tex, uv + vec2(texel.x * r, 0.0)), cl = texture(tex, uv - vec2(texel.x * r, 0.0));
    vec4 cd = texture(tex, uv + vec2(0.0, texel.y * r)), cu = texture(tex, uv - vec2(0.0, texel.y * r));
    // Off the edge of this bit of skin (see-through, or a very different colour: another part): flat
    float hr = cr.a > 0.5 ? faLum(cr.rgb) : h0, hl = cl.a > 0.5 ? faLum(cl.rgb) : h0;
    float hd = cd.a > 0.5 ? faLum(cd.rgb) : h0, hu = cu.a > 0.5 ? faLum(cu.rgb) : h0;
    float depthWorld = 0.035 * FA_DEPTH;
    float su = -clamp(hr - hl, -0.5, 0.5) * 0.5 * depthWorld * length(gu) / (texel.x * r) * 2.2;
    float sv = -clamp(hd - hu, -0.5, 0.5) * 0.5 * depthWorld * length(gv) / (texel.y * r) * 2.2;
    vec2 l = faLight(n, gu, gv, v, su, sv, smoothstep(0.25, 0.8, h0), fade);
    shade = l.x;
    spec = l.y * 0.6;
}
