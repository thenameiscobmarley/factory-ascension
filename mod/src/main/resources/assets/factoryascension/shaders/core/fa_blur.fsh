#version 150
// Ascension Lighting, pass 2: a depth-aware blur of pass 1 (smooths the noise without bleeding across edges)
uniform sampler2D DiffuseSampler;   // pass 1
uniform sampler2D DepthSampler;
uniform vec2 OutSize;
uniform vec2 Direction;             // (1,0) then (0,1)
in vec2 texCoord;
out vec4 fragColor;

float lin(float d) { return 1.0 / max(1e-5, 1.0 - d); }   // grows with distance; enough for comparing neighbours

void main() {
    float d0 = lin(texture(DepthSampler, texCoord).r);
    vec4 sum = vec4(0.0);
    float wsum = 0.0;
    for (int i = -3; i <= 3; i++) {
        vec2 uv = texCoord + Direction * float(i) / OutSize;
        float w = exp(-float(i * i) / 6.0) * exp(-abs(lin(texture(DepthSampler, uv).r) - d0) / (0.05 * d0));
        sum += texture(DiffuseSampler, uv) * w;
        wsum += w;
    }
    fragColor = sum / max(wsum, 1e-4);
}
