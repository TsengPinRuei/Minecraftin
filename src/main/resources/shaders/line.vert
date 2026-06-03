#version 330 core
// 線框頂點著色器，用於被瞄準方塊的 selection outline。

layout (location = 0) in vec3 aPosition;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
}
