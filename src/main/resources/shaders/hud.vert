#version 330 core
// HUD 頂點著色器；HudRenderer 已經直接產生 NDC 座標，因此不需要矩陣 uniform。

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec4 aColor;

out vec4 vColor;

void main() {
    vColor = aColor;
    gl_Position = vec4(aPosition, 1.0);
}
