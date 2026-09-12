package dev.nonamecrackers2.computelib.mixin.rendersystem.vulkan;

import java.util.Set;
import java.util.function.Function;

import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Properties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.vulkan.VulkanDevice;

import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.CompiledComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceFeaturesExtension;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceLimitsExtension;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceBackendExtension;
import net.minecraft.resources.Identifier;

@Mixin(VulkanDevice.class)
public class MixinVulkanDevice implements GpuDeviceBackendExtension
{
	@Shadow @Final
	private DeviceInfo deviceInfo;
	
	@Inject(method = "<init>", at = @At("TAIL"))
	public void simpleclouds$deviceExtension_init(long windowHandle, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions, CallbackInfo ci, @Local VkPhysicalDeviceLimits limits, @Local VkPhysicalDeviceVulkan11Properties properties, @Local Set<String> extensionNames)
	{
		setExtraDeviceFeatures((DeviceFeaturesExtension)(Object)this.deviceInfo.features());
		setExtraDeviceLimits(limits, (DeviceLimitsExtension)(Object)this.deviceInfo.limits());
	}
	
	@Unique
	private static void setExtraDeviceLimits(VkPhysicalDeviceLimits limits, DeviceLimitsExtension ext)
	{
		for (int i = 0; i < 3; i++)
		{
			ext._setMaxComputeWorkGroupCount(i, limits.maxComputeWorkGroupCount(i));
			ext._setMaxComputeWorkGroupSize(i, limits.maxComputeWorkGroupSize(i));
		}
		ext._setMaxComputeWorkGroupInvocations(limits.maxComputeWorkGroupInvocations());
	}
	
	@Unique
	private static void setExtraDeviceFeatures(DeviceFeaturesExtension ext)
	{
		ext._setComputeShaders(true);
	}
	
	@Override
	public void clearComputePipelineCache()
	{
		throw new UnsupportedOperationException("Vulkan not implemented");
	}
	
	@Override
	public CompiledComputePipeline precompileComputePipeline(ComputePipeline pipeline, Function<Identifier, String> shaderSource)
	{
		throw new UnsupportedOperationException("Vulkan not implemented");
	}
}
