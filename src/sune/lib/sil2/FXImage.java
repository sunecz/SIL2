package sune.lib.sil2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

public final class FXImage {
	
	private static final Method method_new_INT_ARGB_PRE;
	private static final Method method_new_BYTE_BGRA_PRE;
	private static final Method method_new_BYTE_RGB;
	
	private static final Field field_Image_url;
	private static final Field field_Image_inputSource;
	private static final Field field_Image_requestedWidth;
	private static final Field field_Image_requestedHeight;
	private static final Field field_Image_preserveRatio;
	private static final Field field_Image_smooth;
	private static final Field field_Image_backgroundLoading;
	private static final Method method_Image_initialize;
	
	static {
		Method _method_new_INT_ARGB_PRE  = null;
		Method _method_new_BYTE_BGRA_PRE = null;
		Method _method_new_BYTE_RGB      = null;
		try {
			Class<?> clazz = Class.forName("com.sun.prism.Image");
			_method_new_INT_ARGB_PRE  = clazz.getMethod("fromIntArgbPreData",  IntBuffer .class, int.class, int.class);
			_method_new_BYTE_BGRA_PRE = clazz.getMethod("fromByteBgraPreData", ByteBuffer.class, int.class, int.class);
			_method_new_BYTE_RGB      = clazz.getMethod("fromByteRgbData",     ByteBuffer.class, int.class, int.class);
			Reflection.setAccessible(_method_new_INT_ARGB_PRE,  true);
			Reflection.setAccessible(_method_new_BYTE_BGRA_PRE, true);
			Reflection.setAccessible(_method_new_BYTE_RGB,      true);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain methods of the Prism Image class", ex);
		}
		method_new_INT_ARGB_PRE  = _method_new_INT_ARGB_PRE;
		method_new_BYTE_BGRA_PRE = _method_new_BYTE_BGRA_PRE;
		method_new_BYTE_RGB      = _method_new_BYTE_RGB;
		Field _field_Image_url               = null;
		Field _field_Image_inputSource       = null;
		Field _field_Image_requestedWidth    = null;
		Field _field_Image_requestedHeight   = null;
		Field _field_Image_preserveRatio     = null;
		Field _field_Image_smooth            = null;
		Field _field_Image_backgroundLoading = null;
		Method _method_Image_initialize      = null;
		try {
			Class<?> clazz = Image.class;
			_field_Image_url               = clazz.getDeclaredField("url");
			_field_Image_inputSource       = clazz.getDeclaredField("inputSource");
			_field_Image_requestedWidth    = clazz.getDeclaredField("requestedWidth");
			_field_Image_requestedHeight   = clazz.getDeclaredField("requestedHeight");
			_field_Image_preserveRatio     = clazz.getDeclaredField("preserveRatio");
			_field_Image_smooth            = clazz.getDeclaredField("smooth");
			_field_Image_backgroundLoading = clazz.getDeclaredField("backgroundLoading");
			_method_Image_initialize       = clazz.getDeclaredMethod("initialize", Object.class);
			Reflection.setAccessible(_field_Image_url,               true);
			Reflection.setAccessible(_field_Image_inputSource,       true);
			Reflection.setAccessible(_field_Image_requestedWidth,    true);
			Reflection.setAccessible(_field_Image_requestedHeight,   true);
			Reflection.setAccessible(_field_Image_preserveRatio,     true);
			Reflection.setAccessible(_field_Image_smooth,            true);
			Reflection.setAccessible(_field_Image_backgroundLoading, true);
			Reflection.setAccessible(_method_Image_initialize,       true);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain fields and methods of the JavaFX Image class", ex);
		}
		field_Image_url               = _field_Image_url;
		field_Image_inputSource       = _field_Image_inputSource;
		field_Image_requestedWidth    = _field_Image_requestedWidth;
		field_Image_requestedHeight   = _field_Image_requestedHeight;
		field_Image_preserveRatio     = _field_Image_preserveRatio;
		field_Image_smooth            = _field_Image_smooth;
		field_Image_backgroundLoading = _field_Image_backgroundLoading;
		method_Image_initialize       = _method_Image_initialize;
	}
	
	// Forbid anyone to create an instance of this class
	private FXImage() {
	}
	
	private static final void ensureBufferClass(Buffer buffer, Class<?> clazz) {
		if(!clazz.isAssignableFrom(buffer.getClass()))
			throw new IllegalArgumentException("Invalid class of buffer (" + buffer.getClass() + "), "
											+  "must be: " + clazz);
	}
	
	private static final void ensureBufferCapacity(Buffer buffer, int capacity) {
		if((buffer.capacity() != capacity))
			throw new IllegalArgumentException("Invalid buffer capacity (" + buffer.capacity() + "), "
											+  "must be: " + capacity);
	}
	
	private static final Object newImage_INT_ARGB_PRE(Buffer buffer, int width, int height) {
		try {
			ensureBufferClass   (buffer, IntBuffer.class);
			ensureBufferCapacity(buffer, width * height);
			return method_new_INT_ARGB_PRE.invoke(null, buffer, width, height);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to create a new image of type INT_ARGB_PRE", ex);
		}
	}
	
	private static final Object newImage_BYTE_BGRA_PRE(Buffer buffer, int width, int height) {
		try {
			ensureBufferClass   (buffer, ByteBuffer.class);
			ensureBufferCapacity(buffer, width * height * 4);
			return method_new_BYTE_BGRA_PRE.invoke(null, buffer, width, height);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to create a new image of type BYTE_BGRA_PRE", ex);
		}
	}
	
	private static final Object newImage_BYTE_RGB(Buffer buffer, int width, int height) {
		try {
			ensureBufferClass   (buffer, ByteBuffer.class);
			ensureBufferCapacity(buffer, width * height * 3);
			return method_new_BYTE_RGB.invoke(null, buffer, width, height);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to create a new image of type BYTE_RGB", ex);
		}
	}
	
	private static final WritableImage initWritableImage(WritableImage image, Object prismImage, int width, int height) {
		try {
			field_Image_url              .set       (image, null);
			field_Image_inputSource      .set       (image, null);
			field_Image_requestedWidth   .setInt    (image, width);
			field_Image_requestedHeight  .setInt    (image, height);
			field_Image_preserveRatio    .setBoolean(image, false);
			field_Image_smooth           .setBoolean(image, false);
			field_Image_backgroundLoading.setBoolean(image, false);
			method_Image_initialize.invoke(image, prismImage);
			return image;
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to initialize a writable image", ex);
		}
	}
	
	private static final WritableImage createWritableImage(Object prismImage, int width, int height) {
		try {
			return initWritableImage((WritableImage)
						UnsafeInstance.get()
						              .allocateInstance(WritableImage.class),
						prismImage, width, height);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to create a new JavaFX writable image", ex);
		}
	}
	
	private static final void premultiplyPixels_INT_ARGB(Buffer buffer) {
		IntBuffer _buffer = (IntBuffer) buffer;
		for(int i = 0, l = buffer.capacity(); i < l; ++i) {
			_buffer.put(i, Colors.linear2premult(_buffer.get(i)));
		}
	}
	
	private static final void premultiplyPixels_BYTE_BGRA(Buffer buffer) {
		ByteBuffer _buffer = (ByteBuffer) buffer;
		byte[] result = new byte[4];
		for(int i = 0, l = buffer.capacity(); i < l; i += 4) {
			Colors.linear2premult(_buffer.get(i + 2) & 0xff,
			                      _buffer.get(i + 1) & 0xff,
			                      _buffer.get(i)     & 0xff,
			                      _buffer.get(i + 3) & 0xff,
			                      result);
			_buffer.put(i,     result[2]);
			_buffer.put(i + 1, result[1]);
			_buffer.put(i + 2, result[0]);
		}
	}
	
	private static final Buffer newBuffer(PixelFormat<?> format, int width, int height) {
		switch(format.getType()) {
			case INT_ARGB:
			case INT_ARGB_PRE:
				return IntBuffer.allocate(width * height);
			case BYTE_BGRA:
			case BYTE_BGRA_PRE:
				return ByteBuffer.allocate(width * height * 4);
			case BYTE_RGB:
				return ByteBuffer.allocate(width * height * 3);
			default:
				throw new IllegalStateException("Unsupported pixel format type: " + format.getType());
		}
	}
	
	/**
	 * Note that Prism allows only premultiplied versions of an image to be created.*/
	public static final WritableImage create(PixelFormat<?> format, int width, int height) {
		return create(format, newBuffer(format, width, height), width, height);
	}
	
	/**
	 * Note that Prism allows only premultiplied versions of an image to be created.*/
	public static final WritableImage create(PixelFormat<?> format, Buffer buffer, int width, int height) {
		switch(format.getType()) {
			case INT_ARGB:
				premultiplyPixels_INT_ARGB(buffer);
			case INT_ARGB_PRE:
				return createWritableImage(newImage_INT_ARGB_PRE(buffer, width, height), width, height);
			case BYTE_BGRA:
				premultiplyPixels_BYTE_BGRA(buffer);
			case BYTE_BGRA_PRE:
				return createWritableImage(newImage_BYTE_BGRA_PRE(buffer, width, height), width, height);
			case BYTE_RGB:
				return createWritableImage(newImage_BYTE_RGB(buffer, width, height), width, height);
			default:
				throw new IllegalStateException("Unsupported pixel format type: " + format.getType());
		}
	}
	
	public static final void update(Image image) {
		FXInternalUtils.updateImage(image);
	}
	
	public static final void updateSafe(Image image) {
		FXInternalUtils.updateImageSafe(image);
	}
	
	public static final Object getPlatformImage(Image image) {
		return FXInternalUtils.getPlatformImage(image);
	}
}