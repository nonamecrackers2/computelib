package dev.nonamecrackers2.computelib.rendering.systems;

import com.mojang.blaze3d.systems.GpuDeviceBackend;

import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePass;
import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;

public interface CommandEncoderExtension
{
	ComputePass createComputePass();
}
