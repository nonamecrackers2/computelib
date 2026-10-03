package dev.nonamecrackers2.computelib.rendering.pipeline.gl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL43;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.Uniform;

import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputePipeline;
import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputeProgram;
import dev.nonamecrackers2.computelib.rendering.compute.manager.ComputeShaderSourceManager;
import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.CommandEncoderBackendExtension;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceBackendExtension;

public class GlComputePass implements ComputePassBackend
{
	protected final GlDevice device;
	protected final GlCommandEncoder encoder;
	protected final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
	protected final Set<String> dirtyUniforms = new HashSet<>();
	protected @Nullable GlComputePipeline pipeline;
	
	public GlComputePass(GlCommandEncoder commandEncoder, GlDevice device)
	{
		this.encoder = commandEncoder;
		this.device = device;
	}
	
	@Override
	public void pushDebugGroup(Supplier<String> label)
	{
		this.device.debugLabels().pushDebugGroup(label);
	}

	@Override
	public void popDebugGroup()
	{
		this.device.debugLabels().popDebugGroup();
	}
	
	@Override
	public void setPipeline(ComputePipeline pipeline)
	{
		this.pipeline = (GlComputePipeline)((GpuDeviceBackendExtension)(Object)this.device).precompileComputePipeline(pipeline, ComputeShaderSourceManager.getInstance()::getComputeShaderSource);
	}

	@Override
	public void setUniform(String name, GpuBuffer value)
	{
		this.uniforms.put(name, value.slice());
        this.dirtyUniforms.add(name);
	}

	@Override
	public void setUniform(String name, GpuBufferSlice value)
	{
		this.uniforms.put(name, value);
        this.dirtyUniforms.add(name);
	}

	@Override
	public void dispatch(int groupX, int groupY, int groupZ)
	{
		//TODO Validation
		
		if (this.pipeline == null || !this.pipeline.isValid())
			throw new IllegalStateException("No pipeline set or is invalid");
		
		GlComputeProgram currentProgram = this.pipeline.program();
		
		GlStateManager._glUseProgram(currentProgram.getProgramId());
		
		// Copied from GlCommandEncoder#trySetup
		for (Map.Entry<String, Uniform> entry : currentProgram.getUniforms().entrySet())
		{
			String name = entry.getKey();
			boolean isDirty = this.dirtyUniforms.contains(name);
			
			switch ((Uniform) entry.getValue())
			{
			case Uniform.Ubo(int blockBinding):
			{
				if (isDirty)
				{
					GpuBufferSlice bufferView = this.uniforms.get(name);
					GL33C.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, blockBinding, ((GlBuffer) bufferView.buffer()).handle(), bufferView.offset(), bufferView.length());
				}
				break;
			}
			case Uniform.Utb(int location, int samplerIndex, GpuFormat format, int texture):
			{
				GlStateManager._glUniform1i(location, samplerIndex);
				
				GlStateManager._activeTexture(33984 + samplerIndex);
				GL33C.glBindTexture(35882, texture);
				if (isDirty)
				{
					GpuBufferSlice bufferView = this.uniforms.get(name);
					GL33C.glTexBuffer(35882, GlConst.toGlInternalId(format), ((GlBuffer) bufferView.buffer()).handle());
				}
				break;
			}
			default:
				throw new MatchException(null, null);
			}
		}
		
		//SSBOs
		for (Map.Entry<String, Integer> ssboEntry : currentProgram.getSSBOs().entrySet())
		{
			String name = ssboEntry.getKey();
			boolean isDirty = this.dirtyUniforms.contains(name);
			
			if (isDirty)
			{
				int binding = ssboEntry.getValue();
				GpuBufferSlice bufferView = this.uniforms.get(name);
				GL33C.glBindBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, binding, ((GlBuffer)bufferView.buffer()).handle(), bufferView.offset(), bufferView.length());
			}
		}
		
		this.dirtyUniforms.clear();
		
		GL43.glDispatchCompute(groupX, groupY, groupZ);
	}

	@Override
	public void memoryBarrier(int srcStage, int srcAccessMask, int dstStage, int dstAccessMask)
	{
		((CommandEncoderBackendExtension)this.encoder).memoryBarrier(srcStage, srcAccessMask, dstStage, dstAccessMask);
	}
}
