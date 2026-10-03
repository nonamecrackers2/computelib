package dev.nonamecrackers2.computelib.rendering.systems;

import java.util.function.Supplier;

import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;

public interface CommandEncoderBackendExtension
{
	ComputePassBackend createComputePass(Supplier<String> label);
	
	void submitComputePass();
	
	void memoryBarrier(int srcStage, int srcAccessMask, int dstStage, int dstAccessMask);
}
