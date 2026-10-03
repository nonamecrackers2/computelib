package dev.nonamecrackers2.computelib.mixin.rendersystem;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.TracyGpuProfiler;

import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePass;
import dev.nonamecrackers2.computelib.rendering.systems.CommandEncoderBackendExtension;
import dev.nonamecrackers2.computelib.rendering.systems.CommandEncoderExtension;

@Mixin(CommandEncoder.class)
public class MixinCommandEncoder implements CommandEncoderExtension
{
	@Shadow @Final
	private GpuDeviceBackend device;
	@Shadow @Final
	private CommandEncoderBackend backend;
	@Shadow @Final
	private @Nullable TracyGpuProfiler profiler;
	@Unique
	private boolean isInComputePass;
	
	@Override
	public boolean isInComputePass()
	{
		return this.isInComputePass;
	}
	
	@Override
	public ComputePass createComputePass(Supplier<String> label)
	{
		if (this.isInComputePass)
            throw new IllegalStateException("Close the existing compute pass before creating a new one!");
		
		this.isInComputePass = true;
		
		if (this.profiler != null)
            this.profiler.pushZone((CommandEncoder)(Object)this, label.get());
		
		return new ComputePass(this.device, ((CommandEncoderBackendExtension)this.backend).createComputePass(label), this::submitComputePass);
	}
	
	@Override
	public void submitComputePass()
	{
		if (!this.isInComputePass)
            throw new IllegalStateException("Can't submit a compute pass if one isn't open");
		
		this.isInComputePass = false;
		
		((CommandEncoderBackendExtension)this.backend).submitComputePass();
		
		if (this.profiler != null)
            this.profiler.popZone((CommandEncoder)(Object)this);
	}
	
	@Override
	public void memoryBarrier(int srcStage, int srcAccessMask, int dstStage, int dstAccessMask)
	{
		((CommandEncoderBackendExtension)this.backend).memoryBarrier(srcStage, srcAccessMask, dstStage, dstAccessMask);
	}
}
