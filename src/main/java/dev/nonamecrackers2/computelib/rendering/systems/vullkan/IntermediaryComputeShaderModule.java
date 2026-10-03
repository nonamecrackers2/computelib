package dev.nonamecrackers2.computelib.rendering.systems.vullkan;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.spvc.SpvcReflectedResource;
import org.lwjgl.util.spvc.SpvcReflectedResource.Buffer;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanUtils;
import com.mojang.blaze3d.vulkan.VulkanBindGroupLayout.VulkanBindGroupEntryType;
import com.mojang.blaze3d.vulkan.glsl.ShaderCompileException;
import com.mojang.blaze3d.vulkan.glsl.SpvSampler;
import com.mojang.blaze3d.vulkan.glsl.SpvUniformBuffer;
import com.mojang.blaze3d.vulkan.glsl.SpvcUtil;

public record IntermediaryComputeShaderModule(String name, @Nullable ByteBuffer spirv, List<SpvUniformBuffer> buffers, List<SpvSampler> samplers) implements AutoCloseable
{
	public static final IntermediaryComputeShaderModule INVALID = new IntermediaryComputeShaderModule("invalid", null, new ArrayList<>(), new ArrayList<>());
	
	public static IntermediaryComputeShaderModule createFromSpirv(String filename, ByteBuffer spirv) throws ShaderCompileException
	{
		List<SpvUniformBuffer> buffers = new ArrayList<>();
		List<SpvSampler> samplers = new ArrayList<>();

		try (MemoryStack stack = MemoryStack.stackPush())
		{
			PointerBuffer pointer = stack.callocPointer(1);
			IntBuffer intReturnBuffer = stack.callocInt(1);
			throwIfError(Spvc.spvc_context_create(pointer), "Couldn't create spvc context");
			long context = pointer.get(0);

			try
			{
				throwIfError(Spvc.spvc_context_parse_spirv(context, spirv.asIntBuffer(), spirv.remaining() / 4, pointer), "Couldn't parse spirv");
				long ir = pointer.get(0);
				throwIfError(Spvc.spvc_context_create_compiler(context, 0, ir, 1, pointer), "Couldn't create compiler");
				long compiler = pointer.get(0);
				throwIfError(Spvc.spvc_compiler_create_shader_resources(compiler, pointer), "Couldn't create resource list");
				long spvcResources = pointer.get(0);
				PointerBuffer countPointer = stack.callocPointer(1);
				
				// Uniform buffers
				throwIfError(Spvc.spvc_resources_get_resource_list_for_type(spvcResources, Spvc.SPVC_RESOURCE_TYPE_UNIFORM_BUFFER, pointer, countPointer), "Couldn't list uniform buffers");
				long spvcList = pointer.get(0);
				long spvcCount = countPointer.get(0);
				Buffer resources = SpvcReflectedResource.create(spvcList, (int) spvcCount);

				for (int i = 0; i < spvcCount; i++)
				{
					SpvcReflectedResource resource = resources.get(i);
					String name = resource.nameString();
					int bindingOffset = getDecorationOffset(compiler, resource, 33, intReturnBuffer);
					buffers.add(new SpvUniformBuffer(name, bindingOffset));
				}
				
				// Storage buffers
				throwIfError(Spvc.spvc_resources_get_resource_list_for_type(spvcResources, Spvc.SPVC_RESOURCE_TYPE_STORAGE_BUFFER, pointer, countPointer), "Couldn't list storage buffers");
				spvcList = pointer.get(0);
				spvcCount = countPointer.get(0);
				resources = SpvcReflectedResource.create(spvcList, (int) spvcCount);

				for (int i = 0; i < spvcCount; i++)
				{
					SpvcReflectedResource resource = resources.get(i);
					String name = resource.nameString();
					int bindingOffset = getDecorationOffset(compiler, resource, 33, intReturnBuffer);
					buffers.add(new SpvUniformBuffer(name, bindingOffset));
				}

				// Samplers
				throwIfError(Spvc.spvc_resources_get_resource_list_for_type(spvcResources, Spvc.SPVC_RESOURCE_TYPE_SAMPLED_IMAGE, pointer, countPointer), "Couldn't list sampled images");
				spvcList = pointer.get(0);
				spvcCount = countPointer.get(0);
				resources = SpvcReflectedResource.create(spvcList, (int) spvcCount);

				for (int i = 0; i < spvcCount; i++)
				{
					SpvcReflectedResource resource = resources.get(i);
					String name = resource.nameString();
					int bindingOffset = getDecorationOffset(compiler, resource, 33, intReturnBuffer);
					long typeHandle = Spvc.spvc_compiler_get_type_handle(compiler, resource.type_id());
					int dimension = Spvc.spvc_type_get_image_dimension(typeHandle);
					samplers.add(new SpvSampler(name, bindingOffset, dimension));
				}
			}
			finally
			{
				Spvc.spvc_context_destroy(context);
			}
		}

		return new IntermediaryComputeShaderModule(filename, spirv, buffers, samplers);
	}
	
	public void rebind(List<VulkanBindGroupLayout.Entry> entries) throws ShaderCompileException
	{
		if (this.spirv == null)
			throw new IllegalStateException("Attempt to use invalid shader");
		
		IntBuffer spvAsIntBuffer = this.spirv.asIntBuffer();
		Set<String> remainingSamplers = new HashSet<>();
		Set<String> remainingBuffers = new HashSet<>();

		for (SpvUniformBuffer buffer : this.buffers)
			remainingBuffers.add(buffer.name());

		for (SpvSampler sampler : this.samplers)
			remainingSamplers.add(sampler.name());

		for (int i = 0; i < entries.size(); i++)
		{
			VulkanBindGroupLayout.Entry entry = entries.get(i);
			
			VulkanBindGroupEntryType type = entry.type();
			
			if (type == VulkanBindGroupEntryType.UNIFORM_BUFFER || type == ExtendedVulkanBindGroupEntryTypes.STORAGE_BUFFER)
			{
				SpvUniformBuffer ubo = this.getBuffer(entry.name());
				System.out.println("Buffer " + entry.name() + " now binded to " + i);
				if (ubo != null)
				{
					spvAsIntBuffer.put(ubo.bindingOffset(), i);
					remainingBuffers.remove(entry.name());
				}
			}
			else if (type == VulkanBindGroupEntryType.SAMPLED_IMAGE || type == VulkanBindGroupEntryType.TEXEL_BUFFER)
			{
				SpvSampler sampler = this.getSampler(entry.name());
				if (sampler != null)
				{
					if (type == VulkanBindGroupEntryType.SAMPLED_IMAGE ? (sampler.dimensions() != 1 && sampler.dimensions() != 3) : sampler.dimensions() != 5)
						throw new ShaderCompileException("Unsupported " + type + " dimensions '" + SpvcUtil.imageDimensionToString(sampler.dimensions()) + "' for sampler " + entry.name());

					spvAsIntBuffer.put(sampler.bindingOffset(), i);
					remainingSamplers.remove(entry.name());
				}
			}
			else
			{
				throw new ShaderCompileException("Unknown uniform type: " + type);
			}
		}

		if (!remainingBuffers.isEmpty())
			throw new ShaderCompileException("Shader expects uniform buffers which are not being provided: " + remainingBuffers);

		if (!remainingSamplers.isEmpty())
			throw new ShaderCompileException("Shader expects samplers which are not being provided: " + remainingSamplers);
	}
	
	public long createVulkanShaderModule(VulkanDevice device)
	{
		if (this.spirv == null)
			throw new IllegalStateException("Attempt to use invalid shader");

		try (MemoryStack stack = MemoryStack.stackPush())
		{
			VkShaderModuleCreateInfo info = VkShaderModuleCreateInfo.calloc(stack).sType$Default().pCode(this.spirv);
			LongBuffer pointer = stack.callocLong(1);
			VulkanUtils.crashIfFailure(device, VK12.vkCreateShaderModule(device.vkDevice(), info, null, pointer), "Can't compile " + this.name);
			device.instance().debug().setObjectName(device.vkDevice(), 15, pointer.get(0), () -> this.name);
			return pointer.get(0);
		}
	}
	
	private @Nullable SpvUniformBuffer getBuffer(String name)
	{
		for (SpvUniformBuffer buf : this.buffers)
		{
			if (buf.name().equals(name))
				return buf;
		}

		return null;
	}

	private @Nullable SpvSampler getSampler(String name)
	{
		for (SpvSampler sampler : this.samplers)
		{
			if (sampler.name().equals(name))
				return sampler;
		}

		return null;
	}
	
	private static void throwIfError(int result, String message) throws ShaderCompileException
	{
		if (result != 0)
		{
			String name = switch (result)
			{
			case -4 -> "SPVC_ERROR_INVALID_ARGUMENT";
			case -3 -> "SPVC_ERROR_OUT_OF_MEMORY";
			case -2 -> "SPVC_ERROR_UNSUPPORTED_SPIRV";
			case -1 -> "SPVC_ERROR_INVALID_SPIRV";
			default -> Integer.toString(result);
			};
			throw new ShaderCompileException(message + " (" + name + ")");
		}
	}

	private static int getDecorationOffset(long compiler, SpvcReflectedResource resource, int decoration, IntBuffer returnBuffer) throws ShaderCompileException
	{
		if (!Spvc.spvc_compiler_get_binary_offset_for_decoration(compiler, resource.id(), decoration, returnBuffer))
			throw new ShaderCompileException("Couldn't find byte offset for location decoration of " + resource.nameString());
		else
			return returnBuffer.get(0);
	}
	
	@Override
	public void close()
	{
		MemoryUtil.memFree(this.spirv);
	}
}
