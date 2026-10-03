package dev.nonamecrackers2.computelib.rendering.pipeline;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.mojang.blaze3d.pipeline.BindGroupLayout;

import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

/**
 * "id" not entirely used/useful, just an identifier
 * 
 * Shader defines is a supplier since new defines will be wanted
 * every shader reload
 */
public record ComputePipeline(Identifier id, Identifier shaderLocation, Supplier<ShaderDefines> shaderDefines, List<BindGroupLayout> bindGroupLayouts)
{
	public static ComputePipeline.Builder builder(Identifier id)
	{
		return new Builder(id, id);
	}
	
	public static ComputePipeline.Builder builder(Identifier id, Identifier shaderLocation)
	{
		return new Builder(id, shaderLocation);
	}
	
	public static class Builder
	{
		private final Identifier id;
		private final Identifier shaderLocation;
		private Supplier<ShaderDefines> shaderDefines = () -> ShaderDefines.EMPTY;
		private List<BindGroupLayout> bindGroupLayouts = Lists.newArrayList();
		
		private Builder(Identifier id, Identifier shaderLocation)
		{
			this.id = id;
			this.shaderLocation = shaderLocation;
		}
		
		public ComputePipeline.Builder withShaderDefines(Supplier<ShaderDefines> shaderDefines)
		{
			this.shaderDefines = shaderDefines;
			return this;
		}
		
		public ComputePipeline.Builder withBindGroupLayout(BindGroupLayout layout)
		{
			this.bindGroupLayouts.add(layout);
			return this;
		}
		
		public ComputePipeline build()
		{
			return new ComputePipeline(this.id, this.shaderLocation, this.shaderDefines, ImmutableList.copyOf(this.bindGroupLayouts));
		}
	}
}
