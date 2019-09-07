package sune.lib.sil2;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;

/**
 * Contains methods for converting AWT images to JavaFX ones and vice-versa.
 * This class should be faster than {@linkplain SwingFXUtils} class since it uses
 * faster techniques to convert images. Also methods in this class try to keep
 * the internal format of an image, i.e. if the format of an AWT image is
 * {@link BufferedImage.TYPE_4BYTE_ABGR_PRE TYPE_4BYTE_ABGR_PRE} then
 * the converted image will have the same format equivalent in JavaFX.
 * Note that only premultiplied versions of a JavaFX Image can be created
 * internally, thus making it impossible to obtain non-premultiplied versions
 * of an AWT image, with the exception of
 * {@link BufferedImage.TYPE_3BYTE_BGR TYPE_3BYTE_BGR} format.
 * @see SwingFXUtils
 * @since 2.0
 * @author Sune*/
public final class FXAWT {
	
	private static final int UNSUPPORTED_FORMAT = 999;
	/*
	 * Use the premultiplied version since some algorithms in the standard Java libraries
	 * use divisions in them for the non-premultiplied version.
	 */
	private static final int DEFAULT_BUFFERED_IMAGE_FORMAT = BufferedImage.TYPE_INT_ARGB_PRE;
	
	// Forbid anyone to create an instance of this class
	private FXAWT() {
	}
	
	// ----- FX -> AWT
	
	private static final int getBufferedImageType(PixelFormat<?> format) {
		switch(format.getType()) {
			case INT_ARGB_PRE:  return BufferedImage.TYPE_INT_ARGB_PRE;
			case INT_ARGB:      return BufferedImage.TYPE_INT_ARGB;
			case BYTE_BGRA_PRE: return BufferedImage.TYPE_4BYTE_ABGR_PRE;
			case BYTE_BGRA:     return BufferedImage.TYPE_4BYTE_ABGR;
			case BYTE_RGB:      return BufferedImage.TYPE_3BYTE_BGR;
			default:
				// Rather than throwing an exception, notify the caller method
				// that this format is unsupported to take an appropriate action.
				return UNSUPPORTED_FORMAT;
		}
	}
	
	private static final void copyPixels(Buffer original, BufferedImage bimg, PixelFormat<?> format) {
		switch(format.getType()) {
			case INT_ARGB_PRE:
			case INT_ARGB:
				copyPixels_intARGB(original, bimg);
				break;
			case BYTE_BGRA_PRE:
			case BYTE_BGRA:
				copyPixels_byteBGRA2byteABGR(original, bimg);
				break;
			case BYTE_RGB:
				copyPixels_byteRGB2byteBGR(original, bimg);
				break;
			default:
				throw new IllegalStateException("Unsupported pixel format type: " + format.getType());
		}
	}
	
	private static final void copyPixels_intARGB(Buffer original, BufferedImage bimg) {
		// AWT format: TYPE_INT_ARGB
		IntBuffer buffer = (IntBuffer) original;
		int length = buffer.capacity();
		int[] pixels = ((DataBufferInt) bimg.getRaster().getDataBuffer()).getData();
		System.arraycopy(buffer.array(), 0, pixels, 0, length);
	}
	
	private static final void copyPixels_byteBGRA2byteABGR(Buffer original, BufferedImage bimg) {
		// AWT format: TYPE_4BYTE_ABGR
		ByteBuffer buffer = (ByteBuffer) original;
		int length = buffer.capacity();
		byte[] pixels = ((DataBufferByte) bimg.getRaster().getDataBuffer()).getData();
		//   B G R A|B G R A|B G R A|...
		//-> x B G R|A B G R|A B G R|...
		System.arraycopy(buffer.array(), 0, pixels, 1, length - 1);
		//-> Each pixel has an alpha value of the next pixel (except first)
		for(int i = 0, l = length - 4; i < l; i += 4) {
			pixels[i] = pixels[i + 4];
		}
		//-> Last alpha value not copied
		pixels[length - 4] = buffer.get(length - 1);
	}
	
	private static final void copyPixels_byteRGB2byteBGR(Buffer original, BufferedImage bimg) {
		// AWT format: TYPE_3BYTE_BGR
		ByteBuffer buffer = (ByteBuffer) original;
		int length = buffer.capacity();
		byte[] pixels = ((DataBufferByte) bimg.getRaster().getDataBuffer()).getData();
		System.arraycopy(buffer.array(), 0, pixels, 0, length);
		// Reorder: for each pixel swap B <-> R
		for(int i = 0; i < length; i += 3) {
			byte temp = pixels[i];
			pixels[i] = pixels[i + 2];
			pixels[i + 2] = temp;
		}
	}
	
	/**
	 * Creates a new AWT image from the given JavaFX image, copying all its pixels
	 * into the newly created image. The internal format of the newly created image
	 * is based on the given JavaFX image, but the best effort is made so that
	 * it results into an equivalent format in AWT.
	 * @param image Image to convert
	 * @return Converted JavaFX image as an AWT image*/
	public static final BufferedImage awtImage(Image image) {
		int width  = (int) image.getWidth();
		int height = (int) image.getHeight();
		PixelFormat<?> format = image.getPixelReader().getPixelFormat();
		int type = getBufferedImageType(format);
		BufferedImage bimg;
		if((type != UNSUPPORTED_FORMAT)) {
			bimg = new BufferedImage(width, height, type);
			Buffer original = ImageUtils.getPixels(image);
			copyPixels(original, bimg, format);
		} else {
			type = DEFAULT_BUFFERED_IMAGE_FORMAT;
			bimg = new BufferedImage(width, height, type);
			int[] pixels = ((DataBufferInt) bimg.getRaster().getDataBuffer()).getData();
			image.getPixelReader().getPixels(0, 0, width, height,
				WritablePixelFormat.getIntArgbPreInstance(), pixels,
				0, width);
		}
		return bimg;
	}
	
	// -----
	
	// ----- AWT -> FX
	
	private static final IntBuffer convert_intRGB2intARGB(IntBuffer buffer) {
		for(int i = 0, l = buffer.capacity(); i < l; ++i) {
			buffer.put(i, 0xff000000 | (buffer.get(i) & 0x00ffffff));
		}
		return buffer;
	}
	
	private static final IntBuffer convert_intBGR2intARGB(IntBuffer buffer) {
		for(int i = 0, l = buffer.capacity(); i < l; ++i) {
			int value = buffer.get(i);
			buffer.put(i, 0xff000000 | (value & 0x0000ff00) |
			              ((value & 0x00ff0000) >> 16) |
			              ((value & 0x000000ff) << 16));
		}
		return buffer;
	}
	
	private static final ByteBuffer convert_byteABGR2byteBGRA(ByteBuffer buffer) {
		for(int i = 0, l = buffer.capacity(); i < l; i += 4) {
			byte a = buffer.get(i);
			byte b = buffer.get(i + 1);
			byte g = buffer.get(i + 2);
			byte r = buffer.get(i + 3);
			buffer.put(i, b);
			buffer.put(i + 1, g);
			buffer.put(i + 2, r);
			buffer.put(i + 3, a);
		}
		return buffer;
	}
	
	private static final ByteBuffer convert_byteBGR2byteRGB(ByteBuffer buffer) {
		for(int i = 0, l = buffer.capacity(); i < l; i += 3) {
			byte b = buffer.get(i);
			buffer.put(i, buffer.get(i + 2));
			buffer.put(i + 2, b);
		}
		return buffer;
	}
	
	private static final BufferedImage drawCopy(BufferedImage image) {
		BufferedImage bimg = new BufferedImage(image.getWidth(),
			image.getHeight(), DEFAULT_BUFFERED_IMAGE_FORMAT);
		Graphics2D g2d = bimg.createGraphics();
		g2d.drawImage(image, 0, 0, null);
		g2d.dispose();
		return bimg;
	}
	
	/**
	 * Creates a new JavaFX image from the given AWT image, copying all its pixels
	 * into the newly created image. The internal format of the newly created image
	 * is based on the given AWT image, but the best effort is made so that
	 * it results into an equivalent format in JavaFX.
	 * @param image Image to convert
	 * @return Converted AWT image as an JavaFX image*/
	public static final WritableImage fxImage(BufferedImage image) {
		int width  = image.getWidth();
		int height = image.getHeight();
		switch(image.getType()) {
			default:
				// Copy the image to an image with the default supported format.
				image = drawCopy(image);
			// DEFAULT_BUFFERED_IMAGE_FORMAT:
			case BufferedImage.TYPE_INT_ARGB_PRE:
				return FXImage.create(PixelFormat.getIntArgbPreInstance(),
				                      BufferUtils.copy(
					                      IntBuffer.wrap(((DataBufferInt) image.getRaster()
					                    		                               .getDataBuffer())
					                                     .getData())
					                  ),
				                      width, height);
			case BufferedImage.TYPE_INT_ARGB:
				return FXImage.create(PixelFormat.getIntArgbInstance(),
				                      BufferUtils.copy(
					                      IntBuffer.wrap(((DataBufferInt) image.getRaster()
					                    		                               .getDataBuffer())
					                                     .getData())
					                  ),
				                      width, height);
			case BufferedImage.TYPE_INT_RGB:
				return FXImage.create(PixelFormat.getIntArgbInstance(),
				                      convert_intRGB2intARGB(
				                            BufferUtils.copy(
					                      		IntBuffer.wrap(((DataBufferInt) image.getRaster()
					                    		                                     .getDataBuffer())
					                                           .getData())
				                      		)),
				                      width, height);
			case BufferedImage.TYPE_INT_BGR:
				return FXImage.create(PixelFormat.getIntArgbInstance(),
				                      convert_intBGR2intARGB(
				                            BufferUtils.copy(
					                      		IntBuffer.wrap(((DataBufferInt) image.getRaster()
					                    		                                     .getDataBuffer())
					                                           .getData())
				                            )),
				                      width, height);
			case BufferedImage.TYPE_4BYTE_ABGR:
				return FXImage.create(PixelFormat.getByteBgraInstance(),
				                      convert_byteABGR2byteBGRA(
				                            BufferUtils.copy(
					                      		ByteBuffer.wrap(((DataBufferByte) image.getRaster()
					                    		                                       .getDataBuffer())
					                                            .getData())
				                      		)),
				                      width, height);
			case BufferedImage.TYPE_4BYTE_ABGR_PRE:
				return FXImage.create(PixelFormat.getByteBgraPreInstance(),
				                      convert_byteABGR2byteBGRA(
				                            BufferUtils.copy(
					                      		ByteBuffer.wrap(((DataBufferByte) image.getRaster()
					                    		                                       .getDataBuffer())
					                                            .getData())
				                      		)),
				                      width, height);
			case BufferedImage.TYPE_3BYTE_BGR:
				return FXImage.create(PixelFormat.getByteRgbInstance(),
				                      convert_byteBGR2byteRGB(
				                            BufferUtils.copy(
					                      		ByteBuffer.wrap(((DataBufferByte) image.getRaster()
					                    		                                       .getDataBuffer())
					                                            .getData())
				                      		)),
				                      width, height);
		}
	}
	
	// -----
}