package sune.lib.sil2;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Contains various methods that can be used when working with Java's NIO buffers.
 * @since 2.0
 * @author Sune*/
public final class BufferUtils {
	
	// Forbid anyone to create an instance of this class
	private BufferUtils() {
	}
	
	/**
	 * Copies the given buffer to a newly allocated buffer of the same length.
	 * All data in the buffer are copied.
	 * @param buffer Buffer to copy
	 * @return Newly allocated buffer with copied data*/
	public static final <T extends Buffer> T copy(T buffer) {
		@SuppressWarnings("unchecked")
		T copy = (T) newBufferOfType(buffer);
		buffercopy(buffer, copy);
		return copy;
	}
	
	private static final class ArrayLess {
		
		// Unfortunate Java's thing not providing Buffer interface with put(buf) method
		private static final void put(ByteBuffer   src, ByteBuffer   dst) { src.put(dst); }
		private static final void put(CharBuffer   src, CharBuffer   dst) { src.put(dst); }
		private static final void put(ShortBuffer  src, ShortBuffer  dst) { src.put(dst); }
		private static final void put(IntBuffer    src, IntBuffer    dst) { src.put(dst); }
		private static final void put(LongBuffer   src, LongBuffer   dst) { src.put(dst); }
		private static final void put(FloatBuffer  src, FloatBuffer  dst) { src.put(dst); }
		private static final void put(DoubleBuffer src, DoubleBuffer dst) { src.put(dst); }
		
		private static final void buffercopy(Buffer src, int srcOff, Buffer dst, int dstOff, int length) {
			src.position(srcOff).limit(srcOff + length);
			dst.position(dstOff).limit(dstOff + length);
			if((src instanceof ByteBuffer))   put((ByteBuffer)   src, (ByteBuffer)   dst); else
			if((src instanceof CharBuffer))   put((CharBuffer)   src, (CharBuffer)   dst); else
			if((src instanceof ShortBuffer))  put((ShortBuffer)  src, (ShortBuffer)  dst); else
			if((src instanceof IntBuffer))    put((IntBuffer)    src, (IntBuffer)    dst); else
			if((src instanceof LongBuffer))   put((LongBuffer)   src, (LongBuffer)   dst); else
			if((src instanceof FloatBuffer))  put((FloatBuffer)  src, (FloatBuffer)  dst); else
			if((src instanceof DoubleBuffer)) put((DoubleBuffer) src, (DoubleBuffer) dst); else
			throw new IllegalStateException("Unable to copy buffer " + src + " -> " + dst);
			src.rewind();
			dst.rewind();
		}
	}
	
	/**
	 * Copies data of length {@code length} from the {@code src} buffer
	 * to the {@code dst} buffer, starting at the position {@code srcOff}
	 * in the {@code src} buffer and at the position {@code dstOff}
	 * in the {@code dst} buffer.
	 * @param src Source buffer
	 * @param srcOff Offset in the source buffer
	 * @param dst Destination buffer
	 * @param dstOff Offset in the destination buffer
	 * @param length Length of data*/
	public static final void buffercopy(Buffer src, int srcOff, Buffer dst, int dstOff, int length) {
		if(!src.hasArray() || !dst.hasArray()) {
			ArrayLess.buffercopy(src, srcOff, dst, dstOff, length);
			return;
		}
		System.arraycopy(src.array(), srcOff, dst.array(), dstOff, length);
	}
	
	/**
	 * Copies all data from the {@code src} buffer to the {@code dst} buffer.
	 * @param src Source buffer
	 * @param dst Destination buffer*/
	public static final void buffercopy(Buffer src, Buffer dst) {
		buffercopy(src, 0, dst, 0, src.capacity());
	}
	
	private static final void fill(ByteBuffer buf, int val, int epp) {
		for(int i = 0, l = buf.capacity(); i < l; i += epp)
			buf.putInt(i, val);
	}
	
	private static final void fill(CharBuffer buf, int val, int epp) {
		char[] arr = ByteBuffer.allocate(Integer.BYTES)
				               .putInt(val).asCharBuffer()
				               .array();
		buf.limit(buf.capacity());
		for(int i = 0, l = buf.capacity(); i < l; i += epp)
			buf.position(i).put(arr);
		buf.rewind();
	}
	
	private static final void fill(ShortBuffer buf, int val, int epp) {
		short[] arr = ByteBuffer.allocate(Integer.BYTES)
				                .putInt(val).asShortBuffer()
				                .array();
		buf.limit(buf.capacity());
		for(int i = 0, l = buf.capacity(); i < l; i += epp)
			buf.position(i).put(arr);
		buf.rewind();
	}
	
	private static final void fill(IntBuffer buf, int val, int epp) {
		for(int i = 0, l = buf.capacity(); i < l; i += epp)
			buf.put(i, val);
	}
	
	private static final void fill(LongBuffer buf, int val, int epp) {
		long[] arr = ByteBuffer.allocate(Integer.BYTES)
				               .putInt(val).asLongBuffer()
				               .array();
		buf.limit(buf.capacity());
		for(int i = 0, l = buf.capacity(); i < l; i += epp)
			buf.position(i).put(arr);
		buf.rewind();
	}

	private static final void fill(FloatBuffer buf, int val, int epp) {
		float[] arr = ByteBuffer.allocate(Integer.BYTES)
				                .putInt(val).asFloatBuffer()
				                .array();
		buf.limit(buf.capacity());
		for(int i = 0, l = buf.capacity(); i < l; i += epp)
			buf.position(i).put(arr);
		buf.rewind();
	}
	
	private static final void fill(DoubleBuffer buf, int val, int epp) {
		double[] arr = ByteBuffer.allocate(Integer.BYTES)
				                 .putInt(val).asDoubleBuffer()
				                 .array();
		buf.limit(buf.capacity());
		for(int i = 0, l = buf.capacity(); i < l; i += epp)
			buf.position(i).put(arr);
		buf.rewind();
	}
	
	/**
	 * Fills the given buffer with the given integer value {@code value}
	 * each step of value {@code epp}.
	 * @param buf Buffer to be filled
	 * @param val Integer value
	 * @param inc Step value*/
	public static final void fill(Buffer buf, int val, int inc) {
		if((buf instanceof ByteBuffer))   fill((ByteBuffer)   buf, val, inc); else
		if((buf instanceof CharBuffer))   fill((CharBuffer)   buf, val, inc); else
		if((buf instanceof ShortBuffer))  fill((ShortBuffer)  buf, val, inc); else
		if((buf instanceof IntBuffer))    fill((IntBuffer)    buf, val, inc); else
		if((buf instanceof LongBuffer))   fill((LongBuffer)   buf, val, inc); else
		if((buf instanceof FloatBuffer))  fill((FloatBuffer)  buf, val, inc); else
		if((buf instanceof DoubleBuffer)) fill((DoubleBuffer) buf, val, inc); else
		throw new UnsupportedOperationException("Unsupported buffer: " + buf);
	}
	
	/**
	 * Creates a new buffer of the same type as the given buffer {@code buf}.
	 * No data are copied, the new buffer is simply allocated.
	 * @param buf Buffer
	 * @return Newly allocated buffer of the same type as {@code buf}.*/
	public static final Buffer newBufferOfType(Buffer buf) {
		int capacity = buf.capacity();
		if((buf instanceof ByteBuffer))   return ByteBuffer  .allocate(capacity);
		if((buf instanceof CharBuffer))   return CharBuffer  .allocate(capacity);
		if((buf instanceof ShortBuffer))  return ShortBuffer .allocate(capacity);
		if((buf instanceof IntBuffer))    return IntBuffer   .allocate(capacity);
		if((buf instanceof LongBuffer))   return LongBuffer  .allocate(capacity);
		if((buf instanceof FloatBuffer))  return FloatBuffer .allocate(capacity);
		if((buf instanceof DoubleBuffer)) return DoubleBuffer.allocate(capacity);
		throw new UnsupportedOperationException("Unsupported buffer: " + buf);
	}
	
	/**
	 * Creates a new direct buffer of the same type as the given buffer {@code buf}.
	 * No data are copied, the new buffer is simply allocated.
	 * @param buf Buffer
	 * @return Newly allocated direct buffer of the same type as {@code buf}.*/
	public static final Buffer newDirectBufferOfType(Buffer buf) {
		ByteBuffer alc = ByteBuffer.allocateDirect(buf.capacity());
		if((buf instanceof ByteBuffer))   return alc; // Fast return
		if((buf instanceof CharBuffer))   return alc.asCharBuffer();
		if((buf instanceof ShortBuffer))  return alc.asShortBuffer();
		if((buf instanceof IntBuffer))    return alc.asIntBuffer();
		if((buf instanceof LongBuffer))   return alc.asLongBuffer();
		if((buf instanceof FloatBuffer))  return alc.asFloatBuffer();
		if((buf instanceof DoubleBuffer)) return alc.asDoubleBuffer();
		throw new UnsupportedOperationException("Unsupported buffer: " + buf);
	}
}