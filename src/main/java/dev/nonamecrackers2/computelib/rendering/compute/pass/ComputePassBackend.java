package dev.nonamecrackers2.computelib.rendering.compute.pass;

import java.util.function.Supplier;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;

import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;

public interface ComputePassBackend
{
	void pushDebugGroup(final Supplier<String> label);

    void popDebugGroup();
	
	void setPipeline(ComputePipeline pipeline);
	
	void setUniform(String name, GpuBuffer value);

    void setUniform(String name, GpuBufferSlice value);
    
    //TODO: Image units
    
    void dispatch(int groupX, int groupY, int groupZ);
    
    void memoryBarrier(int srcStage, int srcAccessMask, int dstStage, int dstAccessMask);
}
