#version 150
// Ascension Lighting, cinematic helpers (half resolution):
//  Mode 0: bright pass (what glows: light sources, the sun, bright sky) for bloom
//  Mode 1: a wide separable gaussian along Direction (bloom and depth of field blur)
//  Mode 2: a plain copy, downsampled (the scene for depth of field)
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform vec2 Direction;
uniform float Radius;
uniform float Threshold;
uniform int Mode;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (Mode == 0) {
        vec3 c = texture(DiffuseSampler, texCoord).rgb;
        float l = max(c.r, max(c.g, c.b));
        float k = smoothstep(Threshold, Threshold + 0.25, l);
        fragColor = vec4(c * k, 1.0);
        return;
    }
    if (Mode == 2) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
        return;
    }
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = -6; i <= 6; i++) {
        float w = exp(-float(i * i) / 18.0);
        sum += texture(DiffuseSampler, texCoord + Direction * float(i) * Radius / OutSize).rgb * w;
        wsum += w;
    }
    fragColor = vec4(sum / wsum, 1.0);
}
