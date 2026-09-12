package dev.nonamecrackers2.computelib.rendering.systems;

import com.mojang.blaze3d.systems.DeviceInfo;

public interface DeviceLimitsExtension
{
	int maxComputeWorkGroupCountX();
	
	int maxComputeWorkGroupCountY();
	
	int maxComputeWorkGroupCountZ();
	
	int maxComputeWorkGroupSizeX();
	
	int maxComputeWorkGroupSizeY();
	
	int maxComputeWorkGroupSizeZ();
	
	int maxComputeWorkGroupInvocations();
	
	void _setMaxComputeWorkGroupCount(int index, int count);
	
	void _setMaxComputeWorkGroupSize(int index, int size);
	
	void _setMaxComputeWorkGroupInvocations(int count);
	
	static DeviceLimitsExtension get(DeviceInfo info)
	{
		return (DeviceLimitsExtension)(Object)info.limits();
	}
}
