package dev.nonamecrackers2.computelib.rendering.compute.manager;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;

import dev.nonamecrackers2.computelib.mixin.MixinShaderManagerAccessor;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceExtension;
import dev.nonamecrackers2.computelib.test.ComputeLibTestPipelines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Manages compiled shader programs from source .glsl files
 * in the shaders/compute directory. Similar to {@link ShaderManager}
 */
public class ComputeShaderSourceManager extends SimplePreparableReloadListener<Map<Identifier, String>>
{
	private static final Logger LOGGER = LogManager.getLogger("computelib/ComputeShaderManager");
	public static final String EXTENSION = ".comp";
	public static final String DIRECTORY = "shaders";
	public static final FileToIdConverter CONVERTER = new FileToIdConverter(DIRECTORY, EXTENSION);
	private static @Nullable ComputeShaderSourceManager instance;
	private Map<Identifier, String> cache = new HashMap<>();
	
	// Read and apply preprocessing (i.e. resolve #include directives) for compute shader source files
	@Override
	protected Map<Identifier, String> prepare(ResourceManager manager, ProfilerFiller profiler)
	{
		ImmutableMap.Builder<Identifier, String> sourceMap = ImmutableMap.builder();
		
		Map<Identifier, Resource> shaderFiles = manager.listResources(DIRECTORY, ComputeShaderSourceManager::isPossibleShader);
		
		for (var entry : shaderFiles.entrySet())
		{
			Identifier fullPath = entry.getKey();
			if (!isComputeShader(fullPath))
				continue;
			
			Identifier id = CONVERTER.fileToId(fullPath);
			GlslPreprocessor processor = MixinShaderManagerAccessor.computelib$access_createProprocessor(shaderFiles, fullPath);
			
			try (Reader reader = entry.getValue().openAsReader()) {
				sourceMap.put(id, String.join("", processor.process(IOUtils.toString(reader))));
			} catch (IOException e) {
				LOGGER.error("Failed to load compute shader source file at {}", fullPath, e);
			}
		}
		
		return sourceMap.build();
	}

	// Compile the shader source files
	@Override
	protected void apply(Map<Identifier, String> preparations, ResourceManager manager, ProfilerFiller profiler)
	{
		List<ComputePipeline> availablePipelines = ComputeLibTestPipelines.getPipelines();
		List<Identifier> failedLoads = new ArrayList<>();
			
		GpuDeviceExtension device = GpuDeviceExtension.get();
		device.clearComputePipelineCache();
		
		for (ComputePipeline pipeline : availablePipelines)
		{
			if (!device.precompileComputePipeline(pipeline, preparations::get).isValid())
				failedLoads.add(pipeline.id());
			else
				LOGGER.debug("Loaded compute pipeline {}", pipeline.id());
		}

		if (!failedLoads.isEmpty())
		{
			device.clearComputePipelineCache();
			LOGGER.error("Failed to pre-compiled compute programs:\n" + failedLoads.stream().map(entry -> " - " + entry).collect(Collectors.joining("\n")));
			//TODO Catch for error screen?
			this.cache = new HashMap<>();
		}
		else
		{
			this.cache = preparations;
		}
	}
	
	@Override
	public String getName()
	{
		return "Compute Shader Manager";
	}
	
	public @Nullable String getComputeShaderSource(Identifier id)
	{
		return this.cache.get(id);
	}
	
	// We include other shader files for #include directives
	private static boolean isPossibleShader(Identifier loc)
	{
		String path = loc.getPath();
		return path.endsWith(EXTENSION) || ShaderType.byLocation(loc) != null || path.endsWith(".glsl");
	}
	
	private static boolean isComputeShader(Identifier loc)
	{
		return loc.getPath().endsWith(EXTENSION);
	}
	
	public static ComputeShaderSourceManager getInstance()
	{
		if (instance == null)
			instance = new ComputeShaderSourceManager();
		return instance;
	}
}
