package sune.lib.sil2;

import java.nio.Buffer;

/**
 * Collection of methods used for pixels operations.*/
public final class Pixels {
	
	// Forbid anyone to create an instance of this class
	private Pixels() {
	}
	
	/**
	 * Copies a region of pixels defined by {@code srcx}, {@code srcy}, {@code width},
	 * and {@code height} from the {@code src} pixels array to the region of pixels defined
	 * by {@code destx}, {@code desty}, {@code width}, and {@code height}
	 * in the {@code dest} pixels array. Pixels are copied exactly without modifications.
	 * <br>
	 * Note that this method was made for speed, so any arguments checking is omitted.
	 * Therefore any additional arguments checking has to be done by the user.
	 * @param src source pixels array
	 * @param srcx source region's x coordinate
	 * @param srcy source region's y coordinate
	 * @param srcStride the distance between the first pixel of a row and the pixel
	 * on the next row (aka the width of the source image where the pixels are copied from)
	 * @param dst destination pixels array
	 * @param dstx destination region's x coordinate
	 * @param dsty destination region's y coordinate
	 * @param dstStride the distance between the first pixel of a row and the pixel
	 * on the next row (aka the width of the destination image where the pixels are copied to)
	 * @param width region's width
	 * @param height region's height
	 * @param epp The number of elements per pixel*/
	public static final void copy(Buffer src, int srcx, int srcy, int srcStride, Buffer dst, int dstx,
			int dsty, int dstStride, int width, int height, int epp) {
		// Since the width is always the same and since the pixels are stored
		// in the consecutive order from left to right from top to bottom,
		// we can use System.arraycopy to make the copying faster.
		// Note that it is not true that System.arraycopy is always faster,
		// in cases where there is the need to copy just a small portion
		// of an image, it can be faster to just use a good old loop. However,
		// in those scenarios the penalty is so low it can be ignored.
		Object asrc = src.array(), adst = dst.array();
  		for(int isrc = srcy  * srcStride + srcx * epp,
  				idst = dsty  * dstStride + dstx * epp,
  				inum = width * epp;
  				height-- != 0;
  				isrc += srcStride,
  				idst += dstStride) {
  			System.arraycopy(asrc, isrc, adst, idst, inum);
  		}
	}
}