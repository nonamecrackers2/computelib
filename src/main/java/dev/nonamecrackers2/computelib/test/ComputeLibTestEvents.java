package dev.nonamecrackers2.computelib.test;

import javax.annotation.Nullable;

import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.computelib.rendering.buffers.ExtendedGpuBufferUsage;
import dev.nonamecrackers2.computelib.rendering.compute.pass.ComputePass;
import dev.nonamecrackers2.computelib.rendering.systems.GpuDeviceExtension;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppedEvent;

public class ComputeLibTestEvents
{
	private static @Nullable GpuBuffer outBuffer;
	private static @Nullable GpuBuffer valueBuffer;
	
	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent.AfterLevel event)
	{
		if (outBuffer == null)
			outBuffer = RenderSystem.getDevice().createBuffer(() -> "Out", ExtendedGpuBufferUsage.USAGE_SHADER_STORAGE_BUFFER | GpuBuffer.USAGE_MAP_READ, 64); // 16 integers
		
		if (valueBuffer == null)
		{
			valueBuffer = RenderSystem.getDevice().createBuffer(() -> "Value", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16); // Will contain 1 integer, but must be 16 bytes in size
			try (MemoryStack stack = MemoryStack.stackPush()) {
				RenderSystem.getDevice().createCommandEncoder().writeToBuffer(valueBuffer.slice(), Std140Builder.onStack(stack, 16).putInt(6).get());
			}
		}
		
		var commandEncoder = GpuDeviceExtension.get().createExtendedCommandEncoder();
		ComputePass pass = commandEncoder.createComputePass();
		pass.setPipeline(ComputeLibTestPipelines.TEST);
		pass.setUniform("Out", outBuffer);
		pass.setUniform("Value", valueBuffer);
		pass.dispatch(4, 1, 1);
		
		if (!Minecraft.getInstance().isPaused())
		{
			try (GpuBufferSlice.MappedView mapped = outBuffer.map(true, false))
			{
				for (int i = 0; i < 16; i++)
					System.out.print(mapped.data().getInt(i * 4) + ", ");
				System.out.println();
			}
		}
	}
	
	@SubscribeEvent
	public static void onQuit(ClientStoppedEvent event)
	{
		if (outBuffer != null)
			outBuffer.close();
		
		if (valueBuffer != null)
			valueBuffer.close();
	}
}
