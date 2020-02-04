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
	
	private static InternalChannels<?> nativeInternalChannels;
	
	@SuppressWarnings("unchecked")
	private static final <T extends Buffer> InternalChannels<T> nativeInternalChannels() {
		return (InternalChannels<T>) (nativeInternalChannels == null
					? nativeInternalChannels = new InternalChannels<>(ImagePixelFormats.getNativeFormat())
					: nativeInternalChannels);
	}
	
	private static final <T extends Buffer> InternalChannels<T> internalChannels(ImagePixelFormat<T> format) {
		return new InternalChannels<>(format);
	}
	
	private static final <T extends Buffer> int combineMasks(InternalChannels<T> ic, ColorChannel... channels) {
		return ic.combineMasks(InternalColorChannel.fromMany(ic.getFormat(), channels));
	}
	
	private static final <T extends Buffer> void separate(InternalChannels<T> ic, T input, byte[] output, ColorChannel channel) {
		ic.separate(input, output, InternalColorChannel.from(ic.getFormat(), channel));
	}
	
	private static final <T extends Buffer> void separate(InternalChannels<T> ic, T input, byte[] output, int shift) {
		ic.separate(input, output, shift);
	}
	
	private static final <T extends Buffer> void separate(InternalChannels<T> ic, T input,
			byte[] red, byte[] green, byte[] blue, byte[] alpha) {
		ic.separate(input, red, green, blue, alpha);
	}
	
	private static final <T extends Buffer> void separate(InternalChannels<T> ic, T input, byte[] red, byte[] green,
			byte[] blue, byte[] alpha, int x, int y, int width, int height, int stride, boolean premultiply) {
		ic.separate(input, red, green, blue, alpha, x, y, width, height, stride, premultiply);
   	}
	
	private static final <T extends Buffer> void join(InternalChannels<T> ic, byte[] input, T output) {
		ic.join(input, output);
	}
	
	private static final <T extends Buffer> void join(InternalChannels<T> ic, byte[] input, T output, int alpha) {
		ic.join(input, output, alpha);
	}
	
	private static final <T extends Buffer> void join(InternalChannels<T> ic, byte[] red, byte[] green, byte[] blue,
			T output, int alpha) {
 		ic.join(red, green, blue, output, alpha);
 	}
	
	private static final <T extends Buffer> void join(InternalChannels<T> ic, byte[] red, byte[] green, byte[] blue,
			byte[] alpha, T output) {
 		ic.join(red, green, blue, alpha, output);
 	}
	
	private static final <T extends Buffer> void join(InternalChannels<T> ic, byte[] red, byte[] green, byte[] blue,
			byte[] alpha, T output, int x, int y, int width, int height, int stride, boolean premultiply) {
		ic.join(red, green, blue, alpha, output, x, y, width, height, stride, premultiply);
   	}
	
	/**
	 * Combines masks of the given color channels to a single mask.
	 * @param pixelFormat The pixel format
	 * @param channels The channels
	 * @return The combined mask*/
	public static final int combineMasks(ImagePixelFormat<?> pixelFormat, ColorChannel... channels) {
		return combineMasks(internalChannels(pixelFormat), channels);
	}
	
	/**
	 * Combines masks of the given color channels to a single mask.
	 * @param channels The channels
	 * @return The combined mask*/
	public static final int combineMasks(ColorChannel... channels) {
		return combineMasks(nativeInternalChannels(), channels);
	}
	
	/**
	 * Separates the given channel from all values in the {@code input} array and stores
	 * the result in the {@code output} array.
	 * @param pixelFormat The pixel format
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param channel The channel to be separated*/
	public static final <T extends Buffer> void separate(ImagePixelFormat<T> pixelFormat, T input, byte[] output, ColorChannel channel) {
		separate(internalChannels(pixelFormat), input, output, channel);
	}
	
	/**
	 * Separates the given channel from all values in the {@code input} array and stores
	 * the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param channel The channel to be separated*/
	public static final <T extends Buffer> void separate(T input, byte[] output, ColorChannel channel) {
		separate(nativeInternalChannels(), input, output, channel);
	}
	
	/**
	 * Separates a channel, given by the shift, from all values in the {@code input} array
	 * and stores the result in the {@code output} array.
	 * @param pixelFormat The pixel format
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param shift The shift of a channel to be separated*/
	public static final <T extends Buffer> void separate(ImagePixelFormat<T> pixelFormat, T input, byte[] output, int shift) {
		separate(internalChannels(pixelFormat), input, output, shift);
	}
	
	/**
	 * Separates a channel, given by the shift, from all values in the {@code input} array
	 * and stores the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param shift The shift of a channel to be separated*/
	public static final <T extends Buffer> void separate(T input, byte[] output, int shift) {
		separate(nativeInternalChannels(), input, output, shift);
	}
	
	/**
	 * Separates red, green, blue and alpha channels from all values in the {@code input}
	 * array and stores the result in equivalent output array.
	 * @param pixelFormat The pixel format
	 * @param input The input of ARGB int colors
	 * @param red The output for red channel
	 * @param green The output for green channel
	 * @param blue The output for blue channel
	 * @param alpha The output for alpha channel*/
	public static final <T extends Buffer> void separate(ImagePixelFormat<T> pixelFormat, T input,
			byte[] red, byte[] green, byte[] blue, byte[] alpha) {
		separate(internalChannels(pixelFormat), input, red, green, blue, alpha);
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
		separate(nativeInternalChannels(), input, red, green, blue, alpha);
	}
	
	public static final <T extends Buffer> void separate(ImagePixelFormat<T> pixelFormat, T input, byte[] red, byte[] green,
 			byte[] blue, byte[] alpha, int x, int y, int width, int height, int stride, boolean premultiply) {
 		separate(internalChannels(pixelFormat), input, red, green, blue, alpha, x, y, width, height, stride, premultiply);
  	}
	
	public static final <T extends Buffer> void separate(T input, byte[] red, byte[] green, byte[] blue, byte[] alpha,
			int x, int y, int width, int height, int stride, boolean premultiply) {
		separate(nativeInternalChannels(), input, red, green, blue, alpha, x, y, width, height, stride, premultiply);
 	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the input array.
	 * @param pixelFormat The pixel format
	 * @param input The input
	 * @param output The output*/
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat, byte[] input, T output) {
		join(internalChannels(pixelFormat), input, output);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the input array.
	 * @param input The input
	 * @param output The output*/
	public static final <T extends Buffer> void join(byte[] input, T output) {
		join(nativeInternalChannels(), input, output);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green and blue component of the new color is that of
	 * the value in the input array, and value of the alpha component is the same as
	 * the one that is given.
	 * @param pixelFormat The pixel format
	 * @param input The input
	 * @param output The output
	 * @param alpha The value of the alpha component of all new colors*/
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat, byte[] input, T output, int alpha) {
		join(internalChannels(pixelFormat), input, output, alpha);
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
		join(nativeInternalChannels(), input, output, alpha);
	}
	
	/**
	 * Converts values in the given input arrays into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green and blue component of the new color is that of
	 * the value in the respective array, and value of the alpha component is
	 * the same as the one that is given.
	 * @param pixelFormat The pixel format
	 * @param red The red component input
	 * @param green The green component input
	 * @param blue The blue component input
	 * @param output The output
	 * @param alpha The value of the alpha component of all new colors*/
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat,
			byte[] red, byte[] green, byte[] blue, T output, int alpha) {
		join(internalChannels(pixelFormat), red, green, blue, output, alpha);
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
		join(nativeInternalChannels(), red, green, blue, output, alpha);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the respective array.
	 * @param pixelFormat The pixel format
	 * @param red The red component input
	 * @param green The green component input
	 * @param blue The blue component input
	 * @param alpha The alpha component input
	 * @param output The output*/
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat,
			byte[] red, byte[] green, byte[] blue, byte[] alpha, T output) {
		join(internalChannels(pixelFormat), red, green, blue, alpha, output);
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
		join(nativeInternalChannels(), red, green, blue, alpha, output);
	}
	
	public static final <T extends Buffer> void join(ImagePixelFormat<T> pixelFormat, byte[] red, byte[] green, byte[] blue,
			byte[] alpha, T output, int x, int y, int width, int height, int stride, boolean premultiply) {
		join(internalChannels(pixelFormat), red, green, blue, alpha, output, x, y, width, height, stride, premultiply);
 	}
	
	public static final <T extends Buffer> void join(byte[] red, byte[] green, byte[] blue, byte[] alpha, T output,
			int x, int y, int width, int height, int stride, boolean premultiply) {
 		join(nativeInternalChannels(), red, green, blue, alpha, output, x, y, width, height, stride, premultiply);
  	}
}