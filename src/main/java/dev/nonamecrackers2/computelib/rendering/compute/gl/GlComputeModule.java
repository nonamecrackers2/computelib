package dev.nonamecrackers2.computelib.rendering.compute.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.resources.Identifier;

public class GlComputeModule implements AutoCloseable
{
	public static final GlComputeModule INVALID_SHADER = new GlComputeModule(Identifier.fromNamespaceAndPath("simpleclouds-minecraft-gpu-device-extension", "invalid"), -1);
	private final Identifier id;
	private int shaderId;
	
	public GlComputeModule(Identifier id, int shaderId)
	{
		this.id = id;
		this.shaderId = shaderId;
	}
	
	@Override
	public void close()
	{
		if (this.shaderId == -1)
            throw new IllegalStateException("Already closed");

        RenderSystem.assertOnRenderThread();
        GlStateManager.glDeleteShader(this.shaderId);
        this.shaderId = -1;
	}
	
	public Identifier getId()
	{
		return this.id;
	}
	
	public int getShaderId()
	{
		return this.shaderId;
	}
}
