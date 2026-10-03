package dev.nonamecrackers2.computelib.rendering.compute.vulkan;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import com.mojang.blaze3d.vulkan.Destroyable;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;

import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.CompiledComputePipeline;

public record VulkanComputePipeline(ComputePipeline underlying, VulkanDevice device, long pipelineId, long pipelineLayout, VulkanBindGroupLayout layout, long shaderModule) implements CompiledComputePipeline, Destroyable
{
	public static VulkanComputePipeline compile(VulkanDevice device, VulkanBindGroupLayout layout, ComputePipeline pipeline, long shaderModule)
	{
		long pipelineLayout;
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			VkPipelineLayoutCreateInfo createInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default().pSetLayouts(stack.longs(layout.handle()));
			LongBuffer pointer = stack.callocLong(1);
			VulkanUtils.crashIfFailure(device, VK12.vkCreatePipelineLayout(device.vkDevice(), createInfo, null, pointer), "Can't create pipeline for " + pipeline.id());
			pipelineLayout = pointer.get(0);
			device.instance().debug().setObjectName(device.vkDevice(), VK12.VK_OBJECT_TYPE_PIPELINE_LAYOUT, pipelineLayout, () -> "Pipeline layout for " + pipeline.id());
		}
		
		
		try (MemoryStack stack = MemoryStack.stackPush()) 
		{
			ByteBuffer nameMain = stack.UTF8("main");
			VkPipelineShaderStageCreateInfo stage = VkPipelineShaderStageCreateInfo.calloc(stack).sType$Default().stage(VK11.VK_SHADER_STAGE_COMPUTE_BIT).module(shaderModule).pName(nameMain);
			
			VkComputePipelineCreateInfo.Buffer createInfo = VkComputePipelineCreateInfo.calloc(1, stack)
					.sType$Default()
					.flags(0)
					.stage(stage)
					.layout(pipelineLayout);
			
			LongBuffer pointer = stack.callocLong(1);
			VulkanUtils.crashIfFailure(device, VK12.vkCreateComputePipelines(device.vkDevice(), 0L, createInfo, null, pointer), "Can't compile compute pipeline " + pipeline.id());
			long pipelineId = pointer.get(0);
			device.instance().debug().setObjectName(device.vkDevice(), VK12.VK_OBJECT_TYPE_PIPELINE, pipelineId, () -> "Compute Pipeline " + pipeline.id());
			
			return new VulkanComputePipeline(pipeline, device, pipelineId, pipelineLayout, layout, shaderModule);
		}
	}
	
	@Override
	public boolean isValid()
	{
		return this.pipelineId != 0L;
	}

	@Override
	public void destroy()
	{
		if (this.pipelineId != 0L)
		{
			VK12.vkDestroyPipeline(this.device.vkDevice(), this.pipelineId, null);
			VK12.vkDestroyPipelineLayout(this.device.vkDevice(), this.pipelineLayout, null);
			VK12.vkDestroyDescriptorSetLayout(this.device.vkDevice(), this.layout.handle(), null);
			VK12.vkDestroyShaderModule(this.device.vkDevice(), this.shaderModule, null);
		}
	}
}
