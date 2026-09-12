package dev.nonamecrackers2.computelib.mixin.rendersystem.gl;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlDebugLabel;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.DeviceInfo;

import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputeModule;
import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputePipeline;
import dev.nonamecrackers2.computelib.rendering.compute.gl.GlComputeProgram;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceFeaturesExtension;
import dev.nonamecrackers2.computelib.rendering.systems.DeviceLimitsExtension;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceBackendExtension;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public class MixinGlDevice implements GpuDeviceBackendExtension
{
	@Unique
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/MixinGlDevice-EXT");
	@Shadow @Final
	private DeviceInfo deviceInfo;
	@Shadow @Final
	private GlDebugLabel debugLabels;
	@Unique
	private final Map<ComputePipeline, GlComputePipeline> computePipelineCache = new IdentityHashMap<>();
	@Unique
	private final Map<GlComputeCompilationKey, GlComputeModule> computeSourceCache = new HashMap<>();
	
	@Inject(method = "<init>", at = @At("TAIL"))
	public void simpleclouds$deviceExtension_init(long windowHandle, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions, CallbackInfo ci, @Local GLCapabilities capabilities, @Local Set<String> enabledExtensions)
	{
		if (capabilities.GL_ARB_compute_shader)
			enabledExtensions.add("GL_ARB_compute_shader");
		
		setExtraDeviceFeatures(capabilities, (DeviceFeaturesExtension)(Object)this.deviceInfo.features());
		setExtraDeviceLimits(capabilities.GL_ARB_compute_shader, (DeviceLimitsExtension)(Object)this.deviceInfo.limits());
	}
	
	@Unique
	private static void setExtraDeviceLimits(boolean hasCompute, DeviceLimitsExtension ext)
	{
		if (hasCompute)
		{
			for (int i = 0; i < 3; i++)
			{
				ext._setMaxComputeWorkGroupCount(i, GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, i));
				ext._setMaxComputeWorkGroupSize(i, GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, i));
			}
			ext._setMaxComputeWorkGroupInvocations(GL11.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS));
		}
	}
	
	@Unique
	private static void setExtraDeviceFeatures(GLCapabilities capabilities, DeviceFeaturesExtension ext)
	{
		ext._setComputeShaders(capabilities.GL_ARB_compute_shader);
	}
	
	@Inject(method = "close", at = @At("TAIL"))
	public void simpleclouds$closeExtension_close(CallbackInfo ci)
	{
		this.clearComputePipelineCache();
	}
	
	@Override
	public void clearComputePipelineCache()
	{
		for (GlComputePipeline pipeline : this.computePipelineCache.values())
		{
			if (pipeline.program() != GlComputeProgram.INVALID_PROGRAM)
				pipeline.program().close();
		}
		this.computePipelineCache.clear();

		for (GlComputeModule shader : this.computeSourceCache.values())
		{
			if (shader != GlComputeModule.INVALID_SHADER)
				shader.close();
		}
		this.computeSourceCache.clear();
		
		//TODO Needed?
//		String glRenderer = GlStateManager._getString(7937);
//		if (glRenderer.contains("AMD"))
//			sacrificeShaderToOpenGlAndAmd();
	}
	
	@Override
	public GlComputePipeline precompileComputePipeline(ComputePipeline pipeline, Function<Identifier, String> shaderSource)
	{
		//TODO verify capabilities
		return this.computePipelineCache.computeIfAbsent(pipeline, p -> this.compileComputePipeline(p, shaderSource));
	}
	
	// Internal
	
	@Unique
	private GlComputePipeline compileComputePipeline(ComputePipeline pipeline, Function<Identifier, String> shaderSource)
	{
		return new GlComputePipeline(pipeline, this.compileComputeProgram(pipeline, shaderSource));
	}
	
	@Unique
	private GlComputeProgram compileComputeProgram(ComputePipeline pipeline, Function<Identifier, String> shaderSource)
	{
		ShaderDefines defines = pipeline.shaderDefines().get();
		if (defines == null)
		{
			LOGGER.warn("Compute pipeline gave null ShaderDefines, should be at least empty");
			defines = ShaderDefines.EMPTY;
		}
		
		GlComputeModule source = this.getOrCompileComputeShader(pipeline.shaderLocation(), pipeline.shaderDefines().get(), shaderSource);
		if (source == GlComputeModule.INVALID_SHADER)
		{
			LOGGER.error("Couldn't compile pipeline {}: shader {} was invalid", pipeline.id(), source.getId());
			return GlComputeProgram.INVALID_PROGRAM;
		}
		
		try 
		{
			GlComputeProgram program = GlComputeProgram.link(source, pipeline.id().toString());
			program.setupBindGroupLayoutsAndSSBOs(pipeline.bindGroupLayouts(), pipeline.ssbosByBinding());
			//TODO Debug label
			return program;
		} 
		catch (IllegalArgumentException e) 
		{
            LOGGER.error("Couldn't compile program for compute pipeline {}: {}", pipeline.id(), e.getMessage());
            return GlComputeProgram.INVALID_PROGRAM;
        } 
		catch (ShaderManager.CompilationException e) 
		{
            LOGGER.error("Couldn't compile program for compute pipeline {}: {}", pipeline.id(), e);
            return GlComputeProgram.INVALID_PROGRAM;
        }
	}
	
	@Unique
	private GlComputeModule getOrCompileComputeShader(Identifier id, ShaderDefines defines, Function<Identifier, String> shaderSource)
	{
		var key = new GlComputeCompilationKey(id, defines);
		return this.computeSourceCache.computeIfAbsent(key, k -> this.compileComputeShader(key, shaderSource));
	}
	
	@Unique
	private GlComputeModule compileComputeShader(GlComputeCompilationKey key, Function<Identifier, String> shaderSource)
	{
		String sourceCode = shaderSource.apply(key.id());
		if (sourceCode == null)
		{
			LOGGER.error("Couldn't find source for compute shader {}", key.id());
			return GlComputeModule.INVALID_SHADER;
		}
		
		String sourceWithDefines = GlslPreprocessor.injectDefines(sourceCode, key.defines);
		int shaderId = GlStateManager.glCreateShader(GL43.GL_COMPUTE_SHADER);
		GlStateManager.glShaderSource(shaderId, sourceWithDefines);
		GlStateManager.glCompileShader(shaderId);
		if (GlStateManager.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0)
		{
			String error = StringUtils.trim(GL20.glGetShaderInfoLog(shaderId, 32768));
			LOGGER.error("Couldn't compile compute shader {}: {}", key.id, error);
			return GlComputeModule.INVALID_SHADER;
		}
		
		//TODO Debug labels
		return new GlComputeModule(key.id(), shaderId);
	}
			
	private static record GlComputeCompilationKey(Identifier id, ShaderDefines defines)
	{
		@Override
		public final String toString()
		{
			String str = this.id + " (compute)";
			return !this.defines.isEmpty() ? str + " with " + this.defines : str;
		}
	}
}
