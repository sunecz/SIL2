package sune.lib.sil2;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import sune.lib.sil2.FXInternalUtils.PlatformImageWrapper;
import sune.lib.sil2.format.ImagePixelFormat;
import sune.lib.sil2.format.ImagePixelFormats;

/**
 * Represents an image to which can be applied various operations.
 * The "I" in Image stands for Improved.*/
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
	
	/**
	 * Creates a new instance from the given image.
	 * @param image The image*/
	public IImage(Image image) {
		this.image 	  = ImageUtils.toWritable(image);
		this.width 	  = (int) this.image.getWidth();
		this.height   = (int) this.image.getHeight();
		this.wrapper  = FXInternalUtils.getPlatformImageWrapper(this.image);
		this.format   = getImagePixelFormat(image);
		this.original = getTypedWrapperBuffer(wrapper);
		this.pixels   = newPixelsBuffer();
		this.buffer   = newPixelsBuffer();
		this.channels = new InternalChannels<>(format);
		BufferUtils.buffercopy(original, pixels);
		BufferUtils.buffercopy(original, buffer);
	}
	
	/**
	 * Creates a new instance with the given width and height from
	 * the given pixels array.
	 * @param width The width
	 * @param height The height
	 * @param imagePixels The image pixels*/
	public IImage(int width, int height, T imagePixels) {
		this.image 	  = ImageUtils.create(width, height, imagePixels);
		this.width 	  = width;
		this.height   = height;
		this.wrapper  = FXInternalUtils.getPlatformImageWrapper(this.image);
		this.format   = getImagePixelFormat(image);
		this.original = getTypedWrapperBuffer(wrapper);
		this.pixels   = newPixelsBuffer();
		this.buffer   = newPixelsBuffer();
		this.channels = new InternalChannels<>(format);
		BufferUtils.buffercopy(original, pixels);
		BufferUtils.buffercopy(original, buffer);
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
			applyActionRGB((rgb, input, output, i) -> {
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
			applyActionRGB((rgb, input, output, i) -> {
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
			applyActionRGB((rgb, input, output, i) -> {
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
			applyActionRGB((rgb, input, output, i) -> {
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
			applyActionRGB((rgb, input, output, i) -> {
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
			applyActionINT((input, output, i) -> {
				format.setPixel(output, i, (format.getARGB(input, i) & 0x00ffffff) | fval);
			});
		}
		
		/**
		 * Alters the transparency of {@code this} image.
		 * @param value The value*/
		public final void transparency(float value) {
			final float fval = clamp01(value);
			applyActionINT((input, output, i) -> {
				int alpha = (int) ((format.getARGB(input, i) >>> 24) * fval) << 24;
				format.setPixel(output, i, (format.getARGB(input, i) & 0x00ffffff) | alpha);
			});
		}
		
		/**
		 * Alters the hue of {@code this} image.
		 * @param value The value*/
		public final void hue(float value) {
			final float fval = clamp11(value);
			applyActionHSL((hsl, input, output, index) -> {
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
			applyActionHSL((hsl, input, output, index) -> {
				hsl[1] *= fval;
			});
		}
		
		/**
		 * Alters the lightness of {@code this} image.
		 * @param value The value*/
		public final void lightness(float value) {
			final float fval = clamp00(value);
			applyActionHSL((hsl, input, output, index) -> {
				hsl[2] *= fval;
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
			applyActionINT((input, output, i) -> {
				format.setPixel(output, i, (format.getARGB(input, i) & 0xff) <= fval ? 0xffffffff : 0xff000000);
			});
		}
		
		/**
		 * Thresholds {@code this} image, meaning that all pixels that have
		 * the lowest 8-bits greater than or equaled the given value, are set
		 * to white color, otherwise to black color.
		 * @param value The value*/
		public final void thresholdGRT(int value) {
			final int fval = clamp02(value);
			applyActionINT((input, output, i) -> {
				format.setPixel(output, i, (format.getARGB(input, i) & 0xff) >= fval ? 0xffffffff : 0xff000000);
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
			applyActionINT((input, output, i) -> {
				int value = format.getARGB(input, i) & 0xff;
				format.setPixel(output, i, value >= fmin && value <= fmax ? 0xffffffff : 0xff000000);
			});
		}
		
		/**
		 * Thresholds {@code this} image using the {@linkplain #thresholdGRT(int)}
		 * method with a value of {@linkplain IImage#optimalThreshold(IntBuffer, int)}
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
			if((premultiply)) {
				applyActionINT(pixels, buffer, (input, output, index) -> {
					format.setPixel(output, index, Colors.Conversion.linear2premult(format.getARGB(input, index)));
				});
				FastBlur.boxBlur(buffer, pixels, 0, 0, width, height, value, width, channels);
				applyActionINT(pixels, buffer, (input, output, index) -> {
					format.setPixel(output, index, Colors.Conversion.premult2linear(format.getARGB(input, index)));
				});
				swapBuffer();
			} else {
				FastBlur.boxBlur(pixels, buffer, 0, 0, width, height, value, width, channels);
				swapBuffer();
			}
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
			if((premultiply)) {
				applyActionINT(pixels, buffer, (input, output, index) -> {
					format.setPixel(output, index, Colors.Conversion.linear2premult(format.getARGB(input, index)));
				});
				FastBlur.gaussianBlur(buffer, pixels, 0, 0, width, height, value, width, channels);
				applyActionINT(pixels, buffer, (input, output, index) -> {
					format.setPixel(output, index, Colors.Conversion.premult2linear(format.getARGB(input, index)));
				});
				swapBuffer();
			} else {
				FastBlur.gaussianBlur(pixels, buffer, 0, 0, width, height, value, width, channels);
				swapBuffer();
			}
		}
		
		/**
		 * Applies motion blur of the given angle to {@code this} image.
		 * @param angleDeg The angle, in degrees
		 * @param value The value*/
		public final void motionBlur(float angleDeg, float value) {
			int cos = FastMath.round(value * FastMath.cosDeg(angleDeg));
			int sin = FastMath.round(value * FastMath.sinDeg(angleDeg));
			int epp = format.getElementsPerPixel();
			applyActionINT((input, output, index) -> {
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
						// If the loop is ended when it is obvious it continues
						// in a direction outside of the image, it is possible
						// to end the loop early and save some cycles.
						if((px >= 0 && py >= 0 && px < width && py < height)) {
							iclr = format.getARGB(pixels, (py * width + px) * epp);
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
						// If the loop is ended when it is obvious it continues
						// in a direction outside of the image, it is possible
						// to end the loop early and save some cycles.
						if((px >= 0 && py >= 0 && px < width && py < height)) {
							iclr = format.getARGB(pixels, (py * width + px) * epp);
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
				format.setPixel(output, index, r, g, b, a);
			});
		}
		
		/**
		 * Applies sharpening to {@code this} image.
		 * @param value The value*/
		public final void sharpen(float value) {
			int epp = format.getElementsPerPixel();
			applyActionRGB((rgb, input, output, index) -> {
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
			final float maxMag = 1442.5f;
			int epp = format.getElementsPerPixel();
			// Precompute the grayscale values of the pixels
			T gray = newPixelsBuffer();
			for(int i = 0, l = pixels.capacity(); i < l; i += epp)
				format.set(gray, i, Colors.grayscale(format.getARGB(pixels, i)));
			// Do the actual Sobel filtering
			jobs.area(1, 1, width - 1, height - 1, gray, buffer, (rx, ry, rw, rh, input, stride, output) -> {
				float mag, dir;
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
					mag = FastMath.sqrt(gx * gx + gy * gy);
					dir = FastMath.atan2(gy, gx);
					format.setPixel(output, i * epp, Colors.sobel(dir, mag / maxMag));
					++i;
					if((--x == 0)) {
						x  = rw;
						i += ii;
						if((--y == 0))
							break;
					}
				}
			});
			swapBuffer();
			gray = null;
		}
	}
	
	/**
	 * Contains methods for applying image effects.*/
	public final class Effects {
		
		/**
		 * Applies shadow effect to {@code this} image. The shadow is created in
		 * the given angle in distance of the given position and its color is that
		 * of the given color.
		 * @param angle The angle
		 * @param distX The x-coordinate of the distance
		 * @param distY The y-coordinate of the distance
		 * @param color The color*/
		public final void shadow(float angle, float distX, float distY, int color) {
			T output = newPixelsBuffer();
			int epp = format.getElementsPerPixel();
			// Produce the image's shadow of transparency and store it in output
			float dx = FastMath.cos(angle) * distX;
			float dy = FastMath.sin(angle) * distY;
			float x = dx, y = dy;
			for(int i = 0, k = width, l = pixels.capacity() / epp; i < l; ++i) {
				if((x >= 0 && x < width) && (y >= 0 && y < height)) {
					if((format.getARGB(pixels, i * epp) >>> 24) != 0x0) {
						int p = (int) y * width + (int) x;
						format.setPixel(output, p * epp, color);
					}
				}
				++x;
				if((--k == 0)) {
					 k = width;
					 x = dx;
					 ++y;
				}
			}
			// Combine the image's pixels and the shadow's pixels (with blending)
			for(int i = 0, l = pixels.capacity(), c; i < l; i += epp) {
				if((c = format.getARGB(output, i)) != 0x0) {
					format.setPixel(pixels, i, Colors.blend(format.getARGB(pixels, i), c));
				}
			}
			output = null;
		}
	}
	
	@FunctionalInterface
	private static interface Job<T extends Buffer> {
		void execute(int rx, int ry, int rw, int rh, T input, int stride, T output);
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
	}
	
	/**
	 * Provides fast blurring of image pixels. It offers two most used blurs:
	 * Box blur and Gaussian blur. Note that none of these are computed exactly;
	 * these methods are made for speed, not for accuracy.<br><br>
	 * @version 1.0
	 * @author Ivan Kutskir
	 * @author Petr Cipra
	 * @see
	 * <a href="http://blog.ivank.net/fastest-gaussian-blur.html">
	 * 	http://blog.ivank.net/fastest-gaussian-blur.html
	 * </a>*/
	private static final class FastBlur {
		
		public static final <T extends Buffer> void gaussianBlur(T input, T output, int x, int y, int w, int h, int r, int s,
				InternalChannels<T> channels) {
			final CounterLock lock   = new CounterLock(4);
			final int         length = input.capacity();
			final float[]     boxes  = generateBoxes(r, 3);
			byte[] outputR = new byte[length];
			byte[] outputG = new byte[length];
			byte[] outputB = new byte[length];
			byte[] outputA = new byte[length];
			Threads.execute(() -> {
				byte[] inputR = new byte[length];
				channels.separate(input, inputR, channels.getFormat().getShiftR());
				gaussianBlur(inputR, outputR, x, y, w, h, r, s, boxes);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputG = new byte[length];
				channels.separate(input, inputG, channels.getFormat().getShiftG());
				gaussianBlur(inputG, outputG, x, y, w, h, r, s, boxes);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputB = new byte[length];
				channels.separate(input, inputB, channels.getFormat().getShiftB());
				gaussianBlur(inputB, outputB, x, y, w, h, r, s, boxes);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputA = new byte[length];
				channels.separate(input, inputA, channels.getFormat().getShiftA());
				gaussianBlur(inputA, outputA, x, y, w, h, r, s, boxes);
				lock.decrement();
			});
			lock.await();
			channels.join(outputR, outputG, outputB, outputA, output);
		}
		
		public static final <T extends Buffer> void boxBlur(T input, T output, int x, int y, int w, int h, int r, int s,
				InternalChannels<T> channels) {
			final CounterLock lock   = new CounterLock(4);
			final int         length = input.capacity();
			byte[] outputR = new byte[length];
			byte[] outputG = new byte[length];
			byte[] outputB = new byte[length];
			byte[] outputA = new byte[length];
			Threads.execute(() -> {
				byte[] inputR = new byte[length];
				channels.separate(input, inputR, channels.getFormat().getShiftR());
				boxBlur(inputR, outputR, 0, 0, w, h, r, s);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputG = new byte[length];
				channels.separate(input, inputG, channels.getFormat().getShiftG());
				boxBlur(inputG, outputG, 0, 0, w, h, r, s);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputB = new byte[length];
				channels.separate(input, inputB, channels.getFormat().getShiftB());
				boxBlur(inputB, outputB, 0, 0, w, h, r, s);
				lock.decrement();
			});
			Threads.execute(() -> {
				byte[] inputA = new byte[length];
				channels.separate(input, inputA, channels.getFormat().getShiftA());
				boxBlur(inputA, outputA, 0, 0, w, h, r, s);
				lock.decrement();
			});
			lock.await();
			channels.join(outputR, outputG, outputB, outputA, output);
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
		void action(T input, T output, int index);
	}
	
	@FunctionalInterface
	private static interface ActionRGB<T extends Buffer> {
		void action(int[] rgb, T input, T output, int index);
	}
	
	@FunctionalInterface
	private static interface ActionHSL<T extends Buffer> {
		void action(float[] hsl, T input, T output, int index);
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
	
	private final void applyActionINT(T input, T output, ActionINT<T> action) {
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
				for(int i = si, px = sw, py = sh;; ++i) {
					action.action(input, output, i * epp);
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
	
	private final void applyActionRGB(T input, T output, ActionRGB<T> action) {
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
				int[] rgb = new int[4]; int argb;
				for(int i = si, px = sw, py = sh;; ++i) {
					argb   = format.getARGB(input, i * epp);
					rgb[0] = (argb >> 16) & 0xff;
					rgb[1] = (argb >>  8) & 0xff;
					rgb[2] = (argb)       & 0xff;
					rgb[3] = (argb >> 24) & 0xff;
					action.action(rgb, input, output, i * epp);
					format.setPixel(output, i * epp,
					                clamp02(rgb[0]),
					                clamp02(rgb[1]),
					                clamp02(rgb[2]),
					                clamp02(rgb[3]));
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
	
	private final void applyActionHSL(T input, T output, ActionHSL<T> action) {
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
				float[] hsl = new float[4];
				int  [] rgb = new int  [3];
				int argb, red, green, blue, alpha;
				for(int i = si, px = sw, py = sh;; ++i) {
					argb  = format.getARGB(input, i * epp);
					red   = (argb >> 16) & 0xff;
					green = (argb >>  8) & 0xff;
					blue  = (argb)       & 0xff;
					Colors.rgb2hsl(red, green, blue, hsl);
					hsl[3] = ((argb >> 24) & 0xff) * I2F;
					action.action(hsl, input, output, i * epp);
					Colors.hsl2rgb(hsl[0], hsl[1], hsl[2], rgb);
					alpha = (int) (hsl[3] * F2I);
					format.setPixel(output, i * epp,
					                clamp02(rgb[0]),
					                clamp02(rgb[1]),
					                clamp02(rgb[2]),
					                clamp02(alpha));
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
	
	private final void applyActionINT(ActionINT<T> action) {
		applyActionINT(pixels, buffer, action);
		swapBuffer();
	}
	
	private final void applyActionRGB(ActionRGB<T> action) {
		applyActionRGB(pixels, buffer, action);
		swapBuffer();
	}
	
	private final void applyActionHSL(ActionHSL<T> action) {
		applyActionHSL(pixels, buffer, action);
		swapBuffer();
	}
	
	private final int matrixMultiplyRGBA(Matrix4f matrix, int r, int g, int b, int a) {
		float[] result = matrix.multiply(r, g, b, a);
		return Colors.rgba2int(Colors.f2rgba(result[0]),
							   Colors.f2rgba(result[1]),
							   Colors.f2rgba(result[2]),
							   Colors.f2rgba(result[3]));
	}
	
	private final int matrixMultiplyHSLA(Matrix4f matrix, float h, float s, float l, float a) {
		float[] result = matrix.multiply(h, s, l, a);
		return Colors.hsla2int(Colors.f2hsla(result[0]),
							   Colors.f2hsla(result[1]),
							   Colors.f2hsla(result[2]),
							   Colors.f2hsla(result[3]));
	}
	
	@SuppressWarnings("unchecked")
	private final T newPixelsBuffer() {
		return (T) BufferUtils.newBufferOfType(original);
	}
	
	private final void swapBuffer() {
		T parray = pixels;
		pixels = buffer;
		buffer = parray;
	}
	
	/**
	 * Writes all the changes to the underlying JavaFX image.*/
	public final void apply() {
		try {
			BufferUtils.buffercopy(pixels, original);
			wrapper.update();
			FXInternalUtils.updateImageSafe(image);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Applies the given RGBA matrix to {@code this} image.
	 * @param matrix The matrix*/
	public final void applyRGBAMatrix(Matrix4f matrix) {
		applyActionRGB((rgb, input, output, i) -> {
			format.setPixel(output, i, matrixMultiplyRGBA(matrix, rgb[0], rgb[1], rgb[2], rgb[3]));
		});
	}
	
	/**
	 * Applies the given HSLA matrix to {@code this} image.
	 * @param matrix The matrix*/
	public final void applyHSLAMatrix(Matrix4f matrix) {
		applyActionHSL((hsl, input, output, i) -> {
			format.setPixel(output, i, matrixMultiplyHSLA(matrix, hsl[0], hsl[1], hsl[2], hsl[3]));
		});
	}
	
	/**
	 * Applies the given mask to {@code this} image.
	 * @param mask The mask*/
	public final void applyMask(int mask) {
		applyActionINT((input, output, i) -> {
			format.setPixel(output, i, format.getARGB(input, i) & mask);
		});
	}
	
	/**
	 * Gets the histogram of {@code this} image.
	 * @return The histogram as an array of 256 ints.*/
	public final int[] histogram() {
		int epp = format.getElementsPerPixel();
		int[] histogram = new int[256];
		for(int i = 0, l = pixels.capacity(), level; i < l; i += epp) {
			level = format.getARGB(pixels, i) & 0xff;
			++histogram[level];
		}
		return histogram;
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
		applyActionINT((input, output, index) -> {
			format.setPixel(output, index, Colors.Conversion.linear2premult(format.getARGB(input, index)));
		});
	}
	
	/**
	 * Converts all the pixels of {@code this} image to linear version.*/
	public final void toLinearAlpha() {
		applyActionINT((input, output, index) -> {
			format.setPixel(output, index, Colors.Conversion.premult2linear(format.getARGB(input, index)));
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
		setPixel(x, y, Colors.rgba2int(r, g, b, a));
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
		setPixel(x, y, Colors.hsla2int(h, s, l, a));
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
		format.setPixel(pixels, index * format.getElementsPerPixel(), argb);
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
	 * @return The pixels, as an array of ARGB ints*/
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
	
	public ImagePixelFormat<T> getPixelFormat() {
		return format;
	}
}