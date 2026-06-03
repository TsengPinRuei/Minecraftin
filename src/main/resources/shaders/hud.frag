#version 330 core
// HUD 片段著色器，直接輸出頂點色；透明度由 OpenGL blending 處理。

in vec4 vColor;
out vec4 FragColor;

void main() {
    FragColor = vColor;
}
