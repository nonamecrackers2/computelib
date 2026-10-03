package dev.nonamecrackers2.computelib.mixin.rendersystem.vulkan;

import java.util.function.Supplier;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkMemoryBarrier2;
import org.lwjgl.vulkan.VkMemoryBarrier2.Buffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanDevice;

import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePassBackend;
import dev.nonamecrackers2.computelib.rendering.pipeline.vulkan.VulkanComputePass;
import dev.nonamecrackers2.computelib.rendering.systems.CommandEncoderBackendExtension;

@Mixin(VulkanCommandEncoder.class)
public abstract class MixinVulkanCommandEncoder implements CommandEncoderBackendExtension
{
	@Shadow @Final
	private VulkanDevice device;
	
	//TODO: Use compute queue
	@Shadow
	protected abstract VkCommandBuffer commandBuffer();
	
	@Override
	public ComputePassBackend createComputePass(Supplier<String> label)
	{
		this.device.instance().debug().beginDebugGroup(this.commandBuffer(), label);
		
		return new VulkanComputePass(this.device, (VulkanCommandEncoder)(Object)this, this.commandBuffer());
	}
	
	@Override
	public void submitComputePass()
	{
		this.device.instance().debug().endDebugGroup(this.commandBuffer());
	}
	
	@Override
	public void memoryBarrier(int srcStage, int srcAccessMask, int dstStage, int dstAccessMask)
	{
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			Buffer memoryBarrier = VkMemoryBarrier2.calloc(1, stack).sType$Default();
	        memoryBarrier.srcStageMask(srcStage);
	        memoryBarrier.srcAccessMask(srcAccessMask);
	        memoryBarrier.dstStageMask(dstStage);
	        memoryBarrier.dstAccessMask(dstAccessMask);
	        VkDependencyInfo depInfo = VkDependencyInfo.calloc(stack).sType$Default();
	        depInfo.pMemoryBarriers(memoryBarrier);
	        KHRSynchronization2.vkCmdPipelineBarrier2KHR(this.commandBuffer(), depInfo);
		}
	}
}
