package dev.nonamecrackers2.computelib.mixin.rendersystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.systems.DeviceLimits;

import dev.nonamecrackers2.computelib.rendering.systems.DeviceLimitsExtension;

@Mixin(DeviceLimits.class)
public class MixinDeviceLimits implements DeviceLimitsExtension
{
	@Unique
	private int maxComputeWorkGroupCount[] = new int[3];
	@Unique
	private int maxComputeWorkGroupSize[] = new int[3];
	@Unique
	private int maxComputeWorkGroupInvocations;
	
	@Override
	public int maxComputeWorkGroupCountX()
	{
		return this.maxComputeWorkGroupCount[0];
	}
	
	@Override
	public int maxComputeWorkGroupCountY()
	{
		return this.maxComputeWorkGroupCount[1];
	}
	
	@Override
	public int maxComputeWorkGroupCountZ()
	{
		return this.maxComputeWorkGroupCount[2];
	}
	
	@Override
	public int maxComputeWorkGroupSizeX()
	{
		return this.maxComputeWorkGroupSize[0];
	}
	
	@Override
	public int maxComputeWorkGroupSizeY()
	{
		return this.maxComputeWorkGroupSize[1];
	}
	
	@Override
	public int maxComputeWorkGroupSizeZ()
	{
		return this.maxComputeWorkGroupSize[2];
	}
	
	@Override
	public int maxComputeWorkGroupInvocations()
	{
		return this.maxComputeWorkGroupInvocations;
	}
	
	@Override
	public void _setMaxComputeWorkGroupCount(int index, int count)
	{
		this.maxComputeWorkGroupCount[index] = count;
	}
	
	@Override
	public void _setMaxComputeWorkGroupSize(int index, int size)
	{
		this.maxComputeWorkGroupCount[index] = size;
	}
	
	@Override
	public void _setMaxComputeWorkGroupInvocations(int count)
	{
		this.maxComputeWorkGroupInvocations = count;
	}
}
