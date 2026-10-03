package dev.nonamecrackers2.computelib.rendering.compute.vulkan;

import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;

import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.vullkan.IntermediaryComputeShaderModule;

public interface GlslCompilerExtension
{
	IntermediaryComputeShaderModule createComputeIntermediary(String fileName, String source) throws ShaderCompileException;
	
	CompiledModule compileCompute(VulkanDevice device, ComputePipeline pipeline, IntermediaryComputeShaderModule shader) throws ShaderCompileException;
	
	public static record CompiledModule(long shaderId, VulkanBindGroupLayout layout) {}
}
