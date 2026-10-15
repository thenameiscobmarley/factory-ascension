#version 150
// Ascension Lighting, pass 4: back onto the frame. Mode 4 copies the frame; mode 0 writes the final picture
// (frame x shadow + bounced light + rays); 2 and 3 are developer views.
uniform sampler2D DiffuseSampler;   // blurred pass 1
uniform sampler2D RaySampler;       // pass 3
uniform sampler2D SceneSampler;     // a copy of the frame (mode 4 makes it)
uniform int Mode;
uniform float RaysOn;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 l = texture(DiffuseSampler, texCoord);
    // The frame's own alpha is kept everywhere: the game reads it later, so writing 1 would change the picture
    vec4 sceneTex = texture(SceneSampler, texCoord);
    if (Mode == 4) { fragColor = sceneTex; return; }   // copy the frame
    if (Mode == 0) {
        // The frame, shadowed, plus the bounced light and the rays, in one opaque pass (no blending to go wrong)
        fragColor = vec4(sceneTex.rgb * l.a + l.rgb + texture(RaySampler, texCoord).rgb * RaysOn, sceneTex.a);
    }
    else if (Mode == 1) fragColor = vec4(l.rgb + texture(RaySampler, texCoord).rgb * RaysOn, 1.0);
    else if (Mode == 2) fragColor = vec4(vec3(l.a), 1.0);              // debug: the shadow term alone
    else if (Mode == 5) fragColor = sceneTex;   // debug: the copied frame, unchanged
    else fragColor = vec4(l.rgb * 4.0, 1.0);                           // debug: the bounced light, brightened
}
