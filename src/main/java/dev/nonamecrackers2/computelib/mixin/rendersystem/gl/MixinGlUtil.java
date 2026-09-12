package dev.nonamecrackers2.computelib.mixin.rendersystem.gl;

import org.lwjgl.opengl.GL43;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.opengl.GlUtil;

import dev.nonamecrackers2.computelib.rendering.buffers.ExtendedGpuBufferUsage;

@Mixin(GlUtil.class)
public class MixinGlUtil
{
	@Inject(method = "selectBufferBindTarget", at = @At("HEAD"), cancellable = true)
	private static void computelib$ssboSupport_selectBufferBindTarget(int usage, CallbackInfoReturnable<Integer> ci)
	{
		if ((usage & ExtendedGpuBufferUsage.USAGE_SHADER_STORAGE_BUFFER) != 0) // SSBO usage flag
			ci.setReturnValue(GL43.GL_SHADER_STORAGE_BUFFER);
	}
}
