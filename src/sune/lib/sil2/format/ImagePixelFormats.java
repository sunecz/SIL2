package sune.lib.sil2.format;

import java.nio.Buffer;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import sune.lib.sil2.FXImage;
import sune.lib.sil2.ImageUtils;
import sune.lib.sil2.NativeImage;

public final class ImagePixelFormats {
	
	// Forbid anyone to create an instance of this class
	private ImagePixelFormats() {
	}
	
	public static final boolean isSupported(PixelFormat<?> format) {
		switch(format.getType()) {
			case INT_ARGB_PRE:
			case INT_ARGB:
			case BYTE_BGRA_PRE:
			case BYTE_BGRA:
			case BYTE_RGB:
				return true;
			default:
				return false;
		}
	}
	
	public static final ImagePixelFormat<?> from(PixelFormat<?> format) {
		switch(format.getType()) {
			case INT_ARGB_PRE:  return ARGBPreImagePixelFormat.INSTANCE;
			case INT_ARGB:      return ARGBImagePixelFormat   .INSTANCE;
			case BYTE_BGRA_PRE: return BGRAPreImagePixelFormat.INSTANCE;
			case BYTE_BGRA:     return BGRAImagePixelFormat   .INSTANCE;
			case BYTE_RGB:      return RGBImagePixelFormat    .INSTANCE;
			default:
				throw new UnsupportedOperationException("Unsupported pixel format");
		}
	}
	
	public static final ImagePixelFormat<?> from(Image image) {
		return from(image.getPixelReader().getPixelFormat());
	}
	
	public static final ImagePixelFormat<?> getNativeFormat() {
		return from(NativeImage.getNativePixelFormat());
	}
	
	// ----- Image pixel formats' instances
	
	public static final ARGBImagePixelFormat getARGBInstance() {
		return ARGBImagePixelFormat.INSTANCE;
	}
	
	public static final ARGBPreImagePixelFormat getARGBPreInstance() {
		return ARGBPreImagePixelFormat.INSTANCE;
	}
	
	public static final BGRAImagePixelFormat getBGRAInstance() {
		return BGRAImagePixelFormat.INSTANCE;
	}
	
	public static final BGRAPreImagePixelFormat getBGRAPreInstance() {
		return BGRAPreImagePixelFormat.INSTANCE;
	}
	
	public static final RGBImagePixelFormat getRGBInstance() {
		return RGBImagePixelFormat.INSTANCE;
	}
	
	// -----
	
	public static final <S extends Buffer, D extends Buffer> D convert
			(S src, ImagePixelFormat<S> srcFormat,
			 ImagePixelFormat<D> dstFormat,
			 int pixelsCount) {
		if((src == null || srcFormat == null || dstFormat == null || srcFormat == dstFormat
				|| pixelsCount <= 0))
			throw new IllegalArgumentException();
		D dst = dstFormat.newBuffer(pixelsCount);
		convert(src, srcFormat, dst, dstFormat, pixelsCount);
		return dst;
	}
	
	public static final <S extends Buffer, D extends Buffer> void convert
			(S src, ImagePixelFormat<S> srcFormat,
			 D dst, ImagePixelFormat<D> dstFormat,
			 int pixelsCount) {
		if((src == null || dst == null || srcFormat == null || dstFormat == null
				|| srcFormat == dstFormat || pixelsCount <= 0))
			throw new IllegalArgumentException();
		int srcEpp = srcFormat.getElementsPerPixel();
		int dstEpp = dstFormat.getElementsPerPixel();
		for(int i = pixelsCount, s = 0, d = 0; --i >= 0; s += srcEpp, d += dstEpp) {
			dstFormat.setARGB(dst, d, srcFormat.getARGB(src, s));
		}
	}
	
	public static final <D extends Buffer> D convertPixels(Image image, ImagePixelFormat<D> dstFormat) {
		if((image == null || dstFormat == null))
			throw new IllegalArgumentException();
		@SuppressWarnings("unchecked")
		ImagePixelFormat<Buffer> srcFormat = (ImagePixelFormat<Buffer>) from(image);
		int width  = (int) image.getWidth();
		int height = (int) image.getHeight();
		int pixelsCount = width * height;
		Buffer src = ImageUtils.getPixels(image);
		return convert(src, srcFormat, dstFormat, pixelsCount);
	}
	
	/**
	 * Note that Prism allows only premultiplied versions of an image to be created.*/
	public static final <D extends Buffer> Image convertImage(Image image, ImagePixelFormat<D> dstFormat) {
		if((image == null || dstFormat == null))
			throw new IllegalArgumentException();
		return FXImage.create(dstFormat.getWriteFormat(),
		                      convertPixels(image, dstFormat),
		                      (int) image.getWidth(),
		                      (int) image.getHeight());
	}
}