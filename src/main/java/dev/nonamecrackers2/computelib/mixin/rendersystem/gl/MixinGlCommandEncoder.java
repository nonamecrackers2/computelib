package dev.nonamecrackers2.computelib.mixin.rendersystem.gl;

import java.util.function.Supplier;

import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlDevice;

import dev.nonamecrackers2.computelib.rendering.compute.MemoryBarrier;
import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputeProgram;
import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;
import dev.nonamecrackers2.computelib.rendering.pipeline.gl.GlComputePass;
import dev.nonamecrackers2.computelib.rendering.systems.CommandEncoderBackendExtension;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceFeaturesExtension;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class MixinGlCommandEncoder implements CommandEncoderBackendExtension
{
	@Shadow @Final
	private GlDevice device;
	@Unique
	private GlComputeProgram lastComputeProgram;
	
	@Override
	public ComputePassBackend createComputePass(Supplier<String> label)
	{
		if (!((DeviceFeaturesExtension)(Object)this.device.getDeviceInfo().features()).computeShaders())
			throw new UnsupportedOperationException("This device does not support compute shaders");
		
		this.device.debugLabels().pushDebugGroup(label);
		
		return new GlComputePass((GlCommandEncoder)(Object)this, this.device);
	}
	
	@Override
	public void submitComputePass()
	{
		this.device.debugLabels().popDebugGroup();
	}
	
	@Override
	public void memoryBarrier(int srcStage, int srcAccessMask, int dstStage, int dstAccessMask)
	{
		int mask = 0;
		
		if ((srcStage & MemoryBarrier.Stage.ALL_COMMANDS) != 0 || (dstStage & MemoryBarrier.Stage.ALL_COMMANDS) != 0 || (srcStage & MemoryBarrier.Stage.ALL_GRAPHICS) != 0 || (srcStage & MemoryBarrier.Stage.ALL_GRAPHICS) != 0)
		{
			mask = GL42.GL_ALL_BARRIER_BITS;
//			System.out.println("All barrier bits");
		}
		else
		{
			if ((srcAccessMask & MemoryBarrier.Access.UNIFORM_READ) != 0 || (dstAccessMask & MemoryBarrier.Access.UNIFORM_READ) != 0)
			{
				mask |= GL42.GL_UNIFORM_BARRIER_BIT;
//				System.out.println("Uniforms");
			}
			if ((srcAccessMask & MemoryBarrier.Access.SHADER_READ) != 0 || (dstAccessMask & MemoryBarrier.Access.SHADER_READ) != 0 || (srcAccessMask & MemoryBarrier.Access.SHADER_WRITE) != 0 || (dstAccessMask & MemoryBarrier.Access.SHADER_WRITE) != 0)
			{
				mask |= GL43.GL_SHADER_STORAGE_BARRIER_BIT;
//				System.out.println("SSBOs");
			}
			if ((srcAccessMask & MemoryBarrier.Access.TRANSFER_READ) != 0 || (dstAccessMask & MemoryBarrier.Access.TRANSFER_READ) != 0 || (srcAccessMask & MemoryBarrier.Access.TRANSFER_WRITE) != 0 || (dstAccessMask & MemoryBarrier.Access.TRANSFER_WRITE) != 0)
			{
				mask |= GL42.GL_BUFFER_UPDATE_BARRIER_BIT;
//				System.out.println("Transfers");
			}
			if ((srcAccessMask & MemoryBarrier.Access.HOST_READ) != 0 || (dstAccessMask & MemoryBarrier.Access.HOST_READ) != 0 || (srcAccessMask & MemoryBarrier.Access.HOST_WRITE) != 0 || (dstAccessMask & MemoryBarrier.Access.HOST_WRITE) != 0)
			{
				mask = mask | GL42.GL_BUFFER_UPDATE_BARRIER_BIT | GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
//				System.out.println("Host");
			}
			if ((srcAccessMask & MemoryBarrier.Access.MEMORY_READ) != 0 || (dstAccessMask & MemoryBarrier.Access.MEMORY_READ) != 0 || (srcAccessMask & MemoryBarrier.Access.MEMORY_WRITE) != 0 || (dstAccessMask & MemoryBarrier.Access.MEMORY_WRITE) != 0)
			{
				mask = GL42.GL_ALL_BARRIER_BITS;
//				System.out.println("Memory (all barrier bits)");
			}
		}
		
		GL42.glMemoryBarrier(mask);
	}
}
