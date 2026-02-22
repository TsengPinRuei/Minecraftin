// 宣告 GLSL 的語言版本。
#version 330 core
// 下一行程式碼負責執行目前步驟。
in vec2 vUv;
// 下一行程式碼負責執行目前步驟。
in float vLight;
// 下一行程式碼負責執行目前步驟。
in vec3 vWorldPos;

// 下一行程式碼負責執行目前步驟。
uniform sampler2D uAtlas;
// 下一行程式碼負責執行目前步驟。
uniform vec3 uFogColor;
// 下一行程式碼負責執行目前步驟。
uniform vec3 uCameraPos;
// 下一行程式碼負責執行目前步驟。
uniform float uFogNear;
// 下一行程式碼負責執行目前步驟。
uniform float uFogFar;

// 下一行程式碼負責執行目前步驟。
out vec4 FragColor;

// 下一行程式碼負責執行目前步驟。
void main() {
    // 宣告並初始化變數。
    vec4 texel = texture(uAtlas, vUv);
    // 根據條件決定是否進入此邏輯分支。
    if (texel.a < 0.05) {
        // 下一行程式碼負責執行目前步驟。
        discard;
    }

    // 宣告並初始化變數。
    vec3 litColor = texel.rgb * (0.22 + 0.78 * vLight);

    // 宣告並初始化變數。
    float distanceToCamera = distance(vWorldPos, uCameraPos);
    // 宣告並初始化變數。
    float fogFactor = clamp((distanceToCamera - uFogNear) / max(0.001, (uFogFar - uFogNear)), 0.0, 1.0);
    // 宣告並初始化變數。
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);

    // 設定或更新變數的值。
    FragColor = vec4(finalColor, texel.a);
}
