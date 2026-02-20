// 說明：宣告 GLSL 的語言版本。
#version 330 core
// 說明：設定或更新變數的值。
layout (location = 0) in vec3 aPosition;
// 說明：設定或更新變數的值。
layout (location = 1) in vec2 aUv;
// 說明：設定或更新變數的值。
layout (location = 2) in float aLight;

// 說明：下一行程式碼負責執行目前步驟。
uniform mat4 uProjection;
// 說明：下一行程式碼負責執行目前步驟。
uniform mat4 uView;
// 說明：下一行程式碼負責執行目前步驟。
uniform mat4 uModel;

// 說明：下一行程式碼負責執行目前步驟。
out vec2 vUv;
// 說明：下一行程式碼負責執行目前步驟。
out float vLight;
// 說明：下一行程式碼負責執行目前步驟。
out vec3 vWorldPos;

// 說明：下一行程式碼負責執行目前步驟。
void main() {
    // 說明：宣告並初始化變數。
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    // 說明：設定或更新變數的值。
    vUv = aUv;
    // 說明：設定或更新變數的值。
    vLight = aLight;
    // 說明：設定或更新變數的值。
    vWorldPos = worldPos.xyz;
    // 說明：設定或更新變數的值。
    gl_Position = uProjection * uView * worldPos;
}
