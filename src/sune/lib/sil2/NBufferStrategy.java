package sune.lib.sil2;

import java.nio.Buffer;

public final class NBufferStrategy<T extends Buffer> implements BufferStrategy<T> {
	
	private final T        original;
	private final Buffer[] buffers;
	
	public NBufferStrategy(T original, int numOfBuffers) {
		this.original = original;
		this.buffers  = new Buffer[numOfBuffers];
	}
	
	@Override
	public T prepareBuffer(int index) {
		if((index < 0 || index >= buffers.length))
			return original;
		T _buffer = BufferUtils.newBufferOfType(original);
		buffers[index] = _buffer;
		BufferUtils.buffercopy(original, _buffer);
		return _buffer;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T getBuffer(int index) {
		return index >= 0 && index < buffers.length ? (T) buffers[index] : original;
	}
	
	@Override
	public int numberOfBuffers() {
		return buffers.length;
	}
	
	@Override
	public void swap(int i1, int i2) {
		if((i1 < 0 || i1 >= buffers.length ||
			i2 < 0 || i2 >= buffers.length))
			return;
		Buffer _buf = buffers[i1];
		buffers[i1] = buffers[i2];
		buffers[i2] = _buf;
	}
	
	@Override
	public int getBuffersCount() {
		return buffers.length;
	}
}