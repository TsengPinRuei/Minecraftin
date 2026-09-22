#version 330 core

in vec2 vUv;
// x = face shade times AO; y = sky light in [0, 1]; z = block light in [0, 1].
in vec3 vLight;
in vec3 vWorldPos;

uniform sampler2D uAtlas;
uniform vec3 uFogColor;
uniform vec3 uCameraPos;
uniform float uFogNear;
uniform float uFogFar;
// Day/night sky-light multiplier, including the night minimum set by WorldRenderer.
uniform float uDayLight;

out vec4 FragColor;

// Combine atlas color, baked face/AO shading, sky and block light, and distance fog.
void main() {
    vec4 texel = texture(uAtlas, vUv);

    // Discard transparent cutout texels, such as ladder and torch gaps, before writing depth.
    if (texel.a < 0.05) {
        discard;
    }

    // Scale sky light by time of day, keep block light unchanged, and use the brighter
    // contribution.
    float skyContribution = vLight.y * uDayLight;
    float brightness = max(skyContribution, vLight.z);

    // Tint toward warm light wherever block light exceeds sky light, regardless of emitter type.
    float warmth = clamp(vLight.z - skyContribution, 0.0, 1.0) * 0.35;
    vec3 lightTint = mix(vec3(1.0), vec3(1.0, 0.82, 0.58), warmth);

    // Keep a small ambient-light floor so dark cave geometry remains faintly visible.
    vec3 litColor = texel.rgb * lightTint * vLight.x * (0.05 + 0.95 * brightness);

    float distanceToCamera = distance(vWorldPos, uCameraPos);
    float fogFactor = clamp((distanceToCamera - uFogNear) / max(0.001, (uFogFar - uFogNear)), 0.0, 1.0);
    vec3 finalColor = mix(litColor, uFogColor, fogFactor);

    FragColor = vec4(finalColor, texel.a);
}
