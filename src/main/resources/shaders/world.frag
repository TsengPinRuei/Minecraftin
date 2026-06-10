#version 330 core
// 世界方塊的片段著色器：材質取樣、天空光/方塊光合成、AO 面陰影與距離霧。

in vec2 vUv;
in vec3 vLight; // x = 面陰影*AO，y = 天空光 0..1，z = 方塊光 0..1
in vec3 vWorldPos;

uniform sampler2D uAtlas;
uniform vec3 uFogColor;
uniform vec3 uCameraPos;
uniform float uFogNear;
uniform float uFogFar;
uniform float uDayLight; // 晝夜循環提供的天空光倍率，夜晚保留月光下限

out vec4 FragColor;

void main() {
    vec4 texel = texture(uAtlas, vUv);

    // 幾乎透明的像素直接丟棄，避免玻璃或未來 alpha 貼圖仍寫入深度造成遮擋。
    if (texel.a < 0.05) {
        discard;
    }

    // 天空光受晝夜影響、方塊光不受；取兩者較亮者，和 Minecraft 的光照合成一致。
    float skyContribution = vLight.y * uDayLight;
    float brightness = max(skyContribution, vLight.z);

    // 火把類光源帶一點暖色，是 Minecraft 夜景的招牌氛圍。
    float warmth = clamp(vLight.z - skyContribution, 0.0, 1.0) * 0.35;
    vec3 lightTint = mix(vec3(1.0), vec3(1.0, 0.82, 0.58), warmth);

    // 保留少量環境光，讓全黑洞穴仍可勉強辨識輪廓。
    vec3 litColor = texel.rgb * lightTint * vLight.x * (0.05 + 0.95 * brightness);

    float distanceToCamera = distance(vWorldPos, uCameraPos);
    float fogFactor = clamp((distanceToCamera - uFogNear) / max(0.001, (uFogFar - uFogNear)), 0.0, 1.0);
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);

    FragColor = vec4(finalColor, texel.a);
}
