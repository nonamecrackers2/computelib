package dev.nonamecrackers2.computelib.mixin.rendersystem.vulkan;

import java.nio.LongBuffer;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding.Buffer;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout.VulkanBindGroupEntryType;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;

import dev.nonamecrackers2.computelib.rendering.systems.vullkan.ExtendedVulkanBindGroupEntryTypes;

@Mixin(VulkanBindGroupLayout.class)
public class MixinVulkanBindGroupLayout
{
	@Inject(method = "create", at = @At("HEAD"), cancellable = true)
	private static void computelib$supportForSSBOs_create(VulkanDevice device, List<VulkanBindGroupLayout.Entry> entries, String name, CallbackInfoReturnable<VulkanBindGroupLayout> ci)
	{
		long layoutHandle;
		
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			Buffer bindings = VkDescriptorSetLayoutBinding.calloc(entries.size(), stack);

			for (int i = 0; i < entries.size(); i++)
			{
				VulkanBindGroupLayout.Entry entry = entries.get(i);
				VulkanBindGroupEntryType type = entry.type();
				
				int typeInt = -1;
				if (type == VulkanBindGroupEntryType.UNIFORM_BUFFER)
					typeInt = VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
				else if (type == ExtendedVulkanBindGroupEntryTypes.STORAGE_BUFFER)
					typeInt = VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
				else if (type == VulkanBindGroupEntryType.SAMPLED_IMAGE)
					typeInt = VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
				else if (type == VulkanBindGroupEntryType.TEXEL_BUFFER)
					typeInt = VK10.VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER;
				
				if (typeInt == -1)
					throw new IllegalArgumentException("Unknown bind group entry type: " + type);
				
				VkDescriptorSetLayoutBinding binding = VkDescriptorSetLayoutBinding.calloc(stack).descriptorType(typeInt).descriptorCount(1).binding(i).stageFlags(17);
				bindings.put(binding);
			}

			bindings.flip();
			VkDescriptorSetLayoutCreateInfo setCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().flags(1).pBindings(bindings);
			LongBuffer pointer = stack.callocLong(1);
			VulkanUtils.crashIfFailure(device, VK12.vkCreateDescriptorSetLayout(device.vkDevice(), setCreateInfo, null, pointer), "Can't set layout for " + name);
			layoutHandle = pointer.get(0);
		}

        ci.setReturnValue(new VulkanBindGroupLayout(layoutHandle, entries));
	}
}
