package sune.lib.sil2.operation;

import java.nio.Buffer;
import java.util.function.Function;

import sune.lib.sil2.Colors;
import sune.lib.sil2.IImageContext;
import sune.lib.sil2.IImageOperation;
import sune.lib.sil2.format.ImagePixelFormat;

public final class ImageOperations {
	
	// Forbid anyone to create an instance of this class
	private ImageOperations() {
	}
	
	public static final class Histogram<T extends Buffer> implements IImageOperation<T, int[]> {
		
		private final int[] histogram;
		private final Function<Integer, Integer> function;
		
		public Histogram(int[] histogram, Function<Integer, Integer> function) {
			this.histogram = histogram;
			this.function  = function;
		}
		
		@Override
		public final int[] execute(IImageContext<T> context) {
			ImagePixelFormat<T> format = context.getPixelFormat();
			T pixels = context.getPixels();
			int epp = format.getElementsPerPixel();
			for(int i = 0, l = pixels.capacity(), level; i < l; i += epp) {
				level = function.apply(format.getARGB(pixels, i));
				++histogram[level];
			}
			return histogram;
		}
	}
	
	public static final class OtsuOptimalThreshold<T extends Buffer> implements IImageOperation<T, Float> {
		
		@Override
		public final Float execute(IImageContext<T> context) {
			int total = context.getPixels().capacity();
			int[] histogram = new Histogram<T>(new int[256], Colors::grayscale).execute(context);
			int sum = 0;
			for(int i = 1; i < 256; ++i)
				sum += i * histogram[i];
			int   sumB = 0;
			int   wB   = 0;
			int   wF   = 0;
			int   mB   = 0;
			int   mF   = 0;
			float max  = 0.0f;
			float btw  = 0.0f;
			float th1  = 0.0f;
			float th2  = 0.0f;
			for(int i = 0; i < 256; ++i) {
				wB += histogram[i];
				if((wB == 0))
					continue;
				wF = total - wB;
				if((wF == 0))
					break;
				sumB += i * histogram[i];
				mB  = (sumB / wB);
				mF  = (sum - sumB) / wF;
				btw = wB * wF * (mB - mF) * (mB - mF);
				if((btw >= max)) {
					th1 = i;
					if((btw > max))
						th2 = i;
					max = btw;
				}
			}
			return (th1 + th2) * 0.5f;
		}
	}
}