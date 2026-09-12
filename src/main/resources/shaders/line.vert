#version 330 core
// 純色幾何共用的頂點著色器：選取框、日月與雲層。

layout (location = 0) in vec3 aPosition;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
}
