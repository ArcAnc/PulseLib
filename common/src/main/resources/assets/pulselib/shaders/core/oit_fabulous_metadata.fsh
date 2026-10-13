#version 150

/*
 * @author ArcAnc
 * Created at: 13.10.2026
 * Copyright (c) 2026
 * Licensed under "Arc's License of Common Sense"; see the root license file.
 */

uniform sampler2D RevealSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

void main()
{
	// Pack without losing R32F depth precision. Minecraft 1.21.1 caches 12 texture units.
	fragColor = vec4(texture(RevealSampler, texCoord).r, texture(DepthSampler, texCoord).r, 0.0, 1.0);
}
