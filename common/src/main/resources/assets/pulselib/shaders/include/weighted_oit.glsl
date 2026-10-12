#ifndef PULSELIB_WEIGHTED_OIT_GLSL
#define PULSELIB_WEIGHTED_OIT_GLSL

const float PULSELIB_OIT_MIN_WEIGHT = 0.01;
const float PULSELIB_OIT_MAX_WEIGHT = 3000.0;
const float PULSELIB_OIT_EPSILON = 0.00001;

float pulselib_oit_weight(float alpha, float depth) {
    float rawWeight = clamp(
        pow(alpha + 0.01, 4.0) * 10000.0 * pow(1.0 - depth * 0.9, 3.0),
        PULSELIB_OIT_MIN_WEIGHT,
        PULSELIB_OIT_MAX_WEIGHT
    );
    return rawWeight;
}

void pulselib_write_weighted_oit(vec4 color, float depth,
                                 out vec4 accumulation, out float revealage) {
    float alpha = clamp(color.a, 0.0, 1.0);
    float weight = pulselib_oit_weight(alpha, depth);
    accumulation = vec4(color.rgb * alpha, alpha) * weight;
    revealage = -log(max(1.0 - alpha, PULSELIB_OIT_EPSILON));
}

#endif
