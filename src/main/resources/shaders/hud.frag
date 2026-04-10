#version 330 core                      // 指定使用 GLSL 3.30 Core 版本來編譯這個片段著色器。

in vec4 vColor;                        // 接收從頂點著色器傳進來的顏色資料，每個像素都會使用這個顏色。
out vec4 FragColor;                    // 宣告片段著色器的輸出變數，最後畫到畫面上的顏色會存到這裡。

void main() {                          // 主函式，GPU 會對每個片段執行這段程式。
    FragColor = vColor;                // 直接把傳入的顏色指定給輸出顏色，讓物件顯示該顏色。
}