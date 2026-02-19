#version 330 core
in vec2 vUv;
in float vLight;
in vec3 vWorldPos;

uniform sampler2D uAtlas;
uniform vec3 uFogColor;
uniform vec3 uCameraPos;
uniform float uFogNear;
uniform float uFogFar;

out vec4 FragColor;

void main() {
    vec4 texel = texture(uAtlas, vUv);
    if (texel.a < 0.05) {
        discard;
    }

    vec3 litColor = texel.rgb * (0.22 + 0.78 * vLight);

    float distanceToCamera = distance(vWorldPos, uCameraPos);
    float fogFactor = clamp((distanceToCamera - uFogNear) / max(0.001, (uFogFar - uFogNear)), 0.0, 1.0);
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);

    FragColor = vec4(finalColor, texel.a);
}
