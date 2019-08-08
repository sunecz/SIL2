package sune.lib.sil2.format;

import java.nio.ByteBuffer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;

public final class BGRAImagePixelFormat implements ImagePixelFormat<ByteBuffer> {
	
	private static final int ELEMENTS_PER_PIXEL = 4;
	
	public static final BGRAImagePixelFormat INSTANCE = new BGRAImagePixelFormat();
	
	// Forbid anyone to create an instance of this class
	private BGRAImagePixelFormat() {
	}
	
	@Override
	public int getShiftR() {
		return 8;
	}
	
	@Override
	public int getShiftG() {
		return 16;
	}
	
	@Override
	public int getShiftB() {
		return 24;
	}
	
	@Override
	public int getShiftA() {
		return 0;
	}
	
	@Override
	public WritablePixelFormat<ByteBuffer> getReadFormat() {
		return WritablePixelFormat.getByteBgraInstance();
	}
	
	@Override
	public PixelFormat<ByteBuffer> getWriteFormat() {
		return PixelFormat.getByteBgraInstance();
	}
	
	@Override
	public ByteBuffer newBuffer(int length) {
		return ByteBuffer.allocate(length * ELEMENTS_PER_PIXEL);
	}
	
	@Override
	public void set(ByteBuffer dst, int i, int value) {
		dst.put(i, (byte) value);
	}
	
	@Override
	public void setPixel(ByteBuffer dst, int i, int r, int g, int b, int a) {
		dst.put(i,     (byte) b);
		dst.put(i + 1, (byte) g);
		dst.put(i + 2, (byte) r);
		dst.put(i + 3, (byte) a);
	}
	
	@Override
	public void setPixelPre(ByteBuffer dst, int i, int r, int g, int b, int a) {
		int[] out = new int[4];
		ImagePixelFormatUtils.premult2linear((byte) r, (byte) g, (byte) b, (byte) a, out);
		dst.put(i,     (byte) out[0]);
		dst.put(i + 1, (byte) out[1]);
		dst.put(i + 2, (byte) out[2]);
		dst.put(i + 3, (byte) out[3]);
	}
	
	@Override
	public void set(ByteBuffer dst, int i, ByteBuffer src, int k) {
		dst.putInt(i, src.getInt(k));
	}
	
	@Override
	public void setARGB(ByteBuffer dst, int i, int argb) {
		setPixel(dst, i, (argb >> 16) & 0xff,
		                 (argb >>  8) & 0xff,
		                 (argb)       & 0xff,
		                 (argb >> 24) & 0xff);
	}
	
	@Override
	public void setARGBPre(ByteBuffer dst, int i, int argb) {
		setPixelPre(dst, i, (argb >> 16) & 0xff,
		                    (argb >>  8) & 0xff,
		                    (argb)       & 0xff,
		                    (argb >> 24) & 0xff);
	}
	
	@Override
	public void setARGB(ByteBuffer dst, int i, ByteBuffer src, int k) {
		dst.put(i,     src.get(k    ));
		dst.put(i + 1, src.get(k + 1));
		dst.put(i + 2, src.get(k + 2));
		dst.put(i + 3, src.get(k + 3));
	}
	
	@Override
	public int get(ByteBuffer src, int i) {
		return src.get(i) & 0xff;
	}
	
	@Override
	public int getARGB(ByteBuffer src, int i) {
		return ((src.get(i))    & 0xff)        |
			   ((src.get(i + 1) & 0xff) <<  8) |
			   ((src.get(i + 2) & 0xff) << 16) |
			   ((src.get(i + 3) & 0xff) << 24);
	}
	
	@Override
	public int getARGBPre(ByteBuffer src, int i) {
		byte[] out = new byte[4];
		ImagePixelFormatUtils.linear2premult(src.get(i + 2) & 0xff,
		                                     src.get(i + 1) & 0xff,
		                                     src.get(i)     & 0xff,
		                                     src.get(i + 3) & 0xff,
		                                     out);
		return ((out[0] & 0xff)      ) |
			   ((out[1] & 0xff) <<  8) |
			   ((out[2] & 0xff) << 16) |
			   ((out[3] & 0xff) << 24);
	}
	
	@Override
	public int getElementsPerPixel() {
		return ELEMENTS_PER_PIXEL;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj != null && (this == obj || this.getClass() == obj.getClass());
	}
	
	@Override
	public boolean isPremultiplied() {
		return false;
	}
}