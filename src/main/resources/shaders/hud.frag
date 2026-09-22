#version 330 core

in vec4 vColor;
in vec2 vUV;

uniform sampler2D uAtlas;

out vec4 FragColor;

// Multiply atlas samples by vertex color; solid HUD geometry samples the white tile.
void main() {
    FragColor = texture(uAtlas, vUV) * vColor;
}
