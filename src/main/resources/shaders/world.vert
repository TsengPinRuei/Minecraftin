#version 330 core

// Keep the eight-float layout aligned with ChunkMesher and WorldRenderer's attributes (3, 2, 3).
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aUv;
// x = face shade times AO; y = sky light; z = block light.
layout (location = 2) in vec3 aLight;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

out vec2 vUv;
out vec3 vLight;
out vec3 vWorldPos;

void main() {
    // Pass world-space positions so fog uses distance from the actual camera position.
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    vUv = aUv;
    vLight = aLight;
    vWorldPos = worldPos.xyz;
    gl_Position = uProjection * uView * worldPos;
}
