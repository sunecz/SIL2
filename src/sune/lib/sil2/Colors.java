package sune.lib.sil2;

import javafx.scene.paint.Color;

/**
 * Collection of various methods used for color manipulation.
 * @see Color*/
public final class Colors {
	
	private static final float I2F = 1.0f / 255.0f;
	private static final float F2I = 255.0f;
	private static final float IPI = 1.0f / FastMath.PI;
	
	private static final int SHIFT_B = 0;
	private static final int SHIFT_G = 8;
	private static final int SHIFT_R = 16;
	private static final int SHIFT_A = 24;
	private static final int  MASK_A = 0xff << SHIFT_A;
	
	// Forbid anyone to create an instance of this class
	private Colors() {
	}
	
	private static final float max(float a, float b, float c) {
		return a < b ? (b < c ? c : b) : (a < c ? c : a);
	}
	
	private static final float min(float a, float b, float c) {
		return a > b ? (b > c ? c : b) : (a > c ? c : a);
	}
	
	/**
	 * Converts a color, given by the red, green and blue components, to a HSL color
	 * and stores the result in the {@code result} float array. The result is saved
	 * as follows: {@code [ h, s, l ]}, each value in range {@code 0} - {@code 1}.
	 * @param red The red component of a color, in range {@code 0} - {@code 255}
	 * @param green The green component of a color, in range {@code 0} - {@code 255}
	 * @param blue The blue component of a color, in range {@code 0} - {@code 255}
	 * @param result The array where to save the result*/
	public static final void rgb2hsl(int red, int green, int blue, float[] result) {
		float r = red   * I2F;
		float g = green * I2F;
		float b = blue  * I2F;
		float max = max(r, g, b);
		float min = min(r, g, b);
		float h, s, l = (max + min) * 0.5f;
		if((max == min)) {
			h = 0.0f;
			s = 0.0f;
		} else {
			float d = max - min;
			s = l > 0.5f ? d / (2.0f - max - min) : d / (max + min);
			if((max == r)) {
				h = (g - b) / d + (g < b ? 6.0f : 0.0f);
			} else if((max == g)) {
				h = (b - r) / d + 2.0f;
			} else {
				h = (r - g) / d + 4.0f;
			}
			h /= 6.0f;
		}
		result[0] = h;
		result[1] = s;
		result[2] = l;
	}
	
	/**
	 * Converts a color, given by the hue, saturation and lightness components,
	 * to an RGB color and stores the result in the {@code result} int array.
	 * The result is saved as follows: {@code [ r, g, b ]}, each value in range
	 * {@code 0} - {@code 255}.
	 * @param h The hue component of a color, in range {@code 0} - {@code 1}
	 * @param s The saturation component of a color, in range {@code 0} - {@code 1}
	 * @param l The lightness component of a color, in range {@code 0} - {@code 1}
	 * @param result The array where to save the result*/
	public static final void hsl2rgb(float h, float s, float l, int[] result) {
		float r, g, b;
		if((s == 0.0f)) {
			r = l;
			g = l;
			b = l;
		} else {
			float q = l < 0.5f ? l * (1.0f + s) : l + s - l * s;
			float p = 2.0f * l - q;
			r = hue2rgb(p, q, h + 1.0f / 3.0f);
			g = hue2rgb(p, q, h);
			b = hue2rgb(p, q, h - 1.0f / 3.0f);
		}
		result[0] = FastMath.round(r * F2I);
		result[1] = FastMath.round(g * F2I);
		result[2] = FastMath.round(b * F2I);
	}
	
	private static final float hue2rgb(float p, float q, float t) {
		if((t < 0.0f)) t += 1.0f; else if((t > 1.0f)) t -= 1.0;
		if((t * 6.0f < 1.0f)) return p + (q - p) * 6.0f * t;
		if((t * 2.0f < 1.0f)) return q;
		if((t * 3.0f < 2.0f)) return p + (q - p) * 6.0f * (2.0f / 3.0f - t);
        return p;
	}
	
	private static final float _xR =  95.0489f;
	private static final float _yR = 100.0000f;
	private static final float _zR = 108.8840f;
	
	private static final float func_xyz2lab(float t) {
		return t > 0.008856f ? FastMath.pow(t, 1.0f / 3.0f) : (t / 0.128419f) + 0.137931f;
	}
	
	private static final float func_lab2xyz(float t) {
		return t > 0.206897f ? FastMath.pow(t, 3.0f) : 0.128419f * (t - 0.137931f);
	}
	
	// r, g, b are in range <0, 1>
	// x, y, z are in range <0, 1>
	public static final void rgb2xyz(float r, float g, float b, float[] xyz) {
		// https://en.wikipedia.org/wiki/SRGB
		xyz[0] = 0.4124f * r + 0.3576f * g + 0.1805f * b;
		xyz[1] = 0.2126f * r + 0.7152f * g + 0.0722f * b;
		xyz[2] = 0.0193f * r + 0.1192f * g + 0.9504f * b;
	}
	
	public static final void xyz2lab(float x, float y, float z, float[] lab) {
		// // https://en.wikipedia.org/wiki/CIELAB_color_space
		float fx = func_xyz2lab(x / _xR);
		float fy = func_xyz2lab(y / _yR);
		float fz = func_xyz2lab(z / _zR);
		lab[0] = 116.0f * (fy) - 16.0f;
		lab[1] = 500.0f * (fx - fy);
		lab[2] = 200.0f * (fy - fz);
	}
	
	public static final void lab2lch(float l, float a, float b, float[] lch) {
		lch[0] = l;
		lch[1] = (float) Math.hypot(a, b);
		lch[2] = (float) Math.atan2(b, a);
	}
	
	/**
	 * Experimental method, pending documentation.
	 * @experimental
	 * @author Sune*/
	public static final void rgb2hcl(int r, int g, int b, float[] result) {
		float[] xyz = new float[3];
		float[] lab = new float[3];
		float[] lch = new float[3];
		rgb2xyz(r * I2F, g * I2F, b * I2F, xyz);
		xyz2lab(xyz[0],  xyz[1],  xyz[2],  lab);
		lab2lch(lab[0],  lab[1],  lab[2],  lch);
		result[0] = lch[2];
		result[1] = lch[1];
		result[2] = lch[0];
	}
	
	public static final void lch2lab(float l, float c, float h, float[] lab) {
		float a = c * (float) Math.cos(h);
		float b = c * (float) Math.sin(h);
		lab[0] = l;
		lab[1] = a;
		lab[2] = b;
	}
	
	public static final void lab2xyz(float l, float a, float b, float[] xyz) {
		// https://en.wikipedia.org/wiki/CIELAB_color_space
		float fy = (l + 16.0f) / 116.0f;
		float fx = (a) / 500.0f;
		float fz = (b) / 200.0f;
		xyz[0] = _xR * func_lab2xyz(fy + fx);
		xyz[1] = _yR * func_lab2xyz(fy);
		xyz[2] = _zR * func_lab2xyz(fy - fz);
	}
	
	// r, g, b are in range <0, 1>
	// x, y, z are in range <0, 1>
	public static final void xyz2rgb(float x, float y, float z, float[] rgb) {
		// https://en.wikipedia.org/wiki/SRGB
		rgb[0] =  3.2406f * x - 1.5372f * y - 0.4986f * z;
		rgb[1] = -0.9689f * x + 1.8758f * y + 0.0415f * z;
		rgb[2] =  0.0557f * x - 0.2040f * y + 1.0570f * z;
	}
	
	/**
	 * Experimental method, pending documentation.
	 * @experimental
	 * @author Sune*/
	public static final void hcl2rgb(float h, float c, float l, int[] result) {
		float[] lab = new float[3];
		float[] xyz = new float[3];
		float[] rgb = new float[3];
		lch2lab(l,      c,      h,      lab);
		lab2xyz(lab[0], lab[1], lab[2], xyz);
		xyz2rgb(xyz[0], xyz[1], xyz[2], rgb);
		result[0] = FastMath.round(rgb[0] * F2I);
		result[1] = FastMath.round(rgb[1] * F2I);
		result[2] = FastMath.round(rgb[2] * F2I);
	}
	
	/**
	 * Converts a color, given by the ARGB int, to a JavaFX Color.
	 * @param argb The ARGB int to be converted
	 * @return The JavaFX Color representing the given color*/
	public static final Color int2color(int argb) {
		return Color.rgb(((argb >> SHIFT_R) & 0xff),
						 ((argb >> SHIFT_G) & 0xff),
						 ((argb >> SHIFT_B) & 0xff),
						 ((argb >> SHIFT_A) & 0xff) * I2F);
	}
	
	/**
	 * Converts the given JavaFX color to an ARGB int.
	 * @param color The color to be converted
	 * @return The ARGB int representing the given color*/
	public static final int color2int(Color color) {
		if((color == null)) throw new NullPointerException("Invalid color");
		int r = (int) (color.getRed()     * F2I) & 0xff;
		int g = (int) (color.getGreen()   * F2I) & 0xff;
		int b = (int) (color.getBlue()    * F2I) & 0xff;
		int a = (int) (color.getOpacity() * F2I) & 0xff;
		return rgba2int(r, g, b, a);
	}
	
	/**
	 * Inverts an opaque color. Opaque color is a color that has its alpha
	 * component of value {@code 255}.
	 * @param color The ARGB int of a color to be inverted
	 * @return The ARGB int of the inverted color*/
	public static final int invertOpaque(int color) {
		if((color == 0x0)) return 0xff000000;
		int r = 0xff - ((color >> SHIFT_R) & 0xff);
		int g = 0xff - ((color >> SHIFT_G) & 0xff);
		int b = 0xff - ((color >> SHIFT_B) & 0xff);
		return ((    0xff) << SHIFT_A) |
			   ((r & 0xff) << SHIFT_R) |
			   ((g & 0xff) << SHIFT_G) |
			   ((b & 0xff) << SHIFT_B);
	}
	
	/**
	 * Inverts the given color, keeping the value of its alpha component.
	 * @param color The ARGB int of a color to be inverted
	 * @return The ARGB int of the inverted color*/
	public static final int invert(int color) {
		int r = 0xff - ((color >> SHIFT_R) & 0xff);
		int g = 0xff - ((color >> SHIFT_G) & 0xff);
		int b = 0xff - ((color >> SHIFT_B) & 0xff);
		return ((color)    &   MASK_A) |
			   ((r & 0xff) << SHIFT_R) |
			   ((g & 0xff) << SHIFT_G) |
			   ((b & 0xff) << SHIFT_B);
	}
	
	/**
	 * Inverts the given color to black-and-white color, meaning that the inverted
	 * color is either white or black. The color is converted to white or black
	 * based on its average value, that is the average of the sum of its red, green
	 * and blue component. The threshold is set to {@code 175}.
	 * @param color The color to be inverted
	 * @return The black-and-white inverted color
	 * @see #invertBW(int, int)*/
	public static final int invertBW(int color) {
		return invertBW(color, 175);
	}
	
	/**
	 * Inverts the given color to black-and-white color, meaning that the inverted
	 * color is either white or black. The color is converted to white or black
	 * based on its average value, that is the average of the sum of its red, green
	 * and blue component. The inverted color is white if the average value is
	 * greater than the given value {@code val}, otherwise black.
	 * @param color The color to be inverted
	 * @param val The threshold value for black-and-white
	 * @return The black-and-white inverted color*/
	public static final int invertBW(int color, int val) {
		int a = 0xff - (color >>> SHIFT_A);
		if((a > val)) return MASK_A;
		int r = ((color >> SHIFT_R) & 0xff);
		int g = ((color >> SHIFT_G) & 0xff);
		int b = ((color >> SHIFT_B) & 0xff);
		int l = 0xff - (r + g + b) / 3;
		if((l > val)) l = 0xff; else l = 0x00;
		return (MASK_A)       |
			   (l << SHIFT_R) |
			   (l << SHIFT_G) |
			   (l << SHIFT_B);
	}
	
	/**
	 * Inverts the given color to black-and-white color, meaning that the inverted
	 * color is either white or black. The color is converted to white or black
	 * based on value of its alpha component. The threshold is set to {@code 175}.
	 * @param color The color to be inverted
	 * @param val The threshold value for black-and-white
	 * @return The black-and-white inverted color
	 * @see #bwAlpha(int, int)*/
	public static final int bwAlpha(int color) {
		return bwAlpha(color, 175);
	}
	
	/**
	 * Inverts the given color to black-and-white color, meaning that the inverted
	 * color is either white or black. The color is converted to white or black
	 * based on value of its alpha component. The inverted color is white if
	 * the value of alpha component is greater than the given value {@code val},
	 * otherwise black.
	 * @param color The color to be inverted
	 * @param val The threshold value for black-and-white
	 * @return The black-and-white inverted color*/
	public static final int bwAlpha(int color, int val) {
		int a = color >>> SHIFT_A;
		if((a > val)) a = 0xffffffff; else a = MASK_A;
		return a;
	}
	
	/**
	 * Converts the given JavaFX color to a readable string. The readable string
	 * contains information about the color's red, green, blue and alpha component.
	 * @param color The color to be converted
	 * @return The readable string*/
	public static final String toReadableString(Color color) {
		int red   = (int) (color.getRed() 	  * 0xff);
		int green = (int) (color.getGreen()   * 0xff);
		int blue  = (int) (color.getBlue() 	  * 0xff);
		int alpha = (int) (color.getOpacity() * 0xff);
		return String.format("R: %d, G: %d, B: %d, A: %d", red, green, blue, alpha);
	}
	
	/**
	 * Gets the blue component of the given ARGB int color.
	 * @param color The color
	 * @return The blue component*/
	public static final int blue(int color) {
		return (color >> SHIFT_B) & 0xff;
	}
	
	/**
	 * Gets the green component of the given ARGB int color.
	 * @param color The color
	 * @return The green component*/
	public static final int green(int color) {
		return (color >> SHIFT_G) & 0xff;
	}
	
	/**
	 * Gets the red component of the given ARGB int color.
	 * @param color The color
	 * @return The red component*/
	public static final int red(int color) {
		return (color >> SHIFT_R) & 0xff;
	}
	
	/**
	 * Gets the alpha component of the given ARGB int color.
	 * @param color The color
	 * @return The alpha component*/
	public static final int alpha(int color) {
		return (color >> SHIFT_A) & 0xff;
	}
	
	/**
	 * Clamps the given float value to an int value in range {@code 0} - {@code 255}.
	 * @param value The value
	 * @return The clamped value*/
	public static final int f2rgba(float value) {
		if((value <= 0x00)) return 0x00;
		if((value >= 0xff)) return 0xff;
		return (int) value;
	}
	
	public static final int i2rgba(int value) {
		if((value <= 0x00)) return 0x00;
		if((value >= 0xff)) return 0xff;
		return value;
	}
	
	/**
	 * Clamps the given float value to a float value in range {@code 0} - {@code 1}.
	 * @param value The value
	 * @return The clamped value*/
	public static final float f2hsla(float value) {
		if((value <= 0.0f)) return 0.0f;
		if((value >= 1.0f)) return 1.0f;
		return value;
	}
	
	/**
	 * Blends two 32-bit colors, given by the two ARGB ints, using "Over" alpha
	 * compositing method.
	 * @param foreg The foreground color
	 * @param backg The background color
	 * @return The composed color of the two given colors, as an ARGB int*/
	public static final int blend(int foreg, int backg) {
		int fa = foreg >>> SHIFT_A;
		if((fa == 0xff)) return foreg;
		int ba = backg >>> SHIFT_A;
		if((ba == 0x00)) return foreg;
		int fr = (foreg >> SHIFT_R) & 0xff;
		int fg = (foreg >> SHIFT_G) & 0xff;
		int fb = (foreg >> SHIFT_B) & 0xff;
		int br = (backg >> SHIFT_R) & 0xff;
		int bg = (backg >> SHIFT_G) & 0xff;
		int bb = (backg >> SHIFT_B) & 0xff;
		int af = (0xff - fa);
		int aa = FastMath.div255((FastMath.mul255(ba) + fa * (0xff - ba)));
		int nr = FastMath.div255((FastMath.mul255(fr) * fa + br * ba * af) / aa);
		int ng = FastMath.div255((FastMath.mul255(fg) * fa + bg * ba * af) / aa);
		int nb = FastMath.div255((FastMath.mul255(fb) * fa + bb * ba * af) / aa);
		return (aa << SHIFT_A) |
			   (nr << SHIFT_R) |
			   (ng << SHIFT_G) |
			   (nb << SHIFT_B);
	}
	
	/**
	 * Gets the sobel value for the given direction and magnitude.
	 * @param direction The direction, in range {@code <-pi, +pi>}
	 * @param magnitude The normalized magnitude, in range {@code 0} - {@code 1}
	 * @return The sobel value as an ARGB int color.*/
	public static final int sobel(float direction, float magnitude) {
		return hsla(direction * IPI, magnitude, magnitude, 1.0f);
	}
	
	/**
	 * Computed from the Sobel kernel. Maximum value in both x and y
	 * direction is {@code 4 * 255}.<br>
	 * The formula is:<br>
	 * {@code MAX = sqrt((4 * 255)^2 + (4 * 255)^2) = ~1442.497834}.*/
	private static final float SOBEL_MAX_MAGNITUDE = 1442.5f;
	
	public static final int sobelXY(float x, float y) {
		return sobel(FastMath.atan2(y, x), FastMath.hypot(y, x) / SOBEL_MAX_MAGNITUDE);
	}
	
	/**
	 * Converts a color, given by the ARGB int, to a grayscale color. The formula
	 * used for conversion is: {@code 0.299 * R + 0.587 * G + 0.114 * B}.
	 * @param color The color as an ARGB int
	 * @return The grayscale equivalent of the given color as an ARGB int color*/
	public static final int grayscale(int color) {
		return f2rgba(0.299f * ((color >> SHIFT_R) & 0xff)
		            + 0.587f * ((color >> SHIFT_G) & 0xff)
		            + 0.114f * ((color >> SHIFT_B) & 0xff));
	}
	
	/**
	 * Converts the given value to a grayscale color of that value, meaning that
	 * every RGB component of the grayscale color is that value. The value of the alpha
	 * component of the new color is always {@code 255}.
	 * @param val The value
	 * @return The grayscale color as an ARGB int*/
	public static final int int2grayscale(int val) {
		val &= 0xff;
		return (MASK_A) |
			   (val << SHIFT_R) |
			   (val << SHIFT_G) |
			   (val << SHIFT_B);
	}
	
	/**
	 * Converts a color, given by the ARGB int, to a translucent color with the value
	 * of its alpha component to be the product of the color's alpha and the given
	 * value {@code alpha}. The product is done using theirs value in range
	 * {@code 0} - {@code 1}, meaning that if the given {@code alpha} value is of value
	 * {@code 255}, the resulting color keeps the value of the {@code color}'s alpha
	 * component, and if the given {@code alpha} value is of value {@code 0},
	 * the resulting color has the value of its alpha component equaled to {@code 0}.
	 * @param color The color
	 * @param alpha The alpha
	 * @return The converted translucent color*/
	public static final int translucent(int color, int alpha) {
		float ac = (color >>> SHIFT_A) * I2F;
		float aa = (alpha)             * I2F;
		float ar = (ac * aa)           * F2I;
		return (color & ~MASK_A) | ((int) ar << SHIFT_A);
	}
	
	/**
	 * Converts a 32-bit color, given by the ARGB int, from linear alpha
	 * to premultiplied alpha.
	 * @param argb The color
	 * @return The premultiplied color as an ARGB int*/
	public static final int linear2premult(int argb) {
		int a = argb >>> SHIFT_A;
		int r = FastMath.div255(((argb >> SHIFT_R) & 0xff) * a);
		int g = FastMath.div255(((argb >> SHIFT_G) & 0xff) * a);
		int b = FastMath.div255(((argb >> SHIFT_B) & 0xff) * a);
		return (argb & MASK_A) |
			   (r  << SHIFT_R) |
			   (g  << SHIFT_G) |
			   (b  << SHIFT_B);
	}
	
	public static final void linear2premult(int r, int g, int b, int a, byte[] result) {
		result[0] = (byte) FastMath.div255(r * a);
		result[1] = (byte) FastMath.div255(g * a);
		result[2] = (byte) FastMath.div255(b * a);
		result[3] = (byte) a;
	}
	
	/**
	 * Converts a 32-bit color, given by the ARGB int, from premultiplied alpha
	 * to linear alpha.
	 * @param argb The color
	 * @return The linear color as an ARGB int*/
	public static final int premult2linear(int argb) {
		int a = argb >>> SHIFT_A;
		if((a == 0x0)) return 0x0;
		int r = FastMath.mul255((argb >> SHIFT_R) & 0xff) / a;
		int g = FastMath.mul255((argb >> SHIFT_G) & 0xff) / a;
		int b = FastMath.mul255((argb >> SHIFT_B) & 0xff) / a;
		return (argb & MASK_A) |
			   (r  << SHIFT_R) |
			   (g  << SHIFT_G) |
			   (b  << SHIFT_B);
	}
	
	public static final void premult2linear(int r, int g, int b, int a, byte[] result) {
		if((a == 0x0)) {
			result[0] = result[1] = result[2] = result[3] = 0;
			return;
		}
		result[0] = (byte) (FastMath.mul255(r) / a);
		result[1] = (byte) (FastMath.mul255(g) / a);
		result[2] = (byte) (FastMath.mul255(b) / a);
		result[3] = (byte) a;
	}
	
	// ----- COLOR CREATION (ARGB format) [Non-premultiplied colors]
	
	private static final int rgba2int(int r, int g, int b, int a) {
		return ((a & 0xff) << SHIFT_A) |
			   ((r & 0xff) << SHIFT_R) |
			   ((g & 0xff) << SHIFT_G) |
			   ((b & 0xff) << SHIFT_B);
	}
	
	public static final int rgb(int r, int g, int b) {
		return rgba(r, g, b, 0xff);
	}
	
	/**
	 * Converts a color, given by the red, green, blue and alpha components,
	 * to an int. The result is an ARGB int with 8-bit depth per component.
	 * @param r The red component of a color, in range {@code 0} - {@code 255}
	 * @param g The green component of a color, in range {@code 0} - {@code 255}
	 * @param b The blue component of a color, in range {@code 0} - {@code 255}
	 * @param a The alpha component of a color, in range {@code 0} - {@code 255}
	 * @return The ARGB int representation of the given color*/
	public static final int rgba(int r, int g, int b, int a) {
		return rgba2int(r, g, b, a);
	}
	
	public static final int hsl(float h, float s, float l) {
		return hsla(h, s, l, 1.0f);
	}
	
	/**
	 * Converts a color, given by the hue, saturation, lightness and alpha components,
	 * to an int. The result is an ARGB int with 8-bit depth per component.
	 * @param h The hue component of a color, in range {@code 0} - {@code 1}
	 * @param s The saturation component of a color, in range {@code 0} - {@code 1}
	 * @param l The lightness component of a color, in range {@code 0} - {@code 1}
	 * @param a The alpha component of a color, in range {@code 0} - {@code 1}
	 * @return The ARGB int representation of the given color*/
	public static final int hsla(float h, float s, float l, float a) {
		int[] rgb = new int[3];
		Colors.hsl2rgb(h, s, l, rgb);
		return rgba(rgb[0], rgb[1], rgb[2], FastMath.round(a * F2I));
	}
	
	public static final int hcl(float h, float c, float l) {
		return hcla(h, c, l, 1.0f);
	}
	
	public static final int hcla(float h, float c, float l, float a) {
		int[] rgb = new int[3];
		hcl2rgb(h, c, l, rgb);
		return rgba(rgb[0], rgb[1], rgb[2], FastMath.round(a * F2I));
	}
	
	public static final int xyz(float x, float y, float z) {
		float[] rgb = new float[3];
		xyz2rgb(x, y, z, rgb);
		return rgba(FastMath.round(rgb[0] * F2I),
		            FastMath.round(rgb[1] * F2I),
		            FastMath.round(rgb[2] * F2I),
		            0xff);
	}
	
	public static final int lab(float l, float a, float b) {
		float[] xyz = new float[3];
		lab2xyz(l, a, b, xyz);
		return xyz(xyz[0], xyz[1], xyz[2]);
	}
	
	// -----
	
	// ----- COLOR CREATION (ARGB format) [Premultiplied colors]
	
	private final int rgba2intPre(int r, int g, int b, int a) {
		byte[] premult = new byte[4];
		linear2premult(r, g, b, a, premult);
		r = premult[0];
		g = premult[1];
		b = premult[2];
		a = premult[3];
		return ((a & 0xff) << 24) |
			   ((r & 0xff) << 16) |
			   ((g & 0xff) <<  8) |
			   ((b & 0xff));
	}
	
	public final int rgbPre(int r, int g, int b) {
		return rgbaPre(r, g, b, 0xff);
	}
	
	public final int rgbaPre(int r, int g, int b, int a) {
		return rgba2intPre(r, g, b, a);
	}
	
	public final int hslPre(float h, float s, float l) {
		return hslaPre(h, s, l, 1.0f);
	}
	
	public final int hslaPre(float h, float s, float l, float a) {
		int[] rgb = new int[3];
		Colors.hsl2rgb(h, s, l, rgb);
		return rgbaPre(rgb[0], rgb[1], rgb[2], FastMath.round(a * F2I));
	}
	
	public final int hclPre(float h, float c, float l) {
		return hclaPre(h, c, l, 1.0f);
	}
	
	public final int hclaPre(float h, float c, float l, float a) {
		int[] rgb = new int[3];
		Colors.hcl2rgb(h, c, l, rgb);
		return rgbaPre(rgb[0], rgb[1], rgb[2], FastMath.round(a * F2I));
	}
	
	public final int xyzPre(float x, float y, float z) {
		float[] rgb = new float[3];
		Colors.xyz2rgb(x, y, z, rgb);
		return rgbaPre(FastMath.round(rgb[0] * F2I),
		               FastMath.round(rgb[1] * F2I),
		               FastMath.round(rgb[2] * F2I),
		               0xff);
	}
	
	public final int labPre(float l, float a, float b) {
		float[] xyz = new float[3];
		Colors.lab2xyz(l, a, b, xyz);
		return xyzPre(xyz[0], xyz[1], xyz[2]);
	}
	
	public final int colorPre(Color color) {
		if((color == null)) throw new NullPointerException("Invalid color");
		int r = FastMath.round((float) color.getRed()     * F2I);
		int g = FastMath.round((float) color.getGreen()   * F2I);
		int b = FastMath.round((float) color.getBlue()    * F2I);
		int a = FastMath.round((float) color.getOpacity() * F2I);
		return rgbaPre(r, g, b, a);
	}
	
	public final int fromARGBPre(int argb) {
		int a = (argb >> 24) & 0xff;
		int r = (argb >> 16) & 0xff;
		int g = (argb >>  8) & 0xff;
		int b = (argb)       & 0xff;
		byte[] linear = new byte[4];
		premult2linear(r, g, b, a, linear);
		r = linear[0];
		g = linear[1];
		b = linear[2];
		a = linear[3];
		return rgba(r, g, b, a);
	}
	
	public final int toARGBPre(int color) {
		int a = (color >> 24) & 0xff;
		int r = (color >> 16) & 0xff;
		int g = (color >>  8) & 0xff;
		int b = (color)       & 0xff;
		byte[] premult = new byte[4];
		linear2premult(r, g, b, a, premult);
		r = premult[0];
		g = premult[1];
		b = premult[2];
		a = premult[3];
		return (a << 24) | (r << 16) | (g << 8) | (b);
	}
	
	// -----
}