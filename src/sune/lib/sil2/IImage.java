package sune.lib.sil2;

import static sune.lib.sil2.StructuresConfiguration.BACKGROUND;
import static sune.lib.sil2.StructuresConfiguration.BLANK;
import static sune.lib.sil2.StructuresConfiguration.FOREGROUND;

import java.nio.Buffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
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
public final class IImage<T extends Buffer> implements IImageContext<T> {
	
	// TODO: Clean Up
	
	private static final float F2I = 255.0f;
	private static final float I2F = 1.0f / 255.0f;
	
	private WritableImage image;
	private PlatformImageWrapper wrapper;
	private int offX;
	private int offY;
	private int subWidth;
	private int subHeight;
	private int width;
	private int height;
	private ImagePixelFormat<T> format;
	
	private T original;
	private T pixels;
	private T buffer;
	
	private InternalChannels<T> channels;
	private final Convolution convolution = new Convolution();
	private final Structures structures = new Structures();
	private final Operations operations = new Operations();
	
	private BufferStrategy<T> bufferStrategy;
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
			T _buffer = BufferUtils.newBufferOfType(original);
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
		this.subWidth  = width;
		this.subHeight = height;
	}
	
	private IImage(IImage<T> iimg, int x, int y, int width, int height) {
		this.image 	  = iimg.image;
		this.width 	  = iimg.width;
		this.height   = iimg.height;
		this.wrapper  = iimg.wrapper;
		this.format   = iimg.format;
		this.original = iimg.original;
		this.channels = iimg.channels;
		this.bufferStrategy = iimg.bufferStrategy;
		this.buffer = iimg.buffer;
		this.pixels = iimg.pixels;
		this.ptrPixels = iimg.ptrPixels;
		this.ptrBuffer = iimg.ptrBuffer;
		this.offX = x;
		this.offY = y;
		this.subWidth = width;
		this.subHeight = height;
	}
	
	public final IImage<T> subImage(int x, int y, int width, int height) {
		if((x < offX || y < offY || width > subWidth - (x - offX) || height > subHeight - (y - offY)))
			throw new IllegalArgumentException();
		return new IImage<>(this, x, y, width, height);
	}
	
	private static final WritableImage ensureWritableSupported(Image image) {
		// If the image's format is not supported, convert it to a native image
		if(!ImagePixelFormats.isSupported(image.getPixelReader().getPixelFormat()))
			image = NativeImage.ensurePixelFormat(image);
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
	
	private static final int clamp02(int val) {
		if((val <= 0x00)) return 0x00;
		if((val >= 0xff)) return 0xff;
		return val;
	}
	
	private static final int clamp(int val, int min, int max) {
		return val <= min ? min : val >= max ? max : val;
	}
	
	/**
	 * Contains methods for image convolution.*/
	private final class Convolution {
		
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
		
		private final void convolute2d(float[] kernel, int[] indexes,
				T input, T output, int iterations, boolean alphaChannel) {
			float[] fkernel = checkKernel(kernel);
			final CounterLock lock = new CounterLock();
			int ex = offX + subWidth;
			int ey = offY + subHeight;
			int x = offX;
			int y = offY;
			int w = Math.max(subWidth  / 4, 256);
			int h = Math.max(subHeight / 4, 256);
			for(int i = 0; i < iterations; ++i) {
				for(int kx = x, ky = y;;) {
					int sx = kx;
					int sy = ky;
					int sw = kx + w >= ex ? ex - kx : w;
					int sh = ky + h >= ey ? ey - ky : h;
					lock.increment();
					Threads.execute(() -> {
						convolute2d(sx, sy, sw, sh, width, fkernel,
							indexes, input, output, alphaChannel);
						lock.decrement();
					});
					if((kx += w) >= ex) {
						kx  = offX;
						if((ky += h) >= ey)
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
			int sx = offX, ex = sx + subWidth;
			int sy = offY, ey = sy + subHeight;
			int klen = (int) FastMath.sqrt(kernel.length) / 2;
			int maxx = clamp(x + width,  klen, (ex + offX) - klen);
			int maxy = clamp(y + height, klen, (ey + offY) - klen);
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
			if((y <= klen))
				convolute2d_edges(x, y, width, klen, stride - width + x,
								  stride, kernel, indexes, input, output, alphaChannel);
			// Bottom edge
			if((y + height >= ey - klen - 1))
				convolute2d_edges(x, y + height - klen, width, klen, stride - width,
								  stride, kernel, indexes, input, output, alphaChannel);
			// Left edge
			if((x <= klen))
				convolute2d_edges(x, Math.max(y, klen), klen, height - (y + height >= ey ? klen : y), stride - klen,
								  stride, kernel, indexes, input, output, alphaChannel);
			// Right edge
			if((x + width >= ex - klen - 1))
				convolute2d_edges(x + width - klen, Math.max(y, klen), klen, height - (y + height >= ey ? klen : y), stride - klen,
								  stride, kernel, indexes, input, output, alphaChannel);
		}
		
		// Convolute the edges using the Extend method (edge pixels are "copied" over)
		private final void convolute2d_edges(int x, int y, int w, int h, int d, int stride, float[] kernel,
				int[] indexes, T input, T output, boolean alphaChannel) {
			if((w <= 0 || h <= 0)) return; // Nothing to do
			float pxa, pxr, pxg, pxb, mul;
			int klen = (int) FastMath.sqrt(kernel.length) + 1;
			int sx = offX, ex = sx + subWidth;
			int sy = offY, ey = sy + subHeight;
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
					if((kcy < sy))  ind -= ky * stride; else
					if((kcy >= ey)) ind -= ky * stride;
					if((kcx < sx))  ind -= kx; else
					if((kcx >= ex)) ind -= kx;
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
	
	private final class Structures {
		
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
			int ex = offX + subWidth;
			int ey = offY + subHeight;
			int x = offX;
			int y = offY;
			int w = Math.max(subWidth  / 4, 256);
			int h = Math.max(subHeight / 4, 256);
			for(int kx = x, ky = y;;) {
				int sx = kx;
				int sy = ky;
				int sw = kx + w >= ex ? ex - kx : w;
				int sh = ky + h >= ey ? ey - ky : h;
				lock.increment();
				Threads.execute(() -> {
					convolute2d(sx, sy, sw, sh, width, fStructure, indexes, input, output, config);
					lock.decrement();
				});
				if((kx += w) >= ex) {
					kx  = offX;
					if((ky += h) >= ey)
						break;
				}
			}
			lock.await();
		}
		
		private final void convolute2d(int x, int y, int width, int height, int stride,
				int[] structure, int[] indexes, T input, T output, StructuresConfiguration config) {
			int sx = offX, ex = sx + subWidth;
			int sy = offY, ey = sy + subHeight;
			int slen = (int) FastMath.sqrt(structure.length) / 2;
			int maxx = clamp(x + width,  slen, (ex + offX) - slen);
			int maxy = clamp(y + height, slen, (ey + offY) - slen);
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
						if((structure[k] < BLANK
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
			if((y <= slen))
				convolute2d_edges(x, y, width, slen, stride - width + x,
								  stride, structure, indexes, input, output, config);
			// Bottom edge
			if((y + height >= ey - slen - 1))
				convolute2d_edges(x, y + height - slen, width, slen, stride - width,
								  stride, structure, indexes, input, output, config);
			// Left edge
			if((x <= slen))
				convolute2d_edges(x, Math.max(y, slen), slen, height - (y + height >= ey ? slen : y), stride - slen,
								  stride, structure, indexes, input, output, config);
			// Right edge
			if((x + width >= ex - slen - 1))
				convolute2d_edges(x + width - slen, Math.max(y, slen), slen, height - (y + height >= ey ? slen : y), stride - slen,
								  stride, structure, indexes, input, output, config);
		}
		
		private final void convolute2d_edges(int x, int y, int w, int h, int d, int stride, int[] structure,
				int[] indexes, T input, T output, StructuresConfiguration config) {
			if((w <= 0 || h <= 0)) return; // Nothing to do
			int slen = (int) FastMath.sqrt(structure.length) + 1;
			int sx = offX, ex = sx + subWidth;
			int sy = offY, ey = sy + subHeight;
			int epp  = format.getElementsPerPixel();
			for(int i = y * stride + x, c = w, r = h, m = indexes.length, ind, kcx, kcy, kh = slen / 2, pxv;; ++i) {
				if((config.conditionCan.apply(format.getARGB(input, i * epp) & 0xff))) {
					pxv = config.valueTrue;
					for(int k = 0, kx = -kh, ky = -kh, ke = kh+1; k < m; ++k) {
						ind = i + indexes[k];
						kcx = x + (w - c) + kx;
						kcy = y + (h - r) + ky;
						if((kcy < sy))  ind -= ky * stride; else
						if((kcy >= ey)) ind -= ky * stride;
						if((kcx < sx))  ind -= kx; else
						if((kcx >= ex)) ind -= kx;
						if((structure[k] < BLANK
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
	
	private final class Operations {
		
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
			int sx = offX, ex = offX + subWidth;
			int sy = offY, ey = offY + subHeight;
			int[] array = new int[subWidth * subHeight];
			for(int x = sx, y = sy, i = y * width + x, k = 0;; ++i, ++k) {
				int sum = function.apply(format.getARGB(pixels, i) & 0xff);
				if((x > sy))           sum += array[k - 1];
				if((y > sy))           sum += array[k - subWidth];
				if((y > sy && x > sx)) sum -= array[k - subWidth - 1];
				array[k] = sum;
				if((++x == ex)) {
					x = sx;
					if((++y == ey))
						break;
				}
			}
			return array;
		}
	}
	
	/**
	 * Convolutes {@code this} image with the given kernel {@code iterations} times.
	 * @param kernel The kernel
	 * @param iterations The number of iterations
	 * @param alphaChannel If {@code true}, also convolute the alpha channel*/
	@Override
	public final void convolute2d(float[] kernel, int iterations, boolean alphaChannel) {
		convolution.convolute2d(kernel, iterations, alphaChannel);
	}
	
	@Override
	public final void structureConvolute2d(int[] structure, StructuresConfiguration config) {
		structures.convolute2d(structure, config);
	}
	
	// Saves the current state of the image's pixels to the operations stack
	@Override
	public final void opSave() {
		opSave(pixels);
	}
	
	public final void opSave(T buffer) {
		operations.stack.push(BufferUtils.copy(buffer));
	}
	
	// Removes the first element on the operations stack and returns it
	@Override
	public final T opRemove() {
		return operations.stack.pop();
	}
	
	@Override
	public final void opOr() {
		operations.or();
	}
	
	@Override
	public final void opAnd() {
		operations.and();
	}
	
	@Override
	public final void opNot() {
		operations.not();
	}
	
	@Override
	public final int[] opIntegral(Function<Integer, Integer> function) {
		return operations.integral(function);
	}
	
	// ----- Experimental
	
	@Override
	public final <R> R applyOperation(IImageOperation<T, R> operation) {
		if((operation == null))
			throw new IllegalArgumentException("Operation cannot be null");
		return operation.execute(this);
	}
	
	// -----
	
	@FunctionalInterface
	public static interface Job2D<T extends Buffer> {
		void execute(int rx, int ry, int rw, int rh, T input, int stride, T output);
	}
	
	@FunctionalInterface
	public static interface Job1D<T extends Buffer> {
		void execute(int i, int x, int y, T input, int stride, T output);
	}
	
	@Override
	public final void applyAreaJob(int x, int y, int width, int height, T input, T output, Job2D<T> job) {
		final CounterLock lock = new CounterLock();
		int ex = Math.min(x + width, offX + subWidth);
		int ey = Math.min(y + height, offY + subHeight);
		int w = Math.max(width  / 4, 256);
		int h = Math.max(height / 4, 256);
		for(int kx = x, ky = y;;) {
			int sx = kx;
			int sy = ky;
			int sw = kx + w >= ex ? ex - kx : w;
			int sh = ky + h >= ey ? ey - ky : h;
			lock.increment();
			Threads.execute(() -> {
				job.execute(sx, sy, sw, sh, input, IImage.this.width, output);
				lock.decrement();
			});
			if((kx += w) >= ex) {
				kx  = x;
				if((ky += h) >= ey)
					break;
			}
		}
		lock.await();
	}
	
	@Override
	public final void applyLineHJob(int x, int y, int width, T input, T output, Job1D<T> job) {
		for(int stride = IImage.this.width, sx = x, i = y * stride + x, kx = width; kx-- != 0; ++sx, ++i) {
			job.execute(i, sx, y, input, stride, output);
		}
	}
	
	@Override
	public final void applyLineVJob(int x, int y, int height, T input, T output, Job1D<T> job) {
		for(int stride = IImage.this.width, sy = y, i = y * stride + x, ky = height; ky-- != 0; ++sy, i += stride) {
			job.execute(i, x, sy, input, stride, output);
		}
	}
	
	@FunctionalInterface
	public static interface ActionINT<T extends Buffer> {
		void action(T input, T output, int index, VariableStore varStore);
	}
	
	@FunctionalInterface
	public static interface ActionRGB<T extends Buffer> {
		void action(int[] rgb, T input, T output, int index, VariableStore varStore);
	}
	
	@FunctionalInterface
	public static interface ActionFloat<T extends Buffer> {
		void action(float[] arr, T input, T output, int index, VariableStore varStore);
	}
	
	@FunctionalInterface
	private static interface ConversionAction<A, B> {
		void convert(A a, B b);
	}
	
	@FunctionalInterface
	private static interface ThreadedAction<T> {
		void action(int i, int epp, VariableStore varStore);
	}
	
	public static interface VariableStore {
		VariableStore copy();
		void prepare();
		Object get(int addr);
	}
	
	private static final EmptyVariableStore VAR_STORE_EMPTY = EmptyVariableStore.INSTANCE;
	private static final RGBVariableStore   VAR_STORE_RGB   = RGBVariableStore.INSTANCE;
	private static final FloatVariableStore VAR_STORE_FLOAT = FloatVariableStore.INSTANCE;
	
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
	
	private final void applyThreadedAction(T input, T output, VariableStore varStore, ThreadedAction<T> action) {
		final CounterLock lock = new CounterLock();
		int epp = format.getElementsPerPixel();
		int ex = offX + subWidth;
		int ey = offY + subHeight;
		int x = offX;
		int y = offY;
		int w = Math.max(subWidth  / 4, 256);
		int h = Math.max(subHeight / 4, 256);
		for(int kx = x, ky = y;;) {
			int sx = kx;
			int sy = ky;
			int sw = kx + w >= ex ? ex - kx : w;
			int sh = ky + h >= ey ? ey - ky : h;
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
			if((kx += w) >= ex) {
				kx  = offX;
				if((ky += h) >= ey)
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
	
	@Override
	public final void applyActionINT(ActionINT<T> action) {
		applyActionINT(pixels, buffer, action);
		swapBuffer();
	}
	
	@Override
	public final void applyActionRGB(ActionRGB<T> action) {
		applyActionRGB(pixels, buffer, action);
		swapBuffer();
	}
	
	@Override
	public final void applyActionHSL(ActionFloat<T> action) {
		applyActionFloat(pixels, buffer, action,
 			(rgb, hsl) -> Colors.rgb2hsl(rgb[0], rgb[1], rgb[2], hsl),
 			(hsl, rgb) -> Colors.hsl2rgb(hsl[0], hsl[1], hsl[2], rgb));
 		swapBuffer();
 	}
	
	@Override
	public final void applyActionHCL(ActionFloat<T> action) {
		applyActionFloat(pixels, buffer, action,
			(rgb, hcl) -> Colors.rgb2hcl(rgb[0], rgb[1], rgb[2], hcl),
			(hcl, rgb) -> Colors.hcl2rgb(hcl[0], hcl[1], hcl[2], rgb));
		swapBuffer();
	}
	
	@Override
	public final void swapBuffer() {
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
	 * Applies the given mask to {@code this} image.
	 * @param mask The mask*/
	public final void applyMask(int mask) {
		applyActionINT((input, output, i, varStore) -> {
			format.setARGB(output, i, format.getARGB(input, i) & mask);
		});
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
		ptrBuffer = -1;
		ptrPixels = -1;
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
		setPixel((y + offY) * width + (x + offX), argb);
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
		return getPixel((y + offY) * width + (x + offX));
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
	@Override
	public final T getPixels() {
		return pixels;
	}
	
	@Override
	public final T getBuffer() {
		return buffer;
	}
	
	@Override
	public final int getX() {
		return offX;
	}
	
	@Override
	public final int getY() {
		return offY;
	}
	
	/**
	 * Gets the width of {@code this} image.
	 * @return The width*/
	@Override
	public final int getWidth() {
		return subWidth;
	}
	
	/**
	 * Gets the height of {@code this} image.
	 * @return The height*/
	@Override
	public final int getHeight() {
		return subHeight;
	}
	
	@Override
	public final int getStride() {
		return width;
	}
	
	@Override
	public final int getSourceWidth() {
		return width;
	}
	
	@Override
	public final int getSourceHeight() {
		return height;
	}
	
	/**
	 * Gets the pixel format of {@code this} image.
	 * @return The pixel format*/
	@Override
	public final ImagePixelFormat<T> getPixelFormat() {
		return format;
	}
	
	@Override
	public final InternalChannels<T> getChannels() {
		return channels;
	}
}