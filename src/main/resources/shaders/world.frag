#version 330 core
// 世界方塊的片段著色器，負責材質取樣、簡易面亮度與距離霧。

in vec2 vUv;
in float vLight;
in vec3 vWorldPos;

uniform sampler2D uAtlas;
uniform vec3 uFogColor;
uniform vec3 uCameraPos;
uniform float uFogNear;
uniform float uFogFar;

out vec4 FragColor;

void main() {
    vec4 texel = texture(uAtlas, vUv);

    // 幾乎透明的像素直接丟棄，避免玻璃或未來 alpha 貼圖仍寫入深度造成遮擋。
    if (texel.a < 0.05) {
        discard;
    }

    // vLight 是 mesher 依面向寫入的簡易亮度；保留 0.22 的環境光，避免陰面全黑。
    vec3 litColor = texel.rgb * (0.22 + 0.78 * vLight);

    float distanceToCamera = distance(vWorldPos, uCameraPos);
    float fogFactor = clamp((distanceToCamera - uFogNear) / max(0.001, (uFogFar - uFogNear)), 0.0, 1.0);
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);

    FragColor = vec4(finalColor, texel.a);
}
