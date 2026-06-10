#version 330 core
// 純色片段著色器；選取框、太陽、月亮與雲層共用，顏色與透明度由 Java 端 uniform 控制。

uniform vec3 uColor;
uniform float uAlpha;
out vec4 FragColor;

void main() {
    FragColor = vec4(uColor, uAlpha);
}
