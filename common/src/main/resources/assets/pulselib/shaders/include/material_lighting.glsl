#ifndef PULSELIB_MATERIAL_LIGHTING_GLSL
#define PULSELIB_MATERIAL_LIGHTING_GLSL

void pulselib_two_sided_light(vec3 normal, vec3 light0, vec3 light1,
                              out vec4 backColor, out vec4 frontColor) {
    vec4 untinted = vec4(1.0);
    backColor = minecraft_mix_light(light0, light1, -normal, untinted);
    frontColor = minecraft_mix_light(light0, light1, normal, untinted);
}

#endif
