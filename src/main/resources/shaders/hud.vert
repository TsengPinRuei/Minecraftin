#version 330 core                                  // 指定這支著色器使用 GLSL 3.30 Core 版本。

layout (location = 0) in vec3 aPosition;           // 從頂點屬性位置 0 讀入頂點座標，型別是三維向量。
layout (location = 1) in vec4 aColor;              // 從頂點屬性位置 1 讀入頂點顏色，型別是四維向量。

out vec4 vColor;                                   // 宣告輸出變數，將頂點顏色傳給片段著色器使用。

void main() {                                      // 主函式，GPU 會對每一個頂點執行這段程式。
    vColor = aColor;                               // 把目前頂點的顏色存到 vColor，讓後續片段著色器接收。
    gl_Position = vec4(aPosition, 1.0);            // 把三維座標補上一個 w 值 1.0，轉成四維齊次座標後輸出到裁剪空間。
}