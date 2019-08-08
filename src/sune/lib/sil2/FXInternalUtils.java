package sune.lib.sil2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;

import javafx.application.Platform;
import javafx.scene.image.Image;

final class FXInternalUtils {
	
	public static final class PlatformImageWrapper {
		
		// Reflection
		private static final Class<?> clazz;
		private static final Field field_serial;
		private static final Field field_buffer;
		
		static {
			Class<?> _clazz = null;
			Field _field_serial = null;
			Field _field_buffer = null;
			try {
				_clazz = Class.forName("com.sun.prism.Image");
				_field_serial = _clazz.getDeclaredField("serial");
				_field_buffer = _clazz.getDeclaredField("pixelBuffer");
				Reflection.setAccessible(_field_serial, true);
				Reflection.setAccessible(_field_buffer, true);
			} catch(Exception ex) {
				throw new IllegalStateException("Unable to obtain fields from platform image class.");
			}
			clazz = _clazz;
			field_serial = _field_serial;
			field_buffer = _field_buffer;
		}
		
		// Properties
		private final Object image;
		
		public PlatformImageWrapper(Object image) {
			if((image == null))
				throw new IllegalArgumentException("Image cannot be null");
			if((clazz != image.getClass()))
				throw new IllegalArgumentException("Not a prism image");
			this.image = image;
		}
		
		public void update() throws Exception {
			++((int[]) field_serial.get(image))[0];
		}
		
		public Buffer buffer() throws Exception {
			return (Buffer) field_buffer.get(image);
		}
		
		public Buffer bufferRewind() {
			try {
				return buffer().rewind();
			} catch(Exception ex) {
				throw new IllegalStateException("Unable to obtain buffer");
			}
		}
	}
	
	// Forbid anyone to create an instance of this class
	private FXInternalUtils() {
	}
	
	private static Method method_getPlatformImage;
	private static Method method_pixelsDirty;
	private static boolean inited;
	
	private static final void initFields() throws Exception {
		if((inited)) return;
		method_getPlatformImage = Image.class.getDeclaredMethod("getPlatformImage");
		method_pixelsDirty      = Image.class.getDeclaredMethod("pixelsDirty");
		Reflection.setAccessible(method_getPlatformImage, true);
		Reflection.setAccessible(method_pixelsDirty,      true);
		inited = true;
	}
	
	private static final Object _getPlatformImage(Image image) throws Exception {
		initFields();
		return method_getPlatformImage.invoke(image);
	}
	
	private static final void _updateImage(Image image) throws Exception {
		initFields();
		method_pixelsDirty.invoke(image);
	}
	
	public static final PlatformImageWrapper getPlatformImageWrapper(Image image) {
		try {
			Object platformImage = _getPlatformImage(image);
			return new PlatformImageWrapper(platformImage);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain platform image wrapper from FX image: " + image);
		}
	}
	
	public static final Object getPlatformImage(Image image) {
		try {
			return _getPlatformImage(image);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain platform image from FX image: " + image);
		}
	}
	
	public static final void updateImage(Image image) {
		try {
			_updateImage(image);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to update FX image: " + image);
		}
	}
	
	public static final void updateImageSafe(Image image) {
		Platform.runLater(() -> updateImage(image));
	}
}