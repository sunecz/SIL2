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
		
		private final float rad;
		private final float distX;
		private final float distY;
		private final int color;
		
		public Shadow2D(float rad, float distX, float distY, int color) {
			this.rad   = rad;
			this.distX = distX;
			this.distY = distY;
			this.color = color;
		}
		
		@Override
		public final Void execute(IImageContext<T> context) {
			int stride = context.getStride();
			int sx = context.getX(), ex = sx + context.getWidth();
			int sy = context.getY(), ey = sy + context.getHeight();
			ImagePixelFormat<T> format = context.getPixelFormat();
			T pixels = context.getPixels();
			T buffer = context.getBuffer();
			int epp = format.getElementsPerPixel();
			BufferUtils.fill(buffer, 0x0, epp);
			// Produce the image's shadow of transparency and store it in output
			float dx = FastMath.cos(rad) * distX, fx;
			float dy = FastMath.sin(rad) * distY, fy;
			for(int x = sx, y = sy, i = (y * stride + x) * epp, d = (stride - (ex - sx)) * epp;; i += epp) {
				fx = x + dx;
				fy = y + dy;
				if((fx >= sx && fx < ex) && (fy >= sy && fy < ey)) {
					if((format.getARGB(pixels, i) >>> 24) != 0x0) {
						format.setARGB(buffer, ((int) fy * stride + (int) fx) * epp, color);
					}
				}
				if((++x == ex)) {
					 x  = sx;
					 i += d;
					 if((++y == ey))
						 break;
				}
			}
			// Combine the image's pixels and the shadow's pixels (with blending)
			for(int x = sx, y = sy, i = (y * stride + x) * epp, d = (stride - (ex - sx)) * epp;; i += epp) {
				format.setARGB(pixels, i, Colors.blend(format.getARGB(pixels, i), format.getARGB(buffer, i)));
				if((++x == ex)) {
					 x  = sx;
					 i += d;
					 if((++y == ey))
						 break;
				}
			}
			return null;
		}
	}
}