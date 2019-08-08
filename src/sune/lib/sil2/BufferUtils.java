package sune.lib.sil2;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import sune.lib.sil2.format.ARGBImagePixelFormat;
import sune.lib.sil2.format.ARGBPreImagePixelFormat;
import sune.lib.sil2.format.BGRAImagePixelFormat;
import sune.lib.sil2.format.BGRAPreImagePixelFormat;
import sune.lib.sil2.format.ImagePixelFormat;
import sune.lib.sil2.format.RGBImagePixelFormat;

public final class BufferUtils {
	
	// Forbid anyone to create an instance of this class
	private BufferUtils() {
	}
	
	public static final <T extends Buffer> T copy(T buffer, ImagePixelFormat<T> format) {
		T copy = format.newBuffer(buffer.capacity() / format.getElementsPerPixel());
		buffercopy(buffer, copy);
		return copy;
	}
	
	public static final <T extends Buffer> T copy(T buffer) {
		@SuppressWarnings("unchecked")
		T copy = (T) newBufferOfType(buffer);
		buffercopy(buffer, copy);
		return copy;
	}
	
	public static final void buffercopy(Buffer src, int srcOff, Buffer dst, int dstOff, int length) {
		System.arraycopy(src.array(), srcOff, dst.array(), dstOff, length);
	}
	
	public static final void buffercopy(Buffer src, Buffer dst) {
		buffercopy(src, 0, dst, 0, src.capacity());
	}
	
	private static final void fill(ByteBuffer buffer, int value, int epp) {
		for(int i = 0, l = buffer.capacity(); i < l; i += epp) {
			buffer.putInt(i, value);
		}
	}
	
	private static final void fill(IntBuffer buffer, int value, int epp) {
		for(int i = 0, l = buffer.capacity(); i < l; i += epp) {
			buffer.put(i, value);
		}
	}
	
	public static final void fill(Buffer buffer, int value, int epp) {
		if((buffer instanceof ByteBuffer)) fill((ByteBuffer) buffer, value, epp); else
		if((buffer instanceof IntBuffer))  fill((IntBuffer)  buffer, value, epp); else
		throw new UnsupportedOperationException("Unsupported buffer: " + buffer);
	}
	
	public static final Buffer newBufferOfType(Buffer buffer) {
		int capacity = buffer.capacity();
		if((buffer instanceof ByteBuffer)) return ByteBuffer.allocate(capacity);
		if((buffer instanceof IntBuffer))  return IntBuffer .allocate(capacity);
		throw new UnsupportedOperationException("Unsupported buffer: " + buffer);
	}
	
	public static final Buffer ensureType(Buffer buffer, ImagePixelFormat<?> format) {
		if((format instanceof ARGBImagePixelFormat ||
			format instanceof ARGBPreImagePixelFormat)) {
			return ensureType(buffer, IntBuffer.class);
		}
		if((format instanceof BGRAImagePixelFormat ||
			format instanceof BGRAPreImagePixelFormat ||
			format instanceof RGBImagePixelFormat)) {
			return ensureType(buffer, ByteBuffer.class);
		}
		throw new UnsupportedOperationException("Unsupported format: " + format);
	}
	
	public static final Buffer ensureType(Buffer buffer, Buffer bufferOfType) {
		Class<? extends Buffer> clazz = ByteBuffer.class;
		if((bufferOfType instanceof IntBuffer)) clazz = IntBuffer.class;
		return ensureType(buffer, clazz);
	}
	
	public static final Buffer ensureType(Buffer buffer, Class<? extends Buffer> clazz) {
		if((buffer instanceof ByteBuffer)) {
			ByteBuffer buf = (ByteBuffer) buffer;
			if((clazz == ByteBuffer.class)) return buf;
			if((clazz == IntBuffer.class)) {
				IntBuffer copy = IntBuffer.allocate(buf.capacity() / Integer.BYTES);
				copy.put(buf.asIntBuffer());
				return copy;
			}
		} else
		if((buffer instanceof IntBuffer)) {
			IntBuffer buf = (IntBuffer) buffer;
			if((clazz == IntBuffer.class)) return buf;
			if((clazz == ByteBuffer.class)) {
				ByteBuffer copy = ByteBuffer.allocate(buf.capacity() * Integer.BYTES);
				copy.asIntBuffer().put(buf);
				return copy;
			}
		}
		throw new UnsupportedOperationException("Unsupported conversion: " + buffer.getClass() + " -> " + clazz);
	}
}