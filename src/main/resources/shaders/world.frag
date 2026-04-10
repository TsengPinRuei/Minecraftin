#version 330 core                                               // 指定這支片段著色器使用 GLSL 3.30 Core 版本。

in vec2 vUv;                                                    // 接收從頂點著色器傳來的 UV 座標，用來決定要取材質貼圖的哪個位置。
in float vLight;                                                // 接收從頂點著色器傳來的光照強度，控制目前片段的明暗。
in vec3 vWorldPos;                                              // 接收目前片段在世界座標中的位置，用來計算與攝影機的距離。

uniform sampler2D uAtlas;                                       // 接收材質圖集貼圖，之後會根據 UV 座標從這張貼圖取樣顏色。
uniform vec3 uFogColor;                                         // 接收霧的顏色，距離越遠的物體會越接近這個顏色。
uniform vec3 uCameraPos;                                        // 接收攝影機在世界座標中的位置。
uniform float uFogNear;                                         // 接收霧開始產生影響的距離。
uniform float uFogFar;                                          // 接收霧完全蓋住物體顏色的距離。

out vec4 FragColor;                                             // 宣告片段著色器的輸出變數，用來存放最後畫到畫面上的顏色。

void main() {                                                   // 主函式，GPU 會對每個片段執行這段程式。
    vec4 texel = texture(uAtlas, vUv);                          // 根據 UV 座標從材質圖集中取出目前片段的顏色與透明度。

    if (texel.a < 0.05) {                                       // 如果取出的像素透明度非常低，表示這個像素幾乎透明。
        discard;                                                // 直接捨棄這個片段，不把它畫到畫面上。
    }

    vec3 litColor = texel.rgb * (0.22 + 0.78 * vLight);         // 將貼圖顏色乘上光照係數，算出受光影響後的顏色。

    float distanceToCamera = distance(vWorldPos, uCameraPos);   // 計算目前片段到攝影機之間的距離。
    float fogFactor = clamp((distanceToCamera - uFogNear) / max(0.001, (uFogFar - uFogNear)), 0.0, 1.0); // 根據距離計算霧的混合比例，並限制在 0 到 1 之間。
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);      // 依照霧的比例，把原本顏色和霧的顏色混合在一起。

    FragColor = vec4(finalColor, texel.a);                      // 將最終顏色和原本的透明度組合後輸出到畫面上。
}