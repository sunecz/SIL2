package sune.lib.sil2.format;

import java.nio.Buffer;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import sune.lib.sil2.NativeImage;

public final class ImagePixelFormats {
	
	// Forbid anyone to create an instance of this class
	private ImagePixelFormats() {
	}
	
	public static final ImagePixelFormat<?> from(PixelFormat<?> format) {
		switch(format.getType()) {
			case INT_ARGB_PRE:
				return new ARGBPreImagePixelFormat();
			case INT_ARGB:
				return new ARGBImagePixelFormat();
			case BYTE_RGB:
				return new RGBImagePixelFormat();
			case BYTE_BGRA_PRE:
				return new BGRAPreImagePixelFormat();
			case BYTE_BGRA:
				return new BGRAImagePixelFormat();
			default:
				throw new UnsupportedOperationException("Unsupported pixel format");
		}
	}
	
	public static final ImagePixelFormat<?> from(Image image) {
		return from(image.getPixelReader().getPixelFormat());
	}
	
	public static final ImagePixelFormat<Buffer> fromAsBuffer(PixelFormat<?> format) {
		@SuppressWarnings("unchecked")
		ImagePixelFormat<Buffer> pixelFormat = (ImagePixelFormat<Buffer>) from(format);
		return pixelFormat;
	}
	
	public static final ImagePixelFormat<?> getNativeFormat() {
		return from(NativeImage.getNativePixelFormat());
	}
}