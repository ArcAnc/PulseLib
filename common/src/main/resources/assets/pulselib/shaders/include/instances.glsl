#ifndef PULSELIB_INSTANCES_GLSL
#define PULSELIB_INSTANCES_GLSL

// std140: mat4 + vec4 + vec2 + vec2 + ivec4 = 112 bytes, shared with PRhiInstanceLayout.
struct PulseInstance {
    mat4 InstanceModel;
    vec4 InstanceColor;
    vec2 InstanceLight;
    vec2 InstanceOverlay;
    ivec4 InstanceDeformer;
};

layout(std140) uniform InstanceData {
    PulseInstance data[PULSELIB_INSTANCE_CAPACITY];
};

#endif
