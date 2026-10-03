package dev.nonamecrackers2.computelib.rendering.compute.pass;

import java.util.function.Supplier;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.GpuDeviceBackend;

import dev.nonamecrackers2.computelib.rendering.buffers.ExtendedGpuBufferUsage;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceLimitsExtension;

// Handles input validation before calling the appropriate backend (GL/Vulkan)
public class ComputePass implements AutoCloseable
{
	private final GpuDeviceBackend device;
	private final DeviceLimits deviceLimits;
	private final ComputePassBackend backend;
	private final Runnable onFinish;
	private boolean isClosed;
	private int pushedDebugGroups;
	
	public ComputePass(GpuDeviceBackend device, ComputePassBackend backend, Runnable onFinish)
	{
		this.device = device;
        this.deviceLimits = device.getDeviceInfo().limits();
		this.backend = backend;
		this.onFinish = onFinish;
	}
	
	public void pushDebugGroup(Supplier<String> label)
	{
		if (this.isClosed)
			throw new IllegalStateException("Can't use a closed compute pass");

		this.pushedDebugGroups++;
		this.backend.pushDebugGroup(label);
	}

	public void popDebugGroup()
	{
		if (this.isClosed)
			throw new IllegalStateException("Can't use a closed compute pass");

		if (this.pushedDebugGroups == 0)
			throw new IllegalStateException("Can't pop more debug groups than was pushed!");

		this.pushedDebugGroups--;
		this.backend.popDebugGroup();
	}
	
	public void setPipeline(ComputePipeline pipeline)
	{
		if (this.isClosed)
			throw new IllegalStateException("Can't use a closed compute pass");
		
		this.backend.setPipeline(pipeline);
	}
	
	public void setUniform(String name, GpuBuffer value)
	{
		if (this.isClosed)
			throw new IllegalStateException("Can't use a closed compute pass");
		
		this.backend.setUniform(name, value);
	}

	public void setUniform(String name, GpuBufferSlice value)
	{
		if (this.isClosed)
			throw new IllegalStateException("Can't use a closed compute pass");
		
		int alignment = this.device.getDeviceInfo().limits().minUniformOffsetAlignment();
		//TODO Is this check required for SSBOs?
        if ((value.buffer().usage() & ExtendedGpuBufferUsage.USAGE_SHADER_STORAGE_BUFFER) == 0 && value.offset() % alignment > 0L)
            throw new IllegalArgumentException("Uniform buffer offset must be aligned to " + alignment);
		
		this.backend.setUniform(name, value);
	}
    
	public void dispatch(int group)
	{
		this.dispatch(group, 1, 1);
	}
	
	public void dispatch(int groupX, int groupY)
	{
		this.dispatch(groupX, groupY, 1);
	}
	
	public void dispatch(int groupX, int groupY, int groupZ)
	{
		if (this.isClosed)
			throw new IllegalStateException("Can't use a closed compute pass");
		
		DeviceLimitsExtension l = (DeviceLimitsExtension)(Object)this.deviceLimits;
		
		if (groupX > l.maxComputeWorkGroupCountX() || groupY > l.maxComputeWorkGroupCountY() || groupZ > l.maxComputeWorkGroupCountZ())
			throw new IllegalArgumentException("Work group count too large! Wanted: x=" + groupX + ", y=" + groupY + ", z=" + groupZ + "; Max allowed: x=" + l.maxComputeWorkGroupCountX() + ", y=" + l.maxComputeWorkGroupCountY() + ", z=" + l.maxComputeWorkGroupCountZ());
		else if (groupX <= 0 || groupY <= 0 || groupZ <= 0)
			throw new IllegalArgumentException("Work group count must be greater than zero!");
		
		this.backend.dispatch(groupX, groupY, groupZ);
	}
	
	/**
	 * A memory barrier between the src stage and dst stage with
	 * the required access types
	 * 
	 * @param srcStage
	 * @param srcAccessMask
	 * @param dstStage
	 * @param dstAccessMask
	 */
	public void memoryBarrier(int srcStage, int srcAccessMask, int dstStage, int dstAccessMask)
	{
		if (this.isClosed)
			throw new IllegalStateException("Can't use a closed compute pass");
		
		this.backend.memoryBarrier(srcStage, srcAccessMask, dstStage, dstAccessMask);
	}
	
	@Override
	public void close()
	{
		if (!this.isClosed)
		{
			this.isClosed = true;
			if (this.pushedDebugGroups > 0)
				throw new IllegalStateException("Render pass had debug groups left open!");

			this.onFinish.run();
		}
	}
}
