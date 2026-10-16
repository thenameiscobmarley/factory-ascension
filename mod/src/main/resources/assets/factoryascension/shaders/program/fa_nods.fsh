#version 150
// Night vision through image-intensifier tubes: everything brightened into one phosphor colour (green, or the cold
// blue-white of white phosphor), heavy moving grain, bright sparkles of scintillation, bloom haloes round every light,
// a soft blur, the tube's round edge, and a faint flicker of the gain.
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Clock, White;

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

void main() {
    vec2 px = 1.0 / OutSize;
    // A soft blur (tubes aren't sharp)
    float l = lum(texture(DiffuseSampler, texCoord).rgb) * 0.4;
    l += lum(texture(DiffuseSampler, texCoord + vec2(px.x, 0.0)).rgb) * 0.15;
    l += lum(texture(DiffuseSampler, texCoord - vec2(px.x, 0.0)).rgb) * 0.15;
    l += lum(texture(DiffuseSampler, texCoord + vec2(0.0, px.y)).rgb) * 0.15;
    l += lum(texture(DiffuseSampler, texCoord - vec2(0.0, px.y)).rgb) * 0.15;
    // Haloes: bright things bleed light round themselves
    float halo = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.7854;
        vec2 o = vec2(cos(a), sin(a)) * px * 9.0;
        float b = lum(texture(DiffuseSampler, texCoord + o).rgb);
        halo += max(0.0, b - 0.75);
    }
    l = l + halo * 0.35;
    // Gain: lift the darks hard, roll off the brights
    float gain = 1.0 + 0.04 * sin(Clock * 37.0) * hash(vec2(floor(Clock * 12.0), 1.0));
    l = 1.0 - exp(-l * 2.6 * gain);
    // Grain and scintillation
    vec2 cell = floor(texCoord * OutSize / (White > 0.5 ? 1.0 : 1.5));
    float n = hash(cell + fract(Clock * 17.31) * 731.0);
    l += (n - 0.5) * (White > 0.5 ? 0.16 : 0.24);
    if (hash(cell * 1.7 + fract(Clock * 23.7) * 517.0) > 0.9993) l += 0.8;
    // Phosphor
    vec3 tint = White > 0.5 ? vec3(0.80, 0.92, 1.0) : vec3(0.38, 1.0, 0.32);
    vec3 col = tint * clamp(l, 0.0, 1.2);
    // The tube: a round view, dark and soft at its edge
    float aspect = OutSize.x / OutSize.y;
    vec2 c = (texCoord - 0.5) * vec2(aspect, 1.0);
    float r = length(c);
    float tube = 1.0 - smoothstep(0.43, 0.5, r);
    col *= tube * (1.0 - 0.35 * smoothstep(0.2, 0.47, r));
    fragColor = vec4(col, 1.0);
}
