#version 330 core
// 世界方塊的頂點著色器。
// attribute layout 必須與 ChunkMesher.STRIDE_FLOATS 以及 WorldRenderer 建立 Mesh 時的 (3, 2, 1) 對齊。

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec2 aUv;
layout (location = 2) in float aLight;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

out vec2 vUv;
out float vLight;
out vec3 vWorldPos;

void main() {
    // 先輸出世界座標給 fragment shader，讓霧效可以用真實相機距離計算。
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    vUv = aUv;
    vLight = aLight;
    vWorldPos = worldPos.xyz;
    gl_Position = uProjection * uView * worldPos;
}
