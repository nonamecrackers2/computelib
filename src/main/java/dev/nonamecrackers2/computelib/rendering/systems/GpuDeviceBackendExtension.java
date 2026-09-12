package dev.nonamecrackers2.computelib.rendering.systems;

import java.util.function.Function;

import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import net.minecraft.resources.Identifier;

public interface GpuDeviceBackendExtension
{
	void clearComputePipelineCache();
	
	CompiledComputePipeline precompileComputePipeline(ComputePipeline pipeline, Function<Identifier, String> shaderSource);
}
