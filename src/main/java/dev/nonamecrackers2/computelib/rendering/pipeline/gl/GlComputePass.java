package dev.nonamecrackers2.computelib.rendering.pipeline.gl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL43;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.systems.GpuDeviceBackend;

import dev.nonamecrackers2.computelib.mixin.rendersystem.gl.MixinGlCommandEncoder;
import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputePipeline;
import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputeProgram;
import dev.nonamecrackers2.computelib.rendering.compute.manager.ComputeShaderSourceManager;
import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceBackendExtension;

public class GlComputePass implements ComputePassBackend
{
	protected final GpuDeviceBackend device;
	protected final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
	protected final Set<String> dirtyUniforms = new HashSet<>();
	protected @Nullable GlComputePipeline pipeline;
	
	public GlComputePass(GpuDeviceBackend device)
	{
		this.device = device;
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
		
		if (this.pipeline == null)
			throw new IllegalStateException("No pipeline set");
		
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
					GL33C.glBindBufferRange(35345, blockBinding, ((GlBuffer) bufferView.buffer()).handle(), bufferView.offset(), bufferView.length());
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
		
		// Update the binded buffer for an SSBO if it changed
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
}
