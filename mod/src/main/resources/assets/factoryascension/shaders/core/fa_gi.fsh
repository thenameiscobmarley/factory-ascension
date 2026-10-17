#version 150
// Ascension Lighting, pass 1 (half resolution): screen-space traced ambient occlusion and one bounce of light.
// Around each pixel, rays are marched through the depth buffer in a rotated spiral; samples that rise above the
// surface block the sky (shadow) and pass on their own colour as bounced light. Output: rgb = bounced light already
// tinted by this pixel's colour, a = how much open sky it sees (1 = none blocked).
uniform sampler2D DiffuseSampler;   // the frame's colour
uniform sampler2D DepthSampler;     // the frame's depth
uniform mat4 InvProjMat;
uniform vec2 OutSize;
uniform float Radius;               // in blocks
uniform int Samples;
uniform float Strength;
uniform mat4 ProjMat;
uniform vec3 SunDir;                // towards the sun (or moon), in view space
uniform float SunLight;             // how strong its shadows are (0 at night, in rain, underground)
uniform int ShadowSteps;
uniform sampler2D VoxelSampler;     // the blocks around you (client/render/VoxelShadow): 96 slices of 96 x 64, 12 x 8
uniform mat4 ViewToWorld;           // view space -> world directions (rotation only)
uniform vec3 CamMod;                // the camera's world position, modulo the grid size
uniform vec3 SunWorld;              // towards the sun, world space
uniform float VoxReady;
in vec2 texCoord;
out vec4 fragColor;

vec3 viewPos(vec2 uv) {
    float d = texture(DepthSampler, uv).r;
    vec4 p = InvProjMat * vec4(uv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec2 toScreen(vec3 v) {
    vec4 c = ProjMat * vec4(v, 1.0);
    return c.xy / c.w * 0.5 + 0.5;
}

float ign(vec2 px) {   // interleaved gradient noise: cheap, even per-pixel rotation
    return fract(52.9829189 * fract(dot(px, vec2(0.06711056, 0.00583715))));
}

// How much the block at grid cell c (world position modulo the size) stops the sun
float vox(ivec3 c) {
    c = ivec3(mod(vec3(c), vec3(96.0, 64.0, 96.0)));
    ivec2 t = ivec2((c.z % 12) * 96 + c.x, (c.z / 12) * 64 + c.y);
    return texelFetch(VoxelSampler, t, 0).r;
}

void main() {
    float d = texture(DepthSampler, texCoord).r;
    if (d >= 0.99999) { fragColor = vec4(0.0, 0.0, 0.0, 1.0); return; }   // sky
    vec3 p = viewPos(texCoord);
    // The surface normal from neighbouring depths
    vec2 texel = 1.0 / OutSize;
    vec3 n = normalize(cross(viewPos(texCoord + vec2(texel.x, 0.0)) - p, viewPos(texCoord + vec2(0.0, texel.y)) - p));
    if (dot(n, p) > 0.0) n = -n;
    float dist = max(0.5, -p.z);
    // A fixed size in the world, smaller far away; but never more than a small part of the screen (right up against a
    // wall it would reach off the screen and leave smudges down its sides)
    float screenRadius = min(Radius / dist * 0.9, 0.06);
    float rot = ign(gl_FragCoord.xy) * 6.2831853;
    float occl = 0.0;
    vec3 bounce = vec3(0.0);
    float total = 0.0;
    for (int i = 0; i < 32; i++) {
        if (i >= Samples) break;
        float t = (float(i) + 0.5) / float(Samples);
        float a = rot + float(i) * 2.39996323;          // golden-angle spiral
        vec2 uv = texCoord + vec2(cos(a), sin(a)) * screenRadius * t;
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) continue;
        vec3 q = viewPos(uv);
        vec3 v = q - p;
        float len = length(v);
        float facing = max(0.0, dot(n, v / max(len, 1e-4)) - 0.05);
        float range = clamp(1.0 - len / (Radius * 2.0), 0.0, 1.0);
        float w = facing * range;
        occl += w;
        bounce += texture(DiffuseSampler, uv).rgb * w;
        total += 1.0;
    }
    if (total < 1.0) { fragColor = vec4(0.0, 0.0, 0.0, 1.0); return; }
    // Shadow: how much of the hemisphere nearby geometry blocks, curved so corners darken and open ground doesn't
    float blocked = clamp(occl / total * 4.5 * Strength, 0.0, 1.0);
    // Near the screen's edges half the samples are missing: fade it out there rather than guess
    vec2 edge = min(texCoord, 1.0 - texCoord);
    blocked *= smoothstep(0.0, 0.05, min(edge.x, edge.y));
    float ao = clamp(1.0 - blocked * 0.75, 0.25, 1.0);
    // Bounce: the average colour of what blocks the sky, lighting this surface, tinted by its own colour
    vec3 albedo = texture(DiffuseSampler, texCoord).rgb;
    vec3 gi = (occl > 0.0 ? bounce / occl : vec3(0.0)) * albedo * blocked * 1.4 * Strength;
    // Sun shadows, in the world: from this pixel's spot toward the sun through the grid of blocks around you, so a tree
    // casts the same shadow wherever you look from (screen-space marching lost whatever went off screen). Leaves let a
    // little through; a jittered start softens the edge. Close up, a short screen-space march adds contact shadows
    // (mobs, players, things the grid doesn't hold).
    float sun = 1.0;
    float reachFade = 1.0 - smoothstep(30.0, 44.0, dist);
    if (SunLight > 0.0 && reachFade > 0.0 && dot(n, SunDir) > 0.02) {
        float shade = 0.0;
        if (VoxReady > 0.5) {
            vec3 off = (ViewToWorld * vec4(p, 0.0)).xyz;
            vec3 nw = normalize((ViewToWorld * vec4(n, 0.0)).xyz);
            vec3 sd = normalize(SunWorld);
            vec3 q = CamMod + off + nw * 0.06;
            ivec3 start = ivec3(floor(q));
            float stepLen = 0.3;
            q += sd * stepLen * ign(gl_FragCoord.xy + 17.0);
            float light = 1.0;
            int steps = ShadowSteps * 2;
            for (int i = 0; i < 96; i++) {
                if (i >= steps || light < 0.05) break;
                q += sd * stepLen;
                vec3 rel = q - CamMod;
                if (abs(rel.x) > 46.0 || abs(rel.z) > 46.0 || rel.y < -22.0 || rel.y > 38.0) break;   // out of the grid
                ivec3 c = ivec3(floor(q));
                if (c == start) continue;
                float o = vox(c);
                if (o > 0.0) light *= 1.0 - (o > 0.99 ? 1.0 : o * 0.55);
            }
            shade = 1.0 - light;
        }
        // Contact: a short march through the depth buffer (a block and a half), for what the grid doesn't hold
        const float REACH = 1.5;
        float stepS = REACH / 8.0;
        vec3 qs = p + n * 0.05 + SunDir * stepS * ign(gl_FragCoord.xy + 31.0);
        for (int i = 0; i < 8; i++) {
            qs += SunDir * stepS;
            if (qs.z > -0.05) break;
            vec2 uv = toScreen(qs);
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) break;
            if (texture(DepthSampler, uv).r >= 0.99999) continue;
            float diff = viewPos(uv).z - qs.z;
            if (diff > 0.05 && diff < 0.5) { shade = max(shade, 0.8 - float(i) / 8.0 * 0.4); break; }
        }
        sun = 1.0 - 0.45 * SunLight * shade * reachFade;
    }
    fragColor = vec4(gi, ao * sun);
}
