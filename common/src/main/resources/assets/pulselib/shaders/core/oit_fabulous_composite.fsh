#version 150

/*
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * Licensed under "Arc's License of Common Sense"; see the root license file.
 */

uniform sampler2D AccumSampler;
uniform sampler2D OitMetadataSampler;
uniform sampler2D TranslucentSampler;
uniform sampler2D TranslucentDepthSampler;
uniform sampler2D ItemEntitySampler;
uniform sampler2D ItemEntityDepthSampler;
uniform sampler2D ParticlesSampler;
uniform sampler2D ParticlesDepthSampler;
uniform sampler2D WeatherSampler;
uniform sampler2D WeatherDepthSampler;
uniform sampler2D CloudsSampler;
uniform sampler2D CloudsDepthSampler;

in vec2 texCoord;
out vec4 fragColor;

vec4 frontColors[5];
float frontDepths[5];
int frontCount = 0;

void insertFront(vec4 color, float depth, float oitDepth)
{
	if (color.a == 0.0 || depth >= oitDepth)
	{
		return;
	}
	int index = frontCount++;
	frontColors[index] = color;
	frontDepths[index] = depth;
	while (index > 0 && frontDepths[index] > frontDepths[index - 1])
	{
		vec4 colorSwap = frontColors[index - 1];
		float depthSwap = frontDepths[index - 1];
		frontColors[index - 1] = frontColors[index];
		frontDepths[index - 1] = frontDepths[index];
		frontColors[index] = colorSwap;
		frontDepths[index] = depthSwap;
		index--;
	}
}

void main()
{
	vec4 accumulated = texture(AccumSampler, texCoord);
	vec2 metadata = texture(OitMetadataSampler, texCoord).rg;
	float alpha = clamp(1.0 - exp(-metadata.r), 0.0, 1.0);
	if (alpha <= 0.00001 || accumulated.a <= 0.00001)
	{
		discard;
	}
	float depth = metadata.g;
	insertFront(texture(TranslucentSampler, texCoord), texture(TranslucentDepthSampler, texCoord).r, depth);
	insertFront(texture(ItemEntitySampler, texCoord), texture(ItemEntityDepthSampler, texCoord).r, depth);
	insertFront(texture(ParticlesSampler, texCoord), texture(ParticlesDepthSampler, texCoord).r, depth);
	insertFront(texture(WeatherSampler, texCoord), texture(WeatherDepthSampler, texCoord).r, depth);
	insertFront(texture(CloudsSampler, texCoord), texture(CloudsDepthSampler, texCoord).r, depth);

	vec3 front = vec3(0.0);
	float transmission = 1.0;
	for (int index = 0; index < frontCount; index++)
	{
		front = front * (1.0 - frontColors[index].a) + frontColors[index].rgb;
		transmission *= 1.0 - frontColors[index].a;
	}
	vec3 averageColor = accumulated.rgb / accumulated.a;
	if (any(isnan(averageColor)) || any(isinf(averageColor)))
	{
		averageColor = vec3(0.0);
	}
	// Existing image C = front + transmission * behind.
	// Alpha blending this source inserts OIT behind front without drawing front twice.
	fragColor = vec4(front + transmission * max(averageColor, vec3(0.0)), alpha);
}
