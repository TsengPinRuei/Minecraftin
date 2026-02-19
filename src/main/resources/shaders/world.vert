#version 330 core
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aUv;
layout (location = 2) in float aLight;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

out vec2 vUv;
out float vLight;
out vec3 vWorldPos;

void main() {
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    vUv = aUv;
    vLight = aLight;
    vWorldPos = worldPos.xyz;
    gl_Position = uProjection * uView * worldPos;
}
