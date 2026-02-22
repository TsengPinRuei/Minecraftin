// 宣告 GLSL 的語言版本。
#version 330 core
// 設定或更新變數的值。
layout (location = 0) in vec3 aPosition;

// 下一行程式碼負責執行目前步驟。
uniform mat4 uProjection;
// 下一行程式碼負責執行目前步驟。
uniform mat4 uView;
// 下一行程式碼負責執行目前步驟。
uniform mat4 uModel;

// 下一行程式碼負責執行目前步驟。
void main() {
    // 設定或更新變數的值。
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
}
