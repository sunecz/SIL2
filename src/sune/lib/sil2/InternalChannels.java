package sune.lib.sil2;

import java.nio.Buffer;

import sune.lib.sil2.format.ImagePixelFormat;

/**
 * Collection of methods related to work with color channels.*/
final class InternalChannels<T extends Buffer> {
	
	// TODO: Update JavaDoc
	
	private final ImagePixelFormat<T> format;
	
	private final int shiftB;
	private final int shiftG;
	private final int shiftR;
	private final int shiftA;
	
	public InternalChannels(ImagePixelFormat<T> pixelFormat) {
		format = pixelFormat;
		shiftB = format.getShiftB();
		shiftG = format.getShiftG();
		shiftR = format.getShiftR();
		shiftA = format.getShiftA();
	}
	
	/**
	 * Combines masks of the given color channels to a single mask.
	 * @param channels The channels
	 * @return The combined mask*/
	public final int combineMasks(InternalColorChannel... channels) {
		int mask = 0x0;
		for(int i = 0, l = channels.length; i < l; ++i)
			mask |= channels[i].getMask();
		return mask;
	}
	
	/**
	 * Separates the given channel from all values in the {@code input} array and stores
	 * the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param channel The channel to be separated*/
	public final void separate(T input, byte[] output, InternalColorChannel channel) {
		separate(input, output, channel.getShift());
	}
	
	/**
	 * Separates the given channel from all values in the {@code input} array and stores
	 * the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param channel The channel to be separated*/
	public final void separatePre(T input, byte[] output, InternalColorChannel channel) {
		separatePre(input, output, channel.getShift());
	}
	
	/**
	 * Separates the given channel from all values in the {@code input} array and stores
	 * the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param channel The channel to be separated
	 * @param premultiply If {@code true}, premultiplies the pixels*/
	public final void separate(T input, byte[] output, InternalColorChannel channel, boolean premultiply) {
		if((premultiply)) separatePre(input, output, channel);
		else              separate   (input, output, channel);
	}
	
	/**
	 * Separates a channel, given by the shift, from all values in the {@code input} array
	 * and stores the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param shift The shift of a channel to be separated*/
	public final void separate(T input, byte[] output, int shift) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, l = input.capacity(); i < l; i+=epp, ++k) {
			output[k] = (byte) ((format.getARGB(input, i) >> shift) & 0xff);
		}
	}
	
	/**
	 * Separates a channel, given by the shift, from all values in the {@code input} array
	 * and stores the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param shift The shift of a channel to be separated*/
	public final void separatePre(T input, byte[] output, int shift) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, l = input.capacity(); i < l; i+=epp, ++k) {
			output[k] = (byte) ((format.getARGBPre(input, i) >> shift) & 0xff);
		}
	}
	
	/**
	 * Separates a channel, given by the shift, from all values in the {@code input} array
	 * and stores the result in the {@code output} array.
	 * @param input The input of ARGB int colors
	 * @param output The output
	 * @param shift The shift of a channel to be separated
	 * @param premultiply If {@code true}, premultiplies the pixels*/
	public final void separate(T input, byte[] output, int shift, boolean premultiply) {
		if((premultiply)) separatePre(input, output, shift);
		else              separate   (input, output, shift);
	}
	
	/**
	 * Separates red, green, blue and alpha channels from all values in the {@code input}
	 * array and stores the result in equivalent output array.
	 * @param input The input of ARGB int colors
	 * @param red The output for red channel
	 * @param green The output for green channel
	 * @param blue The output for blue channel
	 * @param alpha The output for alpha channel*/
	public final void separate(T input, byte[] red, byte[] green, byte[] blue, byte[] alpha) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, l = input.capacity(), color; i < l; i+=epp, ++k) {
			color    = format.getARGB(input, i);
			red  [k] = (byte) ((color >> shiftR) & 0xff);
			green[k] = (byte) ((color >> shiftG) & 0xff);
			blue [k] = (byte) ((color >> shiftB) & 0xff);
			alpha[k] = (byte) ((color >> shiftA) & 0xff);
		}
	}
	
	/**
	 * Separates red, green, blue and alpha channels from all values in the {@code input}
	 * array and stores the result in equivalent output array.
	 * @param input The input of ARGB int colors
	 * @param red The output for red channel
	 * @param green The output for green channel
	 * @param blue The output for blue channel
	 * @param alpha The output for alpha channel*/
	public final void separatePre(T input, byte[] red, byte[] green, byte[] blue, byte[] alpha) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, l = input.capacity(), color; i < l; i+=epp, ++k) {
			color    = format.getARGBPre(input, i);
			red  [k] = (byte) ((color >> shiftR) & 0xff);
			green[k] = (byte) ((color >> shiftG) & 0xff);
			blue [k] = (byte) ((color >> shiftB) & 0xff);
			alpha[k] = (byte) ((color >> shiftA) & 0xff);
		}
	}
	
	/**
	 * Separates red, green, blue and alpha channels from all values in the {@code input}
	 * array and stores the result in equivalent output array.
	 * @param input The input of ARGB int colors
	 * @param red The output for red channel
	 * @param green The output for green channel
	 * @param blue The output for blue channel
	 * @param alpha The output for alpha channel
	 * @param premultiply If {@code true}, premultiplies the pixels*/
	public final void separate(T input, byte[] red, byte[] green, byte[] blue, byte[] alpha, boolean premultiply) {
		if((premultiply)) separatePre(input, red, green, blue, alpha);
		else              separate   (input, red, green, blue, alpha);
	}
	
	public final void separate(T input, byte[] red, byte[] green, byte[] blue, byte[] alpha,
			int x, int y, int width, int height, int stride) {
		int epp = format.getElementsPerPixel(), sk = stride - width, si = sk * epp;
		for(int r = height, c = width, k = y * stride + x, i = k * epp, color;; i+=epp, ++k) {
			color    = format.getARGB(input, i);
			red  [k] = (byte) ((color >> shiftR) & 0xff);
			green[k] = (byte) ((color >> shiftG) & 0xff);
			blue [k] = (byte) ((color >> shiftB) & 0xff);
			alpha[k] = (byte) ((color >> shiftA) & 0xff);
			if((--c == 0)) {
				c  = width;
				k += sk;
				i += si;
				if((--r == 0))
					break;
			}
		}
	}
	
	public final void separatePre(T input, byte[] red, byte[] green, byte[] blue, byte[] alpha,
			int x, int y, int width, int height, int stride) {
		int epp = format.getElementsPerPixel(), sk = stride - width, si = sk * epp;
		for(int r = height, c = width, k = y * stride + x, i = k * epp, color;; i+=epp, ++k) {
			color    = format.getARGBPre(input, i);
			red  [k] = (byte) ((color >> shiftR) & 0xff);
			green[k] = (byte) ((color >> shiftG) & 0xff);
			blue [k] = (byte) ((color >> shiftB) & 0xff);
			alpha[k] = (byte) ((color >> shiftA) & 0xff);
			if((--c == 0)) {
				c  = width;
				k += sk;
				i += si;
				if((--r == 0))
					break;
			}
		}
	}
	
	public final void separate(T input, byte[] red, byte[] green, byte[] blue, byte[] alpha,
			int x, int y, int width, int height, int stride, boolean premultiply) {
		if((premultiply)) separatePre(input, red, green, blue, alpha, x, y, width, height, stride);
		else              separate   (input, red, green, blue, alpha, x, y, width, height, stride);
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the input array.
	 * @param input The input
	 * @param output The output*/
	public final void join(byte[] input, T output) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, l = output.capacity(), color; i < l; i+=epp, ++k) {
			color = input[k] & 0xff;
			format.setARGB(output, i, (color << shiftA) |
			                          (color << shiftR) |
			                          (color << shiftG) |
			                          (color << shiftB));
		}
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the input array.
	 * @param input The input
	 * @param output The output*/
	public final void joinPre(byte[] input, T output) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, l = output.capacity(), color; i < l; i+=epp, ++k) {
			color = input[k] & 0xff;
			format.setARGBPre(output, i, (color << shiftA) |
			                             (color << shiftR) |
			                             (color << shiftG) |
			                             (color << shiftB));
		}
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green, blue and alpha component of the new color is
	 * that of the value in the input array.
	 * @param input The input
	 * @param output The output
	 * @param premultiply If {@code true}, premultiplies the pixels*/
	public final void join(byte[] input, T output, boolean premultiply) {
		if((premultiply)) joinPre(input, output);
		else              join   (input, output);
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
	public final void join(byte[] input, T output, int alpha) {
		int epp = format.getElementsPerPixel();
		int valueA = (alpha & 0xff) << shiftA;
		for(int i = 0, k = 0, l = output.capacity(), color; i < l; i+=epp, ++k) {
			color = input[k] & 0xff;
			format.setARGB(output, i, (color << shiftR) |
			                          (color << shiftG) |
			                          (color << shiftB) |
			                          (valueA));
		}
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
	public final void joinPre(byte[] input, T output, int alpha) {
		int epp = format.getElementsPerPixel();
		int valueA = (alpha & 0xff) << shiftA;
		for(int i = 0, k = 0, l = output.capacity(), color; i < l; i+=epp, ++k) {
			color = input[k] & 0xff;
			format.setARGBPre(output, i, (color << shiftR) |
			                             (color << shiftG) |
			                             (color << shiftB) |
			                             (valueA));
		}
	}
	
	/**
	 * Converts values in the given input array into ARGB int colors and store
	 * them in the given output array. This method converts the input so that
	 * value of each of red, green and blue component of the new color is that of
	 * the value in the input array, and value of the alpha component is the same as
	 * the one that is given.
	 * @param input The input
	 * @param output The output
	 * @param alpha The value of the alpha component of all new colors
	 * @param premultiply If {@code true}, premultiplies the pixels*/
	public final void join(byte[] input, T output, int alpha, boolean premultiply) {
		if((premultiply)) joinPre(input, output, alpha);
		else              join   (input, output, alpha);
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
	public final void join(byte[] red, byte[] green, byte[] blue, T output, int alpha) {
		int epp = format.getElementsPerPixel();
		int valueA = (alpha & 0xff) << shiftA;
		for(int i = 0, k = 0, l = output.capacity(); i < l; i+=epp, ++k) {
			format.setARGB(output, i, ((red  [k] & 0xff) << shiftR) |
			                          ((green[k] & 0xff) << shiftG) |
			                          ((blue [k] & 0xff) << shiftB) |
			                          ((valueA)));
		}
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
	public final void joinPre(byte[] red, byte[] green, byte[] blue, T output, int alpha) {
		int epp = format.getElementsPerPixel();
		int valueA = (alpha & 0xff) << shiftA;
		for(int i = 0, k = 0, l = output.capacity(); i < l; i+=epp, ++k) {
			format.setARGBPre(output, i, ((red  [k] & 0xff) << shiftR) |
			                             ((green[k] & 0xff) << shiftG) |
			                             ((blue [k] & 0xff) << shiftB) |
			                             ((valueA)));
		}
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
	 * @param alpha The value of the alpha component of all new colors
	 * @param premultiply If {@code true}, premultiplies the pixels*/
	public final void join(byte[] red, byte[] green, byte[] blue, T output, int alpha, boolean premultiply) {
		if((premultiply)) joinPre(red, green, blue, output, alpha);
		else              join   (red, green, blue, output, alpha);
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
	public final void join(byte[] red, byte[] green, byte[] blue, byte[] alpha, T output) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, l = output.capacity(); i < l; i+=epp, ++k) {
			format.setARGB(output, i, ((alpha[k] & 0xff) << shiftA) |
			                          ((red  [k] & 0xff) << shiftR) |
			                          ((green[k] & 0xff) << shiftG) |
			                          ((blue [k] & 0xff) << shiftB));
		}
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
	public final void joinPre(byte[] red, byte[] green, byte[] blue, byte[] alpha, T output) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, l = output.capacity(); i < l; i+=epp, ++k) {
			format.setARGBPre(output, i, ((alpha[k] & 0xff) << shiftA) |
			                             ((red  [k] & 0xff) << shiftR) |
			                             ((green[k] & 0xff) << shiftG) |
			                             ((blue [k] & 0xff) << shiftB));
		}
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
	 * @param output The output
	 * @param premultiply If {@code true}, premultiplies the pixels*/
	public final void join(byte[] red, byte[] green, byte[] blue, byte[] alpha, T output, boolean premultiply) {
		if((premultiply)) joinPre(red, green, blue, alpha, output);
		else              join   (red, green, blue, alpha, output);
	}
	
	public final void join(byte[] red, byte[] green, byte[] blue, byte[] alpha, T output,
			int x, int y, int width, int height, int stride) {
		int epp = format.getElementsPerPixel(), sk = stride - width, si = sk * epp;
  		for(int r = height, c = width, k = y * stride + x, i = k * epp;; i+=epp, ++k) {
  			format.setARGB(output, i, ((alpha[k] & 0xff) << shiftA) |
  			                          ((red  [k] & 0xff) << shiftR) |
  			                          ((green[k] & 0xff) << shiftG) |
  			                          ((blue [k] & 0xff) << shiftB));
  			if((--c == 0)) {
  				c  = width;
  				k += sk;
  				i += si;
  				if((--r == 0))
  					break;
  			}
  		}
	}
	
	public final void joinPre(byte[] red, byte[] green, byte[] blue, byte[] alpha, T output,
			int x, int y, int width, int height, int stride) {
		int epp = format.getElementsPerPixel(), sk = stride - width, si = sk * epp;
  		for(int r = height, c = width, k = y * stride + x, i = k * epp;; i+=epp, ++k) {
  			format.setARGBPre(output, i, ((alpha[k] & 0xff) << shiftA) |
  			                             ((red  [k] & 0xff) << shiftR) |
  			                             ((green[k] & 0xff) << shiftG) |
  			                             ((blue [k] & 0xff) << shiftB));
  			if((--c == 0)) {
  				c  = width;
  				k += sk;
  				i += si;
  				if((--r == 0))
  					break;
  			}
  		}
	}
	
	public final void join(byte[] red, byte[] green, byte[] blue, byte[] alpha, T output,
			int x, int y, int width, int height, int stride, boolean premultiply) {
		if((premultiply)) joinPre(red, green, blue, alpha, output, x, y, width, height, stride);
		else              join   (red, green, blue, alpha, output, x, y, width, height, stride);
	}
	
	public final ImagePixelFormat<T> getFormat() {
		return format;
	}
}