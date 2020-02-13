package sune.lib.sil2;

import java.nio.Buffer;

public interface BufferStrategy<T extends Buffer> {
	
	T prepareBuffer(int index);
	T getBuffer(int index);
	int numberOfBuffers();
	void swap(int i1, int i2);
	int getBuffersCount();
}