#version 330

vec3 OFFSET[6] = vec3[](
    vec3(0., 0., 0.),
    vec3(1., 0., 0.),
    vec3(0., 0., 0.),
    vec3(0., 1., 0.),
    vec3(0., 0., 0.),
    vec3(0., 0., 1.)
);
vec3 NORMAL_TO_DIR1[6] = vec3[](
    vec3(0., 1., 0.),
    vec3(0., 1., 0.),
    vec3(1., 0., 0.),
    vec3(1., 0., 0.),
    vec3(1., 0., 0.),
    vec3(1., 0., 0.)
);
vec3 NORMAL_TO_DIR2[6] = vec3[](
    vec3(0., 0., 1.),
    vec3(0., 0., 1.),
    vec3(0., 0., 1.),
    vec3(0., 0., 1.),
    vec3(0., 1., 0.),
    vec3(0., 1., 0.)
);

float maxFogDist = 1000;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;

layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

in int[] normal;
in vec3[] color;
in vec4[] occlusion;

out vec3 fragColor;
out float fragOcclusion;
out float fragFog;

void main()
{
    vec3 pos = gl_in[0].gl_Position.xyz + OFFSET[normal[0]];
    vec3 dir1 = NORMAL_TO_DIR1[normal[0]];
    vec3 dir2 = NORMAL_TO_DIR2[normal[0]];
    float fog = pow(.01, pow(length(modelViewMatrix * vec4(pos + dir1/2 + dir2/2, 1.)) / maxFogDist, 2));
    mat4 mvp = projectionMatrix * modelViewMatrix;

    if (occlusion[0].x + occlusion[0].z < occlusion[0].y + occlusion[0].w) {
        gl_Position = mvp * vec4(pos, 1);
        fragColor = color[0];
        fragOcclusion = occlusion[0].x;
        fragFog = fog;
        EmitVertex();

        gl_Position = mvp * vec4(pos + dir1, 1.);
        fragColor = color[0];
        fragOcclusion = occlusion[0].y;
        fragFog = fog;
        EmitVertex();

        gl_Position = mvp * vec4(pos + dir2, 1.);
        fragColor = color[0];
        fragOcclusion = occlusion[0].w;
        fragFog = fog;
        EmitVertex();

        gl_Position = mvp * vec4(pos + dir1 + dir2, 1.);
        fragColor = color[0];
        fragOcclusion = occlusion[0].z;
        fragFog = fog;
        EmitVertex();
    }
    else {
        gl_Position = mvp * vec4(pos + dir1, 1.);
        fragColor = color[0];
        fragOcclusion = occlusion[0].y;
        fragFog = fog;
        EmitVertex();

        gl_Position = mvp * vec4(pos, 1);
        fragColor = color[0];
        fragOcclusion = occlusion[0].x;
        fragFog = fog;
        EmitVertex();

        gl_Position = mvp * vec4(pos + dir1 + dir2, 1.);
        fragColor = color[0];
        fragOcclusion = occlusion[0].z;
        fragFog = fog;
        EmitVertex();

        gl_Position = mvp * vec4(pos + dir2, 1.);
        fragColor = color[0];
        fragOcclusion = occlusion[0].w;
        fragFog = fog;
        EmitVertex();
    }

    EndPrimitive();
}