#version 330 core
// 線框片段著色器；顏色由 Java 端 uniform 控制。

uniform vec3 uColor;
out vec4 FragColor;

void main() {
    FragColor = vec4(uColor, 1.0);
}
