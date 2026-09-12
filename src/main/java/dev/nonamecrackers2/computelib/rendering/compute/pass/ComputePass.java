package dev.nonamecrackers2.computelib.rendering.compute.pass;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.DeviceFeatures;
import com.mojang.blaze3d.systems.DeviceLimits;
import com.mojang.blaze3d.systems.GpuDeviceBackend;

import dev.nonamecrackers2.computelib.rendering.buffers.ExtendedGpuBufferUsage;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceLimitsExtension;

// Handles input validationb before calling the appropriate backend (GL/Vulkan)
public class ComputePass
{
	private final GpuDeviceBackend device;
	private final DeviceLimits deviceLimits;
	private final ComputePassBackend backend;
	
	public ComputePass(GpuDeviceBackend device, ComputePassBackend backend)
	{
		this.device = device;
        this.deviceLimits = device.getDeviceInfo().limits();
		this.backend = backend;
	}
	
	public void setPipeline(ComputePipeline pipeline)
	{
		this.backend.setPipeline(pipeline);
	}
	
	public void setUniform(String name, GpuBuffer value)
	{
		this.backend.setUniform(name, value);
	}

	public void setUniform(String name, GpuBufferSlice value)
	{
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
		DeviceLimitsExtension l = (DeviceLimitsExtension)(Object)this.deviceLimits;
		
		if (groupX > l.maxComputeWorkGroupCountX() || groupY > l.maxComputeWorkGroupCountY() || groupZ > l.maxComputeWorkGroupCountZ())
			throw new IllegalArgumentException("Work group count too large! Wanted: x=" + groupX + ", y=" + groupY + ", z=" + groupZ + "; Max allowed: x=" + l.maxComputeWorkGroupCountX() + ", y=" + l.maxComputeWorkGroupCountY() + ", z=" + l.maxComputeWorkGroupCountZ());
		else if (groupX <= 0 || groupY <= 0 || groupZ <= 0)
			throw new IllegalArgumentException("Work group count must be greater than zero!");
		
		this.backend.dispatch(groupX, groupY, groupZ);
	}
}
