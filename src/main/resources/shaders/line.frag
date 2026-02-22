// 宣告 GLSL 的語言版本。
#version 330 core
// 下一行程式碼負責執行目前步驟。
uniform vec3 uColor;
// 下一行程式碼負責執行目前步驟。
out vec4 FragColor;

// 下一行程式碼負責執行目前步驟。
void main() {
    // 設定或更新變數的值。
    FragColor = vec4(uColor, 1.0);
}
