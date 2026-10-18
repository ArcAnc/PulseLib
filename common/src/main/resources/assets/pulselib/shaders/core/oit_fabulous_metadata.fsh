#version 150

uniform sampler2D RevealSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

void main()
{
	fragColor = vec4(texture(RevealSampler, texCoord).r, texture(DepthSampler, texCoord).r, 0.0, 1.0);
}
