package dev.nonamecrackers2.computelib.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.shaders.UniformType;

import dev.nonamecrackers2.computelib.ComputeLib;
import dev.nonamecrackers2.computelib.rendering.pipeline.ComputePipeline;
import dev.nonamecrackers2.computelib.rendering.systems.ExtendedUniformTypes;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

//TODO Event driven so users can define
public class ComputeLibTestPipelines
{
	private static final Map<Identifier, ComputePipeline> PIPELINES = new HashMap<>();
	
	public static final ComputePipeline TEST = register(ComputePipeline.builder(ComputeLib.id("compute/test"))
			.withShaderDefines(() -> ShaderDefines.builder().build())
			.withBindGroupLayout(BindGroupLayout.builder().withUniform("Value", UniformType.UNIFORM_BUFFER).withUniform("Out", ExtendedUniformTypes.STORAGE_BUFFER).build())
			.build());
	
	private static ComputePipeline register(ComputePipeline pipeline)
	{
		if (PIPELINES.containsKey(pipeline.id()))
			throw new IllegalArgumentException("Already contains id " + pipeline.id());
		PIPELINES.put(pipeline.id(), pipeline);
		return pipeline;
	}
	
	public static List<ComputePipeline> getPipelines()
	{
		return ImmutableList.copyOf(PIPELINES.values());
	}
	
	public static void bootstrap() {}
}
