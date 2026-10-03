package dev.nonamecrackers2.computelib.rendering.compute;

public class MemoryBarrier
{
	private MemoryBarrier() {}
	
	public static class Stage
	{
		public static final int VERTEX_SHADER = 8;
		public static final int FRAGMENT_SHADER = 128;
		public static final int COMPUTE_SHADER = 2048;
		public static final int TRANSFER = 4096;
		public static final int HOST = 16384;
		public static final int ALL_GRAPHICS = 32768;
		public static final int ALL_COMMANDS = 65536;
		
		private Stage() {}
	}
	
	public static class Access
	{
		/** Uniform buffer reads */
		public static final int UNIFORM_READ = 8;
		/** Texel buffers, sampled images, SSBOs, images */
		public static final int SHADER_READ = 32;
		/** Texel buffers, sampled images, SSBOs, images */
		public static final int SHADER_WRITE = 64;
		/** Copy operations */
		public static final int TRANSFER_READ = 2048;
		/** Clear/copy operations */
		public static final int TRANSFER_WRITE = 4096;
		/** Reading data by the host */
		public static final int HOST_READ = 8192;
		/** Writing data by the host */
		public static final int HOST_WRITE = 16384;
		/** All memory read access */
		public static final int MEMORY_READ = 32768;
		/** All memory write accesses */
		public static final int MEMORY_WRITE = 65536;
		
		private Access() {}
	}
}
