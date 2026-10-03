package dev.nonamecrackers2.computelib.rendering.compute;

import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

public record ComputeCompilationKey(Identifier id, ShaderDefines defines) 
{
	@Override
	public final String toString()
	{
		String str = this.id + " (compute)";
		return !this.defines.isEmpty() ? str + " with " + this.defines : str;
	}
}
