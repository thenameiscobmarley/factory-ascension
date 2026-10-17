#version 150
#moj_import <fog.glsl>
// Chunks taken out of a block as it's broken. The face is cut into irregular pieces (a Voronoi pattern); as the
// break goes on, more of them are gone (Progress). A gone piece is a hole: looking into it you see its walls (the
// pieces around it, side-on and darker) and its floor (the block's inside, darker still), shifted with your view
// (parallax), so it reads as real depth. Around each hole, a thin dark rim. Everything else is left to the block.
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float Progress;     // 0..1
uniform vec4 SpriteBox;     // the face's sprite: min u, min v, max u, max v
uniform vec3 FaceU, FaceV, FaceN;   // world directions of the face's u, v and outward normal
uniform float Seed;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewPos;
out vec4 fragColor;

vec2 hash2(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3))) + Seed;
    return fract(sin(p) * 43758.5453);
}

float hash1(vec2 p) {
    return fract(sin(dot(p, vec2(41.3, 289.1)) + Seed * 7.1) * 15731.743);
}

// Nearest piece: its id (cell) and the gap to the next piece's edge
void cells(vec2 f, out vec2 id, out float edge) {
    vec2 g = floor(f), fr = fract(f);
    float d1 = 9.0, d2 = 9.0;
    for (int y = -1; y <= 1; y++)
        for (int x = -1; x <= 1; x++) {
            vec2 o = vec2(x, y);
            vec2 c = o + hash2(g + o) * 0.85 + 0.075;
            float d = length(c - fr);
            if (d < d1) { d2 = d1; d1 = d; id = g + o; } else if (d < d2) d2 = d;
        }
    edge = d2 - d1;
}

// Is the piece gone (and how deep is its hole, in blocks)?
float gone(vec2 id) {
    float h = hash1(id);
    // Pieces nearer the middle go first; the edges hold on longest
    vec2 c = (id + 0.5) / 4.0 - 0.5;
    float order = h * 0.75 + length(c) * 0.5;
    return Progress * 1.15 > order + 0.05 ? 0.06 + 0.16 * hash1(id + 3.1) * smoothstep(0.0, 0.3, Progress * 1.15 - order) + 0.04 : 0.0;
}

vec4 tex(vec2 f) {
    vec2 uv = mix(SpriteBox.xy, SpriteBox.zw, clamp(f, 0.002, 0.998));
    vec4 c = textureLod(Sampler0, uv, 0.0);   // full detail: inside the march the GPU can't pick a mip level
    c.a = 1.0;
    return c;
}

void main() {
    vec2 sz = SpriteBox.zw - SpriteBox.xy;
    vec2 f = (texCoord0 - SpriteBox.xy) / sz;            // 0..1 across the face
    vec2 id;
    float edge;
    cells(f * 4.0, id, edge);
    float depth = gone(id);
    if (depth <= 0.0) {
        // Not gone: only a dark rim where it meets a hole
        vec2 id2;
        float e2;
        // Look a little way out for a missing neighbour
        float rim = 0.0;
        for (int k = 0; k < 4; k++) {
            vec2 o = vec2(k == 0 ? 0.06 : k == 1 ? -0.06 : 0.0, k == 2 ? 0.06 : k == 3 ? -0.06 : 0.0);
            cells((f + o) * 4.0, id2, e2);
            if (id2 != id && gone(id2) > 0.0) rim = max(rim, 1.0 - smoothstep(0.0, 0.12, edge));
        }
        if (rim < 0.02) discard;
        fragColor = linear_fog(vec4(0.0, 0.0, 0.0, rim * 0.55), vertexDistance, FogStart, FogEnd, FogColor);
        return;
    }
    // A hole: march into it along the view
    vec3 v = normalize(viewPos);
    float into = max(0.15, -dot(v, FaceN));
    vec2 run = vec2(dot(v, FaceU), dot(v, FaceV)) / into;  // across the face per block of depth
    vec4 col = vec4(0.0);
    bool hit = false;
    const int STEPS = 10;
    for (int i = 1; i <= STEPS; i++) {
        float d = depth * float(i) / float(STEPS);
        vec2 p = f + run * d;
        vec2 pid;
        float pe;
        cells(p * 4.0, pid, pe);
        if (pid != id && gone(pid) < d) {
            // A wall: the next piece, seen side-on
            col = tex(p) * (0.85 - 0.3 * d / max(depth, 0.01));
            hit = true;
            break;
        }
        if (p.x < 0.0 || p.y < 0.0 || p.x > 1.0 || p.y > 1.0) { col = tex(clamp(p, 0.0, 1.0)) * 0.6; hit = true; break; }
    }
    if (!hit) {
        // The floor: the block's inside, in shadow
        vec2 p = f + run * depth;
        col = tex(p) * (0.68 - depth * 0.8);
        col.rgb *= vec3(0.95, 0.9, 0.86);   // the inside: a little warmer, in shadow
    }
    col.rgb *= vertexColor.rgb;
    col.a = 1.0;
    fragColor = linear_fog(col * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
