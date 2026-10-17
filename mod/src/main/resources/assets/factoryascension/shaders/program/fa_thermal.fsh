#version 150
// Thermal imaging: the world as heat. Living things (drawn in a marker colour by client/ThermalClient: magenta, its
// strength how warm they are) glow white-hot through yellow and orange; fire, lava and lamps are hot too; everything
// else is cold, a dim grey-blue picked out from its shape. Grain, a slight blur, and for a scope, the round view.
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Clock, Scope;
in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 palette(float h) {
    // cold black-blue -> purple -> red -> orange -> yellow -> white (white-hot)
    vec3 c = mix(vec3(0.02, 0.02, 0.06), vec3(0.25, 0.05, 0.45), smoothstep(0.0, 0.35, h));
    c = mix(c, vec3(0.85, 0.15, 0.1), smoothstep(0.35, 0.55, h));
    c = mix(c, vec3(1.0, 0.6, 0.05), smoothstep(0.55, 0.75, h));
    c = mix(c, vec3(1.0, 0.95, 0.4), smoothstep(0.75, 0.9, h));
    return mix(c, vec3(1.0), smoothstep(0.9, 1.0, h));
}

float heatAt(vec2 uv) {
    vec3 c = texture(DiffuseSampler, uv).rgb;
    // The marker: magenta (r = b, no green) = something alive, how warm = how bright
    float marker = step(0.35, c.r) * step(abs(c.r - c.b), 0.08) * step(c.g, 0.12);
    float live = marker * (0.62 + 0.38 * c.r);
    // Fire, lava, lamps: bright and warm-coloured
    float lum = dot(c, vec3(0.299, 0.587, 0.114));
    float fire = smoothstep(0.7, 0.95, lum) * smoothstep(0.1, 0.35, c.r - c.b);
    // Everything else: cold, its shape kept faintly
    float cold = lum * 0.32;
    return max(max(live, fire * 0.85), cold);
}

void main() {
    vec2 px = 1.0 / OutSize;
    float h = heatAt(texCoord) * 0.5 + (heatAt(texCoord + vec2(px.x, 0.0)) + heatAt(texCoord - vec2(px.x, 0.0)) + heatAt(texCoord + vec2(0.0, px.y))
        + heatAt(texCoord - vec2(0.0, px.y))) * 0.125;
    h += (hash(texCoord * OutSize + floor(Clock * 30.0)) - 0.5) * 0.04;
    vec3 col = palette(clamp(h, 0.0, 1.0));
    // A scope's round view
    if (Scope > 0.5) {
        vec2 d = (texCoord - 0.5) * vec2(OutSize.x / OutSize.y, 1.0);
        col *= 1.0 - smoothstep(0.42, 0.45, length(d));
    }
    fragColor = vec4(col, 1.0);
}
