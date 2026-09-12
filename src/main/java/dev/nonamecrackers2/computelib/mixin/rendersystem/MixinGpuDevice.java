package dev.nonamecrackers2.computelib.mixin.rendersystem;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.sun.jdi.request.InvalidRequestStateException;

import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.CompiledComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceBackendExtension;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceExtension;
import net.minecraft.resources.Identifier;

@Mixin(GpuDevice.class)
public class MixinGpuDevice implements GpuDeviceExtension
{
	@Shadow @Final
	private GpuDeviceBackend backend;
	
	@Override
	public void clearComputePipelineCache()
	{
		this.tryCast().clearComputePipelineCache();
	}
	
	@Override
	public CompiledComputePipeline precompileComputePipeline(ComputePipeline pipeline, Function<Identifier, String> shaderSource)
	{
		return this.tryCast().precompileComputePipeline(pipeline, shaderSource);
	}
	
	@Unique
	private GpuDeviceBackendExtension tryCast()
	{
		if (this.backend instanceof GpuDeviceBackendExtension ext)
			return ext;
		else
			throw new InvalidRequestStateException("Current GPU device backend does not support ComputeLib extensions");
	}
}
