package dev.nonamecrackers2.computelib.mixin.rendersystem.vulkan;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import com.mojang.blaze3d.vulkan.glsl.SpvSampler;
import com.mojang.blaze3d.vulkan.glsl.SpvUniformBuffer;

import dev.nonamecrackers2.computelib.rendering.compute.vulkan.GlslCompilerExtension;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.ExtendedUniformTypes;
import dev.nonamecrackers2.computelib.rendering.systems.vullkan.ExtendedVulkanBindGroupEntryTypes;
import dev.nonamecrackers2.computelib.rendering.systems.vullkan.IntermediaryComputeShaderModule;
import dev.nonamecrackers2.computelib.rendering.systems.vullkan.VulkanBindGroupLayoutHelper;
import net.minecraft.client.renderer.ShaderDefines;

@Mixin(GlslCompiler.class)
public class MixinGlslCompiler implements GlslCompilerExtension
{
	@Shadow @Final
	private long shaderCompiler;
	@Shadow @Final
    private long shaderOptions;
	@Shadow @Final
    private ShaderDefines globalDefines;
	
	// Copy of GlslCompiler#createIntermediary but explicitly for compute shaders
	@Override
	public IntermediaryComputeShaderModule createComputeIntermediary(String fileName, String source) throws ShaderCompileException
	{
		source = GlslPreprocessor.injectDefines(source, this.globalDefines);
		ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, false);
		ByteBuffer filenameBuffer = MemoryUtil.memUTF8(fileName);
		ByteBuffer entrypointBuffer = MemoryUtil.memUTF8("main");
		long result = Shaderc.shaderc_compile_into_spv(this.shaderCompiler, sourceBuffer, Shaderc.shaderc_compute_shader, filenameBuffer, entrypointBuffer, this.shaderOptions);

		try
		{
			int status = Shaderc.shaderc_result_get_compilation_status(result);
			if (status != 0)
				throw new ShaderCompileException("Couldn't parse GLSL: " + Shaderc.shaderc_result_get_error_message(result));

			ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
			ByteBuffer copy = MemoryUtil.memCalloc(spirv.remaining());
			MemoryUtil.memCopy(spirv, copy);
			return IntermediaryComputeShaderModule.createFromSpirv(fileName, copy);
		}
		finally
		{
			Shaderc.shaderc_result_release(result);
			MemoryUtil.memFree(entrypointBuffer);
			MemoryUtil.memFree(filenameBuffer);
			MemoryUtil.memFree(sourceBuffer);
		}
	}
	
	@Override
	public CompiledModule compileCompute(VulkanDevice device, ComputePipeline pipeline, IntermediaryComputeShaderModule shader) throws ShaderCompileException
	{
		String pipelineName = pipeline.id().toString();
        List<VulkanBindGroupLayout.Entry> entries = new ArrayList<>();
        addComputeToBindGroup(entries, shader, pipeline);
        shader.rebind(entries); // Rebinds the binding IDs for all buffers in the shader code in the order provided by "entries"
        long shaderId = shader.createVulkanShaderModule(device);
        VulkanBindGroupLayout layout = VulkanBindGroupLayoutHelper.createForCompute(device, entries, pipelineName);
        return new CompiledModule(shaderId, layout);
	}
	
	@Unique
	private static void addComputeToBindGroup(List<VulkanBindGroupLayout.Entry> entries, IntermediaryComputeShaderModule shader, ComputePipeline pipeline) throws ShaderCompileException
	{
		List<BindGroupLayout.UniformDescription> flattened = BindGroupLayout.flattenUniforms(pipeline.bindGroupLayouts());
		
		for (SpvUniformBuffer buffer : shader.buffers())
		{
			String name = buffer.name();
			Optional<BindGroupLayout.UniformDescription> uniformDescription = flattened.stream().filter(d -> d.name().equals(name)).findFirst();
			if (uniformDescription.isEmpty())
				throw new ShaderCompileException("Unable to find shader defined uniform (" + name + ")");

			Optional<VulkanBindGroupLayout.Entry> entry = entries.stream().filter(e -> e.name().equals(name)).findFirst();
			if (entry.isPresent())
				continue; // Already defined
			
			UniformType type = uniformDescription.get().type();
			VulkanBindGroupLayout.VulkanBindGroupEntryType vulkanUniformType;
			if (type == UniformType.UNIFORM_BUFFER)
				vulkanUniformType = VulkanBindGroupLayout.VulkanBindGroupEntryType.UNIFORM_BUFFER;
			else if (type == ExtendedUniformTypes.STORAGE_BUFFER)
				vulkanUniformType = ExtendedVulkanBindGroupEntryTypes.STORAGE_BUFFER;
			else
				throw new ShaderCompileException("Unknown uniform type: " + type);
			
			entries.add(new VulkanBindGroupLayout.Entry(vulkanUniformType, name, null));
		}

		for (SpvSampler sampler : shader.samplers())
		{
			String name = sampler.name();
			Optional<BindGroupLayout.UniformDescription> uniformDescription = BindGroupLayout.flattenUniforms(pipeline.bindGroupLayouts()).stream().filter(d -> d.name().equals(name)).findFirst();
			if (uniformDescription.isPresent())
			{
				if (sampler.dimensions() != 5)
					throw new ShaderCompileException("UTB (" + name + ") must have type of SpvDimBuffer");

				if (entries.stream().noneMatch(e -> e.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.TEXEL_BUFFER && e.name().equals(name)))
					entries.add(new VulkanBindGroupLayout.Entry(VulkanBindGroupLayout.VulkanBindGroupEntryType.TEXEL_BUFFER, name, uniformDescription.get().gpuFormat()));
			}
			else
			{
				if (BindGroupLayout.flattenSamplers(pipeline.bindGroupLayouts()).stream().noneMatch(name::equals))
					throw new ShaderCompileException("Unable to find shader defined uniform (" + name + ")");

				if (sampler.dimensions() != 1 && sampler.dimensions() != 3)
					throw new ShaderCompileException("Sampled texture (" + name + ") must have type of SpvDim2D or SpvDimCube");

				if (entries.stream().noneMatch(e -> e.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.SAMPLED_IMAGE && e.name().equals(name)))
					entries.add(new VulkanBindGroupLayout.Entry(VulkanBindGroupLayout.VulkanBindGroupEntryType.SAMPLED_IMAGE, name, null));
			}
		}
	}
}
