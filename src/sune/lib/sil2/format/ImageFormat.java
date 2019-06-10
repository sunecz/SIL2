package sune.lib.sil2.format;

import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * Represents an image format.*/
public enum ImageFormat {
	
	/**
	 * BMP (Windows Bitmap) format.*/
	BMP(TYPE_INT_RGB,      "*.bmp"),
	/**
	 * GIF (Graphics Interchange Format) format.*/
	GIF(TYPE_BYTE_INDEXED, "*.gif"),
	/**
	 * JPEG (Joint Photographic Experts Group) format.*/
	JPG(TYPE_INT_RGB,      "*.jpg", "*.jpeg"),
	/**
	 * PNG (Portable Network Graphics) format.*/
	PNG(TYPE_INT_ARGB,     "*.png");
	
	private final int      imageType;
	private final String[] extensions;
	private final String   name;
	
	private ImageFormat(int imageType, String... extensions) {
		this.imageType  = imageType;
		this.extensions = extensions;
		this.name       = name().toLowerCase();
	}
	
	/**
	 * Gets an image format from the given file name.
	 * @param name The file name
	 * @return The image format, or null, if no supported image format
	 * with that file type was found, or if no file type is present.*/
	public static final ImageFormat fromFileName(String name) {
		int index = name.lastIndexOf('.');
		if((index > 0)) {
			String type = name.substring(index + 1);
			for(ImageFormat format : values()) {
				String[] extensions = format.getFileExtensions();
				for(String extension : extensions) {
					extension = extension.substring(2);
					if((type.equalsIgnoreCase(extension))) {
						return format;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the internal image type of {@code this} image format.
	 * @return The internal image type*/
	public int getImageType() {
		return imageType;
	}
	
	/**
	 * Gets the array of file extensions of {@code this} image format.
	 * @return The array of file extensions*/
	public String[] getFileExtensions() {
		return extensions;
	}
	
	/**
	 * Gets the internal ImageIO name of {@code this} image format.
	 * @return The internal ImageIO name*/
	public String getImageIOName() {
		return name;
	}
}