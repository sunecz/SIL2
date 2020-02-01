package sune.lib.sil2.operation;

import java.nio.Buffer;

import sune.lib.sil2.Colors;
import sune.lib.sil2.FastMath;
import sune.lib.sil2.IImageContext;
import sune.lib.sil2.IImageOperation;
import sune.lib.sil2.format.ImagePixelFormat;

public final class Adjustments {
	
	// TODO: Update JavaDoc
	
	// Forbid anyone to create an instance of this class
	private Adjustments() {
	}
	
	/**
	 * Inverts colors of {@code this} image.*/
	public static final class Invert<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.applyActionRGB((rgb, input, output, i, varStore) -> {
				rgb[0] = 0xff - rgb[0];
				rgb[1] = 0xff - rgb[1];
				rgb[2] = 0xff - rgb[2];
			});
			return null;
		}
	}
	
	/**
	 * Converts {@code this} image to a grayscale version.
	 * Formula used:<br>
	 * {@code 0.299 * R + 0.587 * G + 0.114 * B}.*/
	public static final class Grayscale<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			context.applyActionRGB((rgb, input, output, i, varStore) -> {
				int gray = Colors.f2rgba(0.299f * rgb[0] + 0.587f * rgb[1] + 0.114f * rgb[2]);
				rgb[0] = gray;
				rgb[1] = gray;
				rgb[2] = gray;
			});
			return null;
		}
	}
	
	/**
	 * Alters the brightness of {@code this} image.
	 * @param value The value*/
	public static final class Brightness<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Brightness(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final float fval = IImageUtils.clamp11(value) * IImageUtils.F2I;
			context.applyActionRGB((rgb, input, output, i, varStore) -> {
				rgb[0] = Colors.f2rgba(rgb[0] + fval);
				rgb[1] = Colors.f2rgba(rgb[1] + fval);
				rgb[2] = Colors.f2rgba(rgb[2] + fval);
			});
			return null;
		}
	}
	
	/**
	 * Alters the contrast of {@code this} image.
	 * @param value The value*/
	public static final class Contrast<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Contrast(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final float fval = IImageUtils.clamp00(value);
			context.applyActionRGB((rgb, input, output, i, varStore) -> {
				rgb[0] = Colors.f2rgba(fval * (rgb[0] - 128) + 128);
				rgb[1] = Colors.f2rgba(fval * (rgb[1] - 128) + 128);
				rgb[2] = Colors.f2rgba(fval * (rgb[2] - 128) + 128);
			});
			return null;
		}
	}
	
	/**
	 * Alters the gamma of {@code this} image.
	 * @param value The value*/
	public static final class Gamma<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Gamma(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final float fval = 1.0f / value;
			context.applyActionRGB((rgb, input, output, i, varStore) -> {
				rgb[0] = Colors.f2rgba(FastMath.pow(rgb[0] * IImageUtils.I2F, fval) * IImageUtils.F2I);
				rgb[1] = Colors.f2rgba(FastMath.pow(rgb[1] * IImageUtils.I2F, fval) * IImageUtils.F2I);
				rgb[2] = Colors.f2rgba(FastMath.pow(rgb[2] * IImageUtils.I2F, fval) * IImageUtils.F2I);
			});
			return null;
		}
	}
	
	/**
	 * Alters the alpha value of {@code this} image.
	 * @param value The value*/
	public static final class Alpha<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int value;
		
		public Alpha(int value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final ImagePixelFormat<T> format = context.getPixelFormat();
			final int fval = value << 24;
			context.applyActionINT((input, output, i, varStore) -> {
				format.setARGB(output, i, (format.getARGB(input, i) & 0x00ffffff) | fval);
			});
			return null;
		}
	}
	
	/**
	 * Alters the transparency of {@code this} image.
	 * @param value The value*/
	public static final class Transparency<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Transparency(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final ImagePixelFormat<T> format = context.getPixelFormat();
			final float fval = IImageUtils.clamp01(value);
			context.applyActionINT((input, output, i, varStore) -> {
				int alpha = (int) ((format.getARGB(input, i) >>> 24) * fval) << 24;
				format.setARGB(output, i, (format.getARGB(input, i) & 0x00ffffff) | alpha);
			});
			return null;
		}
	}
	
	/**
	 * Alters the hue of {@code this} image.
	 * @param value The value*/
	public static final class Hue<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Hue(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final float fval = IImageUtils.clamp11(value);
			context.applyActionHSL((hsl, input, output, index, varStore) -> {
				float hue = hsl[0] + fval;
				if((hue < 0.0f)) hue += 1.0f; else
				if((hue > 1.0f)) hue -= 1.0f;
				hsl[0] = hue;
			});
			return null;
		}
	}
	
	/**
	 * Alters the saturation of {@code this} image.
	 * @param value The value*/
	public static final class Saturation<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Saturation(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final float fval = IImageUtils.clamp00(value);
			context.applyActionHSL((hsl, input, output, index, varStore) -> {
				hsl[1] *= fval;
			});
			return null;
		}
	}
	
	/**
	 * Alters the lightness of {@code this} image.
	 * @param value The value*/
	public static final class Lightness<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Lightness(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final float fval = IImageUtils.clamp00(value);
			context.applyActionHCL((hcl, input, output, index, varStore) -> {
				hcl[2] *= fval;
			});
			return null;
		}
	}
	
	/**
	 * Alters the chroma of {@code this} image.
	 * @param value The value*/
	public static final class Chroma<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Chroma(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final float fval = IImageUtils.clamp00(value);
			context.applyActionHCL((hcl, input, output, index, varStore) -> {
				hcl[1] *= fval;
			});
			return null;
		}
	}
	
	/**
	 * Thresholds {@code this} image, meaning that all pixels that have
	 * the lowest 8-bits greater than or equaled the given value, are set
	 * to white color, otherwise to black color.
	 * @param value The value*/
	public static final class Threshold<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int value;
		
		public Threshold(int value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			new ThresholdGRT<T>(value).execute(context);
			return null;
		}
	}
	
	/**
	 * Thresholds {@code this} image, meaning that all pixels that have
	 * the lowest 8-bits lower than or equaled the given value, are set
	 * to white color, otherwise to black color.
	 * @param value The value*/
	public static final class ThresholdLWR<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int value;
		
		public ThresholdLWR(int value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final ImagePixelFormat<T> format = context.getPixelFormat();
			final int fval = IImageUtils.clamp02(value);
			context.applyActionINT((input, output, i, varStore) -> {
				format.setARGB(output, i, (format.getARGB(input, i) & 0xff) <= fval ? 0xffffffff : 0xff000000);
			});
			return null;
		}
	}
	
	/**
	 * Thresholds {@code this} image, meaning that all pixels that have
	 * the lowest 8-bits greater than or equaled the given value, are set
	 * to white color, otherwise to black color.
	 * @param value The value*/
	public static final class ThresholdGRT<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int value;
		
		public ThresholdGRT(int value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final ImagePixelFormat<T> format = context.getPixelFormat();
			final int fval = IImageUtils.clamp02(value);
			context.applyActionINT((input, output, i, varStore) -> {
				format.setARGB(output, i, (format.getARGB(input, i) & 0xff) >= fval ? 0xffffffff : 0xff000000);
			});
			return null;
		}
	}
	
	/**
	 * Thresholds {@code this} image, meaning that all pixels that have
	 * the lowest 8-bits between the given value {@code min} and the given
	 * value {@code max} (both inclusive), are set to white color,
	 * otherwise to black color.
	 * @param min The minimum value
	 * @param max The maximum value*/
	public static final class ThresholdBTW<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int min;
		private final int max;
		
		public ThresholdBTW(int min, int max) {
			this.min = min;
			this.max = max;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			final ImagePixelFormat<T> format = context.getPixelFormat();
			final int fmin = IImageUtils.clamp02(min);
			final int fmax = IImageUtils.clamp02(max);
			context.applyActionINT((input, output, i, varStore) -> {
				int value = format.getARGB(input, i) & 0xff;
				format.setARGB(output, i, value >= fmin && value <= fmax ? 0xffffffff : 0xff000000);
			});
			return null;
		}
	}
	
	/**
	 * Thresholds {@code this} image using the {@linkplain #thresholdGRT(int)}
	 * method with a value of {@linkplain IImage#optimalThreshold(int[], int)}
	 * method and {@code this} image's histogram.*/
	public static final class HistogramThreshold<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			new Grayscale<T>().execute(context);
			float threshold = new ImageOperations.OtsuOptimalThreshold<T>().execute(context);
			new ThresholdGRT<T>((int) threshold).execute(context);
			return null;
		}
	}
	
	/**
	 * Sets all the shadows of {@code this} image to white color.
	 * Shadows are pixels that have grayscale value of less than {@code 85}.*/
	public static final class Shadows<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			new Grayscale<T>().execute(context);
			// luminance <= 84
			new ThresholdLWR<T>(84).execute(context);
			return null;
		}
	}
	
	/**
	 * Sets all the middle tones of {@code this} image to white color.
	 * Middle tones are pixels that have grayscale value between
	 * {@code 85} and {@code 170} (both inclusive).*/
	public static final class MiddleTones<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			new Grayscale<T>().execute(context);
			// 85 <= luminance <= 170
			new ThresholdBTW<T>(85, 170).execute(context);
			return null;
		}
	}
	
	/**
	 * Sets all the lights of {@code this} image to white color.
	 * Lights are pixels that have grayscale value of greater than {@code 170}.*/
	public static final class Lights<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			new Grayscale<T>().execute(context);
			// luminance >= 171
			new ThresholdGRT<T>(171).execute(context);
			return null;
		}
	}
}