#ifndef PULSELIB_DEFORMERS_GLSL
#define PULSELIB_DEFORMERS_GLSL

uniform samplerBuffer DeformerOperations;
uniform samplerBuffer DeformerValues;

vec4 pulselib_deformer_operation_fetch(int index) {
    return texelFetch(DeformerOperations, index);
}

vec4 pulselib_deformer_value_fetch(int index) {
    return texelFetch(DeformerValues, index);
}

const float PULSELIB_EPSILON = 0.00001;
const int PULSELIB_MAX_DEFORMERS = 8;

vec3 pulselib_rotate(vec3 value, vec3 axis, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return value * c + cross(axis, value) * s + axis * dot(axis, value) * (1.0 - c);
}

float pulselib_value(int valueOffset, int index) {
    return pulselib_deformer_value_fetch(valueOffset + index).x;
}

mat3 pulselib_outer(vec3 column, vec3 row) {
    return mat3(column * row.x, column * row.y, column * row.z);
}

mat3 pulselib_rotation_matrix(vec3 axis, float angle) {
    return mat3(
        pulselib_rotate(vec3(1.0, 0.0, 0.0), axis, angle),
        pulselib_rotate(vec3(0.0, 1.0, 0.0), axis, angle),
        pulselib_rotate(vec3(0.0, 0.0, 1.0), axis, angle)
    );
}

vec3 pulselib_apply_operation(vec3 position, vec4 meta, vec4 p0, vec4 p1, vec4 p2,
                               int valueOffset, inout mat3 jacobian) {
    int type = int(meta.x + 0.5);
    vec3 origin = p0.xyz;
    vec3 relative = position - origin;
    float value0 = meta.y < 0.0 ? 0.0 : pulselib_value(valueOffset, int(meta.y + 0.5));

    if (type == 1) { // stretch
        vec3 axis = p1.xyz;
        float factor = max(value0, PULSELIB_EPSILON);
        jacobian = (mat3(1.0) + pulselib_outer(axis, axis) * (factor - 1.0)) * jacobian;
        return relative + axis * ((factor - 1.0) * dot(relative, axis)) + origin;
    }
    if (type == 2) { // squash
        vec3 axis = p1.xyz;
        float axialScale = max(value0, PULSELIB_EPSILON);
        float radialScale = inversesqrt(axialScale);
        vec3 axial = axis * dot(relative, axis);
        jacobian = (mat3(1.0) * radialScale + pulselib_outer(axis, axis) * (axialScale - radialScale)) * jacobian;
        return axial * axialScale + (relative - axial) * radialScale + origin;
    }
    if (type == 3) { // taper
        vec3 axis = p1.xyz;
        float along = dot(relative, axis);
        float progress = clamp((along + p1.w) / (p0.w + p1.w), 0.0, 1.0);
        float scale = 1.0 + (max(value0, PULSELIB_EPSILON) - 1.0) * progress;
        vec3 axial = axis * along;
        vec3 radial = relative - axial;
        float scaleSlope = along > -p1.w && along < p0.w ?
            (max(value0, PULSELIB_EPSILON) - 1.0) / (p0.w + p1.w) : 0.0;
        mat3 localJacobian = pulselib_outer(axis, axis) +
            (mat3(1.0) - pulselib_outer(axis, axis)) * scale +
            pulselib_outer(radial, axis) * scaleSlope;
        jacobian = localJacobian * jacobian;
        return (relative - axial) * scale + axial + origin;
    }
    if (type == 4) { // twist
        vec3 axis = p1.xyz;
        float along = dot(relative, axis);
        float clampedAlong = clamp(along, -p1.w, p0.w);
        float twist = value0 * clampedAlong / (p0.w + p1.w);
        mat3 rotation = pulselib_rotation_matrix(axis, twist);
        vec3 rotated = rotation * relative;
        float twistSlope = along > -p1.w && along < p0.w ? value0 / (p0.w + p1.w) : 0.0;
        jacobian = (rotation + pulselib_outer(cross(axis, rotated), axis) * twistSlope) * jacobian;
        return rotated + origin;
    }
    if (type == 5) { // bend
        if (abs(value0) < PULSELIB_EPSILON)
            return position;
        vec3 length = p1.xyz;
        vec3 axis = p2.xyz;
        vec3 radial = normalize(cross(axis, length));
        float along = dot(relative, length);
        float radialOffset = dot(relative, radial);
        float axialOffset = dot(relative, axis);
        float clampedAlong = clamp(along, -p1.w, p0.w);
        float curvature = value0 / (p0.w + p1.w);
        float radius = 1.0 / curvature;
        float theta = curvature * clampedAlong;
        vec3 center = radial * radius;
        mat3 rotation = pulselib_rotation_matrix(axis, theta);
        vec3 rotatedCenter = rotation * (center - radial * radialOffset);
        vec3 bent = center - rotatedCenter;
        vec3 tangent = rotation * length;
        float inside = along > -p1.w && along < p0.w ? 1.0 : 0.0;
        vec3 lengthDerivative = (1.0 - inside) * tangent + inside * curvature *
            (-cross(axis, rotatedCenter) + (along - clampedAlong) * cross(axis, tangent));
        mat3 localJacobian = pulselib_outer(lengthDerivative, length) +
            pulselib_outer(rotation * radial, radial) + pulselib_outer(axis, axis);
        jacobian = localJacobian * jacobian;
        return bent + (along - clampedAlong) * tangent + axialOffset * axis + origin;
    }
    if (type == 6) { // wave
        vec3 length = p1.xyz;
        float along = dot(relative, length);
        if (along < -p1.w || along > p0.w)
            return position;
        float phase = meta.z < 0.0 ? 0.0 : pulselib_value(valueOffset, int(meta.z + 0.5));
        float progress = (along + p1.w) / (p0.w + p1.w);
        float envelope = sin(3.14159265359 * progress);
        float angle = 6.28318530718 * along / p2.w + phase;
        float displacementSlope = value0 * (
            3.14159265359 / (p0.w + p1.w) * cos(3.14159265359 * progress) * sin(angle) +
            envelope * cos(angle) * 6.28318530718 / p2.w);
        jacobian = (mat3(1.0) + pulselib_outer(p2.xyz, length) * displacementSlope) * jacobian;
        return position + p2.xyz * (value0 * envelope * sin(angle));
    }
    if (type == 7) { // hinge
        vec3 length = p1.xyz;
        if (dot(relative, length) <= 0.0)
            return position;
        mat3 rotation = pulselib_rotation_matrix(p2.xyz, value0);
        jacobian = rotation * jacobian;
        return rotation * relative + origin;
    }
    return position;
}

void pulselib_apply_deformers(vec3 position, ivec3 instanceDeformer, out vec3 result, out mat3 jacobian) {
    result = position;
    jacobian = mat3(1.0);
    for (int operation = 0; operation < PULSELIB_MAX_DEFORMERS; operation++) {
        if (operation >= instanceDeformer.z)
            break;
        int offset = instanceDeformer.x + operation * 4;
        result = pulselib_apply_operation(result,
            pulselib_deformer_operation_fetch(offset),
            pulselib_deformer_operation_fetch(offset + 1),
            pulselib_deformer_operation_fetch(offset + 2),
            pulselib_deformer_operation_fetch(offset + 3),
            instanceDeformer.y, jacobian);
    }
}

vec3 pulselib_apply_deformers(vec3 position, ivec3 instanceDeformer) {
    vec3 result;
    mat3 ignoredJacobian;
    pulselib_apply_deformers(position, instanceDeformer, result, ignoredJacobian);
    return result;
}

void pulselib_deform_vertex(vec3 position, vec3 normal, ivec3 instanceDeformer,
                            out vec3 deformedPosition, out vec3 deformedNormal) {
    mat3 jacobian;
    pulselib_apply_deformers(position, instanceDeformer, deformedPosition, jacobian);
    vec3 result = cross(jacobian[1], jacobian[2]) * normal.x +
        cross(jacobian[2], jacobian[0]) * normal.y +
        cross(jacobian[0], jacobian[1]) * normal.z;
    deformedNormal = dot(result, result) < 0.000000000001 ? normal : normalize(result);
}

#endif
