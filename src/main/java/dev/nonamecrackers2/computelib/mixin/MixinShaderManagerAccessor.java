package dev.nonamecrackers2.computelib.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;

import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

@Mixin(ShaderManager.class)
public interface MixinShaderManagerAccessor
{
	@Invoker("createPreprocessor")
	static GlslPreprocessor computelib$access_createProprocessor(Map<Identifier, Resource> files, Identifier location)
	{
		throw new AssertionError();
	}
}
