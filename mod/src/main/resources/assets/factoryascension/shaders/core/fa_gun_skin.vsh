#version 150
// Factory Ascension weapon skins, per pixel (client/gun/GunSkinGpu). The mesh hands over, besides the usual:
// UV0 = the point's x, y on the gun (model units), UV1 = (z * 64, skin number), Color = the gun's own colour there.
#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 baseColor;
out vec4 lightColor;
out float diffuse;
out vec3 objPos;
out vec3 viewNormal;
flat out int skin;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(ModelViewMat, Position, FogShape);
    baseColor = Color;
    lightColor = texelFetch(Sampler2, UV2 / 16, 0);
    diffuse = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.0)).r;
    objPos = vec3(UV0, float(UV1.x) / 64.0);
    viewNormal = Normal;
    skin = UV1.y;
}
