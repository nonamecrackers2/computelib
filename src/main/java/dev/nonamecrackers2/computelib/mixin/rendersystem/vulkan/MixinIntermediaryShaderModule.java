package dev.nonamecrackers2.computelib.mixin.rendersystem.vulkan;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.spvc.SpvcReflectedResource;
import org.lwjgl.util.spvc.SpvcReflectedResource.Buffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import com.mojang.blaze3d.vulkan.glsl.SpvUniformBuffer;

@Mixin(IntermediaryShaderModule.class)
public abstract class MixinIntermediaryShaderModule// implements IntermediaryShaderModuleExtension
{
//	@Unique
//	private static @Nullable List<SpvShaderStorageBuffer> currentCreateShaderStorageBuffers;
//	@Unique
//	private List<SpvShaderStorageBuffer> shaderStorageBuffers = new ArrayList<>();
	
	@Shadow
	private static void throwIfError(int result, String message) throws ShaderCompileException
	{
		throw new AssertionError();
	}
	
	@Shadow
	private static int getDecorationOffset(long compiler, SpvcReflectedResource resource, int decoration, IntBuffer returnBuffer) throws ShaderCompileException
	{
		throw new AssertionError();
	}
	
	@Inject(method = "createFromSpirv", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vulkan/glsl/IntermediaryShaderModule;throwIfError(ILjava/lang/String;)V", ordinal = 7, shift = At.Shift.BY, by = -2))
	private static void computelib$supportForSSBOs_findSSBOs_createFromSpirv(String filename, ByteBuffer spirv, CallbackInfoReturnable<IntermediaryShaderModule> ci, @Local(ordinal = 0) List<SpvUniformBuffer> uniformBuffers, @Local(ordinal = 0) PointerBuffer pointer, @Local(ordinal = 0) IntBuffer intReturnBuffer, @Local(ordinal = 2) long compiler, @Local(ordinal = 3) long spvcResources, @Local(ordinal = 1) PointerBuffer countPointer) throws ShaderCompileException
	{
//		if (currentCreateShaderStorageBuffers != null)
//			throw new AssertionError();
//		
//		currentCreateShaderStorageBuffers = new ArrayList<>();
//		
		throwIfError(Spvc.spvc_resources_get_resource_list_for_type(spvcResources, Spvc.SPVC_RESOURCE_TYPE_STORAGE_BUFFER, pointer, countPointer), "Couldn't list shader storage buffers");
        long spvcListAddress = pointer.get(0);
        long resourceCount = countPointer.get(0);
        Buffer buffer = SpvcReflectedResource.create(spvcListAddress, (int)resourceCount);

        for (int i = 0; i < resourceCount; i++) 
        {
            SpvcReflectedResource resource = buffer.get(i);
            String name = resource.nameString();
            int bindingOffset = getDecorationOffset(compiler, resource, 33, intReturnBuffer);
            uniformBuffers.add(new SpvUniformBuffer(name, bindingOffset));
            System.out.println("Found resource: " + name + ", binding: " + bindingOffset);
//            currentCreateShaderStorageBuffers.add(new SpvUniformBuffer(name, bindingOffset));
        }
	}
	
//	@Inject(method = "createFromSpirv", at = @At("RETURN"))
//	private static void computelib$supportForSSBOs_create_createFromSpirv(String filename, ByteBuffer spirv, CallbackInfoReturnable<IntermediaryShaderModule> ci)
//	{
//		if (currentCreateShaderStorageBuffers == null)
//			throw new AssertionError();
//		
//		((MixinIntermediaryShaderModule)(Object)ci.getReturnValue()).shaderStorageBuffers = currentCreateShaderStorageBuffers;
//		currentCreateShaderStorageBuffers = null;
//	}
//	
//	@Override
//	public List<SpvShaderStorageBuffer> getShaderStorageBuffers()
//	{
//		return this.shaderStorageBuffers;
//	}
}
