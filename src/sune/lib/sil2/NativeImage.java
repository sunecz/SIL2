package sune.lib.sil2;

import java.lang.reflect.Method;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelFormat.Type;
import javafx.scene.image.WritableImage;

public final class NativeImage {
	
	private static final PixelFormat<?> nativePixelFormat;
	
	// Forbid anyone to create an instance of this class
	private NativeImage() {
	}
	
	private static final Object getToolkit() throws Exception {
		Class<?> clazz = Class.forName("com.sun.javafx.tk.Toolkit");
		Method method = clazz.getMethod("getToolkit");
		Reflection.setAccessible(method, true);
		return method.invoke(null);
	}
	
	private static final Object createPlatformImage(Object toolkit, int width, int height) throws Exception {
		Class<?> clazz = Class.forName("com.sun.javafx.tk.Toolkit");
		Method method = clazz.getMethod("createPlatformImage", int.class, int.class);
		Reflection.setAccessible(method, true);
		return method.invoke(toolkit, width, height);
	}
	
	private static final PixelFormat<?> getPlatformPixelFormat(Object platformImage) throws Exception {
		Class<?> clazz = Class.forName("com.sun.javafx.tk.PlatformImage");
		Method method = clazz.getMethod("getPlatformPixelFormat");
		Reflection.setAccessible(method, true);
		return (PixelFormat<?>) method.invoke(platformImage);
	}
	
	static {
		PixelFormat<?> format = null;
		try {
			Object platformImage = createPlatformImage(getToolkit(), 1, 1);
			format = getPlatformPixelFormat(platformImage);
			platformImage = null; // Help the GC
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain the native image format", ex);
		}
		nativePixelFormat = format;
	}
	
	public static final Image ensurePixelFormat(Image image) {
		PixelFormat<?> format = image.getPixelReader().getPixelFormat();
		Type typeImg = format.getType();
		Type typeNat = nativePixelFormat.getType();
		// If the formats are the same, no neede to convert the image
		if((typeImg == typeNat)) return image;
		int width  = (int) image.getWidth();
		int height = (int) image.getHeight();
		WritableImage dstImg = new WritableImage(width, height);
		dstImg.getPixelWriter().setPixels(0, 0, width, height, image.getPixelReader(), 0, 0);
		return dstImg;
	}
	
	public static final PixelFormat<?> getNativePixelFormat() {
		return nativePixelFormat;
	}
}
