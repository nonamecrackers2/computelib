package dev.nonamecrackers2.computelib.rendering.pipeline.vulkan;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRPushDescriptor;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkBufferViewCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuBuffer;
import com.mojang.blaze3d.vulkan.VulkanRenderPass;
import com.mojang.blaze3d.vulkan.VulkanUtils;

import dev.nonamecrackers2.computelib.rendering.buffers.ExtendedGpuBufferUsage;
import dev.nonamecrackers2.computelib.rendering.compute.manager.ComputeShaderSourceManager;
import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;
import dev.nonamecrackers2.computelib.rendering.compute.vulkan.VulkanComputePipeline;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.CommandEncoderBackendExtension;
import dev.nonamecrackers2.computelib.rendering.systems.ExtendedUniformTypes;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceBackendExtension;
import dev.nonamecrackers2.computelib.rendering.systems.vullkan.ExtendedVulkanBindGroupEntryTypes;

//TODO Command buffer using compute queue
public class VulkanComputePass implements ComputePassBackend
{
	protected final VulkanDevice device;
	protected final VulkanCommandEncoder encoder;
	protected final VkCommandBuffer commandBuffer;
	protected final Map<String, GpuBufferSlice> uniforms = new HashMap<>();
	protected final Map<String, GpuBufferSlice> ssbos = new HashMap<>();
	protected @Nullable VulkanComputePipeline pipeline;
	protected boolean anyDescriptorDirty = false;
	protected int pushedDebugGroups = 0;
	
	public VulkanComputePass(VulkanDevice device, VulkanCommandEncoder encoder, VkCommandBuffer commandBuffer)
	{
		this.device = device;
		this.encoder = encoder;
		this.commandBuffer = commandBuffer;
	}
	
	@Override
	public void pushDebugGroup(Supplier<String> label)
	{
		this.pushedDebugGroups++;
        this.device.instance().debug().beginDebugGroup(this.commandBuffer, label);
	}

	@Override
	public void popDebugGroup()
	{
		if (this.pushedDebugGroups == 0)
            throw new IllegalStateException("Can't pop more debug groups than was pushed!");

        this.pushedDebugGroups--;
        this.device.instance().debug().endDebugGroup(this.commandBuffer);
	}
	
	@Override
	public void setPipeline(ComputePipeline pipeline)
	{
		this.pipeline = (VulkanComputePipeline)((GpuDeviceBackendExtension)(Object)this.device).precompileComputePipeline(pipeline, ComputeShaderSourceManager.getInstance()::getComputeShaderSource);
		
		VK12.vkCmdBindPipeline(this.commandBuffer, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, this.pipeline.pipelineId());
	}

	@Override
	public void setUniform(String name, GpuBuffer value)
	{
		this.uniforms.put(name, value.slice());
        this.anyDescriptorDirty = true;
	}

	@Override
	public void setUniform(String name, GpuBufferSlice value)
	{
		this.uniforms.put(name, value);
        this.anyDescriptorDirty = true;
	}

	private void pushDescriptors()
	{
		if (this.anyDescriptorDirty)
		{
			if (VulkanRenderPass.VALIDATION)
			{
				for (BindGroupLayout.UniformDescription uniform : BindGroupLayout.flattenUniforms(this.pipeline.underlying().bindGroupLayouts()))
				{
					GpuBufferSlice value = this.uniforms.get(uniform.name());
					if (value == null)
						throw new IllegalStateException("Missing uniform " + uniform.name() + " (should be " + uniform.type() + ")");

					if ((uniform.type() == UniformType.UNIFORM_BUFFER || uniform.type() == ExtendedUniformTypes.STORAGE_BUFFER) && value.buffer().isClosed())
						throw new IllegalStateException("Uniform buffer " + uniform.name() + " is already closed");

					if (uniform.type() == UniformType.UNIFORM_BUFFER && (value.buffer().usage() & GpuBuffer.USAGE_UNIFORM) == 0)
						throw new IllegalStateException("Uniform buffer " + uniform.name() + " must have GpuBuffer.USAGE_UNIFORM");
					
					if (uniform.type() == ExtendedUniformTypes.STORAGE_BUFFER && (value.buffer().usage() & ExtendedGpuBufferUsage.USAGE_SHADER_STORAGE_BUFFER) == 0)
						throw new IllegalStateException("Storage buffer " + uniform.name() + " must have ExtendedGpuBufferUsage.USAGE_SHADER_STORAGE_BUFFER");
					
					if (uniform.type() == UniformType.TEXEL_BUFFER)
					{
						if (value.offset() != 0L || value.length() != value.buffer().size())
							throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");

						if ((value.buffer().usage() & GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER) == 0)
							throw new IllegalStateException("Uniform texel buffer " + uniform.name() + " must have GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER");

						if (uniform.gpuFormat() == null)
							throw new IllegalStateException("Invalid uniform texel buffer " + uniform.name() + " (missing a texture format)");
					}
				}
			}

			VulkanBindGroupLayout layout = this.pipeline.layout();

			try (MemoryStack stack = MemoryStack.stackPush())
			{
				VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(layout.entries().size(), stack);

				for (int i = 0; i < layout.entries().size(); i++)
				{
					VulkanBindGroupLayout.Entry entry = layout.entries().get(i);
					
					VkWriteDescriptorSet set = writes.get().sType$Default();
					set.dstBinding(i);
					set.dstArrayElement(0);
					set.descriptorCount(1);
					
					if (entry.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.UNIFORM_BUFFER || entry.type() == ExtendedVulkanBindGroupEntryTypes.STORAGE_BUFFER)
					{
						GpuBufferSlice buffer = this.uniforms.get(entry.name());
						if (buffer == null)
							throw new IllegalStateException("Missing uniform " + entry.name() + " (should be " + entry.type() + ")");

						VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
						bufferInfo.buffer(((VulkanGpuBuffer) buffer.buffer()).vkBuffer());
						bufferInfo.offset(buffer.offset());
						bufferInfo.range(buffer.length());
						if (entry.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.UNIFORM_BUFFER)
							set.descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
						else
							set.descriptorType(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
						set.pBufferInfo(bufferInfo);
					} //TODO Support for samplers
//					else if (entry.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.SAMPLED_IMAGE)
//					{
//						VulkanRenderPass.TextureViewAndSampler value = this.textures.get(entry.name());
//						if (value == null)
//						{
//							throw new IllegalStateException("Missing sampler " + entry.name());
//						}
//
//						org.lwjgl.vulkan.VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
//						imageInfo.sampler(value.sampler.vkSampler());
//						imageInfo.imageView(value.view.vkImageView());
//						imageInfo.imageLayout(1);
//						set.descriptorType(1);
//						set.pImageInfo(imageInfo);
//					}
					else if (entry.type() == VulkanBindGroupLayout.VulkanBindGroupEntryType.TEXEL_BUFFER)
					{
						GpuBufferSlice buffer = this.uniforms.get(entry.name());
						if (buffer == null)
							throw new IllegalStateException("Missing uniform " + entry.name() + " (should be " + entry.type() + ")");

						LongBuffer bufferViewPtr = stack.callocLong(1);

						try (MemoryStack s = stack.push())
						{
							assert entry.texelBufferFormat() != null;
							VkBufferViewCreateInfo viewCreateInfo = VkBufferViewCreateInfo.calloc(stack).sType$Default();
							viewCreateInfo.buffer(((VulkanGpuBuffer)buffer.buffer()).vkBuffer());
							viewCreateInfo.offset(buffer.offset());
							viewCreateInfo.range(buffer.length());
							viewCreateInfo.format(VulkanConst.toVk(entry.texelBufferFormat()));
							VulkanUtils.crashIfFailure(this.device, VK12.vkCreateBufferView(this.device.vkDevice(), viewCreateInfo, null, bufferViewPtr), "Couldn't create buffer view for texel buffer");
							long bufferViewHandle = bufferViewPtr.get(0);
							this.encoder.queueForDestroy(() -> VK12.vkDestroyBufferView(this.device.vkDevice(), bufferViewHandle, null));
						}

						set.descriptorType(4);
						set.pTexelBufferView(bufferViewPtr);
					}
				}
				
				KHRPushDescriptor.vkCmdPushDescriptorSetKHR(this.commandBuffer, VK10.VK_PIPELINE_BIND_POINT_COMPUTE, this.pipeline.pipelineLayout(), 0, writes.flip());
			}
			
			this.anyDescriptorDirty = false;
		}
	}
	
	@Override
	public void dispatch(int groupX, int groupY, int groupZ)
	{
		if (this.pipeline == null || !this.pipeline.isValid())
			throw new IllegalStateException("No pipeline set or is invalid");
		
		this.pushDescriptors();
		
		VK10.vkCmdDispatch(this.commandBuffer, groupX, groupY, groupZ);
	}
	
	@Override
	public void memoryBarrier(int srcStage, int srcAccessMask, int dstStage, int dstAccessMask)
	{
		((CommandEncoderBackendExtension)this.encoder).memoryBarrier(srcStage, srcAccessMask, dstStage, dstAccessMask);
	}
}
