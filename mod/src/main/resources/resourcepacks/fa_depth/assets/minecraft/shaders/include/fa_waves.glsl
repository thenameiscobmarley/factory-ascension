// Factory Ascension water: the wave field, shared by the water shaders and (the same numbers) client/render/Waves.java,
// so boats ride exactly the waves you see. Each wave is (m, n, height, speed): it runs along (m, n) / 256 cycles per
// block, so the whole field repeats every 256 blocks: the camera is passed mod 256 (no float trouble far out).
const vec4 FA_WAVES[4] = vec4[](vec4(23.0, 11.0, 0.030, 1.3), vec4(-15.0, 29.0, 0.022, 1.7), vec4(41.0, -27.0, 0.012, 2.6), vec4(-61.0, -47.0, 0.007, 3.4));
// Small ripples: only bend the light, never move the surface
const vec4 FA_RIPPLES[3] = vec4[](vec4(97.0, -131.0, 0.004, 5.1), vec4(-150.0, 83.0, 0.003, 6.3), vec4(211.0, 177.0, 0.002, 7.7));
const float FA_TAU = 6.2831853;

// Height of the surface at world (x, z) mod 256, time t seconds; .yz = its slope along x and z
vec3 fa_wave(vec2 p, float t) {
    vec3 h = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        vec2 k = FA_WAVES[i].xy * (FA_TAU / 256.0);
        float a = dot(k, p) + FA_WAVES[i].w * t;
        h += FA_WAVES[i].z * vec3(sin(a), k * cos(a));
    }
    return h;
}

vec2 fa_ripple_slope(vec2 p, float t) {
    vec2 s = vec2(0.0);
    for (int i = 0; i < 3; i++) {
        vec2 k = FA_RIPPLES[i].xy * (FA_TAU / 256.0);
        s += FA_RIPPLES[i].z * k * cos(dot(k, p) + FA_RIPPLES[i].w * t);
    }
    return s;
}

// Is this vertex water? Water is the only translucent block tinted blue by the biome (glass, ice, honey are white)
bool fa_is_water(vec4 color) {
    return color.b > color.r + 0.15 && color.b > 0.35;
}
