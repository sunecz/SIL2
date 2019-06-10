package sune.lib.sil2;

import java.nio.Buffer;

import sune.lib.sil2.format.ImagePixelFormat;
import sune.lib.sil2.format.ImagePixelFormats;

/**
 * Collection of methods related to work with color channels.*/
public final class Channels {
	
	// Forbid anyone to create an instance of this class
	private Channels() {
	}
	
	private static ImagePixelFormat<?> nativePixelFormat;
	@SuppressWarnings("unchecked")
	private static final <T extends Buffer> ImagePixelFormat<T> nativePixelFormat() {
		return (ImagePixelFormat<T>)
					(nativePixelFormat == null
						? (nativePixelFormat = ImagePixelFormats.getNativeFormat())
						: (nativePixelFormat));
	}
	
	private static final <T extends Buffer> InternalChannels<T> internalChannels(ImagePixelFormat<T> format) {
		return new InternalChannels<>(format);
	}
	
	/**
	 * Combines masks of the given color channels to a single mask.
	 * @param channels The channels
	 * @return The combined mask*/
	public static final int combineMasks(ImagePixelFormat<?> pixelFormat, ColorChannel... channels) {
		return internalChannels(pixelFormat).combineMasks(InternalColorChannel.fromMany(pixelFormat, channels));
	}
	
	/**
	 * Combines masks of the given color channels to a single mask.
	 * @param channels The channels
	 * @return The combined mask*/
	public static final int combineMasks(ColorChannel... channels) {
		return combineMasks(nativePixelFormat(), channels);
	}
	
	/**
	 * Separates the given channel from all values in the {@code input} array and stores
	 * the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param channel The channel to be separated*/
	public static final <T extends Buffer> void separate(ImagePixelFormat<T> pixelFormat, T input, byte[] output, ColorChannel channel) {
		internalChannels(pixelFormat).separate(input, output, InternalColorChannel.from(pixelFormat, channel));
	}
	
	/**
	 * Separates the given channel from all values in the {@code input} array and stores
	 * the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param channel The channel to be separated*/
	public static final <T extends Buffer> void separate(T input, byte[] output, ColorChannel channel) {
		separate(nativePixelFormat(), input, output, channel);
	}
	
	/**
	 * Separates a channel, given by the shift, from all values in the {@code input} array
	 * and stores the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param shift The shift of a channel to be separated*/
	public static final <T extends Buffer> void separate(ImagePixelFormat<T> pixelFormat, T input, byte[] output, int shift) {
		internalChannels(pixelFormat).separate(input, output, shift);
	}
	
	/**
	 * Separates a channel, given by the shift, from all values in the {@code input} array
	 * and stores the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param shift The shift of a channel to be separated*/
	public static final <T extends Buffer> void separate(T input, byte[] output, int shift) {
		separate(nativePixelFormat(), input, output, shift);
	}
	
	/**
	 * Separates red, green, blue and alpha channels from all values in the {@code input}
	 * array and stores the result in equivalent output array.
	 * @param input The input of ARGB int colors
	 * @param red The output for red channel
	 * @param green The output for green channel
	 * @param blue The output for blue channel
	 * @param alpha The output for alpha channel*/
	public static final <T extends Buffer> void separate(ImagePixelFormat<T> pixelFormat, T input,
			byte[] red, byte[] green, byte[] blue, byte[] alpha) {
		internalChannels(pixelFormat).separate(input, red, green, blue, alpha);
	}
	
	/**
	 * Separates red, green, blue and alpha channels from all values in the {@code input}
	 * array and stores the result in equivalent output array.
	 * @param input The input of ARGB int colors
	 * @param red The output for red channel
	 * @param green The output for green channel
	 * @param blue The output for blue channel
	 * @param alpha The output for alpha channel*/
	public static final <T extends Buffer> void separate(T input, byte[] red, byte[] green, byte[] blue, byte[] alpha) {
		separate(nativePixelFormat(), input, red, green, blue, alpha);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the input array.
	 * @param input The input
	 * @param output The output*/
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat, byte[] input, T output) {
		internalChannels(pixelFormat).join(input, output);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the input array.
	 * @param input The input
	 * @param output The output*/
	public static final <T extends Buffer> void join(byte[] input, T output) {
		join(nativePixelFormat(), input, output);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green and blue component of the new color is that of
	 * the value in the input array, and value of the alpha component is the same as
	 * the one that is given.
	 * @param input The input
	 * @param output The output
	 * @param alpha The value of the alpha component of all new colors*/
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat, byte[] input, T output, int alpha) {
		internalChannels(pixelFormat).join(input, output, alpha);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green and blue component of the new color is that of
	 * the value in the input array, and value of the alpha component is the same as
	 * the one that is given.
	 * @param input The input
	 * @param output The output
	 * @param alpha The value of the alpha component of all new colors*/
	public static final <T extends Buffer> void join(byte[] input, T output, int alpha) {
		join(nativePixelFormat(), input, output, alpha);
	}
	
	/**
	 * Converts values in the given input arrays into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green and blue component of the new color is that of
	 * the value in the respective array, and value of the alpha component is
	 * the same as the one that is given.
	 * @param red The red component input
	 * @param green The green component input
	 * @param blue The blue component input
	 * @param output The output
	 * @param alpha The value of the alpha component of all new colors*/
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat,
			byte[] red, byte[] green, byte[] blue, T output, int alpha) {
		internalChannels(pixelFormat).join(red, green, blue, output, alpha);
	}
	
	/**
	 * Converts values in the given input arrays into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green and blue component of the new color is that of
	 * the value in the respective array, and value of the alpha component is
	 * the same as the one that is given.
	 * @param red The red component input
	 * @param green The green component input
	 * @param blue The blue component input
	 * @param output The output
	 * @param alpha The value of the alpha component of all new colors*/
	public static final <T extends Buffer> void join(byte[] red, byte[] green, byte[] blue, T output, int alpha) {
		join(nativePixelFormat(), red, green, blue, output, alpha);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the respective array.
	 * @param red The red component input
	 * @param green The green component input
	 * @param blue The blue component input
	 * @param alpha The alpha component input
	 * @param output The output*/
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat,
			byte[] red, byte[] green, byte[] blue, byte[] alpha, T output) {
		internalChannels(pixelFormat).join(red, green, blue, alpha, output);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the respective array.
	 * @param red The red component input
	 * @param green The green component input
	 * @param blue The blue component input
	 * @param alpha The alpha component input
	 * @param output The output*/
	public static final <T extends Buffer> void join(byte[] red, byte[] green, byte[] blue, byte[] alpha, T output) {
		join(nativePixelFormat(), red, green, blue, alpha, output);
	}
}