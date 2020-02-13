package sune.lib.sil2;

import java.nio.Buffer;

public final class NBufferStrategyFactory<T extends Buffer> implements BufferStrategyFactory<T> {
	
	private final int numOfBuffers;
	
	public NBufferStrategyFactory(int numOfBuffers) {
		if((numOfBuffers <= 0))
			throw new IllegalArgumentException("Number of buffers must be > 0");
		this.numOfBuffers = numOfBuffers;
	}
	
	@Override
	public BufferStrategy<T> create(T original) {
		return new NBufferStrategy<>(original, numOfBuffers);
	}
}