// 說明：宣告 GLSL 的語言版本。
#version 330 core
// 說明：下一行程式碼負責執行目前步驟。
in vec2 vUv;
// 說明：下一行程式碼負責執行目前步驟。
in float vLight;
// 說明：下一行程式碼負責執行目前步驟。
in vec3 vWorldPos;

// 說明：下一行程式碼負責執行目前步驟。
uniform sampler2D uAtlas;
// 說明：下一行程式碼負責執行目前步驟。
uniform vec3 uFogColor;
// 說明：下一行程式碼負責執行目前步驟。
uniform vec3 uCameraPos;
// 說明：下一行程式碼負責執行目前步驟。
uniform float uFogNear;
// 說明：下一行程式碼負責執行目前步驟。
uniform float uFogFar;

// 說明：下一行程式碼負責執行目前步驟。
out vec4 FragColor;

// 說明：下一行程式碼負責執行目前步驟。
void main() {
    // 說明：宣告並初始化變數。
    vec4 texel = texture(uAtlas, vUv);
    // 說明：根據條件決定是否進入此邏輯分支。
    if (texel.a < 0.05) {
        // 說明：下一行程式碼負責執行目前步驟。
        discard;
    }

    // 說明：宣告並初始化變數。
    vec3 litColor = texel.rgb * (0.22 + 0.78 * vLight);

    // 說明：宣告並初始化變數。
    float distanceToCamera = distance(vWorldPos, uCameraPos);
    // 說明：宣告並初始化變數。
    float fogFactor = clamp((distanceToCamera - uFogNear) / max(0.001, (uFogFar - uFogNear)), 0.0, 1.0);
    // 說明：宣告並初始化變數。
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);

    // 說明：設定或更新變數的值。
    FragColor = vec4(finalColor, texel.a);
}
