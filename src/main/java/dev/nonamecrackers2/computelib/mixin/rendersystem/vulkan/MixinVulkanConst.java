package dev.nonamecrackers2.computelib.mixin.rendersystem.vulkan;

import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vulkan.VulkanConst;

import dev.nonamecrackers2.computelib.rendering.buffers.ExtendedGpuBufferUsage;

@Mixin(VulkanConst.class)
public class MixinVulkanConst
{
	@Inject(method = "bufferUsageToVk", at = @At("RETURN"), cancellable = true)
	private static void bufferUsageToVk(int usage, CallbackInfoReturnable<Integer> ci) 
	{
		int flags = ci.getReturnValue();
		
		if ((usage & ExtendedGpuBufferUsage.USAGE_SHADER_STORAGE_BUFFER) != 0)
			flags |= VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
		
		ci.setReturnValue(flags);
	}
}
