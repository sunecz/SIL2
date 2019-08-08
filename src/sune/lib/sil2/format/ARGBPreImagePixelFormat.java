package sune.lib.sil2.format;

import java.nio.IntBuffer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;

public final class ARGBPreImagePixelFormat implements ImagePixelFormat<IntBuffer> {
	
	private static final int ELEMENTS_PER_PIXEL = 1;
	
	public static final ARGBPreImagePixelFormat INSTANCE = new ARGBPreImagePixelFormat();
	
	// Forbid anyone to create an instance of this class
	private ARGBPreImagePixelFormat() {
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
	public WritablePixelFormat<IntBuffer> getReadFormat() {
		return WritablePixelFormat.getIntArgbPreInstance();
	}
	
	@Override
	public PixelFormat<IntBuffer> getWriteFormat() {
		return PixelFormat.getIntArgbPreInstance();
	}
	
	@Override
	public IntBuffer newBuffer(int length) {
		return IntBuffer.allocate(length * ELEMENTS_PER_PIXEL);
	}
	
	@Override
	public void set(IntBuffer dst, int i, int value) {
		dst.put(i, value);
	}
	
	@Override
	public void setPixel(IntBuffer dst, int i, int r, int g, int b, int a) {
		dst.put(i, ImagePixelFormatUtils.linear2premult(((a & 0xff) << 24) |
		                                                ((r & 0xff) << 16) |
		                                                ((g & 0xff) <<  8) |
		                                                ((b & 0xff))));
	}
	
	@Override
	public void setPixelPre(IntBuffer dst, int i, int r, int g, int b, int a) {
		dst.put(i, ((r & 0xff) << 16) |
		           ((g & 0xff) <<  8) |
		           ((b & 0xff))       |
		           ((a & 0xff) << 24));
	}
	
	@Override
	public void set(IntBuffer dst, int i, IntBuffer src, int k) {
		dst.put(i, src.get(k));
	}
	
	@Override
	public void setARGB(IntBuffer dst, int i, int argb) {
		dst.put(i, ImagePixelFormatUtils.linear2premult(argb));
	}
	
	@Override
	public void setARGBPre(IntBuffer dst, int i, int argb) {
		dst.put(i, argb);
	}

	@Override
	public void setARGB(IntBuffer dst, int i, IntBuffer src, int k) {
		dst.put(i, src.get(k));
	}
	
	@Override
	public int get(IntBuffer src, int i) {
		return src.get(i);
	}
	
	@Override
	public int getARGB(IntBuffer src, int i) {
		return ImagePixelFormatUtils.premult2linear(src.get(i));
	}
	
	@Override
	public int getARGBPre(IntBuffer src, int i) {
		return src.get(i);
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
		return true;
	}
}