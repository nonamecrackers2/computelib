package dev.nonamecrackers2.computelib.rendering.compute.gl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL43;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.shaders.UniformType;

import dev.nonamecrackers2.computelib.rendering.systems.ExtendedUniformTypes;
import dev.nonamecrackers2.computelib.rendering.systems.gl.BindingManager;
import net.minecraft.client.renderer.ShaderManager;

public class GlComputeProgram implements AutoCloseable
{
	private static final Logger LOGGER = LogManager.getLogger("computelib/GlComputeProgram");
	public static final GlComputeProgram INVALID_PROGRAM = new GlComputeProgram(-1, "invalid");
	// We can't support simple uniforms since they don't exist in Vulkan
	private final Map<String, Uniform> uniformsByName = new HashMap<>();
	// In OpenGL, SSBO bindings are global instead of per-shader like Vulkan, so we need
	// to keep track of which ones we are using and free them when we are done
	private final Map<String, Integer> ssbosByName = new HashMap<>();
    private final int programId;
    private final String debugLabel;

    private GlComputeProgram(int programId, String debugLabel)
    {
    	this.programId = programId;
    	this.debugLabel = debugLabel;
    }
    
    public static GlComputeProgram link(GlComputeModule shader, String debugLabel) throws ShaderManager.CompilationException
    {
    	int programId = GlStateManager.glCreateProgram();
    	if (programId <= 0)
            throw new ShaderManager.CompilationException("Could not create compute shader program (returned program ID " + programId + ")");
    	
    	if (shader.getShaderId() < 0)
    		throw new ShaderManager.CompilationException("Compiled compute shader module no longer valid (ID " + shader.getShaderId() + ")");
    	
    	GlStateManager.glAttachShader(programId, shader.getShaderId());
    	LOGGER.debug("Attached compiled shader id={} to compute shader id={}", shader.getId(), programId);
    	GlStateManager.glLinkProgram(programId);
    	
    	int status = GlStateManager.glGetProgrami(programId, GL20.GL_LINK_STATUS);
    	if (status == 0)
			throw new ShaderManager.CompilationException("An error occured when linking program containing computer shader " + debugLabel + ". Log output: " + GlStateManager.glGetProgramInfoLog(programId, 32768));
    	
    	return new GlComputeProgram(programId, debugLabel);
    }
    
    public void setupBindGroupLayoutsAndSSBOs(List<BindGroupLayout> layouts)
    {
    	// Portion copied from GlProgram
		BindGroupLayout.ensureCompatible(layouts);
		List<BindGroupLayout.UniformDescription> uniforms = BindGroupLayout.flattenUniforms(layouts);
		List<String> samplers = BindGroupLayout.flattenSamplers(layouts);
		int nextUboBinding = 0;
		int nextSamplerIndex = 0;

		for (BindGroupLayout.UniformDescription uniformDescription : uniforms)
		{
			String uniformName = uniformDescription.name();

			System.out.println(uniformName);
			
			UniformType type = uniformDescription.type();
			if (type == UniformType.UNIFORM_BUFFER)
			{
				int index = GL33C.glGetUniformBlockIndex(this.programId, uniformName);
				if (index != -1)
				{
					int uboBinding = nextUboBinding++;
					GL33C.glUniformBlockBinding(this.programId, index, uboBinding);
					this.uniformsByName.put(uniformName, new Uniform.Ubo(uboBinding));
				}
			}
			else if (type == ExtendedUniformTypes.STORAGE_BUFFER)
			{
				int index = GL43.glGetProgramResourceIndex(this.programId, GL43.GL_SHADER_STORAGE_BLOCK, uniformName);
				if (index != -1)
				{
					int binding = BindingManager.getAvailableShaderStorageBinding();
					GL43.glShaderStorageBlockBinding(this.programId, index, binding);
					this.ssbosByName.put(uniformName, binding);
					BindingManager.useShaderStorageBinding(binding);
				}
			}
			else if (type == UniformType.TEXEL_BUFFER)
			{
				int location = GlStateManager._glGetUniformLocation(this.programId, uniformName);
				if (location == -1)
				{
					LOGGER.warn("{} shader program does not use utb {} defined in the pipeline. This might be a bug.", this.debugLabel, uniformName);
					continue;
				}
				
				int samplerIndex = nextSamplerIndex++;
				this.uniformsByName.put(uniformName, new Uniform.Utb(location, samplerIndex, Objects.requireNonNull(uniformDescription.gpuFormat())));
			}
		}

		for (String sampler : samplers)
		{
			int location = GlStateManager._glGetUniformLocation(this.programId, sampler);
			if (location == -1)
			{
				LOGGER.warn("{} compute shader program does not use sampler {} defined in the pipeline. This might be a bug.", this.debugLabel, sampler);
			}
			else
			{
				int samplerIndex = nextSamplerIndex++;
				this.uniformsByName.put(sampler, new Uniform.Sampler(location, samplerIndex));
			}
		}

		int totalDefinedBlocks = GlStateManager.glGetProgrami(this.programId, 35382);
		for (int i = 0; i < totalDefinedBlocks; i++)
		{
			String name = GL33C.glGetActiveUniformBlockName(this.programId, i);
			if (!this.uniformsByName.containsKey(name))
				LOGGER.warn("Found unknown and unsupported uniform {} in {} compute shader", name, this.debugLabel);
		}
    }
    
	@Override
	public void close()
	{
		//TODO Releasing shader buffer bindings
		this.uniformsByName.values().forEach(Uniform::close);
        GlStateManager.glDeleteProgram(this.programId);
	}
	
	public int getProgramId()
	{
		return this.programId;
	}

	@Override
	public String toString()
	{
		return this.debugLabel;
	}

	public String getDebugLabel()
	{
		return this.debugLabel;
	}

	public Map<String, Uniform> getUniforms()
	{
		return this.uniformsByName;
	}
	
	public Map<String, Integer> getSSBOs()
	{
		return this.ssbosByName;
	}
}
