package dev.nonamecrackers2.computelib.mixin.rendersystem;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout.VulkanBindGroupEntryType;

import dev.nonamecrackers2.computelib.rendering.systems.ExtendedUniformTypes;
import dev.nonamecrackers2.computelib.rendering.systems.vullkan.ExtendedVulkanBindGroupEntryTypes;

// https://github.com/LudoCrypt/Noteblock-Expansion-Forge/blob/main/src/main/java/net/ludocrypt/nbexpand/mixin/NoteblockInstrumentMixin.java
// https://github.com/Viola-Siemens/Instrument-Plus-Plus/blob/Forge_1.20.1_v3.1.X/src/main/java/com/hexagram2021/ipp/mixin/NoteBlockInstrumentMixin.java
@Mixin(UniformType.class)
public class MixinUniformType
{
	private MixinUniformType(String enumName, int ord)
	{
		throw new AssertionError();
	}
	
	@Shadow @Final @Mutable
	private static UniformType[] $VALUES;
	
	private static UniformType create(String enumName, int ord)
	{
		return (UniformType)(Object)new MixinUniformType(enumName, ord);
	}
	
	@Inject(method = "<clinit>", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/shaders/UniformType;$VALUES:[Lcom/mojang/blaze3d/shaders/UniformType;", shift = Shift.AFTER))
	private static void storymod$injectEnum(CallbackInfo ci)
	{
		int length = $VALUES.length;
		$VALUES = Arrays.copyOf($VALUES, length + 1);

		ExtendedUniformTypes.STORAGE_BUFFER = $VALUES[length] = create("STORAGE_BUFFER", length);
	}
}
