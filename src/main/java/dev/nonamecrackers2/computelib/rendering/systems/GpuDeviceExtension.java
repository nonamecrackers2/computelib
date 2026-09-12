package dev.nonamecrackers2.computelib.rendering.systems;

import java.util.function.Function;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import net.minecraft.resources.Identifier;

public interface GpuDeviceExtension
{
	void clearComputePipelineCache();
	
	CompiledComputePipeline precompileComputePipeline(ComputePipeline pipeline, Function<Identifier, String> shaderSource);
	
	@SuppressWarnings("unchecked")
	default <T extends CommandEncoder & CommandEncoderExtension> T createExtendedCommandEncoder()
	{
		return (T)(Object)get().createCommandEncoder();
	}
	
	@SuppressWarnings("unchecked")
	static <T extends GpuDevice & GpuDeviceExtension> T get()
	{
		return (T)(Object)RenderSystem.getDevice();
	}
}
