package dev.nonamecrackers2.computelib.mixin.rendersystem.gl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.opengl.GlDevice;

import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputeProgram;
import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;
import dev.nonamecrackers2.computelib.rendering.pipeline.gl.GlComputePass;
import dev.nonamecrackers2.computelib.rendering.systems.CommandEncoderBackendExtension;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceFeaturesExtension;

//TODO Be able to modify SSBOs without creating a compute pass, compute pass should delegate to these more global methods
//TODO Memory barrier extraction, not tied to a compute pass
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class MixinGlCommandEncoder implements CommandEncoderBackendExtension
{
	@Shadow @Final
	private GlDevice device;
	@Unique
	private GlComputeProgram lastComputeProgram;
	
	@Override
	public ComputePassBackend createComputePass()
	{
		if (!((DeviceFeaturesExtension)(Object)this.device.getDeviceInfo().features()).computeShaders())
			throw new UnsupportedOperationException("This device does not support compute shaders");
		
		return new GlComputePass(this.device);
	}
}
