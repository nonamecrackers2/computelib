package dev.nonamecrackers2.computelib.rendering.compute.gl;

import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.CompiledComputePipeline;

public record GlComputePipeline(ComputePipeline underlying, GlComputeProgram program) implements CompiledComputePipeline
{
	@Override
	public boolean isValid()
	{
		return this.program != GlComputeProgram.INVALID_PROGRAM;
	}
}
