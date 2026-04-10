#version 330 core                                  // 指定這支頂點著色器使用 GLSL 3.30 Core 版本。

layout (location = 0) in vec3 aPosition;           // 從頂點屬性位置 0 讀入頂點的三維座標。
layout (location = 1) in vec2 aUv;                 // 從頂點屬性位置 1 讀入頂點的 UV 座標，用來對應貼圖位置。
layout (location = 2) in float aLight;             // 從頂點屬性位置 2 讀入光照強度數值。

uniform mat4 uProjection;                          // 接收投影矩陣，用來把 3D 場景投影到 2D 螢幕上。
uniform mat4 uView;                                // 接收視角矩陣，用來表示攝影機的位置與觀看方向。
uniform mat4 uModel;                               // 接收模型矩陣，用來表示物件本身的平移、旋轉與縮放。

out vec2 vUv;                                      // 宣告輸出變數，將 UV 座標傳給片段著色器。
out float vLight;                                  // 宣告輸出變數，將光照強度傳給片段著色器。
out vec3 vWorldPos;                                // 宣告輸出變數，將世界座標位置傳給片段著色器。

void main() {                                      // 主函式，GPU 會對每一個頂點執行這段程式。
    vec4 worldPos = uModel * vec4(aPosition, 1.0); // 先把頂點座標套用模型矩陣，算出它在世界空間中的位置。
    vUv = aUv;                                     // 把目前頂點的 UV 座標傳給片段著色器。
    vLight = aLight;                               // 把目前頂點的光照強度傳給片段著色器。
    vWorldPos = worldPos.xyz;                      // 取出世界座標的 x、y、z，傳給片段著色器使用。
    gl_Position = uProjection * uView * worldPos;  // 將世界座標再套用視角與投影矩陣，算出頂點在畫面上的最終位置。
}