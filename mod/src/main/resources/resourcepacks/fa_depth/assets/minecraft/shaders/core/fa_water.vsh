#version 150
// Factory Ascension: translucent blocks as the game draws them, except water, whose top rises and falls with the
// waves (include/fa_waves.glsl). FaWater = camera position mod 256 (x, z), unused, time in seconds; set every frame by
// client/render/Waves.java. Before that's ever set it's all zero: still water, as the game's.
#moj_import <light.glsl>
#moj_import <fog.glsl>
#moj_import <fa_waves.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;
uniform vec4 FaWater;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightColor;
out vec2 texCoord0;
out vec3 faPos;
out vec2 faWorld;
out float faWaterTop;

void main() {
    vec3 pos = Position + ChunkOffset;
    // Not wrapped here: FaWater is already the camera mod 256, and a wrap inside one quad would squash a wave cycle
    faWorld = pos.xz + FaWater.xz;
    faWaterTop = 0.0;
    if (fa_is_water(Color) && Normal.y > 0.5 && FaWater.w > 0.0) {
        pos.y += fa_wave(faWorld, FaWater.w).x;
        faWaterTop = 1.0;
    }
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    vertexDistance = fog_distance(ModelViewMat, pos, FogShape);
    lightColor = minecraft_sample_lightmap(Sampler2, UV2);
    vertexColor = Color * lightColor;
    texCoord0 = UV0;
    faPos = pos;
}
