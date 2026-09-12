package dev.nonamecrackers2.computelib.rendering.systems;

import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;

public interface CommandEncoderBackendExtension
{
	ComputePassBackend createComputePass();
}
