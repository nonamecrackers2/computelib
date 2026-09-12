package dev.nonamecrackers2.computelib.rendering.pipeline;

import dev.nonamecrackers2.computelib.rendering.BindingManager;

@FunctionalInterface
public interface BindingSupplier
{
	static final BindingSupplier NEXT_AVAILABLE = () -> BindingManager.getAvailableShaderStorageBinding();
	
	int fetchBinding();
	
	static BindingSupplier constant(int value)
	{
		return () -> value;
	}
}
