package dev.nonamecrackers2.computelib.mixin.rendersystem;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuDeviceBackend;

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
	
	@Override
	public ComputePass createComputePass()
	{
		return new ComputePass(this.device, ((CommandEncoderBackendExtension)this.backend).createComputePass());
	}
}
