package sune.lib.sil2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import javax.imageio.ImageIO;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import sune.lib.sil2.format.ImageFormat;

public final class FXImageIO {
	
	private static final OpenOption[] OPEN_OPTIONS = { StandardOpenOption.READ };
	private static final OpenOption[] SAVE_OPTIONS = { StandardOpenOption.CREATE, StandardOpenOption.WRITE };
	
	// Forbid anyone to create an instance of this class
	private FXImageIO() {
	}
	
	// ----- OPENING IMAGES
	
	private static final class ImageFrame {
		
		public final int width;
		public final int height;
		public final ByteBuffer buffer;
		public final int type;
		public final byte[][] palette;
		
		public ImageFrame(int width, int height, ByteBuffer buffer, int type, byte[][] palette) {
			this.width = width;
			this.height = height;
			this.buffer = buffer;
			this.type = type;
			this.palette = palette;
		}
	}
	
	// Methods and fields used for reading an image
	private static final Method method_ImageStorage_loadAll;
	private static final Field field_ImageFrame_width;
	private static final Field field_ImageFrame_height;
	private static final Field field_ImageFrame_imageData;
	private static final Field field_ImageFrame_imageType;
	private static final Field field_ImageFrame_palette;
	
	static {
		Method _method_ImageStorage_loadAll = null;
		Field _field_ImageFrame_width = null;
		Field _field_ImageFrame_height = null;
		Field _field_ImageFrame_imageData = null;
		Field _field_ImageFrame_imageType = null;
		Field _field_ImageFrame_palette = null;
		try {
			// Image Storage methods
			Class<?> clazz = Class.forName("com.sun.javafx.iio.ImageStorage");
			Class<?> clazz_ImageLoadListener = Class.forName("com.sun.javafx.iio.ImageLoadListener");
			_method_ImageStorage_loadAll = clazz.getMethod("loadAll",
				InputStream.class, clazz_ImageLoadListener, double.class, double.class,
				boolean.class, float.class, boolean.class);
			Reflection.setAccessible(_method_ImageStorage_loadAll, true);
			// Image Frame methods
			Class<?> clazz_frame = Class.forName("com.sun.javafx.iio.ImageFrame");
			_field_ImageFrame_width = clazz_frame.getDeclaredField("width");
			_field_ImageFrame_height = clazz_frame.getDeclaredField("height");
			_field_ImageFrame_imageData = clazz_frame.getDeclaredField("imageData");
			_field_ImageFrame_imageType = clazz_frame.getDeclaredField("imageType");
			_field_ImageFrame_palette = clazz_frame.getDeclaredField("palette");
			Reflection.setAccessible(_field_ImageFrame_width, true);
			Reflection.setAccessible(_field_ImageFrame_height, true);
			Reflection.setAccessible(_field_ImageFrame_imageData, true);
			Reflection.setAccessible(_field_ImageFrame_imageType, true);
			Reflection.setAccessible(_field_ImageFrame_palette, true);
			
		} catch(ClassNotFoundException
					| NoSuchMethodException
					| SecurityException
					| IllegalArgumentException
					| IllegalAccessException
					| NoSuchFieldException ex) {
			throw new IllegalStateException("Unable to initialize methods and fields", ex);
		}
		method_ImageStorage_loadAll = _method_ImageStorage_loadAll;
		field_ImageFrame_width = _field_ImageFrame_width;
		field_ImageFrame_height = _field_ImageFrame_height;
		field_ImageFrame_imageData = _field_ImageFrame_imageData;
		field_ImageFrame_imageType = _field_ImageFrame_imageType;
		field_ImageFrame_palette = _field_ImageFrame_palette;
	}
	
	private static final Object[] loadFrames(InputStream stream)
			throws IllegalAccessException,
			       InvocationTargetException,
			       IllegalArgumentException {
		return (Object[]) method_ImageStorage_loadAll.invoke(null, stream, null, 0.0, 0.0, true, 1.0f, false);
	}
	
	private static final ImageFrame createImageFrame(Object frame)
			throws IllegalAccessException,
		           InvocationTargetException,
		           IllegalArgumentException {
		int width = field_ImageFrame_width.getInt(frame);
		int height = field_ImageFrame_height.getInt(frame);
		ByteBuffer buffer = (ByteBuffer) field_ImageFrame_imageData.get(frame);
		Enum<?> type = (Enum<?>) field_ImageFrame_imageType.get(frame);
		byte[][] palette = (byte[][]) field_ImageFrame_palette.get(frame);
		return new ImageFrame(width, height, buffer, type.ordinal(), palette);
	}
	
	private static final Image convertImageFrame2Image(ImageFrame frame) {
		return convertImageFrame2Image(frame.width, frame.height, frame.buffer, frame.type, frame.palette);
	}
	
	private static final Image convertImageFrame2Image(int width, int height, ByteBuffer buffer,
			int type, byte[][] palette) {
		// Image types are in the order as defined in ImageType class
		switch(type) {
			// GRAY
			case 0: return convertGray2Image(buffer, width, height);
			// GRAY_ALPHA
			case 1: return convertGrayAlpha2Image(buffer, width, height);
			// GRAY_ALPHA_PRE
			case 2: return convertGrayAlphaPre2Image(buffer, width, height);
			// PALETTE
			case 3: return convertPalette2Image(buffer, width, height, palette);
			// PALETTE_ALPHA
			case 4: return convertPaletteAlpha2Image(buffer, width, height, palette);
			// PALETTE_ALPHA_PRE
			case 5: return convertPaletteAlphaPre2Image(buffer, width, height, palette);
			// PALETTE_TRANS (Not supported)
			//case 6: return convertPaletteTrans2Image(buffer, width, height, palette);
			// RGB
			case 7: return convertRGB2Image(buffer, width, height);
			// RGBA
			case 8: return convertRGBA2Image(buffer, width, height);
			// RGBA_PRE
			case 9: return convertRGBAPre2Image(buffer, width, height);
			default:
				throw new UnsupportedOperationException("Unsupported image type: " + type);
		}
	}
	
	private static final Image convertGray2Image(ByteBuffer buffer, int width, int height) {
		// Since FXImage.create does not support GRAY image types, it is needed to
		// convert the buffer to the RGB format.
		// The solution here is to actually support BYTE_INDEXED format and then
		// convert the buffer to an indexed buffer.
		byte[] array = buffer.array();
		int length = array.length;
		byte[] newArray = new byte[length * 3]; // RGB
		byte v;
		for(int i = length - 1, k = 3 * length - 3; i >= 0; --i, k -= 3) {
			v = array[i];
			newArray[k]   = v;
			newArray[k+1] = v;
			newArray[k+2] = v;
		}
		return FXImage.create(PixelFormat.getByteRgbInstance(),
		                      ByteBuffer.wrap(newArray), width, height);
	}
	
	private static final Image convertGrayAlpha2Image(ByteBuffer buffer, int width, int height) {
		byte[] array = buffer.array();
		int length = array.length;
		byte[] newArray = new byte[length * 4]; // BGRA
		byte v, a;
		for(int i = length - 2, k = 4 * length - 4; i >= 0; i -= 2, k -= 4) {
			v = array[i];
			a = array[i+1];
			newArray[k]   = v;
			newArray[k+1] = v;
			newArray[k+2] = v;
			newArray[k+3] = a;
		}
		return FXImage.create(PixelFormat.getByteBgraInstance(),
		                      ByteBuffer.wrap(newArray), width, height);
	}
	
	private static final Image convertGrayAlphaPre2Image(ByteBuffer buffer, int width, int height) {
		byte[] array = buffer.array();
		int length = array.length;
		byte[] newArray = new byte[length * 4]; // BGRA_PRE
		byte v, a;
		for(int i = length - 2, k = 4 * length - 4; i >= 0; i -= 2, k -= 4) {
			v = array[i];
			a = array[i+1];
			newArray[k]   = v;
			newArray[k+1] = v;
			newArray[k+2] = v;
			newArray[k+3] = a;
		}
		return FXImage.create(PixelFormat.getByteBgraPreInstance(),
		                      ByteBuffer.wrap(newArray), width, height);
	}
	
	private static final Image convertPalette2Image(ByteBuffer buffer, int width, int height, byte[][] palette) {
		byte[] array = buffer.array();
		int length = array.length;
		byte[] newArray = new byte[length * 3]; // RGB
		byte[] r = palette[0];
		byte[] g = palette[1];
		byte[] b = palette[2];
		int v;
		for(int i = length - 1, k = 3 * length - 3; i >= 0; --i, k -= 3) {
			v = array[i] & 0xff;
			newArray[k]   = r[v];
			newArray[k+1] = g[v];
			newArray[k+2] = b[v];
		}
		return FXImage.create(PixelFormat.getByteRgbInstance(),
		                      ByteBuffer.wrap(newArray), width, height);
	}
	
	private static final Image convertPaletteAlpha2Image(ByteBuffer buffer, int width, int height, byte[][] palette) {
		byte[] array = buffer.array();
		int length = array.length;
		byte[] newArray = new byte[length * 4]; // BGRA
		byte[] r = palette[0];
		byte[] g = palette[1];
		byte[] b = palette[2];
		byte[] a = palette[3];
		int v;
		for(int i = length - 1, k = 4 * length - 4; i >= 0; --i, k -= 4) {
			v = array[i] & 0xff;
			newArray[k]   = b[v];
			newArray[k+1] = g[v];
			newArray[k+2] = r[v];
			newArray[k+3] = a[v];
		}
		return FXImage.create(PixelFormat.getByteBgraInstance(),
		                      ByteBuffer.wrap(newArray), width, height);
	}
	
	private static final Image convertPaletteAlphaPre2Image(ByteBuffer buffer, int width, int height, byte[][] palette) {
		byte[] array = buffer.array();
		int length = array.length;
		byte[] newArray = new byte[length * 4]; // BGRA_PRE
		byte[] r = palette[0];
		byte[] g = palette[1];
		byte[] b = palette[2];
		byte[] a = palette[3];
		int v;
		for(int i = length - 1, k = 4 * length - 4; i >= 0; --i, k -= 4) {
			v = array[i] & 0xff;
			newArray[k]   = b[v];
			newArray[k+1] = g[v];
			newArray[k+2] = r[v];
			newArray[k+3] = a[v];
		}
		return FXImage.create(PixelFormat.getByteBgraPreInstance(),
		                      ByteBuffer.wrap(newArray), width, height);
	}
	
	private static final Image convertRGB2Image(ByteBuffer buffer, int width, int height) {
		// Direct conversion. Note: reflection overhead is the real bottleneck here,
		// it is probably not possible to do it any faster than directly calling
		// the actual JavaFX core methods, unless, of course, creating custom reader.
		// It would be insignificantly faster to just use new Image(stream),
		// instead of FXImageIO.open(stream).
		return FXImage.create(PixelFormat.getByteRgbInstance(), buffer, width, height);
	}
	
	private static final Image swapRBChannels(PixelFormat<?> format, ByteBuffer buffer, int width, int height) {
		byte[] array = buffer.array();
		byte t;
		// Swap R <-> B
		for(int i = array.length - 4, k = i + 2; i >= 0; i -= 4, k -= 4) {
			t = array[i];
			array[i] = array[k];
			array[k] = t;
		}
		return FXImage.create(format, buffer, width, height);
	}
	
	private static final Image convertRGBA2Image(ByteBuffer buffer, int width, int height) {
		// Direct conversion with swapping. Cannot go any faster here either (see #convertRGB2Image).
		return swapRBChannels(PixelFormat.getByteBgraInstance(), buffer, width, height);
	}
	
	private static final Image convertRGBAPre2Image(ByteBuffer buffer, int width, int height) {
		// Direct conversion with swapping. Cannot go any faster here either (see #convertRGB2Image).
		return swapRBChannels(PixelFormat.getByteBgraPreInstance(), buffer, width, height);
	}
	
	/**
	 * Opens the given file and reads its contents, outputing an image.
	 * @param file The file
	 * @return The image represented in the given file
	 * @throws IOException if an I/O error occurs.*/
	public static final Image open(File file) throws IOException {
		return open(new BufferedInputStream(new FileInputStream(file)));
	}
	
	/**
	 * Opens the given file and reads its contents, outputing an image.
	 * @param file The file
	 * @return The image represented in the given file
	 * @throws IOException if an I/O error occurs.*/
	public static final Image open(Path file) throws IOException {
		return open(new BufferedInputStream(Files.newInputStream(file, OPEN_OPTIONS)));
	}
	
	/**
	 * Reads the given input stream and gets an image from it.
	 * @param input The input stream
	 * @return The image represented in the given file
	 * @throws IOException if an I/O error occurs.*/
	public static final Image open(InputStream input) throws IOException {
		Objects.requireNonNull(input, "Input stream cannot be null");
		try(InputStream _input = input) {
			Object[] frames = loadFrames(input);
			if((frames == null || frames.length <= 0))
				return null;
			// Convert just the first image frame
			ImageFrame frame = createImageFrame(frames[0]);
			return convertImageFrame2Image(frame);
		} catch(IllegalAccessException
					| InvocationTargetException
					| IllegalArgumentException ex) {
			throw new IOException("Unable to open file", ex);
		}
	}
	
	// -----
	
	// ----- SAVING IMAGES
	
	/**
	 * Writes the given image as the given image format to the given file.
	 * @param image The image
	 * @param format The image format
	 * @param file The file
	 * @return {@code true}, if successfully written, otherwise {@code false}
	 * @throws IOException if an I/O error occurs.*/
	public static final boolean save(Image image, ImageFormat format, File file) throws IOException {
		return save(image, format, new BufferedOutputStream(new FileOutputStream(file)));
	}
	
	/**
	 * Writes the given image as the given image format to the given file.
	 * @param image The image
	 * @param format The image format
	 * @param file The file
	 * @return {@code true}, if successfully written, otherwise {@code false}
	 * @throws IOException if an I/O error occurs.*/
	public static final boolean save(Image image, ImageFormat format, Path file) throws IOException {
		return save(image, format, new BufferedOutputStream(Files.newOutputStream(file, SAVE_OPTIONS)));
	}
	
	/**
	 * Writes the given image as the given image format to the given output stream.
	 * @param image The image
	 * @param format The image format
	 * @param output The output stream
	 * @return {@code true}, if successfully written, otherwise {@code false}
	 * @throws IOException if an I/O error occurs.*/
	public static final boolean save(Image image, ImageFormat format, OutputStream output) throws IOException {
		Objects.requireNonNull(image,  "Image cannot be null");
		Objects.requireNonNull(format, "Image format cannot be null");
		Objects.requireNonNull(output, "Output stream cannot be null");
		try(OutputStream _output = output) {
			// Since the bug with pink-toned image (for JPG) should be fixed now,
			// it is possible to convert directly from FX to AWT and then save.
			// Bug: https://bugs.openjdk.java.net/browse/JDK-8114609
			return ImageIO.write(FXAWT.awtImage(image), format.getImageIOName(), _output);
		}
	}
	
	// -----
}