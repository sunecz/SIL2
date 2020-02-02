package sune.lib.sil2.operation;

import java.nio.Buffer;

import sune.lib.sil2.Colors;
import sune.lib.sil2.FastBlur;
import sune.lib.sil2.FastMath;
import sune.lib.sil2.IImageContext;
import sune.lib.sil2.IImageOperation;
import sune.lib.sil2.MatrixUtils;
import sune.lib.sil2.format.ImagePixelFormat;

public final class Filters {
	
	// TODO: Update JavaDoc
	
	// Forbid anyone to create an instance of this class
	private Filters() {
	}
	
	/**
	 * Applies smoothing to {@code this} image.*/
	public static final class Smooth<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			// Normalized float kernel
			final float[] kernel = {
				0.1f, 0.1f, 0.1f,
				0.1f, 0.2f, 0.1f,
				0.1f, 0.1f, 0.1f
			};
			context.convolute2d(kernel, 1, true);
			return null;
		}
	}
	
	/**
	 * Applies box blur to {@code this} image.
	 * @param value The value
	 * @param premultiply If {@code true}, the pixels are premultiplied*/
	public static final class BoxBlur<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int value;
		private final boolean premultiply;
		
		public BoxBlur(int value) {
			this(value, false);
		}
		
		public BoxBlur(int value, boolean premultiply) {
			this.value = value;
			this.premultiply = premultiply;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			FastBlur.boxBlur(context.getPixels(), context.getBuffer(),
				context.getX(), context.getY(), context.getWidth(), context.getHeight(), value,
				context.getStride(), context.getChannels(), premultiply);
			context.swapBuffer();
			return null;
		}
	}
	
	/**
	 * Applies gaussian blur to {@code this} image.
	 * @param value The value
	 * @param premultiply If {@code true}, the pixels are premultiplied*/
	public static final class GaussianBlur<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final int value;
		private final boolean premultiply;
		
		public GaussianBlur(int value) {
			this(value, false);
		}
		
		public GaussianBlur(int value, boolean premultiply) {
			this.value = value;
			this.premultiply = premultiply;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			FastBlur.gaussianBlur(context.getPixels(), context.getBuffer(),
  				context.getX(), context.getY(), context.getWidth(), context.getHeight(), value,
				context.getStride(), context.getChannels(), premultiply);
			context.swapBuffer();
			return null;
		}
	}
	
	/**
	 * Applies motion blur of the given angle to {@code this} image.
	 * @param angleDeg The angle, in degrees
	 * @param value The value
	 * @param premultiply If {@code true}, premultiplies the pixels*/
	public static final class MotionBlur<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float angleDeg;
		private final float value;
		private final boolean premultiply;
		
		public MotionBlur(float angleDeg, float value) {
			this(angleDeg, value, false);
		}
		
		public MotionBlur(float angleDeg, float value, boolean premultiply) {
			this.angleDeg = angleDeg;
			this.value = value;
			this.premultiply = premultiply;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			int stride = context.getStride();
			int sx = context.getX(), ex = sx + context.getWidth();
			int sy = context.getY(), ey = sy + context.getHeight();
			ImagePixelFormat<T> format = context.getPixelFormat();
			T pixels = context.getPixels();
			int cos = FastMath.round(value * FastMath.cosDeg(angleDeg));
			int sin = FastMath.round(value * FastMath.sinDeg(angleDeg));
			int epp = format.getElementsPerPixel();
			context.applyActionINT((input, output, index, varStore) -> {
				int x = (index / epp) % stride, y = (index / epp) / stride;
				int tx = x + cos, ty = y + sin;
				int ux = x - cos, uy = y - sin;
				int suma = 0, sumr = 0, sumg = 0, sumb = 0;
				int idiv = 0, iclr;
				// Bresenham's line algorithm
				int dx = Math.abs(ux - tx);
				int dy = Math.abs(uy - ty);
				int qx = dx;
				int qy = dy;
				int px = tx;
				int py = ty;
				int ix = ux < tx ? -1 : +1;
				int iy = uy < ty ? -1 : +1;
				if((dx >= dy)) {
					for(int i = 0; i <= dx; i++) {
						if((qy += dy) >= dx) {
							qy -= dx;
							py += iy;
						}
						px += ix;
						// Get the pixel's color components and add them to the sums
						if((px >= sx && py >= sy && px < ex && py < ey)) {
							if((premultiply)) iclr = format.getARGBPre(pixels, (py * stride + px) * epp);
							else              iclr = format.getARGB   (pixels, (py * stride + px) * epp);
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
						if((px >= sx && py >= sy && px < ex && py < ey)) {
							if((premultiply)) iclr = format.getARGBPre(pixels, (py * stride + px) * epp);
							else              iclr = format.getARGB   (pixels, (py * stride + px) * epp);
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
			return null;
		}
	}
	
	/**
	 * Applies sharpening to {@code this} image.
	 * @param value The value*/
	public static final class Sharpen<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float value;
		
		public Sharpen(float value) {
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			int stride = context.getStride();
			int sx = context.getX(), ex = sx + context.getWidth();
			int sy = context.getY(), ey = sy + context.getHeight();
			ImagePixelFormat<T> format = context.getPixelFormat();
			int epp = format.getElementsPerPixel();
			context.applyActionRGB((rgb, input, output, index, varStore) -> {
				int x = (index / epp) % stride, y = (index / epp) / stride;
				boolean isl = x > sx, isr = x < ex - 1;
				boolean ist = y > sy, isb = y < ey - 1;
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
						iclr = format.getARGB(input, index - stride * epp - epp);
						crsr += (iclr >> 16) & 0xff;
						crsg += (iclr >>  8) & 0xff;
						crsb += (iclr)       & 0xff;
						++divc;
					}
					if((isb)) {
						iclr = format.getARGB(input, index + stride * epp - epp);
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
						iclr = format.getARGB(input, index - stride * epp + epp);
						crsr += (iclr >> 16) & 0xff;
						crsg += (iclr >>  8) & 0xff;
						crsb += (iclr)       & 0xff;
						++divc;
					}
					if((isb)) {
						iclr = format.getARGB(input, index + stride * epp + epp);
						crsr += (iclr >> 16) & 0xff;
						crsg += (iclr >>  8) & 0xff;
						crsb += (iclr)       & 0xff;
						++divc;
					}
				}
				if((ist)) {
					iclr = format.getARGB(input, index - stride * epp);
					crsr += (iclr >> 16) & 0xff;
					crsg += (iclr >>  8) & 0xff;
					crsb += (iclr)       & 0xff;
					++divc;
				}
				if((isb)) {
					iclr = format.getARGB(input, index + stride * epp);
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
			return null;
		}
	}
	
	/**
	 * Applies sharpening of edges to {@code this} image.*/
	public static final class SharpenEdges<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			// Normalized float kernel
			final float[] kernel = {
				-0.166667f, -0.166667f, -0.166667f,
				-0.166667f, +2.333333f, -0.166667f,
				-0.166667f, -0.166667f, -0.166667f,
			};
			context.convolute2d(kernel, 1, true);
			return null;
		}
	}
	
	/**
	 * Applies unsharp mask to {@code this} image.*/
	public static final class UnsharpMask<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
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
			context.convolute2d(kernel, 1, true);
			return null;
		}
	}
	
	/**
	 * Applies edge detection kernel to {@code this} image.*/
	public static final class EdgeDetection<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			// Denormalized float kernel (since sum=0.0f)
			final float[] kernel = {
				-1.0f, -1.0f, -1.0f,
				-1.0f, +8.0f, -1.0f,
				-1.0f, -1.0f, -1.0f
			};
			context.convolute2d(kernel, 1, false);
			return null;
		}
	}
	
	/**
	 * Applies emboss effect to {@code this} image.
	 * @param angleDeg Angle of the emboss effect, in degrees
	 * @param value Strength of the emboss effect
	 * @param depth Depth of the emboss effect*/
	public static final class Emboss<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float angleDeg;
		private final float value;
		
		public Emboss(float angleDeg, float value) {
			this.angleDeg = angleDeg;
			this.value = value;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			// Normalized float kernel
			final float[] kernel = {
				-value, -1.0f,  0.0f,
				-1.0f,   1.0f,  1.0f,
				 0.0f,   1.0f,  value
			};
			MatrixUtils.rotate(kernel, (float) Math.toRadians(angleDeg));
			context.convolute2d(kernel, 1, true);
			return null;
		}
	}
	
	/**
	 * Applies mean filter to {@code this} image.*/
	public static final class Mean<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			// Normalized float kernel
			final float[] kernel = {
				0.11111f, 0.11111f, 0.11111f,
				0.11111f, 0.11111f, 0.11111f,
				0.11111f, 0.11111f, 0.11111f
			};
			context.convolute2d(kernel, 1, true);
			return null;
		}
	}
	
	/**
	 * Applies sobel filter to {@code this} image.*/
	public static final class Sobel<T extends Buffer> implements IImageOperation<T, Void> {
		
		@Override
		public final Void execute(IImageContext<T> context) {
			int sx = context.getX(), ex = sx + context.getWidth();
			int sy = context.getY(), ey = sy + context.getHeight();
			int width = context.getWidth();
			int height = context.getHeight();
			int swidth = context.getSourceWidth();
			int sheight = context.getSourceHeight();
			T pixels = context.getPixels();
			T buffer = context.getBuffer();
			ImagePixelFormat<T> format = context.getPixelFormat();
			int epp = format.getElementsPerPixel();
			// Precompute the grayscale values of the pixels
			int str = context.getStride();
			for(int x = sx, y = sy, i = (y * str + x) * epp, d = (str - (ex - sx)) * epp;; i += epp) {
				format.set(buffer, i, Colors.grayscale(format.getARGB(buffer, i)));
				if((++x == ex)) {
					 x  = sx;
					 i += d;
					 if((++y == ey))
						 break;
				}
			}
			int tx = Math.max(1, sx), ty = Math.max(1, sy);
			int ux = Math.min(swidth - 2, ex), uy = Math.min(sheight - 2, ey);
			// Do the actual Sobel filtering
			context.applyAreaJob(tx, ty, ux, uy, buffer, pixels, (rx, ry, rw, rh, input, stride, output) -> {
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
			if((sy == 0)) {
				context.applyLineHJob(sx, sy, width, buffer, pixels, (i, x, y, input, stride, output) -> {
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
					if((x == swidth - 1)) {
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
			}
			// Bottom line
			if((ey == sheight)) {
				context.applyLineHJob(sx, ey - 1, width, buffer, pixels, (i, x, y, input, stride, output) -> {
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
					if((x == swidth - 1)) {
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
			}
			// Left line
			if((sx == 0)) {
				context.applyLineVJob(sx, Math.max(sy, 1), height - 2, buffer, pixels, (i, x, y, input, stride, output) -> {
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
			}
			// Right line
			if((ex == swidth)) {
				context.applyLineVJob(ex - 1, Math.max(sy, 1), height - 2, buffer, pixels, (i, x, y, input, stride, output) -> {
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
			return null;
		}
	}
}