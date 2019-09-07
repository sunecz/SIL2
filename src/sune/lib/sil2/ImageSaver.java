package sune.lib.sil2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import javax.imageio.ImageIO;

import javafx.scene.image.Image;
import sune.lib.sil2.format.ImageFormat;

/**
 * Class containing methods for saving images.*/
public final class ImageSaver {
	
	private static final OpenOption[] OPEN_OPTIONS = { StandardOpenOption.CREATE, StandardOpenOption.WRITE };
	
	// Forbid anyone to create an instance of this class
	private ImageSaver() {
	}
	
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
		return save(image, format, new BufferedOutputStream(Files.newOutputStream(file, OPEN_OPTIONS)));
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
			int width  = (int) image.getWidth();
			int height = (int) image.getHeight();
			// Fixes bug with a pink-toned image
			BufferedImage iimg = FXAWT.awtImage(image);
			BufferedImage bimg = new BufferedImage(width, height, format.getImageType());
			Graphics2D g2d = bimg.createGraphics();
			// Make sure that the background is white, when there is a loss of transparency
			if((bimg.getTransparency() == Transparency.OPAQUE)) {
				g2d.setBackground(Color.WHITE);
				g2d.clearRect(0, 0, width, height);
			}
			g2d.drawRenderedImage(iimg, null);
			g2d.dispose();
			return ImageIO.write(bimg, format.getImageIOName(), _output);
		}
	}
}