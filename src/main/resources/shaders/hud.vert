// 說明：宣告 GLSL 的語言版本。
#version 330 core
// 說明：設定或更新變數的值。
layout (location = 0) in vec3 aPosition;
// 說明：設定或更新變數的值。
layout (location = 1) in vec4 aColor;

// 說明：下一行程式碼負責執行目前步驟。
out vec4 vColor;

// 說明：下一行程式碼負責執行目前步驟。
void main() {
    // 說明：設定或更新變數的值。
    vColor = aColor;
    // 說明：設定或更新變數的值。
    gl_Position = vec4(aPosition, 1.0);
}
