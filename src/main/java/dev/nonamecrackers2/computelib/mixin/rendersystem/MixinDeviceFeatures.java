package dev.nonamecrackers2.computelib.mixin.rendersystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.systems.DeviceFeatures;

import dev.nonamecrackers2.computelib.rendering.systems.DeviceFeaturesExtension;

@Mixin(DeviceFeatures.class)
public class MixinDeviceFeatures implements DeviceFeaturesExtension
{
	@Unique
	private boolean computeShaders;

	@Override
	public boolean computeShaders()
	{
		return this.computeShaders;
	}

	@Override
	public void _setComputeShaders(boolean flag)
	{
		this.computeShaders = flag;
	}
}
