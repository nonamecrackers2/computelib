package dev.nonamecrackers2.computelib;

import dev.nonamecrackers2.computelib.event.ComputeLibEvents;
import dev.nonamecrackers2.computelib.test.ComputeLibTestEvents;
import dev.nonamecrackers2.computelib.test.ComputeLibTestPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ComputeLib.MODID)
public class ComputeLib
{
	public static final String MODID = "computelib";

	public ComputeLib(IEventBus modEventBus, ModContainer modContainer)
	{
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(ComputeLibEvents::registerReloadListeners);
		
		// Test events
		NeoForge.EVENT_BUS.register(ComputeLibTestEvents.class);
		
		ComputeLibTestPipelines.bootstrap(); //TODO bad 
	}

	private void commonSetup(FMLCommonSetupEvent event)
	{
	}
	
	public static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(MODID, path);
	}
}
