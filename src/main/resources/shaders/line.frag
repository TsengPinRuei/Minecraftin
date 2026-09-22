#version 330 core

// Use Java-supplied color and alpha for selection outlines, sky bodies, and clouds.
uniform vec3 uColor;
uniform float uAlpha;
out vec4 FragColor;

void main() {
    FragColor = vec4(uColor, uAlpha);
}
