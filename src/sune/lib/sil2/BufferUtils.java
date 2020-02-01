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
 * Contains various utility methods for Java NIO buffers.
 * @since 2.0
 * @author Sune
 */
public final class BufferUtils {
	
	// Forbid anyone to create an instance of this class
	private BufferUtils() {
	}
	
	/**
	 * Copies the given buffer to a newly allocated buffer of the same length.
	 * All data in the buffer are copied.
	 * @param buffer Buffer to copy
	 * @return Newly allocated buffer with copied data
	 */
	public static final <T extends Buffer> T copy(T buffer) {
		T copy = newBufferOfType(buffer);
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
	 * @param length Length of data
	 */
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
	 * @param dst Destination buffer
	 */
	public static final void buffercopy(Buffer src, Buffer dst) {
		buffercopy(src, 0, dst, 0, src.capacity());
	}
	
	private static abstract class Inserter<T extends Buffer, N extends Number> {
		
		final T buf;
		Inserter(T buf) { this.buf = buf; }
		
		abstract T getInsertBuffer(N val);
		abstract void put(T dst, T src);
		
		@SuppressWarnings("unchecked")
		void fill(N val, int inc) {
			T ins = getInsertBuffer(val);
			int bcp = buf.capacity();
			int acp = ins.capacity();
			int rem = bcp % acp;
			buf.rewind().limit(bcp);
			for(int i = 0, l = bcp - acp + rem; i < l; i += inc)
				put(buf, (T) ins.rewind());
			if((rem > 0))
				put(buf, (T) ins.rewind().limit(rem));
			buf.rewind();
		}
	}
	
	// ----- IntInserters
	
	private static abstract class IntInserter<T extends Buffer> extends Inserter<T, Integer> {
		
		private static final int SIZE = Integer.BYTES;
		IntInserter(T buf) { super(buf); }
		ByteBuffer alloc(int val) { return ByteBuffer.allocate(SIZE).putInt(0, val); }
	}
	
	private static final class ByteBufferIntInserter extends IntInserter<ByteBuffer> {
		
		public ByteBufferIntInserter(ByteBuffer buf) { super(buf); }
		@Override ByteBuffer getInsertBuffer(Integer val) { return null; }
		@Override void put(ByteBuffer dst, ByteBuffer src) { /* Do nothing */ }
		
		/* Direct and faster method for filling. */
		@Override
		void fill(Integer val, int inc) {
			for(int i = 0, l = buf.capacity(); i < l; i += inc)
				buf.putInt(i, val);
		}
	}
	
	private static final class CharBufferIntInserter extends IntInserter<CharBuffer> {
		
		public CharBufferIntInserter(CharBuffer buf) { super(buf); }
		@Override CharBuffer getInsertBuffer(Integer val) { return alloc(val).asCharBuffer(); }
		@Override void put(CharBuffer dst, CharBuffer src) { dst.put(src); }
	}
	
	private static final class ShortBufferIntInserter extends IntInserter<ShortBuffer> {
		
		public ShortBufferIntInserter(ShortBuffer buf) { super(buf); }
		@Override ShortBuffer getInsertBuffer(Integer val) { return alloc(val).asShortBuffer(); }
		@Override void put(ShortBuffer dst, ShortBuffer src) { dst.put(src); }
	}
	
	private static final class IntBufferIntInserter extends IntInserter<IntBuffer> {
		
		public IntBufferIntInserter(IntBuffer buf) { super(buf); }
		@Override IntBuffer getInsertBuffer(Integer val) { return null; }
		@Override void put(IntBuffer dst, IntBuffer src) { /* Do nothing */ }
		
		/* Direct and faster method for filling. */
		@Override
		void fill(Integer val, int inc) {
			for(int i = 0, l = buf.capacity(); i < l; i += inc)
				buf.put(i, val);
		}
	}
	
	private static final class LongBufferIntInserter extends IntInserter<LongBuffer> {
		
		public LongBufferIntInserter(LongBuffer buf) { super(buf); }
		@Override LongBuffer getInsertBuffer(Integer val) { return alloc(val).asLongBuffer(); }
		@Override void put(LongBuffer dst, LongBuffer src) { dst.put(src); }
	}
	
	private static final class FloatBufferIntInserter extends IntInserter<FloatBuffer> {
		
		public FloatBufferIntInserter(FloatBuffer buf) { super(buf); }
		@Override FloatBuffer getInsertBuffer(Integer val) { return alloc(val).asFloatBuffer(); }
		@Override void put(FloatBuffer dst, FloatBuffer src) { dst.put(src); }
	}
	
	private static final class DoubleBufferIntInserter extends IntInserter<DoubleBuffer> {
		
		public DoubleBufferIntInserter(DoubleBuffer buf) { super(buf); }
		@Override DoubleBuffer getInsertBuffer(Integer val) { return alloc(val).asDoubleBuffer(); }
		@Override void put(DoubleBuffer dst, DoubleBuffer src) { dst.put(src); }
	}
	
	private static final IntInserter<?> intInserter(Buffer buf) {
		if((buf instanceof ByteBuffer))   return new ByteBufferIntInserter  ((ByteBuffer)   buf);
		if((buf instanceof CharBuffer))   return new CharBufferIntInserter  ((CharBuffer)   buf);
		if((buf instanceof ShortBuffer))  return new ShortBufferIntInserter ((ShortBuffer)  buf);
		if((buf instanceof IntBuffer))    return new IntBufferIntInserter   ((IntBuffer)    buf);
		if((buf instanceof LongBuffer))   return new LongBufferIntInserter  ((LongBuffer)   buf);
		if((buf instanceof FloatBuffer))  return new FloatBufferIntInserter ((FloatBuffer)  buf);
		if((buf instanceof DoubleBuffer)) return new DoubleBufferIntInserter((DoubleBuffer) buf);
		throw new UnsupportedOperationException("Unsupported buffer: " + buf);
	}
	
	// -----
	
	/**
	 * Fills the given buffer buffer with an integer value {@code value}
	 * each {@code inc} step.
	 * @param buf Buffer to be filled
	 * @param val Integer value
	 * @param inc Step value
	 */
	public static final void fill(Buffer buf, int val, int inc) {
		intInserter(buf).fill(val, inc);
	}
	
	/**
	 * Creates a new buffer of the same type as the given buffer {@code buf}.
	 * No data are copied, the new buffer is simply allocated.
	 * @param buf Buffer
	 * @return Newly allocated buffer of the same type as {@code buf}.
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends Buffer> T newBufferOfType(T buf) {
		int capacity = buf.capacity();
		if((buf instanceof ByteBuffer))   return (T) ByteBuffer  .allocate(capacity);
		if((buf instanceof CharBuffer))   return (T) CharBuffer  .allocate(capacity);
		if((buf instanceof ShortBuffer))  return (T) ShortBuffer .allocate(capacity);
		if((buf instanceof IntBuffer))    return (T) IntBuffer   .allocate(capacity);
		if((buf instanceof LongBuffer))   return (T) LongBuffer  .allocate(capacity);
		if((buf instanceof FloatBuffer))  return (T) FloatBuffer .allocate(capacity);
		if((buf instanceof DoubleBuffer)) return (T) DoubleBuffer.allocate(capacity);
		throw new UnsupportedOperationException("Unsupported buffer: " + buf);
	}
	
	/**
	 * Creates a new direct buffer of the same type as the given buffer {@code buf}.
	 * No data are copied, the new buffer is simply allocated.
	 * @param buf Buffer
	 * @return Newly allocated direct buffer of the same type as {@code buf}.
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends Buffer> T newDirectBufferOfType(T buf) {
		ByteBuffer alc = ByteBuffer.allocateDirect(buf.capacity());
		if((buf instanceof ByteBuffer))   return (T) alc; // Fast return
		if((buf instanceof CharBuffer))   return (T) alc.asCharBuffer();
		if((buf instanceof ShortBuffer))  return (T) alc.asShortBuffer();
		if((buf instanceof IntBuffer))    return (T) alc.asIntBuffer();
		if((buf instanceof LongBuffer))   return (T) alc.asLongBuffer();
		if((buf instanceof FloatBuffer))  return (T) alc.asFloatBuffer();
		if((buf instanceof DoubleBuffer)) return (T) alc.asDoubleBuffer();
		throw new UnsupportedOperationException("Unsupported buffer: " + buf);
	}
}