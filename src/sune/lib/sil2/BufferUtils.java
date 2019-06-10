package sune.lib.sil2;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import sune.lib.sil2.format.ImagePixelFormat;

public final class BufferUtils {
	
	// Forbid anyone to create an instance of this class
	private BufferUtils() {
	}
	
	public static final <T extends Buffer> T copy(T buffer, ImagePixelFormat<T> format) {
		T copy = format.newBuffer(buffer.capacity() / format.getElementsPerPixel());
		buffercopy(buffer, copy);
		return copy;
	}
	
	public static final void buffercopy(Buffer src, int srcOff, Buffer dst, int dstOff, int length) {
		System.arraycopy(src.array(), srcOff, dst.array(), dstOff, length);
	}
	
	public static final void buffercopy(Buffer src, Buffer dst) {
		buffercopy(src, 0, dst, 0, src.capacity());
	}
	
	private static final void fillBuffer(ByteBuffer buffer, int value, int epp) {
		for(int i = 0, l = buffer.capacity(); i < l; i += epp) {
			buffer.putInt(i, value);
		}
	}
	
	private static final void fillBuffer(IntBuffer buffer, int value, int epp) {
		for(int i = 0, l = buffer.capacity(); i < l; i += epp) {
			buffer.put(i, value);
		}
	}
	
	public static final void fill(Buffer buffer, int value, int epp) {
		if((buffer instanceof ByteBuffer)) fillBuffer((ByteBuffer) buffer, value, epp); else
		if((buffer instanceof IntBuffer))  fillBuffer((IntBuffer)  buffer, value, epp); else
		throw new UnsupportedOperationException("Unsupported buffer: " + buffer);
	}
	
	public static final Buffer newBufferOfType(Buffer buffer) {
		int capacity = buffer.capacity();
		if((buffer instanceof ByteBuffer)) return ByteBuffer.allocate(capacity);
		if((buffer instanceof IntBuffer))  return IntBuffer .allocate(capacity);
		throw new UnsupportedOperationException("Buffer: " + buffer);
	}
	
	private static Class<?> CLASS_B2I_BUFFER_B;
	private static Class<?> CLASS_B2I_BUFFER_L;
	private static Field FIELD_B2I_BUFFER_B_BB;
	private static Field FIELD_B2I_BUFFER_L_BB;
	private static boolean inited;
	
	private static final void initFields() {
		if((inited)) return;
		try {
			CLASS_B2I_BUFFER_B = Class.forName("java.nio.ByteBufferAsIntBufferB");
			CLASS_B2I_BUFFER_L = Class.forName("java.nio.ByteBufferAsIntBufferL");
			FIELD_B2I_BUFFER_B_BB = CLASS_B2I_BUFFER_B.getDeclaredField("bb");
			FIELD_B2I_BUFFER_L_BB = CLASS_B2I_BUFFER_B.getDeclaredField("bb");
			Reflection.setAccessible(FIELD_B2I_BUFFER_B_BB, true);
			Reflection.setAccessible(FIELD_B2I_BUFFER_L_BB, true);
			inited = true;
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to initialize fields", ex);
		}
	}
	
	private static final ByteBuffer _intBuffer2byteBuffer_B_BB(IntBuffer buffer) {
		try {
			return (ByteBuffer) FIELD_B2I_BUFFER_B_BB.get(buffer);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain the byte buffer field", ex);
		}
	}
	
	private static final ByteBuffer _intBuffer2byteBuffer_L_BB(IntBuffer buffer) {
		try {
			return (ByteBuffer) FIELD_B2I_BUFFER_B_BB.get(buffer);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain the byte buffer field", ex);
		}
	}
	
	public static final ByteBuffer intBuffer2byteBuffer(IntBuffer buffer) {
		initFields();
		Class<?> clazz = buffer.getClass();
		if((clazz == CLASS_B2I_BUFFER_B)) return _intBuffer2byteBuffer_B_BB(buffer);
		if((clazz == CLASS_B2I_BUFFER_L)) return _intBuffer2byteBuffer_L_BB(buffer);
		throw new UnsupportedOperationException("Unsupported class: " + clazz);
	}
}