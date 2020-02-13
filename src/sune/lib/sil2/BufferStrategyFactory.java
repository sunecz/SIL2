package sune.lib.sil2;

import java.nio.Buffer;

public interface BufferStrategyFactory<T extends Buffer> {
	
	BufferStrategy<T> create(T original);
}