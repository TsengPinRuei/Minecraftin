#version 330 core                          // 指定這支片段著色器使用 GLSL 3.30 Core 版本。

uniform vec3 uColor;                       // 接收從 Java 程式傳進來的顏色值，包含紅、綠、藍三個分量。
out vec4 FragColor;                        // 宣告片段著色器的輸出變數，用來存放最後輸出的像素顏色。

void main() {                              // 主函式，GPU 會對每個片段執行這段程式。
    FragColor = vec4(uColor, 1.0);         // 把 uColor 補上透明度 1.0，組成 RGBA 顏色後輸出。
}