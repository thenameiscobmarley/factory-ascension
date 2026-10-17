#version 150
// Factory Ascension weapon skins, per pixel: the same 28 looks as client/gun/GunSkins (which still has them per
// corner, as the fallback), worked out for every pixel from where it is on the gun, so the patterns are sharp and
// move smoothly. Numbers are the skins' order in GunSkinGpu.IDS.
#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float FaSkinTime;
uniform float FaHeat;

in float vertexDistance;
in vec4 baseColor;
in vec4 lightColor;
in float diffuse;
in vec3 objPos;
in vec3 viewNormal;
flat in int skin;

out vec4 fragColor;

// ---- Noise (value noise, as GunSkins) ----
float hash3(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}
float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(hash3(i), hash3(i + vec3(1, 0, 0)), f.x), mix(hash3(i + vec3(0, 1, 0)), hash3(i + vec3(1, 1, 0)), f.x), f.y);
    float b = mix(mix(hash3(i + vec3(0, 0, 1)), hash3(i + vec3(1, 0, 1)), f.x), mix(hash3(i + vec3(0, 1, 1)), hash3(i + vec3(1, 1, 1)), f.x), f.y);
    return mix(a, b, f.z);
}
float fbm(vec3 p, int oct) {
    float s = 0.0, amp = 0.5, tot = 0.0;
    for (int i = 0; i < 5; i++) {
        if (i >= oct) break;
        s += noise(p) * amp;
        tot += amp;
        p *= 2.03;
        amp *= 0.5;
    }
    return clamp(0.5 + (s / tot - 0.5) * 2.6, 0.0, 1.0);
}
float ridge(vec3 p, float w) {
    float n = abs(fbm(p, 3) - 0.5) * 2.0;
    return max(0.0, 1.0 - n / w);
}
vec3 hex(int c) {
    return vec3(float((c >> 16) & 255), float((c >> 8) & 255), float(c & 255)) / 255.0;
}
float sm(float t) {
    t = clamp(t, 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}
float lum(vec3 c) {
    return dot(c, vec3(0.3, 0.59, 0.11));
}

void main() {
    vec3 P = objPos;
    vec3 N = normalize(viewNormal);
    float t = FaSkinTime;
    vec3 b = baseColor.rgb;
    // The studio highlight, as GunSkins.sheen
    float hl = max(0.0, N.x * 0.25 + N.y * 0.88 + N.z * 0.4);
    #define SHEEN(pw, k) (pow(hl, pw) * (k))
    float up = max(0.0, N.y);
    vec3 c = b;
    float glow = 0.0;

    if (skin == 0) {            // obsidian
        vec3 q = P * 0.22;
        float v = ridge(q + vec3(t * 0.05, 0.0, -t * 0.03), 0.12);
        float pulse = 0.75 + 0.25 * sin(t * 1.7 + q.x * 3.0);
        vec3 glass = vec3(0.07, 0.065, 0.07) + SHEEN(30.0, 0.55) + 0.03 * noise(P * 3.0);
        c = mix(glass, vec3(1.0, 0.42 * pulse, 0.08), v * pulse);
        glow = v * 0.9;
    } else if (skin == 1) {     // snow
        float n = fbm(P * 0.5, 4);
        vec3 under = mix(b, hex(0x9AAABB), 0.55 + 0.2 * n);
        c = mix(under, hex(0xF4F8FC), sm(up * 1.4 + n * 0.3));
        c += step(0.993, hash3(floor(P * 12.0) + floor(t * 3.0))) * 0.5;
    } else if (skin == 2) {     // black water
        float w = sin(P.x * 0.9 + t * 2.6 + fbm(P * vec3(0.2, 0.3, 0.3), 2) * 4.0) * 0.5 + 0.5;
        float foam = sm((w - 0.86) / 0.14) * (0.6 + 0.4 * noise(vec3(P.xy * 2.0, t)));
        vec3 deep = mix(hex(0x05090E), hex(0x0E2A36), w);
        c = mix(deep, hex(0xB8D8DC), foam) + SHEEN(40.0, 0.5);
    } else if (skin == 3) {     // torn
        vec3 q = P * 0.35;
        float n = fbm(q, 3);
        vec3 camo = n < 0.42 ? hex(0x4E5434) : n < 0.55 ? hex(0x8A7A52) : hex(0x3A3022);
        float rip = fbm(q * 2.5 + vec3(11, 0, 0), 4);
        c = rip > 0.64 ? b : rip > 0.6 ? camo * 0.55 : camo * (0.85 + 0.3 * noise(q * 9.0));
    } else if (skin == 4) {     // gold leaf
        float band = pow(max(0.0, sin(P.x * 0.25 - t * 1.6)), 24.0);
        float n = noise(P * 1.5) * 0.7 + noise(P * 6.0) * 0.3;
        c = mix(hex(0x9A6A1A), hex(0xF0C450), 0.45 + 0.35 * n + 0.4 * lum(b)) + band * 0.5 + SHEEN(18.0, 0.35);
    } else if (skin == 5) {     // lava
        vec3 q = P * 0.3 - vec3(t * 0.25, 0, 0);
        float n = fbm(q, 5);
        float hot = sm((n - 0.45) * 4.0);
        vec3 crust = mix(hex(0x1E0E08), hex(0x3A1A0E), noise(q * 6.0));
        c = mix(crust, mix(hex(0xE04808), hex(0xFFD040), hot * hot), hot);
        glow = hot;
    } else if (skin == 6) {     // frost
        vec3 q = P * 0.4;
        float crack = ridge(q, 0.08);
        float shimmer = 0.06 * sin(t * 2.0 + q.x * 4.0 + q.z * 3.0);
        c = mix(mix(hex(0x8CC8DC), hex(0xE6F6FC), fbm(q, 4) + shimmer), vec3(1.0), crack) + SHEEN(30.0, 0.4);
    } else if (skin == 7) {     // neon grid
        float gx = abs(mod(P.x - t * 2.0, 2.0) - 1.0), gy = abs(mod(P.y + P.z, 2.0) - 1.0);
        float line = max(sm((gx - 0.9) / 0.1), sm((gy - 0.92) / 0.08));
        vec3 lc = mix(hex(0x30F0FF), hex(0x9CFF40), 0.5 + 0.5 * sin(P.x * 0.2 + t));
        c = mix(vec3(0.08 + SHEEN(20.0, 0.2), 0.09, 0.1), lc, line);
        glow = line;
    } else if (skin == 8) {     // digital woodland
        vec3 q = floor(P / 0.6);
        float n = fbm(q * 0.35, 2) + (hash3(q) - 0.5) * 0.25;
        int k = int(clamp(n * 4.4 - 0.4, 0.0, 3.0));
        vec3 pal = k == 0 ? hex(0x2E3A22) : k == 1 ? hex(0x4E5A34) : k == 2 ? hex(0x6E6A44) : hex(0x8A7C56);
        c = pal * (0.9 + 0.25 * lum(b));
    } else if (skin == 9) {     // arctic
        float n = fbm(P * 0.3, 4);
        c = (n < 0.42 ? hex(0xE8ECEE) : n < 0.56 ? hex(0xA8AEB2) : hex(0x6A7074)) + SHEEN(12.0, 0.1);
    } else if (skin == 10) {    // tiger
        float s = sin(P.x * 1.1 + fbm(P * vec3(0.2, 0.4, 0.4), 4) * 7.0);
        c = s > 0.45 ? hex(0x1E1E16) : s > 0.1 ? hex(0x4C5228) : hex(0x7A7038);
    } else if (skin == 11) {    // carbon fibre
        float x = P.x * 3.0, y = (P.y + P.z) * 3.0;
        bool warp = mod(floor(x) + floor(y), 2.0) < 0.5;
        float f = warp ? pow(sin(fract(x) * 3.14159), 2.0) : pow(sin(fract(y) * 3.14159), 2.0);
        float sh = SHEEN(10.0, 0.6) * (warp ? 1.0 : 0.4) * (0.7 + 0.3 * sin(t * 0.8));
        c = vec3(0.09, 0.09, 0.1) + 0.06 * f + sh;
    } else if (skin == 12) {    // damascus
        // Many thin folded layers, steel-grey, with a soft contrast (not zebra stripes)
        float w = sin((P.y + P.z * 0.4) * 11.0 + fbm(P * 0.35, 4) * 18.0);
        c = mix(hex(0x4A4D52), hex(0x9DA1A6), smoothstep(-0.6, 0.6, w)) + SHEEN(24.0, 0.35);
    } else if (skin == 13) {    // rust
        float n = fbm(P * 0.45, 4);
        vec3 rust = mix(hex(0x6A2E12), hex(0xB4602A), noise(P * 4.0) * 0.7 + noise(P * 12.0) * 0.3);
        c = n < 0.4 ? b : mix(b, rust, sm((n - 0.4) * 6.0));
    } else if (skin == 14) {    // moss
        float n = fbm(P * 0.6, 4);
        vec3 moss = mix(hex(0x2E4A18), hex(0x6A8A2E), n);
        c = mix(b * 0.8, moss, sm(up * 1.2 + n * 0.6 - 0.15));
    } else if (skin == 15) {    // bone
        vec3 q = P * 0.5;
        vec3 ivory = mix(hex(0xB8A888), hex(0xEEE4CC), fbm(q, 4)) + SHEEN(14.0, 0.15);
        c = mix(ivory, hex(0x4A3E2A), ridge(q * 1.5, 0.05));
    } else if (skin == 16) {    // galaxy
        vec3 q = P * 0.18;
        float n1 = fbm(q + vec3(t * 0.03, 0, 0), 5), n2 = fbm(q + vec3(7, -t * 0.02, 3), 5);
        c = mix(hex(0x060A1A), hex(0x1C3C6A), n1);
        c = mix(c, hex(0x2EA8A0), sm((n2 - 0.55) * 3.0));
        c = mix(c, hex(0xE89A48), sm((n1 - 0.68) * 4.0) * 0.7);
        vec3 cell = floor(P * 6.0);
        float star = step(0.985, hash3(cell)) * (0.6 + 0.4 * sin(t * 4.0 + hash3(cell + 3.0) * 30.0));
        c += star;
        glow = 0.15 + star * 0.8;
    } else if (skin == 17) {    // toxic
        vec3 q = P * 0.4 + vec3(0, t * 0.3, 0);
        float n = fbm(q, 4);
        c = mix(hex(0x1E3A0C), hex(0x8CF02E), n) + (noise(q * 5.0) > 0.82 ? 0.3 : 0.0);
        glow = 0.25 + 0.35 * n;
    } else if (skin == 18) {    // blood
        c = mix(hex(0x2A0404), hex(0x8A1010), fbm(P * 0.5, 4)) + SHEEN(30.0, 0.35);
    } else if (skin == 19) {    // chrome
        vec3 sky = mix(hex(0x8AB0D8), hex(0xF0F4FA), max(0.0, N.y));
        vec3 ground = mix(hex(0x5A5048), hex(0x2A2622), max(0.0, -N.y));
        float h = pow(max(0.0, sin(N.z * 2.5 + t * 0.4)), 30.0);
        c = mix(N.y >= 0.0 ? sky : ground, hex(0x9A9EA4), 0.25) + h * 0.6;
    } else if (skin == 20) {    // circuit
        float x = P.x, y = P.y + P.z;
        float tx = abs(fract(x * 1.3) - 0.5), ty = abs(fract(y * 1.7) - 0.5);
        bool trace = (tx > 0.43 && hash3(vec3(floor(y * 1.7), 1, 2)) > 0.4) || (ty > 0.44 && hash3(vec3(floor(x * 1.3), 2, 3)) > 0.5);
        float pulse = pow(max(0.0, sin(x * 0.8 - t * 5.0 + y)), 16.0);
        c = trace ? mix(hex(0xB07838), hex(0xA0FFD0), pulse) : mix(hex(0x0E2414), hex(0x1C3C22), noise(vec3(x * 3.0, y * 3.0, 0.0)));
        glow = trace ? pulse * 0.9 : 0.0;
    } else if (skin == 21) {    // blossom
        float n = fbm(P * 0.6, 4);
        c = mix(hex(0xF4D8E0), hex(0xE07898), sm((n - 0.45) * 4.0)) + SHEEN(12.0, 0.12);
    } else if (skin == 22) {    // inferno
        vec3 q = P * 0.5 - vec3(0, t * 0.9, 0);
        float n = fbm(q, 4);
        float heat = sm((n - 0.5) * 3.0 + up * 0.4);
        c = mix(hex(0x140A06), mix(hex(0xC03A08), hex(0xFFB030), heat), heat);
        glow = heat;
    } else if (skin == 23) {    // glitch
        float frame = floor(t * 12.0);
        vec2 blk = floor(vec2(P.x / 1.5, P.y / 0.7));
        float h = hash3(vec3(blk, frame));
        if (h > 0.9) c = h > 0.96 ? hex(0x40FFE0) : hex(0xFF4060);
        else if (h > 0.85) c = vec3(0.94);
        else if (hash3(vec3(blk.y, frame, 9.0)) > 0.93) c = vec3(0.2, 1.4, 1.4) * lum(b);
        glow = h > 0.85 ? 0.9 : 0.0;
    } else if (skin == 24) {    // hazard
        float s = fract((P.x + P.y) * 0.5);
        c = (s < 0.5 ? hex(0xE8B818) : hex(0x1A1A18)) * (0.9 + 0.1 * noise(P * 8.0)) + SHEEN(14.0, 0.15);
    } else if (skin == 25) {    // ghillie
        c = mix(hex(0x3E4428), hex(0x7A7448), fbm(P * 0.8, 4));
    } else if (skin == 26) {    // ember
        float n = fbm(P * 0.7, 4);
        float heat = min(1.0, sm((n - 0.55) * 4.0) * (0.6 + 0.4 * sin(t * 2.0 + n * 9.0) + FaHeat));
        c = mix(b * 0.6, mix(hex(0xA02808), hex(0xFFA040), heat), heat);
        glow = heat;
    } else if (skin == 27) {    // aurora
        float rib = sin(P.y * 1.4 + sin(P.x * 0.35 + t * 0.9) * 2.2 + P.z * 0.3);
        float a = sm(rib);
        c = mix(hex(0x0A1418), mix(hex(0x30E080), hex(0x40C0E0), 0.5 + 0.5 * sin(P.x * 0.2 - t * 0.5)), a * 0.85) + SHEEN(30.0, 0.25);
        glow = a * 0.6;
    }

    // Fine surface: the gun's detail texture, mapped from the side each face looks toward (as the mesh does)
    vec3 fn = abs(normalize(cross(dFdx(P), dFdy(P))));
    vec2 duv = fn.x >= fn.y && fn.x >= fn.z ? vec2(P.z, -P.y) : fn.y >= fn.z ? P.xz : vec2(P.x, -P.y);
    float grain = texture(Sampler0, duv / 8.0).r;
    c *= 0.82 + 0.36 * grain;

    vec3 lit = c * diffuse * lightColor.rgb;
    vec3 col = mix(lit, c, clamp(glow, 0.0, 1.0));
    fragColor = linear_fog(vec4(col, 1.0) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
