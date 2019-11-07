package sune.lib.sil2;

import java.nio.Buffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import sune.lib.sil2.FXInternalUtils.PlatformImageWrapper;
import sune.lib.sil2.format.ImagePixelFormat;
import sune.lib.sil2.format.ImagePixelFormats;

/**
 * Represents an image to which can be applied various operations.
 * The "I" in Image stands for Improved.
 * @param <T> The type of an underlying buffer this image uses*/
public final class IImage<T extends Buffer> {
	
	private static final float F2I = 255.0f;
	private static final float I2F = 1.0f / 255.0f;
	
	private WritableImage image;
	private PlatformImageWrapper wrapper;
	private int width;
	private int height;
	private ImagePixelFormat<T> format;
	
	protected T original;
	protected T pixels;
	protected T buffer;
	
	protected InternalChannels<T> channels;
	
	/**
	 * Image convolution.*/
	public final Convolution convolution = new Convolution();
	/**
	 * Image adjustments.*/
	public final Adjustments adjustments = new Adjustments();
	/**
	 * Image filters.*/
	public final Filters     filters     = new Filters();
	/**
	 * Image effects.*/
	public final Effects     effects     = new Effects();
	
	protected BufferStrategy<T> bufferStrategy;
	private int ptrBuffer = 0;
	private int ptrPixels = 1;
	
	private static interface BufferStrategy<T extends Buffer> {
		T prepareBuffer(int index);
		T getBuffer    (int index);
		int numberOfBuffers();
		void swap(int i1, int i2);
	}
	
	private static final class NBufferStrategy<T extends Buffer> implements BufferStrategy<T> {
		
		private final T        original;
		private final Buffer[] buffers;
		
		public NBufferStrategy(T original, int numOfBuffers) {
			this.original = original;
			this.buffers  = new Buffer[numOfBuffers];
		}
		
		@Override
		public T prepareBuffer(int index) {
			if((index < 0 || index >= buffers.length))
				return original;
			T _buffer = newPixelsBuffer(original);
			buffers[index] = _buffer;
			BufferUtils.buffercopy(original, _buffer);
			return _buffer;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public T getBuffer(int index) {
			return index >= 0 && index < buffers.length ? (T) buffers[index] : original;
		}
		
		@Override
		public int numberOfBuffers() {
			return buffers.length;
		}
		
		@Override
		public void swap(int i1, int i2) {
			if((i1 < 0 || i1 >= buffers.length ||
				i2 < 0 || i2 >= buffers.length))
				return;
			Buffer _buf = buffers[i1];
			buffers[i1] = buffers[i2];
			buffers[i2] = _buf;
		}
	}
	
	@SuppressWarnings("unchecked")
	private static final <T extends Buffer> T newPixelsBuffer(T buffer) {
		return (T) BufferUtils.newBufferOfType(buffer);
	}
	
	/**
	 * Creates a new instance from the given image.
	 * @param image The image*/
	public IImage(Image image) {
		this(image, 2);
	}
	
	/**
	 * Creates a new instance with the given width and height from
	 * the given pixels array.
	 * @param width The width
	 * @param height The height
	 * @param imagePixels The image pixels*/
	public IImage(int width, int height, T imagePixels) {
		this(ImageUtils.create(width, height, imagePixels));
	}
	
	public IImage(int width, int height, T imagePixels, int numOfBuffers) {
		this(ImageUtils.create(width, height, imagePixels), numOfBuffers);
	}
	
	public IImage(Image image, int numOfBuffers) {
		if((image == null))
			throw new IllegalArgumentException("Image cannot be null");
		if((numOfBuffers <= 0))
			throw new IllegalArgumentException("Number of buffers must be > 0");
		this.image 	  = ensureWritableSupported(image);
		this.width 	  = (int) this.image.getWidth();
		this.height   = (int) this.image.getHeight();
		this.wrapper  = FXInternalUtils.getPlatformImageWrapper(this.image);
		this.format   = getImagePixelFormat(this.image);
		this.original = getTypedWrapperBuffer(wrapper);
		this.channels = new InternalChannels<>(format);
		// Buffering
		this.bufferStrategy = new NBufferStrategy<>(original, numOfBuffers);
		this.buffer = bufferStrategy.prepareBuffer(ptrBuffer);
		this.pixels = bufferStrategy.prepareBuffer(ptrPixels);
		if((numOfBuffers > 2)) {
			for(int i = 2; i < numOfBuffers; ++i)
				bufferStrategy.prepareBuffer(i);
		}
	}
	
	private static final WritableImage ensureWritableSupported(Image image) {
		// If the image's format is not supported, convert it to a native image
		if(!ImagePixelFormats.isSupported(image.getPixelReader().getPixelFormat())) {
			image = NativeImage.ensurePixelFormat(image);
		}
		// Ensure that the image is writable
		return ImageUtils.toWritable(image);
	}
	
	@SuppressWarnings("unchecked")
	private static final <T extends Buffer> ImagePixelFormat<T> getImagePixelFormat(Image image) {
		return (ImagePixelFormat<T>) ImagePixelFormats.from(image.getPixelReader().getPixelFormat());
	}
	
	@SuppressWarnings("unchecked")
	private static final <T extends Buffer> T getTypedWrapperBuffer(PlatformImageWrapper wrapper) {
		return (T) wrapper.bufferRewind();
	}
	
	/**
	 * Contains methods for image convolution.*/
	public final class Convolution {
		
		/**
		 * Normalizes the given kernel by its kernel value.
		 * @param kernel The kernel*/
		public final void normalizeKernel(float[] kernel) {
			float kval = kernelValue(kernel);
			for(int i = 0, l = kernel.length; i < l; ++i)
				kernel[i] /= kval;
		}
		
		/**
		 * Convolutes {@code this} image with the given kernel {@code iterations} times.
		 * @param kernel The kernel
		 * @param iterations The number of iterations
		 * @param alphaChannel If {@code true}, also convolute the alpha channel*/
		public final void convolute2d(float[] kernel, int iterations, boolean alphaChannel) {
			convolute2d(kernel, pixels, buffer, iterations, alphaChannel);
			swapBuffer();
		}
		
		private final void convolute2d(float[] kernel, T input, T output, int iterations,
				boolean alphaChannel) {
			int klen = kernel.length;
			int size = (int) FastMath.sqrt(klen) + 1;
			int[] indexes = new int[klen];
			indexKernel(indexes, size, size);
			convolute2d(kernel, indexes, input, output, iterations, alphaChannel);
		}
		
		private final void indexKernel(int[] indexes, int rows, int cols) {
			int hr = rows / 2;
			int hc = cols / 2;
			for(int i = 0, c = cols, x = -hc, y = -hr * width, l = indexes.length; i < l; ++i) {
				/* Creates the indexes of a kernel so that the final index of the pixel
				 * to take from will be: px = index + kernel[i].
				 *
				 * As an example, the index kernel of size 3x3 should look like this:
				 *
				 *     -width-1 -width -width+1
				 *     -1       0       +1
				 *     +width-1 +width +width+1
				 *
				 * So then, the pixels values (separated in all channels RGB and A)
				 * will be taken from the indexes:
				 * 
				 *     index-width-1, index-width, index-width+1, index-1, ...
				 *
				 * The final value of added values divided by the value of the kernel
				 * will be the final value of the pixel on the given index.*/
				indexes[i] = y + x; ++x;
				if((--c == 0)) { c = cols; x = -hc; y += width; }
			}
		}
		
		private final float[] checkKernel(float[] kernel) {
			int rows, cols = rows = (int) FastMath.sqrt(kernel.length);
			/* The middle row is the row where the pixel for what
			 * we want to calculate the value is. Even number of rows
			 * does not have only one middle row but two. Therefore the
			 * number of rows should not be even. In the case of even
			 * sized kernel, just resize the kernel array and set
			 * the values to 0, if the index of the value is outside
			 * of the previous kernel.*/
			if((rows & 1) == 0 || (cols & 1) == 0) {
				int newSize = ++rows * ++cols;
				kernel  	= Arrays.copyOf(kernel, newSize);
			}
			return kernel;
		}
		
		private final float kernelValue(float[] kernel) {
			float kval = 0.0f;
			for(int i = 0, l = kernel.length; i < l; ++i)
				kval += kernel[i];
			return kval == 0.0f ? 1.0f : kval;
		}
		
		private final void convolute2d(float[] kernel, int[] indexes,
				T input, T output, int iterations, boolean alphaChannel) {
			float[] fkernel = checkKernel(kernel);
			final CounterLock lock = new CounterLock();
			int x = 0;
			int y = 0;
			int w = width  / 4;
			int h = height / 4;
			for(int i = 0; i < iterations; ++i) {
				for(int kx = x, ky = y;;) {
					int sx = kx;
					int sy = ky;
					int sw = kx + w >= width  ? width  - kx : w;
					int sh = ky + h >= height ? height - ky : h;
					lock.increment();
					Threads.execute(() -> {
						convolute2d(sx, sy, sw, sh, width, fkernel,
							indexes, input, output, alphaChannel);
						lock.decrement();
					});
					if((kx += w) >= width) {
						kx  = 0;
						if((ky += h) >= height)
							break;
					}
				}
				lock.await();
				if((i != iterations - 1)) {
					// no need to copy output pixels to input ones at the last iteration
					System.arraycopy(output.array(), 0, input.array(), 0, output.capacity());
				}
			}
		}
		
		// Faster version of image convolution that requires the kernel to be normalized
		private final void convolute2d(int x, int y, int width, int height, int stride,
				float[] kernel, int[] indexes, T input, T output, boolean alphaChannel) {
			float pxa, pxr, pxg, pxb, mul;
			int imgw = IImage.this.width;
			int imgh = IImage.this.height;
			int klen = (int) FastMath.sqrt(kernel.length) / 2;
			int maxx = clamp(x + width,  klen, IImage.this.width  - klen);
			int maxy = clamp(y + height, klen, IImage.this.height - klen);
			int minx = clamp(x, klen, maxx - 1);
			int miny = clamp(y, klen, maxy - 1);
			int neww = maxx - minx;
			int newh = maxy - miny;
			int iinc = stride - neww;
			int epp  = format.getElementsPerPixel();
			if((alphaChannel)) {
				for(int i = miny * stride + minx, c = neww, r = newh, m = indexes.length, clr, ind;; ++i) {
					pxa = 0.0f;
					pxr = 0.0f;
					pxg = 0.0f;
					pxb = 0.0f;
					for(int k = 0; k < m; ++k) {
						mul  = kernel[k];
						ind  = i + indexes[k];
						clr  = format.getARGB(input, ind * epp);
						pxa += ((clr >> 24) & 0xff) * mul;
						pxr += ((clr >> 16) & 0xff) * mul;
						pxg += ((clr >>  8) & 0xff) * mul;
						pxb += ((clr)       & 0xff) * mul;
					}
					format.setPixel(output, i * epp,
					                clamp02((int) pxr),
					                clamp02((int) pxg),
					                clamp02((int) pxb),
					                clamp02((int) pxa));
					if((--c == 0)) {
						c  = neww;
						i += iinc;
						if((--r == 0))
							break;
					}
				}
			} else {
				int alpha = 0xff;
				for(int i = miny * stride + minx, c = neww, r = newh, m = indexes.length, clr, ind;; ++i) {
					pxr = 0.0f;
					pxg = 0.0f;
					pxb = 0.0f;
					for(int k = 0; k < m; ++k) {
						mul  = kernel[k];
						ind  = i + indexes[k];
						clr  = format.getARGB(input, ind * epp);
						pxr += ((clr >> 16) & 0xff) * mul;
						pxg += ((clr >>  8) & 0xff) * mul;
						pxb += ((clr)       & 0xff) * mul;
					}
					format.setPixel(output, i * epp,
					                clamp02((int) pxr),
					                clamp02((int) pxg),
					                clamp02((int) pxb),
					                (alpha));
					if((--c == 0)) {
						c  = neww;
						i += iinc;
						if((--r == 0))
							break;
					}
				}
			}
			// Top edge
			convolute2d_edges(x, 0, width, klen, stride - width,
							  stride, kernel, indexes, input, output, alphaChannel);
			// Bottom edge
			convolute2d_edges(x, imgh - klen, width, klen, stride - width,
							  stride, kernel, indexes, input, output, alphaChannel);
			// Left edge
			convolute2d_edges(0, Math.max(y, klen), klen, height - (y + height >= imgh ? klen : 0), imgw - klen,
							  stride, kernel, indexes, input, output, alphaChannel);
			// Right edge
			convolute2d_edges(imgw - klen, Math.max(y, klen), klen, height - (y + height >= imgh ? klen : 0), imgw - klen,
							  stride, kernel, indexes, input, output, alphaChannel);
		}
		
		// Convolute the edges using the Extend method (edge pixels are "copied" over)
		private final void convolute2d_edges(int x, int y, int w, int h, int d, int stride, float[] kernel,
				int[] indexes, T input, T output, boolean alphaChannel) {
			if((w <= 0 || h <= 0)) return; // Nothing to do
			float pxa, pxr, pxg, pxb, mul;
			int klen = (int) FastMath.sqrt(kernel.length) + 1;
			int imgw = IImage.this.width;
			int imgh = IImage.this.height;
			int epp  = format.getElementsPerPixel();
			for(int i = y * stride + x, c = w, r = h, m = indexes.length, ind, clr, kcx, kcy, kh = klen / 2;; ++i) {
				pxa = 0.0f;
				pxr = 0.0f;
				pxg = 0.0f;
				pxb = 0.0f;
				for(int k = 0, kx = -kh, ky = -kh, ke = kh+1; k < m; ++k) {
					mul = kernel[k];
					ind = i + indexes[k];
					kcx = x + (w - c) + kx;
					kcy = y + (h - r) + ky;
					if((kcy < 0))     ind -= kcy * stride; else
					if((kcy >= imgh)) ind -= (kcy - imgh + 1) * stride;
					if((kcx < 0))     ind -= kcx; else
					if((kcx >= imgw)) ind -= kcx - imgw + 1;
					clr = format.getARGB(input, ind * epp);
					pxa += ((clr >> 24) & 0xff) * mul;
					pxr += ((clr >> 16) & 0xff) * mul;
					pxg += ((clr >>  8) & 0xff) * mul;
					pxb += ((clr)       & 0xff) * mul;
					if((++kx == ke)) {
						kx = -kh;
						if((++ky == ke))
							break;
					}
				}
				if(!alphaChannel) pxa = 0xff;
				format.setPixel(output, i * epp,
				                clamp02((int) pxr),
				                clamp02((int) pxg),
				                clamp02((int) pxb),
				                clamp02((int) pxa));
				if((--c == 0)) {
					c  = w;
					i += d;
					if((--r == 0))
						break;
				}
			}
		}
	}
	
	/**
	 * Contains methods for applying image adjustments.*/
	public final class Adjustments {
		
		/**
		 * Inverts colors of {@code this} image.*/
		public final void invert() {
			applyActionRGB((rgb, input, output, i, varStore) -> {
				rgb[0] = 0xff - rgb[0];
				rgb[1] = 0xff - rgb[1];
				rgb[2] = 0xff - rgb[2];
			});
		}
		
		/**
		 * Converts {@code this} image to a grayscale version.
		 * Formula used:<br>
		 * {@code 0.299 * R + 0.587 * G + 0.114 * B}.*/
		public final void grayscale() {
			applyActionRGB((rgb, input, output, i, varStore) -> {
				int gray = Colors.f2rgba(0.299f * rgb[0] + 0.587f * rgb[1] + 0.114f * rgb[2]);
				rgb[0] = gray;
				rgb[1] = gray;
				rgb[2] = gray;
			});
		}
		
		/**
		 * Alters the brightness of {@code this} image.
		 * @param value The value*/
		public final void brightness(float value) {
			final float fval = clamp11(value) * F2I;
			applyActionRGB((rgb, input, output, i, varStore) -> {
				rgb[0] = Colors.f2rgba(rgb[0] + fval);
				rgb[1] = Colors.f2rgba(rgb[1] + fval);
				rgb[2] = Colors.f2rgba(rgb[2] + fval);
			});
		}
		
		/**
		 * Alters the contrast of {@code this} image.
		 * @param value The value*/
		public final void contrast(float value) {
			final float fval = clamp00(value);
			applyActionRGB((rgb, input, output, i, varStore) -> {
				rgb[0] = Colors.f2rgba(fval * (rgb[0] - 128) + 128);
				rgb[1] = Colors.f2rgba(fval * (rgb[1] - 128) + 128);
				rgb[2] = Colors.f2rgba(fval * (rgb[2] - 128) + 128);
			});
		}
		
		/**
		 * Alters the gamma of {@code this} image.
		 * @param value The value*/
		public final void gamma(float value) {
			final float fval = 1.0f / value;
			applyActionRGB((rgb, input, output, i, varStore) -> {
				rgb[0] = Colors.f2rgba(FastMath.pow(rgb[0] * I2F, fval) * F2I);
				rgb[1] = Colors.f2rgba(FastMath.pow(rgb[1] * I2F, fval) * F2I);
				rgb[2] = Colors.f2rgba(FastMath.pow(rgb[2] * I2F, fval) * F2I);
			});
		}
		
		/**
		 * Alters the alpha value of {@code this} image.
		 * @param value The value*/
		public final void alpha(int value) {
			final int fval = value << 24;
			applyActionINT((input, output, i, varStore) -> {
				format.setARGB(output, i, (format.getARGB(input, i) & 0x00ffffff) | fval);
			});
		}
		
		/**
		 * Alters the transparency of {@code this} image.
		 * @param value The value*/
		public final void transparency(float value) {
			final float fval = clamp01(value);
			applyActionINT((input, output, i, varStore) -> {
				int alpha = (int) ((format.getARGB(input, i) >>> 24) * fval) << 24;
				format.setARGB(output, i, (format.getARGB(input, i) & 0x00ffffff) | alpha);
			});
		}
		
		/**
		 * Alters the hue of {@code this} image.
		 * @param value The value*/
		public final void hue(float value) {
			final float fval = clamp11(value);
			applyActionHSL((hsl, input, output, index, varStore) -> {
				float hue = hsl[0] + fval;
				if((hue < 0.0f)) hue += 1.0f; else
				if((hue > 1.0f)) hue -= 1.0f;
				hsl[0] = hue;
			});
		}
		
		/**
		 * Alters the saturation of {@code this} image.
		 * @param value The value*/
		public final void saturation(float value) {
			final float fval = clamp00(value);
			applyActionHSL((hsl, input, output, index, varStore) -> {
				hsl[1] *= fval;
			});
		}
		
		/**
		 * Alters the lightness of {@code this} image.
		 * @param value The value*/
		public final void lightness(float value) {
			final float fval = clamp00(value);
			applyActionHCL((hcl, input, output, index, varStore) -> {
				hcl[2] *= fval;
			});
		}
		
		/**
		 * Alters the chroma of {@code this} image.
		 * @param value The value*/
		public final void chroma(float value) {
			final float fval = clamp00(value);
			applyActionHCL((hcl, input, output, index, varStore) -> {
				hcl[1] *= fval;
			});
		}
		
		/**
		 * Thresholds {@code this} image, meaning that all pixels that have
		 * the lowest 8-bits greater than or equaled the given value, are set
		 * to white color, otherwise to black color.
		 * @param value The value*/
		public final void threshold(int value) {
			thresholdGRT(value);
		}
		
		/**
		 * Thresholds {@code this} image, meaning that all pixels that have
		 * the lowest 8-bits lower than or equaled the given value, are set
		 * to white color, otherwise to black color.
		 * @param value The value*/
		public final void thresholdLWR(int value) {
			final int fval = clamp02(value);
			applyActionINT((input, output, i, varStore) -> {
				format.setARGB(output, i, (format.getARGB(input, i) & 0xff) <= fval ? 0xffffffff : 0xff000000);
			});
		}
		
		/**
		 * Thresholds {@code this} image, meaning that all pixels that have
		 * the lowest 8-bits greater than or equaled the given value, are set
		 * to white color, otherwise to black color.
		 * @param value The value*/
		public final void thresholdGRT(int value) {
			final int fval = clamp02(value);
			applyActionINT((input, output, i, varStore) -> {
				format.setARGB(output, i, (format.getARGB(input, i) & 0xff) >= fval ? 0xffffffff : 0xff000000);
			});
		}
		
		/**
		 * Thresholds {@code this} image, meaning that all pixels that have
		 * the lowest 8-bits between the given value {@code min} and the given
		 * value {@code max} (both inclusive), are set to white color,
		 * otherwise to black color.
		 * @param min The minimum value
		 * @param max The maximum value*/
		public final void thresholdBTW(int min, int max) {
			final int fmin = clamp02(min);
			final int fmax = clamp02(max);
			applyActionINT((input, output, i, varStore) -> {
				int value = format.getARGB(input, i) & 0xff;
				format.setARGB(output, i, value >= fmin && value <= fmax ? 0xffffffff : 0xff000000);
			});
		}
		
		/**
		 * Thresholds {@code this} image using the {@linkplain #thresholdGRT(int)}
		 * method with a value of {@linkplain IImage#optimalThreshold(int[], int)}
		 * method and {@code this} image's histogram.*/
		public final void histogramThreshold() {
			grayscale();
			thresholdGRT((int) optimalThreshold(histogram(), pixels.capacity()));
		}
		
		/**
		 * Sets all the shadows of {@code this} image to white color.
		 * Shadows are pixels that have grayscale value of less than {@code 85}.*/
		public final void shadows() {
			grayscale();
			// luminance <= 84
			thresholdLWR(84);
		}
		
		/**
		 * Sets all the middle tones of {@code this} image to white color.
		 * Middle tones are pixels that have grayscale value between
		 * {@code 85} and {@code 170} (both inclusive).*/
		public final void middleTones() {
			grayscale();
			// 85 <= luminance <= 170
			thresholdBTW(85, 170);
		}
		
		/**
		 * Sets all the lights of {@code this} image to white color.
		 * Lights are pixels that have grayscale value of greater than {@code 170}.*/
		public final void lights() {
			grayscale();
			// luminance >= 171
			thresholdGRT(171);
		}
	}
	
	/**
	 * Contains methods for applying image filters.*/
	public final class Filters {
		
		/**
		 * Applies smoothing to {@code this} image.*/
		public final void smooth() {
			// Normalized float kernel
			final float[] kernel = {
				0.1f, 0.1f, 0.1f,
				0.1f, 0.2f, 0.1f,
				0.1f, 0.1f, 0.1f
			};
			convolution.convolute2d(kernel, 1, true);
		}
		
		/**
		 * Applies box blur to {@code this} image with no premultiplication.
		 * @param value The value*/
		public final void boxBlur(int value) {
			boxBlur(value, false);
		}
		
		/**
		 * Applies box blur to {@code this} image.
		 * @param value The value
		 * @param premultiply If {@code true}, the pixels are premultiplied*/
		public final void boxBlur(int value, boolean premultiply) {
			FastBlur.boxBlur(pixels, buffer, 0, 0, width, height, value, width, channels, premultiply);
			swapBuffer();
		}
		
		/**
		 * Applies gaussian blur to {@code this} image with no premultiplication.
		 * @param value The value*/
		public final void gaussianBlur(int value) {
			gaussianBlur(value, false);
		}
		
		/**
		 * Applies gaussian blur to {@code this} image.
		 * @param value The value
		 * @param premultiply If {@code true}, the pixels are premultiplied*/
		public final void gaussianBlur(int value, boolean premultiply) {
			FastBlur.gaussianBlur(pixels, buffer, 0, 0, width, height, value, width, channels, premultiply);
			swapBuffer();
		}
		
		/**
		 * Applies motion blur of the given angle to {@code this} image.
		 * @param angleDeg The angle, in degrees
		 * @param value The value*/
		public final void motionBlur(float angleDeg, float value) {
			motionBlur(angleDeg, value, false);
		}
		
		/**
		 * Applies motion blur of the given angle to {@code this} image.
		 * @param angleDeg The angle, in degrees
		 * @param value The value
		 * @param premultiply If {@code true}, premultiplies the pixels*/
		public final void motionBlur(float angleDeg, float value, boolean premultiply) {
			int cos = FastMath.round(value * FastMath.cosDeg(angleDeg));
			int sin = FastMath.round(value * FastMath.sinDeg(angleDeg));
			int epp = format.getElementsPerPixel();
			applyActionINT((input, output, index, varStore) -> {
				int x = (index / epp) % width, y = (index / epp) / width;
				int sx = x + cos, sy = y + sin;
				int ex = x - cos, ey = y - sin;
				int suma = 0, sumr = 0, sumg = 0, sumb = 0;
				int idiv = 0, iclr;
				// Bresenham's line algorithm
				int dx = Math.abs(ex - sx);
				int dy = Math.abs(ey - sy);
				int qx = dx;
				int qy = dy;
				int px = sx;
				int py = sy;
				int ix = ex < sx ? -1 : +1;
				int iy = ey < sy ? -1 : +1;
				if((dx >= dy)) {
					for(int i = 0; i <= dx; i++) {
						if((qy += dy) >= dx) {
							qy -= dx;
							py += iy;
						}
						px += ix;
						// Get the pixel's color components and add them to the sums
						if((px >= 0 && py >= 0 && px < width && py < height)) {
							if((premultiply)) iclr = format.getARGBPre(pixels, (py * width + px) * epp);
							else              iclr = format.getARGB   (pixels, (py * width + px) * epp);
							suma += (iclr >> 24) & 0xff;
							sumr += (iclr >> 16) & 0xff;
							sumg += (iclr >>  8) & 0xff;
							sumb += (iclr)       & 0xff;
							++idiv;
						}
					}
				} else {
					for(int i = 0; i <= dy; i++) {
						if((qx += dx) >= dy) {
							qx -= dy;
							px += ix;
						}
						py += iy;
						// Get the pixel's color components and add them to the sums
						if((px >= 0 && py >= 0 && px < width && py < height)) {
							if((premultiply)) iclr = format.getARGBPre(pixels, (py * width + px) * epp);
							else              iclr = format.getARGB   (pixels, (py * width + px) * epp);
							suma += (iclr >> 24) & 0xff;
							sumr += (iclr >> 16) & 0xff;
							sumg += (iclr >>  8) & 0xff;
							sumb += (iclr)       & 0xff;
							++idiv;
						}
					}
				}
				int a = suma / idiv;
				int r = sumr / idiv;
				int g = sumg / idiv;
				int b = sumb / idiv;
				if((premultiply)) format.setPixelPre(output, index, r, g, b, a);
				else              format.setPixel   (output, index, r, g, b, a);
			});
		}
		
		/**
		 * Applies sharpening to {@code this} image.
		 * @param value The value*/
		public final void sharpen(float value) {
			int epp = format.getElementsPerPixel();
			applyActionRGB((rgb, input, output, index, varStore) -> {
				int x = (index / epp) % width, y = (index / epp) / width;
				boolean isl = x > 0, isr = x < width  - 1;
				boolean ist = y > 0, isb = y < height - 1;
				int curr = rgb[0], curg = rgb[1], curb = rgb[2];
				int crsr = curr,   crsg = curg,   crsb = curb;
				int iclr, divc = 1;
				// Mean filtering (the coarse layer)
				if((isl)) {
					iclr = format.getARGB(input, index - epp);
					crsr += (iclr >> 16) & 0xff;
					crsg += (iclr >>  8) & 0xff;
					crsb += (iclr)       & 0xff;
					++divc;
					if((ist)) {
						iclr = format.getARGB(input, index - width * epp - epp);
						crsr += (iclr >> 16) & 0xff;
						crsg += (iclr >>  8) & 0xff;
						crsb += (iclr)       & 0xff;
						++divc;
					}
					if((isb)) {
						iclr = format.getARGB(input, index + width * epp - epp);
						crsr += (iclr >> 16) & 0xff;
						crsg += (iclr >>  8) & 0xff;
						crsb += (iclr)       & 0xff;
						++divc;
					}
				}
				if((isr)) {
					iclr = format.getARGB(input, index + epp);
					crsr += (iclr >> 16) & 0xff;
					crsg += (iclr >>  8) & 0xff;
					crsb += (iclr)       & 0xff;
					++divc;
					if((ist)) {
						iclr = format.getARGB(input, index - width * epp + epp);
						crsr += (iclr >> 16) & 0xff;
						crsg += (iclr >>  8) & 0xff;
						crsb += (iclr)       & 0xff;
						++divc;
					}
					if((isb)) {
						iclr = format.getARGB(input, index + width * epp + epp);
						crsr += (iclr >> 16) & 0xff;
						crsg += (iclr >>  8) & 0xff;
						crsb += (iclr)       & 0xff;
						++divc;
					}
				}
				if((ist)) {
					iclr = format.getARGB(input, index - width * epp);
					crsr += (iclr >> 16) & 0xff;
					crsg += (iclr >>  8) & 0xff;
					crsb += (iclr)       & 0xff;
					++divc;
				}
				if((isb)) {
					iclr = format.getARGB(input, index + width * epp);
					crsr += (iclr >> 16) & 0xff;
					crsg += (iclr >>  8) & 0xff;
					crsb += (iclr)       & 0xff;
					++divc;
				}
				crsr /= divc;
				crsg /= divc;
				crsb /= divc;
				// Sharpening calculation (Out = In + (In - Coarse) * 0.5)
				rgb[0] += (curr - crsr) * 0.5f * value;
				rgb[1] += (curg - crsg) * 0.5f * value;
				rgb[2] += (curb - crsb) * 0.5f * value;
			});
		}
		
		/**
		 * Applies sharpening of edges to {@code this} image.*/
		public final void sharpenEdges() {
			// Normalized float kernel
			final float[] kernel = {
				-0.166667f, -0.166667f, -0.166667f,
				-0.166667f, +2.333333f, -0.166667f,
				-0.166667f, -0.166667f, -0.166667f,
			};
			convolution.convolute2d(kernel, 1, true);
		}
		
		/**
		 * Applies unsharp mask to {@code this} image.*/
		public final void unsharpMask() {
			// Normalized float kernel
			float n0 = -0.00390625f;
			float n1 = -0.01562500f;
			float n2 = -0.02343750f;
			float n3 = -0.06250000f;
			float n4 = -0.09375000f;
			float n5 = +1.85937500f;
			final float[] kernel = {
				n0, n1, n2, n1, n0,
				n1, n3, n4, n3, n1,
				n2, n4, n5, n4, n2,
				n1, n3, n4, n3, n1,
				n0, n1, n2, n1, n0
			};
			convolution.convolute2d(kernel, 1, true);
		}
		
		/**
		 * Applies edge detection kernel to {@code this} image.*/
		public final void edgeDetection() {
			// Denormalized float kernel (since sum=0.0f)
			final float[] kernel = {
				-1.0f, -1.0f, -1.0f,
				-1.0f, +8.0f, -1.0f,
				-1.0f, -1.0f, -1.0f
			};
			convolution.convolute2d(kernel, 1, false);
		}
		
		/**
		 * Applies emboss to {@code this} image.*/
		public final void emboss() {
			// Normalized float kernel
			final float[] kernel = {
				-2.0f, -1.0f,  0.0f,
				-1.0f,  1.0f,  1.0f,
				 0.0f,  1.0f,  2.0f
			};
			convolution.convolute2d(kernel, 1, true);
		}
		
		/**
		 * Applies emboss effect to {@code this} image.
		 * @param angleDeg Angle of the emboss effect, in degrees
		 * @param value Strength of the emboss effect
		 * @param depth Depth of the emboss effect*/
		public final void emboss(float angleDeg, float value) {
			// Normalized float kernel
			final float[] kernel = {
				-value, -1.0f,  0.0f,
				-1.0f,  1.0f,  1.0f,
				 0.0f,  1.0f,  value
			};
			MatrixUtils.rotate(kernel, (float) Math.toRadians(angleDeg));
			convolution.convolute2d(kernel, 1, true);
		}
		
		/**
		 * Applies mean filter to {@code this} image.*/
		public final void mean() {
			// Normalized float kernel
			final float[] kernel = {
				0.11111f, 0.11111f, 0.11111f,
				0.11111f, 0.11111f, 0.11111f,
				0.11111f, 0.11111f, 0.11111f
			};
			convolution.convolute2d(kernel, 1, true);
		}
		
		/**
		 * Applies sobel filter to {@code this} image.*/
		public final void sobel() {
			int epp = format.getElementsPerPixel();
			// Precompute the grayscale values of the pixels
			for(int i = 0, l = buffer.capacity(); i < l; i += epp)
				format.set(buffer, i, Colors.grayscale(format.getARGB(buffer, i)));
			// Do the actual Sobel filtering
			jobs.area(1, 1, width - 1, height - 1, buffer, pixels, (rx, ry, rw, rh, input, stride, output) -> {
				for(int i = ry * stride + rx, ii = stride - rw, x = rw, y = rh, gx, gy;;) {
					// Sobel x-kernel and y-kernel pass
					gx = -2 * format.get(input, (i - 1) * epp)
						 -1 * format.get(input, (i - stride - 1) * epp)
						 -1 * format.get(input, (i + stride - 1) * epp)
						 +2 * format.get(input, (i + 1) * epp)
						 +1 * format.get(input, (i - stride + 1) * epp)
						 +1 * format.get(input, (i + stride + 1) * epp);
					gy = -2 * format.get(input, (i - stride) * epp)
						 -1 * format.get(input, (i - stride - 1) * epp)
						 +1 * format.get(input, (i + stride - 1) * epp)
						 +2 * format.get(input, (i + stride) * epp)
						 -1 * format.get(input, (i - stride + 1) * epp)
						 +1 * format.get(input, (i + stride + 1) * epp);
					// The Sobel value from normalized magnitude and direction
					format.setARGB(output, i * epp, Colors.sobelXY(gx, gy));
					++i;
					if((--x == 0)) {
						x  = rw;
						i += ii;
						if((--y == 0))
							break;
					}
				}
			});
			/* Use copy method around the edges, i.e. the pixels are "copied"
			 * so that they are outside of the images and act like they were
			 * there all along. This is only done virtually, no pixels are
			 * actually being copied.*/
			// Top line
			jobs.lineH(0, 0, width, buffer, pixels, (i, x, y, input, stride, output) -> {
				int gx, gy;
				// Sobel x-kernel and y-kernel pass
				gx = 0;
				gy = -2 * format.get(input, (i) * epp)
					 +2 * format.get(input, (i + stride) * epp);
				if((x == 0)) {
					gx += -3 * format.get(input, (i) * epp)
						  -1 * format.get(input, (i + stride) * epp);
					gy += -1 * format.get(input, (i) * epp)
						  +1 * format.get(input, (i + stride) * epp);
				} else {
					gx += -3 * format.get(input, (i - 1) * epp)
						  -1 * format.get(input, (i + stride - 1) * epp);
					gy += -1 * format.get(input, (i - 1) * epp)
						  +1 * format.get(input, (i + stride - 1) * epp);
				}
				if((x == width - 1)) {
					gx += +3 * format.get(input, (i) * epp)
						  +1 * format.get(input, (i + stride) * epp);
					gy += -1 * format.get(input, (i) * epp)
						  +1 * format.get(input, (i + stride) * epp);
				} else {
					gx += +3 * format.get(input, (i + 1) * epp)
						  +1 * format.get(input, (i + stride + 1) * epp);
					gy += -1 * format.get(input, (i + 1) * epp)
						  +1 * format.get(input, (i + stride + 1) * epp);
				}
				// The Sobel value from normalized magnitude and direction
				format.setARGB(output, i * epp, Colors.sobelXY(gx, gy));
			});
			// Bottom line
			jobs.lineH(0, height - 1, width, buffer, pixels, (i, x, y, input, stride, output) -> {
				int gx, gy;
				// Sobel x-kernel and y-kernel pass
				gx = 0;
				gy = -2 * format.get(input, (i - stride) * epp)
					 +2 * format.get(input, (i) * epp);
				if((x == 0)) {
					gx += -3 * format.get(input, (i) * epp)
						  -1 * format.get(input, (i - stride) * epp);
					gy += -1 * format.get(input, (i - stride) * epp)
						  +1 * format.get(input, (i) * epp);
				} else {
					gx += -3 * format.get(input, (i - 1) * epp)
						  -1 * format.get(input, (i - stride - 1) * epp);
					gy += -1 * format.get(input, (i - stride - 1) * epp)
						  +1 * format.get(input, (i - 1) * epp);
				}
				if((x == width - 1)) {
					gx += +3 * format.get(input, (i) * epp)
						  +1 * format.get(input, (i - stride) * epp);
					gy += -1 * format.get(input, (i - stride) * epp)
						  +1 * format.get(input, (i) * epp);
				} else {
					gx += +3 * format.get(input, (i + 1) * epp)
						  +1 * format.get(input, (i - stride + 1) * epp);
					gy += -1 * format.get(input, (i - stride + 1) * epp)
						  +1 * format.get(input, (i + 1) * epp);
				}
				// The Sobel value from normalized magnitude and direction
				format.setARGB(output, i * epp, Colors.sobelXY(gx, gy));
			});
			// Left line
			jobs.lineV(0, 1, height - 2, buffer, pixels, (i, x, y, input, stride, output) -> {
				int gx, gy;
				// Sobel x-kernel and y-kernel pass
				gx = -2 * format.get(input, (i) * epp)
					 -1 * format.get(input, (i - stride) * epp)
					 -1 * format.get(input, (i + stride) * epp)
					 +2 * format.get(input, (i + 1) * epp)
					 +1 * format.get(input, (i - stride + 1) * epp)
					 +1 * format.get(input, (i + stride + 1) * epp);
				gy = -2 * format.get(input, (i - stride) * epp)
					 -1 * format.get(input, (i - stride) * epp)
					 +1 * format.get(input, (i + stride) * epp)
					 +2 * format.get(input, (i + stride) * epp)
					 -1 * format.get(input, (i - stride + 1) * epp)
					 +1 * format.get(input, (i + stride + 1) * epp);
				// The Sobel value from normalized magnitude and direction
				format.setARGB(output, i * epp, Colors.sobelXY(gx, gy));
			});
			// Right line
			jobs.lineV(width - 1, 1, height - 2, buffer, pixels, (i, x, y, input, stride, output) -> {
				int gx, gy;
				// Sobel x-kernel and y-kernel pass
				gx = -2 * format.get(input, (i - 1) * epp)
					 -1 * format.get(input, (i - stride - 1) * epp)
					 -1 * format.get(input, (i + stride - 1) * epp)
					 +2 * format.get(input, (i) * epp)
					 +1 * format.get(input, (i - stride) * epp)
					 +1 * format.get(input, (i + stride) * epp);
				gy = -2 * format.get(input, (i - stride) * epp)
					 -1 * format.get(input, (i - stride - 1) * epp)
					 +1 * format.get(input, (i + stride - 1) * epp)
					 +2 * format.get(input, (i + stride) * epp)
					 -1 * format.get(input, (i - stride) * epp)
					 +1 * format.get(input, (i + stride) * epp);
				// The Sobel value from normalized magnitude and direction
				format.setARGB(output, i * epp, Colors.sobelXY(gx, gy));
			});
		}
	}
	
	/**
	 * Contains methods for applying image effects.*/
	public final class Effects {
		
		/**
		 * Applies shadow effect to {@code this} image. The shadow is created in
		 * the given angle in distance of the given position and its color is that
		 * of the given color.
		 * @param angle The angle, in radians
		 * @param distX The x-coordinate of the distance
		 * @param distY The y-coordinate of the distance
		 * @param color The color, as an ARGB int*/
		public final void shadow(float angle, float distX, float distY, int color) {
			int epp = format.getElementsPerPixel();
			BufferUtils.fill(buffer, 0x0, epp);
			// Produce the image's shadow of transparency and store it in output
			float dx = FastMath.cos(angle) * distX;
			float dy = FastMath.sin(angle) * distY;
			float x = dx, y = dy;
			for(int i = 0, k = width;; i += epp) {
				if((x >= 0 && x < width) && (y >= 0 && y < height)) {
					if((format.getARGB(pixels, i) >>> 24) != 0x0) {
						format.setARGB(buffer, ((int) y * width + (int) x) * epp, color);
					}
				}
				++x;
				if((--k == 0)) {
					 k = width;
					 x = dx;
					 if((++y >= height))
						 break;
				}
			}
			// Combine the image's pixels and the shadow's pixels (with blending)
			for(int i = 0, l = pixels.capacity(); i < l; i += epp) {
				format.setARGB(pixels, i, Colors.blend(format.getARGB(pixels, i),
				                                       format.getARGB(buffer, i)));
			}
		}
	}
	
	public static final int BACKGROUND = 0;
	public static final int FOREGROUND = 0xff;
	public static final int BLANK      = 0x100;
	
	private static final class StructuresConfiguration {
		
		public final int valueTrue;
		public final int valueFalse;
		public final BiFunction<Integer, Integer, Boolean> conditionHas;
		public final Function<Integer, Boolean> conditionCan;
		
		public StructuresConfiguration(int valueTrue, int valueFalse,
				BiFunction<Integer, Integer, Boolean> conditionHas,
				Function<Integer, Boolean> conditionCan) {
			this.valueTrue    = valueTrue;
			this.valueFalse   = valueFalse;
			this.conditionHas = conditionHas;
			this.conditionCan = conditionCan;
		}
	}
	
	public final Structures structures = new Structures();
	public final class Structures {
		
		public final StructuresConfiguration DEFAULT_CONFIGURATION
			= new StructuresConfiguration(FOREGROUND, BACKGROUND, (a, b) -> !a.equals(b), (a) -> true);
		
		public final void convolute2d(int[] structure) {
			convolute2d(structure, DEFAULT_CONFIGURATION);
		}
		
		public final void convolute2d(int[] structure, StructuresConfiguration config) {
			// Make sure that the convolution outputs to the current version of pixels
			BufferUtils.buffercopy(pixels, buffer);
			convolute2d(structure, pixels, buffer, config);
			swapBuffer();
		}
		
		private final void convolute2d(int[] structure, T input, T output,
				StructuresConfiguration config) {
			int slen = structure.length;
			int size = (int) FastMath.sqrt(slen) + 1;
			int[] indexes = new int[slen];
			indexStructure(indexes, size, size);
			convolute2d(structure, indexes, input, output, config);
		}
		
		private final void indexStructure(int[] indexes, int rows, int cols) {
			int hr = rows / 2;
			int hc = cols / 2;
			for(int i = 0, c = cols, x = -hc, y = -hr * width, l = indexes.length; i < l; ++i) {
				indexes[i] = y + x; ++x;
				if((--c == 0)) { c = cols; x = -hc; y += width; }
			}
		}
		
		private final int[] checkStructure(int[] structure) {
			int rows, cols = rows = (int) FastMath.sqrt(structure.length);
			if((rows & 1) == 0 || (cols & 1) == 0) {
				int newSize = ++rows * ++cols;
				structure  	= Arrays.copyOf(structure, newSize);
			}
			return structure;
		}
		
		private final void convolute2d(int[] structure, int[] indexes, T input, T output,
				StructuresConfiguration config) {
			int[] fStructure = checkStructure(structure);
			final CounterLock lock = new CounterLock();
			int x = 0;
			int y = 0;
			int w = width  / 4;
			int h = height / 4;
			for(int kx = x, ky = y;;) {
				int sx = kx;
				int sy = ky;
				int sw = kx + w >= width  ? width  - kx : w;
				int sh = ky + h >= height ? height - ky : h;
				lock.increment();
				Threads.execute(() -> {
					convolute2d(sx, sy, sw, sh, width, fStructure, indexes, input, output, config);
					lock.decrement();
				});
				if((kx += w) >= width) {
					kx  = 0;
					if((ky += h) >= height)
						break;
				}
			}
			lock.await();
		}
		
		private final void convolute2d(int x, int y, int width, int height, int stride,
				int[] structure, int[] indexes, T input, T output, StructuresConfiguration config) {
			int imgw = IImage.this.width;
			int imgh = IImage.this.height;
			int slen = (int) FastMath.sqrt(structure.length) / 2;
			int maxx = clamp(x + width,  slen, IImage.this.width  - slen);
			int maxy = clamp(y + height, slen, IImage.this.height - slen);
			int minx = clamp(x, slen, maxx - 1);
			int miny = clamp(y, slen, maxy - 1);
			int neww = maxx - minx;
			int newh = maxy - miny;
			int iinc = stride - neww;
			int epp  = format.getElementsPerPixel();
			for(int i = miny * stride + minx, c = neww, r = newh, m = indexes.length, pxv;; ++i) {
				if((config.conditionCan.apply(format.getARGB(input, i * epp) & 0xff))) {
					pxv = config.valueTrue;
					for(int k = 0; k < m; ++k) {
						if((structure[k] <= FOREGROUND
								&& config.conditionHas.apply(format.getARGB(input, (i + indexes[k]) * epp) & 0xff,
								                             structure[k]))) {
							pxv = config.valueFalse; break;
						}
					}
					format.setPixel(output, i * epp, pxv, pxv, pxv, 0xff);
				}
				if((--c == 0)) {
					c  = neww;
					i += iinc;
					if((--r == 0))
						break;
				}
			}
			// Top edge
			convolute2d_edges(x, 0, width, slen, stride - width,
							  stride, structure, indexes, input, output, config);
			// Bottom edge
			convolute2d_edges(x, imgh - slen, width, slen, stride - width,
							  stride, structure, indexes, input, output, config);
			// Left edge
			convolute2d_edges(0, Math.max(y, slen), slen, height - (y + height >= imgh ? slen : 0), imgw - slen,
							  stride, structure, indexes, input, output, config);
			// Right edge
			convolute2d_edges(imgw - slen, Math.max(y, slen), slen, height - (y + height >= imgh ? slen : 0), imgw - slen,
							  stride, structure, indexes, input, output, config);
		}
		
		private final void convolute2d_edges(int x, int y, int w, int h, int d, int stride, int[] structure,
				int[] indexes, T input, T output, StructuresConfiguration config) {
			if((w <= 0 || h <= 0)) return; // Nothing to do
			int slen = (int) FastMath.sqrt(structure.length) + 1;
			int imgw = IImage.this.width;
			int imgh = IImage.this.height;
			int epp  = format.getElementsPerPixel();
			for(int i = y * stride + x, c = w, r = h, m = indexes.length, ind, kcx, kcy, kh = slen / 2, pxv;; ++i) {
				if((config.conditionCan.apply(format.getARGB(input, i * epp) & 0xff))) {
					pxv = config.valueTrue;
					for(int k = 0, kx = -kh, ky = -kh, ke = kh+1; k < m; ++k) {
						ind = i + indexes[k];
						kcx = x + (w - c) + kx;
						kcy = y + (h - r) + ky;
						if((kcy < 0))     ind -= kcy * stride; else
						if((kcy >= imgh)) ind -= (kcy - imgh + 1) * stride;
						if((kcx < 0))     ind -= kcx; else
						if((kcx >= imgw)) ind -= kcx - imgw + 1;
						if((structure[k] <= FOREGROUND
								&& config.conditionHas.apply(format.getARGB(input, ind * epp) & 0xff,
								                             structure[k]))) {
							pxv = config.valueFalse; break;
						}
						if((++kx == ke)) {
							kx = -kh;
							if((++ky == ke))
								break;
						}
					}
					format.setPixel(output, i * epp, pxv, pxv, pxv, 0xff);
				}
				if((--c == 0)) {
					c  = w;
					i += d;
					if((--r == 0))
						break;
				}
			}
		}
	}
	
	public final Morphology morphology = new Morphology();
	public final class Morphology {
		
		public final void binarize(int threshold) {
			adjustments.grayscale();
			adjustments.threshold(threshold);
		}
		
		private final boolean checkStructure(int[] structure) {
			int sqrt = (int) FastMath.sqrt(structure.length) + 1;
			return sqrt * sqrt == structure.length;
		}
		
		// https://homepages.inf.ed.ac.uk/rbf/HIPR2/hitmiss.htm
		public final void hitAndMiss(int[] structure) {
			if(!checkStructure(structure))
				throw new IllegalArgumentException("Non-square structure");
			structures.convolute2d(structure);
		}
		
		// https://homepages.inf.ed.ac.uk/rbf/HIPR2/thin.htm
		public final void thin(int[] structure) {
			// thin(img, struct) = img - hitAndMiss(struct)
			// thin(img, struct) = AND(img, NOT(hitAndMiss(struct)))
			opSave();
			hitAndMiss(structure);
			operations.not();
			operations.and();
		}
		
		public final void dilation() {
			dilation(3);
		}
		
		// https://homepages.inf.ed.ac.uk/rbf/HIPR2/dilate.htm
		public final void dilation(int size) {
			if((size & 1) == 0)
				throw new IllegalArgumentException("Size must be odd");
			final int[] structure = new int[size * size];
			Arrays.fill(structure, FOREGROUND);
			structures.convolute2d(structure, new StructuresConfiguration(BACKGROUND, FOREGROUND, (a, b) -> a.equals(b), (a) -> a.equals(BACKGROUND)));
		}
		
		public final void erosion() {
			erosion(3);
		}
		
		// https://homepages.inf.ed.ac.uk/rbf/HIPR2/erode.htm
		public final void erosion(int size) {
			if((size & 1) == 0)
				throw new IllegalArgumentException("Size must be odd");
			final int[] structure = new int[size * size];
			Arrays.fill(structure, BACKGROUND);
			structures.convolute2d(structure, new StructuresConfiguration(FOREGROUND, BACKGROUND, (a, b) -> a.equals(b), (a) -> a.equals(FOREGROUND)));
		}
		
		public final void skeletonize() {
			thin(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BLANK,      FOREGROUND, BLANK,
       			FOREGROUND, FOREGROUND, FOREGROUND
       		});
			thin(new int[] {
       			FOREGROUND, FOREGROUND, FOREGROUND,
       			BLANK,      FOREGROUND, BLANK,
       			BACKGROUND, BACKGROUND, BACKGROUND
       		});
			thin(new int[] {
       			FOREGROUND, BLANK,      BACKGROUND,
       			FOREGROUND, FOREGROUND, BACKGROUND,
       			FOREGROUND, BLANK,      BACKGROUND
       		});
			thin(new int[] {
       			BACKGROUND, BLANK,      FOREGROUND,
       			BACKGROUND, FOREGROUND, FOREGROUND,
       			BACKGROUND, BLANK,      FOREGROUND
       		});
			thin(new int[] {
       			BLANK,      BACKGROUND, BACKGROUND,
       			FOREGROUND, FOREGROUND, BACKGROUND,
       			BLANK,      FOREGROUND, BLANK
       		});
			thin(new int[] {
       			BLANK,      FOREGROUND, BLANK,
       			BACKGROUND, FOREGROUND, FOREGROUND,
       			BACKGROUND, BACKGROUND, BLANK
       		});
			thin(new int[] {
       			BLANK,      FOREGROUND, BLANK,
       			FOREGROUND, FOREGROUND, BACKGROUND,
       			BLANK,      BACKGROUND, BACKGROUND
       		});
			thin(new int[] {
       			BACKGROUND, BACKGROUND, BLANK,
       			BACKGROUND, FOREGROUND, FOREGROUND,
       			BLANK,      FOREGROUND, BLANK
       		});
		}
		
		public final void skeletonize45deg() {
			thin(new int[] {
				BLANK,      BACKGROUND, BACKGROUND,
				FOREGROUND, FOREGROUND, BACKGROUND,
				FOREGROUND, FOREGROUND, BLANK
			});
			thin(new int[] {
				FOREGROUND, FOREGROUND, BLANK,
				FOREGROUND, FOREGROUND, BACKGROUND,
				BLANK,      BACKGROUND, BACKGROUND
			});
			thin(new int[] {
				BLANK,      FOREGROUND, FOREGROUND,
				BACKGROUND, FOREGROUND, FOREGROUND,
				BACKGROUND, BACKGROUND, BLANK
			});
			thin(new int[] {
				BACKGROUND, BACKGROUND, BLANK,
				BACKGROUND, FOREGROUND, FOREGROUND,
				BLANK,      FOREGROUND, FOREGROUND
			});
		}
		
		public final void prune() {
			thin(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BACKGROUND, FOREGROUND, BACKGROUND,
       			BACKGROUND, BLANK,      BLANK
       		});
			thin(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BACKGROUND, FOREGROUND, BACKGROUND,
       			BLANK,      BLANK,      BACKGROUND
       		});
			thin(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BACKGROUND, FOREGROUND, BLANK,
       			BACKGROUND, BACKGROUND, BLANK
       		});
			thin(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BLANK,      FOREGROUND, BACKGROUND,
       			BLANK,      BACKGROUND, BACKGROUND
       		});
			thin(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BLANK,      FOREGROUND, BACKGROUND,
       			BLANK,      BLANK,      BACKGROUND
       		});
			thin(new int[] {
       			BLANK,      BLANK,      BACKGROUND,
       			BLANK,      FOREGROUND, BACKGROUND,
       			BACKGROUND, BACKGROUND, BACKGROUND
       		});
			thin(new int[] {
       			BACKGROUND, BLANK,      BLANK,
       			BACKGROUND, FOREGROUND, BLANK,
       			BACKGROUND, BACKGROUND, BACKGROUND
       		});
			thin(new int[] {
       			BACKGROUND, BACKGROUND, BACKGROUND,
       			BACKGROUND, FOREGROUND, BLANK,
       			BACKGROUND, BLANK,      BLANK
       		});
		}
	}
	
	public final Operations operations = new Operations();
	public final class Operations {
		
		public final int FALSE = BACKGROUND;
		public final int TRUE  = FOREGROUND;
		
		public final int COLOR_FALSE = Colors.rgba(BACKGROUND, BACKGROUND, BACKGROUND, 0xff);
		public final int COLOR_TRUE  = Colors.rgba(FOREGROUND, FOREGROUND, FOREGROUND, 0xff);
		
		private final Deque<T> stack = new ArrayDeque<T>();
		
		private final int bool2val(boolean bool) {
			return bool ? COLOR_TRUE : COLOR_FALSE;
		}
		
		// Takes the current state of the image's pixels and do an OR operation
		// with the first buffer on the stack
		public final void or() {
			T buffer = stack.pop();
			applyActionINT((input, output, i, varStore) -> {
				boolean pi = (format.getARGB(input,  i) & 0xff) == TRUE;
				boolean pb = (format.getARGB(buffer, i) & 0xff) == TRUE;
				format.setARGB(output, i, bool2val(pi | pb));
			});
		}
		
		public final void and() {
			T buffer = stack.pop();
			applyActionINT((input, output, i, varStore) -> {
				boolean pi = (format.getARGB(input,  i) & 0xff) == TRUE;
				boolean pb = (format.getARGB(buffer, i) & 0xff) == TRUE;
				format.setARGB(output, i, bool2val(pi & pb));
			});
		}
		
		// Does not use stack
		public final void not() {
			applyActionINT((input, output, i, varStore) -> {
				format.setARGB(output, i, bool2val((format.getARGB(input, i) & 0xff) == FALSE));
			});
		}
		
		// Applies Summed-area table "operator" to the binary image
		// https://en.wikipedia.org/wiki/Summed-area_table
		public final int[] integral(Function<Integer, Integer> function) {
			int[] array = new int[width * height];
			for(int i = 0, x = 0, y = 0;; ++i) {
				int sum = function.apply(format.getARGB(pixels, i) & 0xff);
				if((x > 0))          sum += array[i - 1];
				if((y > 0))          sum += array[i - width];
				if((y > 0 && x > 0)) sum -= array[i - width - 1];
				array[i] = sum;
				if((++x == width)) {
					x = 0;
					if((++y == height))
						break;
				}
			}
			return array;
		}
	}
	
	public final Transforms transforms = new Transforms();
	public final class Transforms {
		
		public final int[] hough(int minContrast) {
			return hough(width, height, minContrast);
		}
		
		// https://rosettacode.org/wiki/Hough_transform#Java
		public final int[] hough(int thetaAxisSize, int radiusAxisSize, int minContrast) {
			int[] data = new int[thetaAxisSize * radiusAxisSize];
			int maxr = (int) FastMath.ceil(FastMath.hypot(width, height));
			int half = radiusAxisSize >>> 1;
			float[] sint = new float[thetaAxisSize];
			float[] cost = new float[thetaAxisSize];
			for(int theta = thetaAxisSize - 1; theta >= 0; --theta) {
				float rad = theta * FastMath.PI / thetaAxisSize;
				sint[theta] = FastMath.sin(rad);
				cost[theta] = FastMath.cos(rad);
			}
			int epp = format.getElementsPerPixel();
			applyActionINT((input, output, i, varStore) -> {
				boolean canApply = false;
				int x = i % width;
				int y = i / width;
				int val = format.getARGB(input, i);
				for(int k = 8; k >= 0; --k) {
					if((k == 4)) continue;
					int newx = x + (i % 3) - 1;
					int newy = y + (i / 3) - 1;
					if((newx < 0 || newx >= width || newy < 0 || newy >= height))
						continue;
					int pxv = format.getARGB(input, (newy * width + newx) * epp);
					if((FastMath.abs(pxv - val) >= minContrast)) {
						canApply = true; break;
					}
				}
				if((canApply)) {
					for(int theta = thetaAxisSize - 1; theta >= 0; --theta) {
						int r = (int) FastMath.round((cost[theta] * x + sint[theta] * y) * half / maxr) + half;
						data[r * radiusAxisSize + theta] += 1;
					}
				}
			});
			return data;
		}
	}
	
	// Saves the current state of the image's pixels to the operations stack
	public final void opSave() {
		opSave(pixels);
	}
	
	public final void opSave(T buffer) {
		operations.stack.push(BufferUtils.copy(buffer));
	}
	
	// Removes the first element on the operations stack and returns it
	public final T opRemove() {
		return operations.stack.pop();
	}
	
	@FunctionalInterface
	private static interface Job<T extends Buffer> {
		void execute(int rx, int ry, int rw, int rh, T input, int stride, T output);
	}
	
	@FunctionalInterface
	private static interface LineJob<T extends Buffer> {
		void execute(int i, int x, int y, T input, int stride, T output);
	}
	
	private final Jobs jobs = new Jobs();
	private final class Jobs {
		
		public final void area(int x, int y, int width, int height, T input, T output, Job<T> job) {
			final CounterLock lock = new CounterLock();
			int w = width  / 4;
			int h = height / 4;
			for(int kx = x, ky = y;;) {
				int sx = kx;
				int sy = ky;
				int sw = kx + w >= width  ? width  - kx : w;
				int sh = ky + h >= height ? height - ky : h;
				lock.increment();
				Threads.execute(() -> {
					job.execute(sx, sy, sw, sh, input, IImage.this.width, output);
					lock.decrement();
				});
				if((kx += w) >= width) {
					kx  = 0;
					if((ky += h) >= height)
						break;
				}
			}
			lock.await();
		}
		
		public final void lineH(int x, int y, int width, T input, T output, LineJob<T> job) {
			for(int stride = IImage.this.width, sx = x, i = y * stride + x, kx = width; kx-- != 0; ++sx, ++i) {
				job.execute(i, sx, y, input, stride, output);
			}
		}
		
		public final void lineV(int x, int y, int height, T input, T output, LineJob<T> job) {
			for(int stride = IImage.this.width, sy = y, i = y * stride + x, ky = height; ky-- != 0; ++sy, i += stride) {
				job.execute(i, x, sy, input, stride, output);
			}
		}
	}
	
	/**
	 * Provides fast blurring of image pixels. It offers two most used blurs:
	 * Box blur and Gaussian blur. Note that none of these are computed exactly;
	 * these methods are made for speed, not for accuracy.<br><br>
	 * @version 1.0
	 * @author Ivan Kutskir
	 * @author Sune
	 * @see
	 * <a href="http://blog.ivank.net/fastest-gaussian-blur.html">
	 * 	http://blog.ivank.net/fastest-gaussian-blur.html
	 * </a>*/
	private static final class FastBlur {
		
		public static final <T extends Buffer> void gaussianBlur(T input, T output, int x, int y, int w, int h, int r, int s,
				InternalChannels<T> channels, boolean premultiply) {
			final CounterLock lock   = new CounterLock(4);
			final int         length = input.capacity();
			final float[]     boxes  = generateBoxes(r, 3);
			byte[] outputR = new byte[length];
			byte[] outputG = new byte[length];
			byte[] outputB = new byte[length];
			byte[] outputA = new byte[length];
			Threads.execute(() -> {
				byte[] inputR = new byte[length];
				channels.separate(input, inputR, channels.getFormat().getShiftR(), false);
				gaussianBlur(inputR, outputR, x, y, w, h, r, s, boxes);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputG = new byte[length];
				channels.separate(input, inputG, channels.getFormat().getShiftG(), false);
				gaussianBlur(inputG, outputG, x, y, w, h, r, s, boxes);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputB = new byte[length];
				channels.separate(input, inputB, channels.getFormat().getShiftB(), false);
				gaussianBlur(inputB, outputB, x, y, w, h, r, s, boxes);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputA = new byte[length];
				channels.separate(input, inputA, channels.getFormat().getShiftA(), false);
				gaussianBlur(inputA, outputA, x, y, w, h, r, s, boxes);
				lock.decrement();
			});
			lock.await();
			channels.join(outputR, outputG, outputB, outputA, output, premultiply);
		}
		
		public static final <T extends Buffer> void boxBlur(T input, T output, int x, int y, int w, int h, int r, int s,
				InternalChannels<T> channels, boolean premultiply) {
			final CounterLock lock   = new CounterLock(4);
			final int         length = input.capacity();
			byte[] outputR = new byte[length];
			byte[] outputG = new byte[length];
			byte[] outputB = new byte[length];
			byte[] outputA = new byte[length];
			Threads.execute(() -> {
				byte[] inputR = new byte[length];
				channels.separate(input, inputR, channels.getFormat().getShiftR(), false);
				boxBlur(inputR, outputR, 0, 0, w, h, r, s);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputG = new byte[length];
				channels.separate(input, inputG, channels.getFormat().getShiftG(), false);
				boxBlur(inputG, outputG, 0, 0, w, h, r, s);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputB = new byte[length];
				channels.separate(input, inputB, channels.getFormat().getShiftB(), false);
				boxBlur(inputB, outputB, 0, 0, w, h, r, s);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputA = new byte[length];
				channels.separate(input, inputA, channels.getFormat().getShiftA(), false);
				boxBlur(inputA, outputA, 0, 0, w, h, r, s);
				lock.decrement();
			});
			lock.await();
			channels.join(outputR, outputG, outputB, outputA, output, premultiply);
		}
		
		private static final float[] generateBoxes(int sigma, int amount) {
			int sg = 12*sigma*sigma;
			int k0 = (int) Math.sqrt(sg / amount + 1);
			if((k0 & 1) == 0) --k0;
			int k1 = k0+2;
			int mk = (int) Math.round((sg-amount*k0*k0-4*amount*k0-3*amount)/(-4*k0-4));
			float[] sizes = new float[amount];
			for(int i = 0; i < amount; ++i)
				sizes[i] = i < mk ? k0 : k1;
			return sizes;
		}
		
		private static final void gaussianBlur(byte[] input, byte[] output, int x, int y, int w, int h,
				int r, int s, float[] bxs) {
			gboxBlur(input, output, x, y, w, h, (int) ((bxs[0] - 1.0f) * 0.5f), s);
			gboxBlur(output, input, x, y, w, h, (int) ((bxs[1] - 1.0f) * 0.5f), s);
			gboxBlur(input, output, x, y, w, h, (int) ((bxs[2] - 1.0f) * 0.5f), s);
		}
		
		private static final void boxBlur(byte[] input, byte[] output, int x, int y, int w, int h, int r, int s) {
			System.arraycopy(input, 0, output, 0, input.length);
			boxBlurHorizontal(output, input,  x, y, w, h, r, s);
			boxBlurVertical  (input,  output, x, y, w, h, r, s);
			boxBlurHorizontal(output, input,  x, y, w, h, r, s);
			boxBlurVertical  (input,  output, x, y, w, h, r, s);
		}
		
		private static final void gboxBlur(byte[] input, byte[] output, int x, int y, int w, int h, int r, int s) {
			System.arraycopy(input, 0, output, 0, input.length);
			boxBlurHorizontal(output, input, x, y, w, h, r, s);
			boxBlurVertical  (input, output, x, y, w, h, r, s);
		}
		
		private static final void boxBlurHorizontal(byte[] input, byte[] output, int x, int y, int w, int h, int r, int s) {
			float iarr = 1.0f / (r+r+1);
			for(int i = 0, k = y * s; i < h; ++i, k+=s) {
				int ti = k+x, li = ti, ri = ti+r, fv = input[ti] & 0xff,
					lv = input[ti+w-1] & 0xff, val = (r+1) * fv;
				for(int j = ti, e = ti+r; j < e; ++j)
					val += input[j] & 0xff;
				for(int j = 0; j <= r; ++j, ++ri, ++ti)
					output[ti] = (byte) FastMath.round((val += (input[ri] & 0xff) - fv) * iarr);
				for(int j = r+1, e = w-r; j < e; ++j, ++ri, ++li, ++ti)
					output[ti] = (byte) FastMath.round((val += (input[ri] & 0xff) - (input[li] & 0xff)) * iarr);
				for(int j = 0; j < r; ++j, ++li, ++ti)
					output[ti] = (byte) FastMath.round((val += lv - (input[li] & 0xff)) * iarr);
			}
		}
		
		private static final void boxBlurVertical(byte[] input, byte[] output, int x, int y, int w, int h, int r, int s) {
			float iarr = 1.0f / (r+r+1);
			for(int i = 0, f = x + y * s, l = r * s, c = s * (h-1); i < w; ++i) {
				int ti = i+f, li = ti, ri = ti+l, fv = input[ti] & 0xff,
					lv = input[ti+c] & 0xff, val = (r+1) * fv;
				for(int j = ti, e = ti+r*s; j < e; j+=s)
					val += input[j] & 0xff;
				for(int j = 0; j <= r; ++j, ri+=s, ti+=s)
					output[ti] = (byte) FastMath.round((val += (input[ri] & 0xff) - fv) * iarr);
				for(int j = r+1, e = h-r; j < e; ++j, li+=s, ri+=s, ti+=s)
					output[ti] = (byte) FastMath.round((val += (input[ri] & 0xff)-(input[li] & 0xff)) * iarr);
				for(int j = 0; j < r; ++j, li+=s, ti+=s)
					output[ti] = (byte) FastMath.round((val += lv-(input[li] & 0xff)) * iarr);
			}
		}
	}
	
	@FunctionalInterface
	private static interface ActionINT<T extends Buffer> {
		void action(T input, T output, int index, VariableStore varStore);
	}
	
	@FunctionalInterface
	private static interface ActionRGB<T extends Buffer> {
		void action(int[] rgb, T input, T output, int index, VariableStore varStore);
	}
	
	@FunctionalInterface
	private static interface ActionFloat<T extends Buffer> {
		void action(float[] arr, T input, T output, int index, VariableStore varStore);
	}
	
	@FunctionalInterface
	private static interface ConversionAction<A, B> {
		void convert(A a, B b);
	}
	
	private static final float clamp01(float val) {
		if((val <= 0.0f)) return 0.0f;
		if((val >= 1.0f)) return 1.0f;
		return val;
	}
	
	private static final float clamp11(float val) {
		if((val <= -1.0f)) return -1.0f;
		if((val >= +1.0f)) return +1.0f;
		return val;
	}
	
	private static final float clamp00(float val) {
		if((val <= 0.0f)) return 0.0f;
		return val;
	}
	
	private static final int clamp02(int val) {
		if((val <= 0x00)) return 0x00;
		if((val >= 0xff)) return 0xff;
		return val;
	}
	
	private static final int clamp(int val, int min, int max) {
		return val <= min ? min : val >= max ? max : val;
	}
	
	@FunctionalInterface
	private static interface ThreadedAction<T> {
		void action(int i, int epp, VariableStore varStore);
	}
	
	private static interface VariableStore {
		VariableStore copy();
		void prepare();
		Object get(int addr);
	}
	
	private static final EmptyVariableStore      VAR_STORE_EMPTY       = EmptyVariableStore.INSTANCE;
	private static final RGBVariableStore        VAR_STORE_RGB         = RGBVariableStore.INSTANCE;
	private static final FloatVariableStore      VAR_STORE_FLOAT       = FloatVariableStore.INSTANCE;
	private static final MatrixRGBAVariableStore VAR_STORE_MATRIX_RGBA = MatrixRGBAVariableStore.INSTANCE;
	private static final MatrixHSLAVariableStore VAR_STORE_MATRIX_HSLA = MatrixHSLAVariableStore.INSTANCE;
	private static final MatrixHCLAVariableStore VAR_STORE_MATRIX_HCLA = MatrixHCLAVariableStore.INSTANCE;
	
	private static final class EmptyVariableStore implements VariableStore {
		
		public static final EmptyVariableStore INSTANCE = new EmptyVariableStore();
		
		@Override public VariableStore copy() { return INSTANCE; }
		@Override public void prepare() {}
		@Override public Object get(int addr) { return null; }
	}
	
	private static final class RGBVariableStore implements VariableStore {
		
		public static final RGBVariableStore INSTANCE = new RGBVariableStore();
		
		private int[] rgb;
		
		@Override
		public VariableStore copy() {
			return new RGBVariableStore();
		}
		
		@Override
		public void prepare() {
			rgb = new int[4];
		}
		
		@Override
		public Object get(int addr) {
			// Always return RGB array, so it can be JITed easily
			return rgb;
		}
	}
	
	private static final class FloatVariableStore implements VariableStore {
		
		public static final FloatVariableStore INSTANCE = new FloatVariableStore();
		
		private float[] arr;
		private int  [] rgb;
		
		@Override
		public VariableStore copy() {
			return new FloatVariableStore();
		}
		
		@Override
		public void prepare() {
			arr = new float[4];
			rgb = new int  [3];
		}
		
		@Override
		public Object get(int addr) {
			return addr == 0 ? rgb : arr;
		}
	}
	
	private static final class MatrixRGBAVariableStore implements VariableStore {
		
		public static final MatrixRGBAVariableStore INSTANCE = new MatrixRGBAVariableStore();
		
		private float[] mtx;
		private int  [] rgb;
		
		@Override
		public VariableStore copy() {
			return new MatrixRGBAVariableStore();
		}
		
		@Override
		public void prepare() {
			mtx = new float[4];
			rgb = new int  [4];
		}
		
		@Override
		public Object get(int addr) {
			return addr == 0 ? rgb : mtx;
		}
	}
	
	private static final class MatrixHSLAVariableStore implements VariableStore {
		
		public static final MatrixHSLAVariableStore INSTANCE = new MatrixHSLAVariableStore();
		
		private float[] mtx;
		private float[] arr;
		private int  [] rgb;
		
		@Override
		public VariableStore copy() {
			return new MatrixHSLAVariableStore();
		}
		
		@Override
		public void prepare() {
			mtx = new float[4];
			arr = new float[4];
			rgb = new int  [3];
		}
		
		@Override
		public Object get(int addr) {
			return addr == 0 ? rgb : addr == 1 ? arr : mtx;
		}
	}
	
	private static final class MatrixHCLAVariableStore implements VariableStore {
		
		public static final MatrixHCLAVariableStore INSTANCE = new MatrixHCLAVariableStore();
		
		private float[] mtx;
		private float[] arr;
		private int  [] rgb;
		
		@Override
		public VariableStore copy() {
			return new MatrixHCLAVariableStore();
		}
		
		@Override
		public void prepare() {
			mtx = new float[4];
			arr = new float[4];
			rgb = new int  [3];
		}
		
		@Override
		public Object get(int addr) {
			return addr == 0 ? rgb : addr == 1 ? arr : mtx;
		}
	}
	
	private final void applyThreadedAction(T input, T output, VariableStore varStore, ThreadedAction<T> action) {
		final CounterLock lock = new CounterLock();
		int epp = format.getElementsPerPixel();
		int x = 0;
		int y = 0;
		int w = width  / 4;
		int h = height / 4;
		for(int kx = x, ky = y;;) {
			int sx = kx;
			int sy = ky;
			int sw = kx + w >= width  ? width  - kx : w;
			int sh = ky + h >= height ? height - ky : h;
			int si = sy * width + sx;
			int ai = width - sw;
			lock.increment();
			Threads.execute(() -> {
				VariableStore localVarStore = varStore.copy();
				localVarStore.prepare();
				for(int i = si, px = sw, py = sh;; ++i) {
					action.action(i, epp, localVarStore);
					if((--px == 0)) {
						px = sw;
						i += ai;
						if((--py == 0))
							break;
					}
				}
				lock.decrement();
			});
			if((kx += w) >= width) {
				kx  = 0;
				if((ky += h) >= height)
					break;
			}
		}
		lock.await();
	}
	
	private final void applyActionINT(T input, T output, ActionINT<T> action) {
		applyThreadedAction(input, output, VAR_STORE_EMPTY, (i, epp, varStore) -> {
			action.action(input, output, i * epp, varStore);
		});
	}
	
	private final void applyActionRGB(T input, T output, ActionRGB<T> action, VariableStore mainVarStore) {
		applyThreadedAction(input, output, mainVarStore, (i, epp, varStore) -> {
			int argb; // Variable declarations
			int[] rgb = (int[]) varStore.get(0);
			argb   = format.getARGB(input, i * epp);
			rgb[0] = (argb >> 16) & 0xff;
			rgb[1] = (argb >>  8) & 0xff;
			rgb[2] = (argb)       & 0xff;
			rgb[3] = (argb >> 24) & 0xff;
			action.action(rgb, input, output, i * epp, varStore);
			format.setPixel(output, i * epp,
			                clamp02(rgb[0]),
			                clamp02(rgb[1]),
			                clamp02(rgb[2]),
			                clamp02(rgb[3]));
		});
	}
	
	private final void applyActionRGB(T input, T output, ActionRGB<T> action) {
		applyActionRGB(input, output, action, VAR_STORE_RGB);
	}
	
	private final void applyActionFloat(T input, T output, ActionFloat<T> action,
			ConversionAction<int[], float[]> convForward,
			ConversionAction<float[], int[]> convInverse) {
		applyActionFloat(input, output, action, convForward, convInverse, VAR_STORE_FLOAT);
	}
	
	private final void applyActionFloat(T input, T output, ActionFloat<T> action,
			ConversionAction<int[], float[]> convForward,
			ConversionAction<float[], int[]> convInverse,
			VariableStore mainVarStore) {
		applyThreadedAction(input, output, mainVarStore, (i, epp, varStore) -> {
			int argb, alpha; // Variable declarations
			float[] arr = (float[]) varStore.get(1);
			int[]   rgb = (int[])   varStore.get(0);
			argb   = format.getARGB(input, i * epp);
			rgb[0] = (argb >> 16) & 0xff;
			rgb[1] = (argb >>  8) & 0xff;
			rgb[2] = (argb)       & 0xff;
			convForward.convert(rgb, arr);
			arr[3] = ((argb >> 24) & 0xff) * I2F;
			action.action(arr, input, output, i * epp, varStore);
			convInverse.convert(arr, rgb);
			alpha = FastMath.round(arr[3] * F2I);
			format.setPixel(output, i * epp,
			                clamp02(rgb[0]),
			                clamp02(rgb[1]),
			                clamp02(rgb[2]),
			                clamp02(alpha));
		});
	}
	
	private final void applyActionRGBAMatrix(T input, T output, ActionRGB<T> action) {
		applyActionRGB(input, output, action, VAR_STORE_MATRIX_RGBA);
	}
	
	private final void applyActionHSLAMatrix(T input, T output, ActionFloat<T> action) {
		applyActionFloat(pixels, buffer, action,
			(rgb, hsl) -> Colors.rgb2hsl(rgb[0], rgb[1], rgb[2], hsl),
			(hsl, rgb) -> Colors.hsl2rgb(hsl[0], hsl[1], hsl[2], rgb),
			VAR_STORE_MATRIX_HSLA);
	}
	
	private final void applyActionHCLAMatrix(T input, T output, ActionFloat<T> action) {
		applyActionFloat(pixels, buffer, action,
			(rgb, hcl) -> Colors.rgb2hcl(rgb[0], rgb[1], rgb[2], hcl),
			(hcl, rgb) -> Colors.hcl2rgb(hcl[0], hcl[1], hcl[2], rgb),
			VAR_STORE_MATRIX_HCLA);
	}
	
	private final void applyActionINT(ActionINT<T> action) {
		applyActionINT(pixels, buffer, action);
		swapBuffer();
	}
	
	private final void applyActionRGB(ActionRGB<T> action) {
		applyActionRGB(pixels, buffer, action);
		swapBuffer();
	}
	
	private final void applyActionHSL(ActionFloat<T> action) {
		applyActionFloat(pixels, buffer, action,
 			(rgb, hsl) -> Colors.rgb2hsl(rgb[0], rgb[1], rgb[2], hsl),
 			(hsl, rgb) -> Colors.hsl2rgb(hsl[0], hsl[1], hsl[2], rgb));
 		swapBuffer();
 	}
	
	private final void applyActionHCL(ActionFloat<T> action) {
		applyActionFloat(pixels, buffer, action,
			(rgb, hcl) -> Colors.rgb2hcl(rgb[0], rgb[1], rgb[2], hcl),
			(hcl, rgb) -> Colors.hcl2rgb(hcl[0], hcl[1], hcl[2], rgb));
		swapBuffer();
	}
	
	private final void applyActionRGBAMatrix(ActionRGB<T> action) {
		applyActionRGBAMatrix(pixels, buffer, action);
		swapBuffer();
	}
	
	private final void applyActionHSLAMatrix(ActionFloat<T> action) {
		applyActionHSLAMatrix(pixels, buffer, action);
		swapBuffer();
	}
	
	private final void applyActionHCLAMatrix(ActionFloat<T> action) {
		applyActionHCLAMatrix(pixels, buffer, action);
		swapBuffer();
	}
	
	private final void swapBuffer() {
		T parray = pixels;
		pixels = buffer;
		buffer = parray;
		// Swap the buffers also in the buffer strategy
		bufferStrategy.swap(ptrBuffer, ptrPixels);
		ptrBuffer = 1 - ptrBuffer;
		ptrPixels = 1 - ptrPixels;
	}
	
	/**
	 * Writes all the changes to the underlying JavaFX image.*/
	public final void apply() {
		try {
			int numOfBuffers = bufferStrategy.numberOfBuffers();
			// Special case for 1 buffer
			if((numOfBuffers == 1)) {
				// Must copy only if the buffers are swapped incorrectly
				if((buffer == original)) {
					BufferUtils.buffercopy(pixels, original);
					swapBuffer();
				}
			} else {
				// Shift contents of the buffers up the line
				T src, dst;
				for(int i = numOfBuffers; i > 1; --i) {
					src = bufferStrategy.getBuffer(i - 1);
					dst = bufferStrategy.getBuffer(i);
					BufferUtils.buffercopy(src, dst);
				}
			}
			// Update the internal JavaFX image, so that changes are apparent
			wrapper.update();
			FXInternalUtils.updateImageSafe(image);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to apply changes to an IImage: " + this, ex);
		}
	}
	
	/**
	 * Applies the given RGBA matrix to {@code this} image.
	 * @param matrix The matrix*/
	public final void applyRGBAMatrix(Matrix4f matrix) {
		applyActionRGBAMatrix((rgb, input, output, i, varStore) -> {
			float[] mtx = (float[]) varStore.get(1);
			matrix.multiply(rgb[0], rgb[1], rgb[2], rgb[3], mtx);
			rgb[0] = Colors.f2rgba(mtx[0]);
			rgb[1] = Colors.f2rgba(mtx[1]);
			rgb[2] = Colors.f2rgba(mtx[2]);
			rgb[3] = Colors.f2rgba(mtx[3]);
		});
	}
	
	/**
	 * Applies the given HSLA matrix to {@code this} image.
	 * @param matrix The matrix*/
	public final void applyHSLAMatrix(Matrix4f matrix) {
		applyActionHSLAMatrix((hsl, input, output, i, varStore) -> {
			float[] mtx = (float[]) varStore.get(2);
			matrix.multiply(hsl[0], hsl[1], hsl[2], hsl[3], mtx);
			hsl[0] = Colors.f2hsla(mtx[0]);
			hsl[1] = Colors.f2hsla(mtx[1]);
			hsl[2] = Colors.f2hsla(mtx[2]);
			hsl[3] = Colors.f2hsla(mtx[3]);
		});
	}
	
	/**
	 * Applies the given HCLA matrix to {@code this} image.
	 * @param matrix The matrix*/
	public final void applyHCLAMatrix(Matrix4f matrix) {
		applyActionHCLAMatrix((hcl, input, output, i, varStore) -> {
			float[] mtx = (float[]) varStore.get(2);
			matrix.multiply(hcl[0], hcl[1], hcl[2], hcl[3], mtx);
			hcl[0] = mtx[0];
			hcl[1] = mtx[1];
			hcl[2] = mtx[2];
			hcl[3] = mtx[3];
		});
	}
	
	/**
	 * Applies the given mask to {@code this} image.
	 * @param mask The mask*/
	public final void applyMask(int mask) {
		applyActionINT((input, output, i, varStore) -> {
			format.setARGB(output, i, format.getARGB(input, i) & mask);
		});
	}
	
	/**
	 * Gets the histogram of {@code this} image.
	 * @return The histogram as an array of 256 ints.*/
	public final int[] histogram() {
		int[] histogram = new int[256];
		histogram(histogram);
		return histogram;
	}
	
	/**
	 * Computes the histogram of {@code this} image using {@linkplain Colors#grayscale}
	 * method and stores the result in the given integer array.
	 * @param histogram The array where to save the computed histogram*/
	public final void histogram(int[] histogram) {
		histogram(histogram, Colors::grayscale);
	}
	
	/**
	 * Computes the histogram of {@code this} image using the given function
	 * and stores the result in the given integer array. The function is applied
	 * to every pixel.
	 * @param histogram The array where to save the computed histogram
	 * @param function The function to be applied to every pixel*/
	public final void histogram(int[] histogram, Function<Integer, Integer> function) {
		int epp = format.getElementsPerPixel();
		for(int i = 0, l = pixels.capacity(), level; i < l; i += epp) {
			level = function.apply(format.getARGB(pixels, i));
			++histogram[level];
		}
	}
	
	/**
	 * Keeps only the given color channels in {@code this} image.
	 * @param channels The color channels*/
	public final void keepChannels(ColorChannel... channels) {
		applyMask(this.channels.combineMasks(InternalColorChannel.fromMany(format, channels)));
	}
	
	/**
	 * Removes the given color channels from {@code this} image.
	 * @param channels The color channels*/
	public final void removeChannels(ColorChannel... channels) {
		applyMask(~this.channels.combineMasks(InternalColorChannel.fromMany(format, channels)));
	}
	
	/**
	 * Calculates the optimal threshold using Otsu's method.
	 * @param histogram The histogram
	 * @param total The total number of pixels
	 * @return The optimal threshold*/
	public final float optimalThreshold(int[] histogram, int total) {
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
	
	/**
	 * Converts all the pixels of {@code this} image to premultiplied version.*/
	public final void toPremultipliedAlpha() {
		applyActionINT((input, output, index, varStore) -> {
			format.setARGB(output, index, Colors.linear2premult(format.getARGB(input, index)));
		});
	}
	
	/**
	 * Converts all the pixels of {@code this} image to linear version.*/
	public final void toLinearAlpha() {
		applyActionINT((input, output, index, varStore) -> {
			format.setARGB(output, index, Colors.premult2linear(format.getARGB(input, index)));
		});
	}
	
	/**
	 * Disposes of all resources held by {@code this} image. Note that after
	 * calling this method the image is unusable and must be recreated.
	 * No checking in any method is present, therefore an external checking
	 * must be done in order to prevent exceptions.*/
	public final void dispose() {
		// Reset all the properties
		image    = null;
		wrapper  = null;
		width    = 0;
		height   = 0;
		format   = null;
		original = null;
		pixels   = null;
		buffer   = null;
		channels = null;
		// Buffering
		bufferStrategy = null;
		ptrBuffer = 0;
		ptrPixels = 1;
	}
	
	/**
	 * Sets a pixel at the given position to the given color.
	 * @param x The x-coordinate of the position
	 * @param y The y-coordinate of the position
	 * @param color The color*/
	public final void setPixel(int x, int y, Color color) {
		setPixel(x, y, Colors.color2int(color));
	}
	
	/**
	 * Sets a pixel at the given position to a color, given by
	 * the given red, green and blue component.
	 * @param x The x-coordinate of the position
	 * @param y The y-coordinate of the position
	 * @param r The red component of the color
	 * @param g The green component of the color
	 * @param b The blue component of the color*/
	public final void setPixel(int x, int y, int r, int g, int b) {
		setPixel(x, y, r, g, b, 0xff);
	}
	
	/**
	 * Sets a pixel at the given position to a color, given by
	 * the given red, green, blue and alpha component.
	 * @param x The x-coordinate of the position
	 * @param y The y-coordinate of the position
	 * @param r The red component of the color
	 * @param g The green component of the color
	 * @param b The blue component of the color
	 * @param a The alpha component of the color*/
	public final void setPixel(int x, int y, int r, int g, int b, int a) {
		setPixel(x, y, Colors.rgba(r, g, b, a));
	}
	
	/**
	 * Sets a pixel at the given position to a color, given by
	 * the given hue, saturation and lightness.
	 * @param x The x-coordinate of the position
	 * @param y The y-coordinate of the position
	 * @param h The hue of the color
	 * @param s The saturation of the color
	 * @param l The lightness of the color*/
	public final void setPixel(int x, int y, float h, float s, float l) {
		setPixel(x, y, h, s, l, 1.0f);
	}
	
	/**
	 * Sets a pixel at the given position to a color, given by
	 * the given hue, saturation, lightness and alpha.
	 * @param x The x-coordinate of the position
	 * @param y The y-coordinate of the position
	 * @param h The hue of the color
	 * @param s The saturation of the color
	 * @param l The lightness of the color
	 * @param a The alpha of the color*/
	public final void setPixel(int x, int y, float h, float s, float l, float a) {
		setPixel(x, y, Colors.hsla(h, s, l, a));
	}
	
	/**
	 * Sets a pixel at the given position to the given color.
	 * @param x The x-coordinate of the position
	 * @param y The y-coordinate of the position
	 * @param argb The color, as an ARGB int*/
	public final void setPixel(int x, int y, int argb) {
		setPixel(y * width + x, argb);
	}
	
	/**
	 * Sets a pixel at the given index to the given color.
	 * Note that this method does <em>NOT</em> check for index bounds.
	 * @param index The index
	 * @param argb The color, as an ARGB int*/
	public final void setPixel(int index, int argb) {
		format.setARGB(pixels, index * format.getElementsPerPixel(), argb);
	}
	
	/**
	 * Sets pixels of {@code this} image to the given pixels.
	 * @param pixels The pixels*/
	public final void setPixels(T pixels) {
		if((pixels.capacity() != this.pixels.capacity()))
			throw new IllegalArgumentException("Invalid array size");
		System.arraycopy(pixels.array(), 0, this.pixels.array(), 0, pixels.capacity());
	}
	
	/**
	 * Gets a pixel color at the given position.
	 * @param x The x-coordinate of the position
	 * @param y The y-coordinate of the position
	 * @return The pixel color, as an ARGB int*/
	public final int getPixel(int x, int y) {
		return getPixel(y * width + x);
	}
	
	/**
	 * Gets a pixel color at the given index.
	 * Note that this method does <em>NOT</em> check for index bounds.
	 * @param index The index
	 * @return The pixel color, as an ARGB int*/
	public final int getPixel(int index) {
		return format.getARGB(pixels, index * format.getElementsPerPixel());
	}
	
	/**
	 * Gets a pixel color at the given position.
	 * @param x The x-coordinate of the position
	 * @param y The y-coordinate of the position
	 * @return The pixel color*/
	public final Color getPixelColor(int x, int y) {
		return Colors.int2color(getPixel(x, y));
	}
	
	/**
	 * Gets a pixel color at the given index.
	 * Note that this method does <em>NOT</em> check for index bounds.
	 * @param index The index
	 * @return The pixel color*/
	public final Color getPixelColor(int index) {
		return Colors.int2color(getPixel(index));
	}
	
	/**
	 * Gets the underlying JavaFX image of {@code this} image.
	 * @return The underlying JavaFX image*/
	public final WritableImage getImage() {
		return image;
	}
	
	/**
	 * Gets pixels of {@code this} image.
	 * @return Buffer containing the pixels*/
	public final T getPixels() {
		return pixels;
	}
	
	/**
	 * Gets the width of {@code this} image.
	 * @return The width*/
	public final int getWidth() {
		return width;
	}
	
	/**
	 * Gets the height of {@code this} image.
	 * @return The height*/
	public final int getHeight() {
		return height;
	}
	
	/**
	 * Gets the pixel format of {@code this} image.
	 * @return The pixel format*/
	public final ImagePixelFormat<T> getPixelFormat() {
		return format;
	}
}