// 說明：宣告 GLSL 的語言版本。
#version 330 core
// 說明：下一行程式碼負責執行目前步驟。
in vec4 vColor;
// 說明：下一行程式碼負責執行目前步驟。
out vec4 FragColor;

// 說明：下一行程式碼負責執行目前步驟。
void main() {
    // 說明：設定或更新變數的值。
    FragColor = vColor;
}
