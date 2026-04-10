#version 330 core                                  // 指定這支頂點著色器使用 GLSL 3.30 Core 版本。

layout (location = 0) in vec3 aPosition;           // 從頂點屬性位置 0 讀入頂點的三維座標。

uniform mat4 uProjection;                          // 接收投影矩陣，用來把 3D 場景投影到 2D 螢幕上。
uniform mat4 uView;                                // 接收視角矩陣，用來表示攝影機的位置與觀看方向。
uniform mat4 uModel;                               // 接收模型矩陣，用來表示物件本身的平移、旋轉與縮放。

void main() {                                      // 主函式，GPU 會對每個頂點執行這段程式。
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0); // 依序套用模型、視角、投影轉換，算出頂點在畫面上的最終位置。
}