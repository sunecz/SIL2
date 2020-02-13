package sune.lib.sil2;

import java.nio.Buffer;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import sune.lib.sil2.format.ImagePixelFormat;
import sune.lib.sil2.format.ImagePixelFormats;

/**
 * Contains static methods used for image manipulation.*/
public final class ImageUtils {
	
	/**
	 * Helper class for holding information about an image data, particularly
	 * that is the pixel array and its width and height.*/
	public static final class ImageData {
	
		/**
		 * The width of the image.*/
		public final int width;
		/**
		 * The height of the image.*/
		public final int height;
		/**
		 * The pixels of the image.*/
		public final Buffer pixels;
	
		private ImageData(int width, int height, Buffer pixels) {
			this.width  = width;
			this.height = height;
			this.pixels = pixels;
		}
	}
	
	// Forbid anyone to create an instance of this class
	private ImageUtils() {
	}
	
	// ----- IMAGE PIXELS
	
	/**
	 * Gets the underlying buffer containing all pixels of the given image.
	 * @param image The image
	 * @return The pixels as a buffer*/
	public static final <T extends Buffer> Buffer getPixels(Image image) {
		return FXInternalUtils.getPlatformImageWrapper(image).bufferRewind();
	}
	
	private static final <T extends Buffer> void setPixels(WritableImage image, int x, int y, int width, int height,
			T pixels, int stride, ImagePixelFormat<T> format) {
		image.getPixelWriter().setPixels(x, y, width, height, format.getWriteFormat(), pixels, stride);
	}
	
	/**
	 * Sets the given pixels into an area, specified by the given x- and y-coordinates and
	 * its width and height, in the given image. The image's width is taken as the stride.
	 * @param image The image
	 * @param x The x-coordinate of the area
	 * @param y The y-coordinate of the area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @param pixels The pixels*/
	public static final void setPixels(WritableImage image, int x, int y, int width, int height,
			Buffer pixels) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int stride = (int) image.getWidth();
		setPixels(image, x, y, width, height, pixels, stride);
	}
	
	/**
	 * Sets the given pixels into an area, specified by the given x- and y-coordinates and
	 * its width and height, in the given image.
	 * @param image The image
	 * @param x The x-coordinate of the area
	 * @param y The y-coordinate of the area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @param stride The stride of the image
	 * @param pixels The pixels*/
	public static final <T extends Buffer> void setPixels(WritableImage image, int x, int y, int width, int height,
			T pixels, int stride) {
		if((image == null)) throw new NullPointerException("Invalid image");
		if((width <= 0 || height <= 0))
			throw new IllegalArgumentException("Invalid size");
		if((x < 0 || y < 0 || x >= width || y >= height))
			throw new IllegalArgumentException("Invalid position");
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> format = (ImagePixelFormat<T>) ImagePixelFormats.from(image.getPixelReader().getPixelFormat());
		setPixels(image, x, y, width, height, pixels, stride, format);
	}
	
	// ----- IMAGE CREATE
	
	/**
	 * Creates a new image with the given dimensions.
	 * @param width The width of the image
	 * @param height The height of the image
	 * @return The newly created image*/
	public static final WritableImage create(int width, int height) {
		if((width <= 0 || height <= 0))
			throw new IllegalArgumentException("Invalid size");
		return new WritableImage(width, height);
	}
	
	/**
	 * Creates a new image with the given dimensions and the given pixels.
	 * @param width The width of the image
	 * @param height The height of the image
	 * @param pixels The pixels
	 * @return The newly created image*/
	public static final WritableImage create(int width, int height, Buffer pixels) {
		if((pixels == null)) throw new NullPointerException("Invalid pixels array");
		WritableImage image = create(width, height);
		int epp = ImagePixelFormats.from(image).getElementsPerPixel();
		setPixels(image, 0, 0, width, height, pixels, width * epp);
		return image;
	}
	
	/**
	 * Creates a new image with the given dimensions and the given color.
	 * @param width The width of the image
	 * @param height The height of the image
	 * @param color The color
	 * @return The newly created image*/
	public static final WritableImage create(int width, int height, int color) {
		WritableImage image = create(width, height);
		ImagePixelFormat<?> format = ImagePixelFormats.from(image);
		Buffer pixels = getPixels(image);
		BufferUtils.fill(pixels, color, format.getElementsPerPixel());
		return image;
	}
	
	// ----- IMAGE CROP
	
	/**
	 * Crops an area, specified by the x- and y-coordinates and its width and height,
	 * in the given source array with the given source's stride and outputs it to
	 * the given destination array to a position, specified by the given x- and y-coordinates,
	 * with the given destination's stride.
	 * @param src The source array
	 * @param srcx The x-coordinate of the source area
	 * @param srcy The y-coordinate of the source area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @param srcStride The source's stride
	 * @param dst The destination array
	 * @param dstx The x-coordinate of the destination position
	 * @param dsty The y-coordinate of the destination position
	 * @param dstStride The destination's stride*/
	public static final void crop(Buffer src, int srcx, int srcy, int width, int height, int srcStride,
			Buffer dst, int dstx, int dsty, int dstStride) {
		if((src == null)) throw new NullPointerException("Invalid source array");
		if((dst == null)) throw new NullPointerException("Invalid destination array");
		if((srcx < 0 || srcy < 0))
			throw new IllegalArgumentException("Invalid source coordinates");
		if((dstx < 0 || dsty < 0))
			throw new IllegalArgumentException("Invalid destination coordinates");
		if((width <= 0 || height <= 0))
			throw new IllegalArgumentException("Invalid cropping dimensions");
		int srci = srcy * srcStride + srcx;
		int dsti = dsty + dstStride + dstx;
		if((srci >= src.capacity()))
			throw new IllegalArgumentException("Invalid source coordinates");
		if((dsti >= dst.capacity()))
			throw new IllegalArgumentException("Invalid destination coordinates");
		for(int iy = 0; iy < height; ++iy, srci += srcStride, dsti += dstStride) {
			// Copy whole rows instead of the individual pixels
			System.arraycopy(src, srci, dst, dsti, width);
		}
	}
	
	/**
	 * Crops an area, specified by the x- and y-coordinates and its width and height,
	 * in the given source array with the given source's stride and outputs it to
	 * the given destination array to the same x- and y-coordinates, with the given
	 * destination's stride.
	 * @param src The source array
	 * @param x The x-coordinate of the source area
	 * @param y The y-coordinate of the source area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @param srcStride The source's stride
	 * @param dst The destination array
	 * @param dstStride The destination's stride*/
	public static final void crop(Buffer src, int x, int y, int width, int height, int srcStride,
			Buffer dst, int dstStride) {
		crop(src, x, y, width, height, srcStride, dst, x, y, dstStride);
	}
	
	/**
	 * Crops an area, specified by the x- and y-coordinates and its width and height,
	 * in the given source array with the given stride and outputs it to
	 * the given destination array to the same x- and y-coordinates, with the same
	 * given stride.
	 * @param src The source array
	 * @param x The x-coordinate of the source area
	 * @param y The y-coordinate of the source area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @param stride The source's stride
	 * @param dst The destination array*/
	public static final void crop(Buffer src, int x, int y, int width, int height, int stride, Buffer dst) {
		crop(src, x, y, width, height, stride, dst, x, y, stride);
	  }
	
	/**
	 * Crops an area, specified by the x- and y-coordinates and its width and height,
	 * in the given source image and outputs it to the given destination image
	 * to a position, specified by the given x- and y-coordinates.
	 * @param src The source image
	 * @param srcx The x-coordinate of the source area
	 * @param srcy The y-coordinate of the source area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @param dst The destination image
	 * @param dstx The x-coordinate of the destination position
	 * @param dsty The y-coordinate of the destination position*/
	public static final void crop(Image src, int srcx, int srcy, int width, int height,
			WritableImage dst, int dstx, int dsty) {
		if((src == null)) throw new NullPointerException("Invalid source image");
		if((dst == null)) throw new NullPointerException("Invalid destination image");
		if((srcx < 0 || srcy < 0 || srcx >= (int) src.getWidth() || srcy >= (int) src.getHeight()))
			throw new IllegalArgumentException("Invalid source coordinates");
		if((dstx < 0 || dsty < 0 || dstx >= (int) dst.getWidth() || dsty >= (int) dst.getHeight()))
			throw new IllegalArgumentException("Invalid destination coordinates");
		dst.getPixelWriter().setPixels(dstx, dsty, width, height, src.getPixelReader(), srcx, srcy);
	  }
	
	/**
	 * Crops an area, specified by the x- and y-coordinates and its width and height,
	 * in the given source image and outputs it to the given destination image
	 * to the same position.
	 * @param src The source image
	 * @param x The x-coordinate of the source area
	 * @param y The y-coordinate of the source area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @param dst The destination image*/
	public static final void crop(Image src, int x, int y, int width, int height, WritableImage dst) {
		crop(src, x, y, width, height, dst, x, y);
	}
	
	/**
	 * Crops an area, specified by the x- and y-coordinates and its width and height,
	 * in the given image to a new image.
	 * @param image The image
	 * @param x The x-coordinate of the source area
	 * @param y The y-coordinate of the source area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @return The cropped image*/
	public static final WritableImage crop(Image image, int x, int y, int width, int height) {
		if((image == null)) throw new NullPointerException("Invalid image");
		if((width <= 0 || height <= 0))
			throw new IllegalArgumentException("Invalid size");
		if((x < 0 || y < 0 || x >= (int) image.getWidth() || y >= (int) image.getHeight()))
			throw new IllegalArgumentException("Invalid position");
		return new WritableImage(image.getPixelReader(), x, y, width, height);
	}
	
	// ----- IMAGE RESIZE
	
	/**
	 * Quickly resizes the given source array with the given width and height
	 * to the given destination width and height and outputs it to the given
	 * destination array. The pixels are resized using a Bresenham's algorithm
	 * and nearest-neighbor-like interpolation is used.
	 * @param src The source array
	 * @param srcw The source width
	 * @param srch The source height
	 * @param dst The destination array
	 * @param dstw The destination width
	 * @param dsth The destination height
	 * @param format The pixel format*/
	// https://web.archive.org/web/20170809062128/http://willperone.net/Code/codescaling.php
	public static final <T extends Buffer> void fastresize(T src, int srcw, int srch, T dst, int dstw, int dsth,
			ImagePixelFormat<T> format) {
		int dx = (srcw / dstw);
		int dy = (srch / dsth) * srcw - srcw;
		int ry = (srch % dsth);
		int rx = (srcw % dstw);
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = 0, x = dstw, y = dsth, ex = 0, ey = 0;;) {
			format.setARGB(dst, k * epp, src, i * epp);
			i += dx;
			k++;
			if((ex += rx) >= dstw) {
				ex -= dstw;
				++i;
			}
			if((--x == 0)) {
				x  = dstw;
				ex = 0;
				i += dy;
				if((ey += ry) >= dsth) {
					ey -= dsth;
					i  += srcw;
				}
				if((--y == 0))
					break;
			}
		}
	}
	
	/**
	 * Quickly resizes the given image to the given width and height and outputs
	 * it to a new image. The pixels are resized using a Bresenham's algorithm and
	 * nearest-neighbor-like interpolation is used.
	 * @param image The image
	 * @param width The width
	 * @param height The height
	 * @return The resized image*/
	public static final <T extends Buffer> WritableImage fastresize(Image image, int width, int height) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int   iwidth  = (int) image.getWidth();
		int   iheight = (int) image.getHeight();
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> format = (ImagePixelFormat<T>) ImagePixelFormats.from(image);
		@SuppressWarnings("unchecked")
		T pixels = (T) getPixels(image);
		T result = format.newBuffer(width * height);
		fastresize(pixels, iwidth, iheight, result, width, height, format);
		return create(width, height, result);
	}
	
	// ----- IMAGE ROTATE
	
	/**
	 * Quickly rotates the given pixels array with the given width and height
	 * by the given angle and outputs it to a new image. Nearest-neighbor-like
	 * interpolation is used.
	 * @param pixels The pixels
	 * @param srcw The image width
	 * @param srch The image height
	 * @param rad The angle of rotation, in radians
	 * @param cx The x-coordinate of the center of rotation
	 * @param cy The y-coordinate of the center of rotation
	 * @param format The pixel format
	 * @return The wrapper object of the rotated image*/
	// https://www.drdobbs.com/architecture-and-design/fast-bitmap-rotation-and-scaling/184416337
	public static final <T extends Buffer> ImageData fastrotate(T pixels, int srcw, int srch, float rad,
			int cx, int cy, ImagePixelFormat<T> format) {
		if((pixels == null)) throw new NullPointerException("Invalid pixels array");
		if((srcw <= 0 || srch <= 0 || pixels.capacity() % (srcw * srch) != 0))
			throw new IllegalArgumentException("Invalid size");
		float cos = (float) Math.cos(rad), acos = Math.abs(cos);
		float sin = (float) Math.sin(rad), asin = Math.abs(sin);
		int dstw = (int) (srcw * acos + srch * asin);
		int dsth = (int) (srch * acos + srcw * asin);
		int ncx = dstw >> 1, ncy = dsth >> 1;
		int epp = format.getElementsPerPixel();
		T dst = BufferUtils.newBufferOfType(pixels, dstw * dsth * epp);
		float su = cx - (ncx * cos + ncy * sin), ru = su, u;
		float sv = cy - (ncy * cos - ncx * sin), rv = sv, v;
		for(int y = 0, i = 0, k, stride = srcw * epp; y < dsth; ++y) {
			u = ru;
			v = rv;
			for(int x = 0; x < dstw; ++x, i += epp) {
				if((u >= 0.0f && v >= 0.0f && u < srcw && v < srch)) {
					k = (int) v * stride + (int) u * epp;
					format.setARGB(dst, i, pixels, k);
				}
				u += cos;
				v -= sin;
			}
			ru += sin;
			rv += cos;
		}
		return new ImageData(dstw, dsth, dst);
	}
	
	/**
	 * Quickly rotates the given image by the given angle and outputs it to a new image.
	 * Nearest-neighbor-like interpolation is used.
	 * @param image The image
	 * @param rad The angle of rotation, in radians
	 * @param cx The x-coordinate of the center of rotation
	 * @param cy The y-coordinate of the center of rotation
	 * @return The rotated image*/
	public static final <T extends Buffer> WritableImage fastrotate(Image image, float rad, int cx, int cy) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int   width  = (int) image.getWidth();
		int   height = (int) image.getHeight();
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> format = (ImagePixelFormat<T>) ImagePixelFormats.from(image);
		@SuppressWarnings("unchecked")
		T pixels = (T) getPixels(image);
		ImageData data = fastrotate(pixels, width, height, rad, width >> 1, height >> 1, format);
		return create(data.width, data.height, data.pixels);
	}
	
	/**
	 * Quickly rotates the given image by the given angle and outputs it to a new image.
	 * Nearest-neighbor-like interpolation is used.
	 * @param image The image
	 * @param rad The angle of rotation, in radians
	 * @return The rotated image*/
	public static final <T extends Buffer> WritableImage fastrotate(Image image, float rad) {
		return fastrotate(image, rad, (int) image.getWidth() >> 1, (int) image.getHeight() >> 1);
	}
	
	// ----- IMAGE FILL
	
	/**
	 * Fills the given pixels array with the given color.
	 * @param pixels The pixels array
	 * @param color The color
	 * @param format The pixel format*/
	public static final <T extends Buffer> void fill(T pixels, int color, ImagePixelFormat<T> format) {
		BufferUtils.fill(pixels, color, format.getElementsPerPixel());
	}
	
	/**
	 * Fills the given image with the given color.
	 * @param image The image
	 * @param color The color*/
	public static final <T extends Buffer> void fill(WritableImage image, int color) {
		if((image == null)) throw new NullPointerException("Invalid image");
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> format = (ImagePixelFormat<T>) ImagePixelFormats.from(image);
		@SuppressWarnings("unchecked")
		T pixels = (T) getPixels(image);
		fill(pixels, color, format);
	}
	
	// ----- IMAGE REPEAT
	
	/**
	 * Repeats an image given by the given source array, an area, specified by the given
	 * x- and y-coordinates and its width and height, and the given source's stride to
	 * the given destination array, an area, specified by the given x- and y-coordinates
	 * and its width and height, and the destination's stride.
	 * @param src The source array
	 * @param srcx The x-coordinate of the source area
	 * @param srcy The y-coordinate of the source araa
	 * @param srcw The width of the source area
	 * @param srch The height of the source area
	 * @param srcStride The source's stride
	 * @param dst The destination array
	 * @param dstx The x-coordinate of the destination area
	 * @param dsty The y-coordinate of the destination area
	 * @param dstw The width of the destination area
	 * @param dsth The height of the destination area
	 * @param dstStride The destination's stride
	 * @param epp The number of elements per pixel*/
	public static final void repeat(Buffer src, int srcx, int srcy, int srcw, int srch, int srcStride,
			Buffer dst, int dstx, int dsty, int dstw, int dsth, int dstStride, int epp) {
		if((src == null)) throw new NullPointerException("Invalid source array");
		if((dst == null)) throw new NullPointerException("Invalid destination array");
		if((srcw <= 0 || srch <= 0 || src.capacity() % (srcw * srch) != 0))
			throw new IllegalArgumentException("Invalid source size");
		if((dstw <= 0 || dsth <= 0 || dst.capacity() % (dstw * dsth) != 0))
			throw new IllegalArgumentException("Invalid destination size");
		if((srcx < 0 || srcy < 0))
			throw new IllegalArgumentException("Invalid source position");
		if((dstx < 0 || dsty < 0))
			throw new IllegalArgumentException("Invalid destination position");
		int cols    = (int) Math.ceil(dstw / (double) srcw);
		int rows    = (int) Math.ceil(dsth / (double) srch);
		int lwidth  = dstw - (cols-1) * srcw;
		int lheight = dsth - (rows-1) * srch;
		for(int r = rows, c = cols, offX = dstx, offY = dsty;;) {
			int iw = c == 1 ? lwidth  : srcw;
			int ih = r == 1 ? lheight : srch;
			Pixels.copy(src, srcx, srcy, srcStride, dst, offX, offY, dstStride, iw, ih, epp);
			offX += srcw;
			if((--c == 0)) {
				c = cols;
				if((--r == 0))
					break;
				offX  = 0;
				offY += srch;
			}
		}
	}
	
	/**
	 * Repeats an image given by the given source array, an area, specified by the given
	 * x- and y-coordinates and its width and height, and the given source's stride to
	 * the given destination image and an area, specified by the given x- and y-coordinates
	 * and its width and height.
	 * @param src The source array
	 * @param srcx The x-coordinate of the source area
	 * @param srcy The y-coordinate of the source araa
	 * @param srcw The width of the source area
	 * @param srch The height of the source area
	 * @param srcStride The source's stride
	 * @param dst The destination image
	 * @param dstx The x-coordinate of the destination area
	 * @param dsty The y-coordinate of the destination area
	 * @param dstw The width of the destination area
	 * @param dsth The height of the destination area*/
	public static final void repeat(Buffer src, int srcx, int srcy, int srcw, int srch, int srcStride,
			WritableImage dst, int dstx, int dsty, int dstw, int dsth) {
		if((dst == null)) throw new NullPointerException("Invalid destination image");
		int width  = (int) dst.getWidth();
		int height = (int) dst.getHeight();
		// Check only if larger, others will be checked in the actual repeat method
		if((dstx >= width || dsty >= height))
			throw new IllegalArgumentException("Invalid destination position");
		if((dstw <= 0 || dstw > width || dsth <= 0 || dsth > height))
			throw new IllegalArgumentException("Invalid destination size");
		int epp = ImagePixelFormats.from(dst).getElementsPerPixel();
		Buffer pixels = getPixels(dst);
		repeat(src, srcx, srcy, srcw, srch, srcStride, pixels, dstx, dsty, dstw, dsth, width * epp, epp);
	}
	
	/**
	 * Repeats an image given by the given source array, an area, specified by the given
	 * x- and y-coordinates and its width and height, and the given source's stride to
	 * the given destination image and an area, specified by the given x- and y-coordinates
	 * and source area's width and height.
	 * @param src The source array
	 * @param srcx The x-coordinate of the source area
	 * @param srcy The y-coordinate of the source araa
	 * @param srcw The width of the source area
	 * @param srch The height of the source area
	 * @param srcStride The source's stride
	 * @param dst The destination image
	 * @param dstx The x-coordinate of the destination area
	 * @param dsty The y-coordinate of the destination area*/
	public static final void repeat(Buffer src, int srcx, int srcy, int srcw, int srch, int srcStride,
			WritableImage dst, int dstx, int dsty) {
		if((dst == null)) throw new NullPointerException("Invalid destination image");
		int width  = (int) dst.getWidth();
		int height = (int) dst.getHeight();
		// Check only if larger, others will be checked in the actual repeat method
		if((dstx >= width || dsty >= height))
			throw new IllegalArgumentException("Invalid destination position");
		int epp = ImagePixelFormats.from(dst).getElementsPerPixel();
		Buffer pixels = getPixels(dst);
		repeat(src, srcx, srcy, srcw, srch, srcStride, pixels, dstx, dsty, width, height, width * epp, epp);
	}
	
	/**
	 * Repeats the given image's area, specified by the given x- and y-coordinates and
	 * its width and height, to the given destination image and an area, specified
	 * by the given x- and y-coordinates and source area's width and height.
	 * @param src The source array
	 * @param srcx The x-coordinate of the source area
	 * @param srcy The y-coordinate of the source araa
	 * @param srcw The width of the source area
	 * @param srch The height of the source area
	 * @param dst The destination image
	 * @param dstx The x-coordinate of the destination area
	 * @param dsty The y-coordinate of the destination area*/
	public static final void repeat(Image src, int srcx, int srcy, int srcw, int srch,
			WritableImage dst, int dstx, int dsty) {
		if((src == null)) throw new NullPointerException("Invalid source image");
		int width  = (int) src.getWidth();
		int height = (int) src.getHeight();
		// Check only if larger, others will be checked in the actual repeat method
		if((srcx >= width || srcy >= height))
			throw new IllegalArgumentException("Invalid source position");
		if((srcw <= 0 || srcw > width || srch <= 0 || srch > height))
			throw new IllegalArgumentException("Invalid destination size");
		int epp = ImagePixelFormats.from(dst).getElementsPerPixel();
		Buffer pixels = getPixels(src);
		repeat(pixels, srcx, srcy, srcw, srch, width * epp, dst, dstx, dsty);
	}
	
	/**
	 * Repeats the given image to the given destination image to a position, specified
	 * by the given x- and y-coordinates.
	 * @param src The source array
	 * @param dst The destination image
	 * @param dstx The x-coordinate of the position
	 * @param dsty The y-coordinate of the position*/
	public static final void repeat(Image src, WritableImage dst, int dstx, int dsty) {
		if((src == null)) throw new NullPointerException("Invalid source image");
		if((dst == null)) throw new NullPointerException("Invalid destination image");
		int width  = (int) dst.getWidth();
		int height = (int) dst.getHeight();
		// Check only if larger, others will be checked in the actual repeat method
		if((dstx >= width || dsty >= height))
			throw new IllegalArgumentException("Invalid destination position");
		int srcw = (int) src.getWidth();
		int srch = (int) src.getHeight();
		repeat(src, 0, 0, srcw, srch, dst, dstx, dsty);
	}
	
	/**
	 * Repeats the given image to the given destination image, filling the destination
	 * image entirely.
	 * @param src The source array
	 * @param dst The destination image*/
	public static final void repeat(Image src, WritableImage dst) {
		if((src == null)) throw new NullPointerException("Invalid source image");
		int width  = (int) src.getWidth();
		int height = (int) src.getHeight();
		repeat(src, 0, 0, width, height, dst, 0, 0);
	}
	
	/**
	 * Repeats the given image's area, specified by the given x- and y-coordinates and
	 * its width and height, to a new image of the given width and height.
	 * @param src The image
	 * @param srcx The x-coordinate of the source area
	 * @param srcy The y-coordinate of the source araa
	 * @param srcw The width of the source area
	 * @param srch The height of the source area
	 * @param dstw The width of the repeated image
	 * @param dsth The height of the repeated image
	 * @return The repeated image*/
	public static final WritableImage repeat(Image src, int srcx, int srcy, int srcw, int srch, int dstw, int dsth) {
		WritableImage dst = create(dstw, dsth);
		repeat(src, srcx, srcy, srcw, srch, dst, 0, 0);
		return dst;
	}
	
	/**
	 * Repeats the given image's area, specified by the given x- and y-coordinates and
	 * the left-over width and height, to a new image of the given width and height.
	 * @param src The image
	 * @param srcx The x-coordinate of the source area
	 * @param srcy The y-coordinate of the source araa
	 * @param dstw The width of the repeated image
	 * @param dsth The height of the repeated image
	 * @return The repeated image*/
	public static final WritableImage repeat(Image src, int srcx, int srcy, int dstw, int dsth) {
		if((src == null)) throw new NullPointerException("Invalid source image");
		WritableImage dst = create(dstw, dsth);
		int srcw = (int) src.getWidth();
		int srch = (int) src.getHeight();
		repeat(src, srcx, srcy, srcw - srcx, srch - srcy, dst, 0, 0);
		return dst;
	}
	
	/**
	 * Repeats the given image to a new image with the given width and height.
	 * @param src The image
	 * @param dstw The repeated image's width
	 * @param dsth The repeated image's height
	 * @return The repeated image*/
	public static final WritableImage repeat(Image src, int dstw, int dsth) {
		WritableImage dst = create(dstw, dsth);
		repeat(src, dst);
		return dst;
	}
	
	// ----- IMAGE COPY
	
	/**
	 * Copies an area, specified by the given x- and y-coordinates and its width and height,
	 * from the given image and outputs it to a new image.
	 * @param image The image
	 * @param x The x-coordinate of the area
	 * @param y The y-coordinate of the area
	 * @param width The width of the area
	 * @param height The height of the area
	 * @return The copied image*/
	public static final WritableImage copy(Image image, int x, int y, int width, int height) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int imgw = (int) image.getWidth();
		int imgh = (int) image.getHeight();
		if((x < 0 || x >= imgw || y < 0 || y >= imgh))
			throw new IllegalArgumentException("Invalid position");
		if((width <= 0 || width > imgw || height <= 0 || height > imgh))
			throw new IllegalArgumentException("Invalid size");
		return new WritableImage(image.getPixelReader(), x, y, width, height);
	}
	
	/**
	 * Copies an area, specified by the given x- and y-coordinates and the left-over width
	 * and height, from the given image and outputs it to a new image.
	 * @param image The image
	 * @param x The x-coordinate of the area
	 * @param y The y-coordinate of the area
	 * @return The copied image*/
	public static final WritableImage copy(Image image, int x, int y) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int width  = (int) image.getWidth();
		int height = (int) image.getHeight();
		return copy(image, x, y, width - x, height - y);
	}
	
	/**
	 * Copies the given image and outputs it to a new image.
	 * @param image The image
	 * @return The copied image*/
	public static final WritableImage copy(Image image) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int width  = (int) image.getWidth();
		int height = (int) image.getHeight();
		return copy(image, 0, 0, width, height);
	}
	
	/**
	 * Copies pixels from the given source image to the given destination
	 * image, if possible. If the destination image has the same dimensions
	 * as the source image, the pixels are only copied and the destination
	 * image is returned. Otherwise, a new image is created with the required
	 * dimensions, so that the source image can be copied, and with the copied
	 * pixels. Due to this, it is recommended to use this method as follows:
	 * <pre>
	 * Image src = ...;
	 * WritableImage dst = ...;
	 * ...
	 * dst = copyPixels(src, dst);
	 * ...</pre>
	 * @param src The source image
	 * @param dst The destination image
	 * @return {@code dst}, if pixels can be copied directly, otherwise
	 * a new image with the copied pixels.*/
	public static final WritableImage copyPixels(Image src, WritableImage dst) {
		if((src == null)) throw new NullPointerException("Invalid source image");
		int sw = (int) src.getWidth();
		int sh = (int) src.getHeight();
		if((dst == null))
			dst = new WritableImage(sw, sh);
		int dw = (int) dst.getWidth();
		int dh = (int) dst.getHeight();
		if((sw == dw && sh == dh)) {
			// Can copy exact pixels (will not create a new image)
			dst.getPixelWriter().setPixels(0, 0, dw, dh, src.getPixelReader(), 0, 0);
		} else {
			// Cannot copy exact pixels since dimensions do not match (will create a new image)
			dst = copy(src);
		}
		return dst;
	}
	
	// ----- IMAGE FLIP
	
	/**
	 * Flips the given pixels, of the given width and height, horizontally.
	 * @param pixels The pixels
	 * @param width The width
	 * @param height The height
	 * @param format The pixel format*/
	public static final <T extends Buffer> void flipHorizontal(T pixels, int width, int height,
			ImagePixelFormat<T> format) {
		if((pixels == null)) throw new NullPointerException("Invalid pixels array");
		if((width <= 0 || height <= 0 || pixels.capacity() % (width * height) != 0))
			throw new IllegalArgumentException("Invalid size");
		int epp = format.getElementsPerPixel();
		for(int i = 0, k = width - 1,
				m = width / 2,
				a = width - m,
				b = width + m, x = m, y = height, t;;) {
			t = format.getARGB(pixels, i * epp);
			format.setARGB(pixels, i * epp, pixels, k * epp);
			format.setARGB(pixels, k * epp, t);
			++i; --k;
			if((--x == 0)) {
				x = m;
				i += a;
				k += b;
				if((--y == 0))
					break;
			}
		}
	}
	
	/**
	 * Flips the given pixels, of the given width and height, vertically.
	 * @param pixels The pixels
	 * @param width The width
	 * @param height The height
	 * @param format The pixel format*/
	public static final <T extends Buffer> void flipVertical(T pixels, int width, int height,
			ImagePixelFormat<T> format) {
		if((pixels == null)) throw new NullPointerException("Invalid pixels array");
		if((width <= 0 || height <= 0 || pixels.capacity() % (width * height) != 0))
			throw new IllegalArgumentException("Invalid size");
		T temp = format.newBuffer(width);
		for(int stride = width * format.getElementsPerPixel(),
				i = 0, k = pixels.capacity() - stride, y = height / 2;;) {
			BufferUtils.buffercopy(pixels, i, temp,   0, stride);
			BufferUtils.buffercopy(pixels, k, pixels, i, stride);
			BufferUtils.buffercopy(temp,   0, pixels, k, stride);
			i += stride;
			k -= stride;
			if((--y == 0))
				break;
		}
	}
	
	/**
	 * Flips the given pixels, of the given width and height, to the left.
	 * @param pixels The pixels
	 * @param width The width
	 * @param height The height
	 * @param format The pixel format*/
	public static final <T extends Buffer> void flipLeft(T pixels, int width, int height,
			ImagePixelFormat<T> format) {
		if((pixels == null)) throw new NullPointerException("Invalid pixels array");
		if((width <= 0 || height <= 0 || pixels.capacity() % (width * height) != 0))
			throw new IllegalArgumentException("Invalid size");
		T copy = BufferUtils.copy(pixels);
		int epp = format.getElementsPerPixel();
		for(int x = width, y = height, i = 0, l = pixels.capacity() / epp, k = l - height, a = l + 1;;) {
			format.setARGB(pixels, k * epp, copy, i * epp);
			++i;
			k -= height;
			if((--x == 0)) {
				x  = width;
				k += a;
				if((--y == 0))
					break;
			}
		}
	}
	
	/**
	 * Flips the given pixels, of the given width and height, to the right.
	 * @param pixels The pixels
	 * @param width The width
	 * @param height The height
	 * @param format The pixel format*/
	public static final <T extends Buffer> void flipRight(T pixels, int width, int height,
			ImagePixelFormat<T> format) {
		if((pixels == null)) throw new NullPointerException("Invalid pixels array");
		if((width <= 0 || height <= 0 || pixels.capacity() % (width * height) != 0))
			throw new IllegalArgumentException("Invalid size");
		T copy = BufferUtils.copy(pixels);
		int epp = format.getElementsPerPixel();
		for(int x = width, y = height, i = 0, l = pixels.capacity() / epp, k = height - 1, a = -l - 1;;) {
			format.setARGB(pixels, k * epp, copy, i * epp);
			++i;
			k += height;
			if((--x == 0)) {
				x  = width;
				k += a;
				if((--y == 0))
					break;
			}
		}
	}
	
	/**
	 * Flips the given image horizontally.
	 * @param image The image
	 * @return The flipped image*/
	public static final <T extends Buffer> WritableImage flipHorizontal(WritableImage image) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int   width  = (int) image.getWidth();
		int   height = (int) image.getHeight();
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> format = (ImagePixelFormat<T>) ImagePixelFormats.from(image);
		@SuppressWarnings("unchecked")
		T pixels = (T) getPixels(image);
		flipHorizontal(pixels, width, height, format);
		return image;
	}
	
	/**
	 * Flips the given image vertically.
	 * @param image The image
	 * @return The flipped image*/
	public static final <T extends Buffer> WritableImage flipVertical(WritableImage image) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int   width  = (int) image.getWidth();
		int   height = (int) image.getHeight();
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> format = (ImagePixelFormat<T>) ImagePixelFormats.from(image);
		@SuppressWarnings("unchecked")
		T pixels = (T) getPixels(image);
		flipVertical(pixels, width, height, format);
		return image;
	}
	
	/**
	 * Flips the given image to the left.
	 * @param image The image
	 * @return The flipped image*/
	public static final <T extends Buffer> WritableImage flipLeft(WritableImage image) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int   width  = (int) image.getWidth();
		int   height = (int) image.getHeight();
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> format = (ImagePixelFormat<T>) ImagePixelFormats.from(image);
		@SuppressWarnings("unchecked")
		T pixels = (T) getPixels(image);
		flipLeft(pixels, width, height, format);
		return create(height, width, pixels);
	}
	
	/**
	 * Flips the given image to the right.
	 * @param image The image
	 * @return The flipped image*/
	public static final <T extends Buffer> WritableImage flipRight(WritableImage image) {
		if((image == null)) throw new NullPointerException("Invalid image");
		int   width  = (int) image.getWidth();
		int   height = (int) image.getHeight();
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> format = (ImagePixelFormat<T>) ImagePixelFormats.from(image);
		@SuppressWarnings("unchecked")
		T pixels = (T) getPixels(image);
		flipRight(pixels, width, height, format);
		return create(height, width, pixels);
	}
	
	// ----- IMAGE COMBINE
	
	/**
	 * Combines the given background and the given foreground and outputs it to
	 * the given result. Combining is done using the {@linkplain Colors#blend(int, int)}
	 * method.
	 * @param background The background
	 * @param foreground The foreground
	 * @param result The result
	 * @param format The pixel format*/
	// Both background and foreground have to have the same width and height
	public static final <T extends Buffer> void combine(T background, T foreground, T result,
			ImagePixelFormat<T> format) {
		if((background == null)) throw new NullPointerException("Invalid background array");
		if((foreground == null)) throw new NullPointerException("Invalid foreground array");
		if((result == null)) throw new NullPointerException("Invalid result array");
		if((background.capacity() != foreground.capacity()))
			throw new IllegalArgumentException("Invalid array sizes");
		if((background.capacity() != result.capacity()))
			throw new IllegalArgumentException("Invalid result array size");
		int epp = format.getElementsPerPixel();
		for(int i = 0, l = foreground.capacity(); i < l; i += epp) {
			// Always blend both colors, no conditions should be here,
			// internal checks are done in the blend method itself.
			format.setARGB(result, i, Colors.blend(format.getARGB(foreground, i),
			                                       format.getARGB(background, i)));
		}
	}
	
	/**
	 * Combines the given background and the given foreground and outputs it to
	 * the given result. Combining is done using the {@linkplain Colors#blend(int, int)}
	 * method.
	 * @param background The background
	 * @param foreground The foreground
	 * @param result The result
	 * @param format The pixel format*/
	public static final <T extends Buffer> void combine(Image background, Image foreground, T result,
			ImagePixelFormat<T> format) {
		if((background == null)) throw new NullPointerException("Invalid background image");
		if((foreground == null)) throw new NullPointerException("Invalid foreground image");
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> formatBGR = (ImagePixelFormat<T>) ImagePixelFormats.from(background);
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> formatFGR = (ImagePixelFormat<T>) ImagePixelFormats.from(foreground);
		if((format != formatBGR || format != formatFGR))
			throw new IllegalArgumentException("Invalid image pixel format");
		@SuppressWarnings("unchecked")
		T pixelsBGR = (T) getPixels(background);
		@SuppressWarnings("unchecked")
		T pixelsFGR = (T) getPixels(foreground);
		combine(pixelsBGR, pixelsFGR, result, format);
	}
	
	/**
	 * Combines the given background and the given foreground and outputs it to
	 * the background. Combining is done using the {@linkplain Colors#blend(int, int)}
	 * method.
	 * @param background The background
	 * @param foreground The foreground
	 * @param format The pixel format*/
	public static final <T extends Buffer> void combine(WritableImage background, Image foreground) {
		if((background == null)) throw new NullPointerException("Invalid background image");
		if((foreground == null)) throw new NullPointerException("Invalid foreground image");
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> formatBGR = (ImagePixelFormat<T>) ImagePixelFormats.from(background);
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> formatFGR = (ImagePixelFormat<T>) ImagePixelFormats.from(foreground);
		if(!formatBGR.equals(formatFGR))
			throw new IllegalArgumentException("Images do not have same image pixel format");
		@SuppressWarnings("unchecked")
		T pixelsBGR = (T) getPixels(background);
		@SuppressWarnings("unchecked")
		T pixelsFGR = (T) getPixels(foreground);
		combine(pixelsBGR, pixelsFGR, pixelsBGR, formatBGR);
	}
	
	/**
	 * Combines the given background and the given foreground and outputs it to
	 * the given buffer. The buffer must have the correct length. Combining is done
	 * using the {@linkplain Colors#blend(int, int)} method.
	 * @param background The background
	 * @param foreground The foreground
	 * @param buffer The output buffer*/
	public static final <T extends Buffer> void combine(Image background, Image foreground, T buffer) {
		if((background == null)) throw new NullPointerException("Invalid background image");
		if((foreground == null)) throw new NullPointerException("Invalid foreground image");
		int width  = (int) background.getWidth();
		int height = (int) background.getHeight();
		if((buffer.capacity() % (width * height) != 0))
			throw new NullPointerException("Incorrect size of an output buffer");
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> formatBGR = (ImagePixelFormat<T>) ImagePixelFormats.from(background);
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> formatFGR = (ImagePixelFormat<T>) ImagePixelFormats.from(foreground);
		if(!formatBGR.equals(formatFGR))
			throw new IllegalArgumentException("Images do not have same image pixel format");
		@SuppressWarnings("unchecked")
		T pixelsBGR = (T) getPixels(background);
		@SuppressWarnings("unchecked")
		T pixelsFGR = (T) getPixels(foreground);
		combine(pixelsBGR, pixelsFGR, buffer, formatBGR);
	}
	
	/**
	 * Combines the given background and the given foreground and outputs it to
	 * a new image. The image must have the same format as the foreground and
	 * the background image.
	 * Combining is done using the {@linkplain Colors#blend(int, int)} method.
	 * @param background The background
	 * @param foreground The foreground
	 * @param output The output image*/
	public static final <T extends Buffer> void combine(Image background, Image foreground, WritableImage output) {
		if((background == null)) throw new NullPointerException("Invalid background image");
		if((foreground == null)) throw new NullPointerException("Invalid foreground image");
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> formatBGR = (ImagePixelFormat<T>) ImagePixelFormats.from(background);
		@SuppressWarnings("unchecked")
		ImagePixelFormat<T> formatFGR = (ImagePixelFormat<T>) ImagePixelFormats.from(foreground);
		if(!formatBGR.equals(formatFGR))
			throw new IllegalArgumentException("Images do not have same image pixel format");
		@SuppressWarnings("unchecked")
		T pixelsBGR = (T) getPixels(background);
		@SuppressWarnings("unchecked")
		T pixelsFGR = (T) getPixels(foreground);
		@SuppressWarnings("unchecked")
		T pixelsOUT = (T) getPixels(output);
		combine(pixelsBGR, pixelsFGR, pixelsOUT, formatBGR);
	}
	
	// ----- IMAGE PLACE
	
	/**
	 * Places the given {@code placeImage} to the given image {@code image} at
	 * the given position.
	 * @param image The image
	 * @param placeImage The image that should be placed
	 * @param x The x-coordinate of the placement
	 * @param y The y-coordinate of the placement*/
	public static final void place(WritableImage image, Image placeImage, int x, int y) {
		if((image == null)) throw new NullPointerException("Invalid image");
		if((placeImage == null)) throw new NullPointerException("Invalid place image");
		int width  = (int) image.getWidth();
		int height = (int) image.getHeight();
		if((x < 0 || x >= width || y < 0 || y >= height))
			throw new IllegalArgumentException("Invalid position");
		int pwidth  = (int) placeImage.getWidth();
		int pheight = (int) placeImage.getHeight();
		image.getPixelWriter().setPixels(x, y, pwidth, pheight, placeImage.getPixelReader(), 0, 0);
	}
	
	// ----- OTHER UTILITIES
	
	/**
	 * Converts the given image to a writable image. If the given image is
	 * a writable image, then the image itself is returned, otherwise
	 * the given image is copied to a new writable image.
	 * @param image The image
	 * @return The writable version of the given image*/
	public static final WritableImage toWritable(Image image) {
		return image instanceof WritableImage ? (WritableImage) image : copy(image);
	}
}