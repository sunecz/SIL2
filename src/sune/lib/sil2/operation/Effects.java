package sune.lib.sil2.operation;

import java.nio.Buffer;

import sune.lib.sil2.BufferUtils;
import sune.lib.sil2.Colors;
import sune.lib.sil2.FastMath;
import sune.lib.sil2.IImageContext;
import sune.lib.sil2.IImageOperation;
import sune.lib.sil2.format.ImagePixelFormat;

public final class Effects {
	
	// TODO: Update JavaDoc
	
	// Forbid anyone to create an instance of this class
	private Effects() {
	}
	
	/**
	 * Applies shadow effect to {@code this} image. The shadow is created in
	 * the given angle in distance of the given position and its color is that
	 * of the given color.
	 * @param angle The angle, in radians
	 * @param distX The x-coordinate of the distance
	 * @param distY The y-coordinate of the distance
	 * @param color The color, as an ARGB int*/
	public static final class Shadow2D<T extends Buffer> implements IImageOperation<T, Void> {
		
		private final float angle;
		private final float distX;
		private final float distY;
		private final int color;
		
		public Shadow2D(float angle, float distX, float distY, int color) {
			this.angle = angle;
			this.distX = distX;
			this.distY = distY;
			this.color = color;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			int width = context.getWidth();
			int height = context.getHeight();
			ImagePixelFormat<T> format = context.getPixelFormat();
			T pixels = context.getPixels();
			T buffer = context.getBuffer();
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
			return null;
		}
	}
}