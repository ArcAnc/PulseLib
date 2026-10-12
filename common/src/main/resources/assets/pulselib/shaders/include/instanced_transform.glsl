#ifndef PULSELIB_INSTANCED_TRANSFORM_GLSL
#define PULSELIB_INSTANCED_TRANSFORM_GLSL

mat4 pulselib_instance_matrix(vec4 row0, vec4 row1, vec4 row2) {
    return mat4(
        vec4(row0.x, row1.x, row2.x, 0.0),
        vec4(row0.y, row1.y, row2.y, 0.0),
        vec4(row0.z, row1.z, row2.z, 0.0),
        vec4(row0.w, row1.w, row2.w, 1.0)
    );
}

vec4 pulselib_transform_position(vec3 position, ivec3 instanceDeformer,
                                 vec4 row0, vec4 row1, vec4 row2,
                                 mat4 modelView, mat4 projection) {
    mat4 instanceMatrix = pulselib_instance_matrix(row0, row1, row2);
    vec3 deformedPosition = pulselib_apply_deformers(position, instanceDeformer);
    return projection * modelView * instanceMatrix * vec4(deformedPosition, 1.0);
}

void pulselib_transform_vertex(vec3 position, vec3 normal, ivec3 instanceDeformer,
                               vec4 row0, vec4 row1, vec4 row2,
                               mat4 modelView, mat4 projection,
                               out vec4 clipPosition, out vec3 transformedNormal) {
    mat4 instanceMatrix = pulselib_instance_matrix(row0, row1, row2);
    vec3 deformedPosition;
    vec3 deformedNormal;
    pulselib_deform_vertex(position, normal, instanceDeformer, deformedPosition, deformedNormal);
    clipPosition = projection * modelView * instanceMatrix * vec4(deformedPosition, 1.0);
    transformedNormal = normalize(transpose(inverse(mat3(instanceMatrix))) * deformedNormal);
}

#endif
