package dev.nonamecrackers2.computelib.event;

import dev.nonamecrackers2.computelib.ComputeLib;
import dev.nonamecrackers2.computelib.rendering.compute.manager.ComputeShaderSourceManager;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

public class ComputeLibEvents
{
	public static void registerReloadListeners(AddClientReloadListenersEvent event)
	{
		event.addListener(ComputeLib.id("compute_source_manager"), ComputeShaderSourceManager.getInstance());
	}
}
