package sune.lib.sil2;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import javax.imageio.ImageIO;

import javafx.scene.image.Image;

/**
 * Class containing methods for opening images.*/
public final class ImageOpener {
	
	// Forbid anyone to create an instance of this class
	private ImageOpener() {
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
		return open(Files.newInputStream(file, StandardOpenOption.READ));
	}
	
	/**
	 * Reads the given input stream and gets an image from it.
	 * @param input The input stream
	 * @return The image represented in the given file
	 * @throws IOException if an I/O error occurs.*/
	public static final Image open(InputStream input) throws IOException {
		Objects.requireNonNull(input, "Input stream cannot be null");
		try(InputStream _input = input) {
			BufferedImage bimg = ImageIO.read(_input);
			if((bimg != null))
				return FXAWT.fxImage(bimg);
		}
		return null;
	}
}