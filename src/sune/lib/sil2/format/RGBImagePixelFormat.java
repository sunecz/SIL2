package sune.lib.sil2.format;

import java.nio.ByteBuffer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;

public final class RGBImagePixelFormat implements ImagePixelFormat<ByteBuffer> {
	
	private static final int ELEMENTS_PER_PIXEL = 3;
	
	public static final RGBImagePixelFormat INSTANCE = new RGBImagePixelFormat();
	
	// Forbid anyone to create an instance of this class
	private RGBImagePixelFormat() {
	}
	
	@Override
	public int getShiftR() {
		return 16;
	}
	
	@Override
	public int getShiftG() {
		return 8;
	}
	
	@Override
	public int getShiftB() {
		return 0;
	}
	
	@Override
	public int getShiftA() {
		return 24;
	}
	
	@Override
	public WritablePixelFormat<ByteBuffer> getReadFormat() {
		return WritablePixelFormat.getByteBgraInstance();
	}
	
	@Override
	public PixelFormat<ByteBuffer> getWriteFormat() {
		return PixelFormat.getByteRgbInstance();
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
		dst.put(i,     (byte) r);
		dst.put(i + 1, (byte) g);
		dst.put(i + 2, (byte) b);
	}
	
	@Override
	public void setPixelPre(ByteBuffer dst, int i, int r, int g, int b, int a) {
		setPixel(dst, i, r, g, b, a);
	}
	
	@Override
	public void set(ByteBuffer dst, int i, ByteBuffer src, int k) {
		dst.put(i, src.get(k));
	}
	
	@Override
	public void setARGB(ByteBuffer dst, int i, int argb) {
		setPixel(dst, i, (argb >> 16) & 0xff,
		                 (argb >>  8) & 0xff,
		                 (argb)       & 0xff,
		                                0xff);
	}
	
	@Override
	public void setARGBPre(ByteBuffer dst, int i, int argb) {
		setPixelPre(dst, i, (argb >> 16) & 0xff,
		                    (argb >>  8) & 0xff,
		                    (argb)       & 0xff,
		                                   0xff);
	}
	
	@Override
	public void setARGB(ByteBuffer dst, int i, ByteBuffer src, int k) {
		dst.put(i,     src.get(k    ));
		dst.put(i + 1, src.get(k + 1));
		dst.put(i + 2, src.get(k + 2));
	}
	
	@Override
	public int get(ByteBuffer src, int i) {
		return src.get(i) & 0xff;
	}
	
	@Override
	public int getARGB(ByteBuffer src, int i) {
		return ((src.get(i + 2)) & 0xff)        |
			   ((src.get(i + 1)  & 0xff) <<  8) |
			   ((src.get(i)      & 0xff) << 16) |
			   ((0xff000000));
	}
	
	@Override
	public int getARGBPre(ByteBuffer src, int i) {
		return getARGB(src, i);
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