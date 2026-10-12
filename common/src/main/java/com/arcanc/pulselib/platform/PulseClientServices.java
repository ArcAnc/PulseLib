package com.arcanc.pulselib.platform;

public final class PulseClientServices
{
	public static final IPulseCameraService CAMERA = PulseServices.load(IPulseCameraService.class);

	private PulseClientServices() { }
}
