package dev.nonamecrackers2.computelib.mixin.rendersystem.vulkan;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanInstance;
import com.mojang.blaze3d.vulkan.VulkanPhysicalDevice;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import com.mojang.blaze3d.vulkan.checkpoints.CheckpointExtension;
import com.mojang.blaze3d.vulkan.glsl.GlslCompiler;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;

import dev.nonamecrackers2.computelib.rendering.compute.ComputeCompilationKey;
import dev.nonamecrackers2.computelib.rendering.compute.vulkan.GlslCompilerExtension;
import dev.nonamecrackers2.computelib.rendering.compute.vulkan.GlslCompilerExtension.CompiledModule;
import dev.nonamecrackers2.computelib.rendering.compute.vulkan.VulkanComputePipeline;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceFeaturesExtension;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceLimitsExtension;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceBackendExtension;
import dev.nonamecrackers2.computelib.rendering.systems.vullkan.IntermediaryComputeShaderModule;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLEnvironment;

//TODO debug output for if we have dedicated compute queue or not
@Mixin(VulkanDevice.class)
public class MixinVulkanDevice implements GpuDeviceBackendExtension
{
	@Unique
	private static final Logger LOGGER = LogManager.getLogger("computelib/VulkanDeviceEXT");
	@Shadow @Final
	private DeviceInfo deviceInfo;
	@Shadow @Final
	private GlslCompiler glslCompiler;
	@Shadow @Final
	private VulkanQueue computeQueue;
	@Unique
	private final Map<ComputePipeline, VulkanComputePipeline> computePipelineCache = new IdentityHashMap<>();
	@Unique
	private final Map<ComputeCompilationKey, IntermediaryComputeShaderModule> computeSourceCache = new HashMap<>();
	
	@Unique
	private VulkanDevice _cast()
	{
		return (VulkanDevice)(Object)this;
	}
	
	@Inject(method = "<init>", at = @At("TAIL"))
	public void computelib$deviceExtension_init(ShaderSource defaultShaderSource, VulkanInstance instance, VulkanPhysicalDevice physicalDevice, Set<String> enabledDeviceExtensions, VkDevice device, long vma, CheckpointExtension checkpointExtension, CallbackInfo ci, @Local VkPhysicalDeviceLimits limits)
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
	
	@Inject(method = "close", at = @At("HEAD"))
	public void computelib$closeExtension_close(CallbackInfo ci)
	{
		this.clearComputePipelineCache();
	}
	
	@Override
	public void clearComputePipelineCache()
	{
//		this.computeQueue.waitIdle();
		this.computePipelineCache.values().forEach(VulkanComputePipeline::destroy);
		this.computePipelineCache.clear();
		this.computeSourceCache.values().forEach(IntermediaryComputeShaderModule::close);
		this.computeSourceCache.clear();
	}
	
	@Override
	public VulkanComputePipeline precompileComputePipeline(ComputePipeline pipeline, Function<Identifier, String> shaderSource)
	{
        return this.computePipelineCache.computeIfAbsent(pipeline, p -> this.compileComputePipeline(p, shaderSource));
	}
	
	@Unique
	private VulkanComputePipeline compileComputePipeline(ComputePipeline pipeline, Function<Identifier, String> shaderSource)
	{
		ShaderDefines defines = pipeline.shaderDefines().get();
		if (defines == null)
		{
			LOGGER.warn("Compute pipeline gave null ShaderDefines, should be at least empty");
			defines = ShaderDefines.EMPTY;
		}
		
		IntermediaryComputeShaderModule shader = this.getOrCompileComputeShader(pipeline.shaderLocation(), defines, shaderSource);
		
		if (shader == IntermediaryComputeShaderModule.INVALID)
		{
			LOGGER.error("Couldn't compile pipeline {}: compute shader {} was invalid", pipeline.id(), pipeline.shaderLocation());
			return new VulkanComputePipeline(pipeline, this._cast(), 0L, 0L, VulkanBindGroupLayout.INVALID_LAYOUT, 0L);
		}
		
		try
		{
			CompiledModule module = ((GlslCompilerExtension)this.glslCompiler).compileCompute(this._cast(), pipeline, shader);
			return VulkanComputePipeline.compile(this._cast(), module.layout(), pipeline, module.shaderId());
		}
		catch (ShaderCompileException e)
		{
			LOGGER.error("Couldn't compile compute pipeline {}: {}", pipeline.id(), e.getMessage());
			return new VulkanComputePipeline(pipeline, this._cast(), 0L, 0L, VulkanBindGroupLayout.INVALID_LAYOUT, 0L);
		}
	}
	
	@Unique
	private IntermediaryComputeShaderModule getOrCompileComputeShader(Identifier id, ShaderDefines defines, Function<Identifier, String> shaderSource)
	{
		ComputeCompilationKey key = new ComputeCompilationKey(id, defines);
		return this.computeSourceCache.computeIfAbsent(key, k -> this.compileComputeShader(k, shaderSource));
	}
	
	@Unique
	private IntermediaryComputeShaderModule compileComputeShader(ComputeCompilationKey key, Function<Identifier, String> shaderSource)
	{
		 String source = shaderSource.apply(key.id());
		if (source == null)
		{
			LOGGER.error("Couldn't find source for compute shader ({})", key.id());
			return IntermediaryComputeShaderModule.INVALID;
		}

		String sourceWithDefines = GlslPreprocessor.injectDefines(source, key.defines());
		
		try
		{
			return ((GlslCompilerExtension)this.glslCompiler).createComputeIntermediary(key.id().toDebugFileName(), sourceWithDefines);
		}
		catch (ShaderCompileException e)
		{
			if (!FMLEnvironment.isProduction())
				LOGGER.error("Couldn't compile compute shader " + key.id() + ":", e);
			else
				LOGGER.error("Couldn't compile compute shader {}: {}", key.id(), e.getMessage());
			return IntermediaryComputeShaderModule.INVALID;
		}
	}
}
