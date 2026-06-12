#version 330 core
// HUD 片段著色器；取樣方塊材質圖集再乘上頂點色。
// 純色幾何（面板、文字）取樣圖集中的純白格，因此輸出等同頂點色。

in vec4 vColor;
in vec2 vUV;

uniform sampler2D uAtlas;

out vec4 FragColor;

void main() {
    FragColor = texture(uAtlas, vUV) * vColor;
}
