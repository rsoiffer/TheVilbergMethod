#version 330

uniform vec4 color;

in vec3 fragColor;
in float fragOcclusion;
in float fragFog;

out vec4 finalColor;

void main() {
    float correctedOcclusion = pow(fragOcclusion, 2.2);
    finalColor = vec4(fragColor * correctedOcclusion, 1) * color;
    finalColor = mix(vec4(.4, .7, 1., 1.), finalColor, fragFog);
}