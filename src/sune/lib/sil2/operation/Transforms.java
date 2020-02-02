package sune.lib.sil2.operation;

import java.nio.Buffer;

import sune.lib.sil2.FastMath;
import sune.lib.sil2.IImageContext;
import sune.lib.sil2.IImageOperation;
import sune.lib.sil2.format.ImagePixelFormat;

public final class Transforms {
	
	// TODO: Update JavaDoc
	
	// Forbid anyone to create an instance of this class
	private Transforms() {
	}
	
	// https://homepages.inf.ed.ac.uk/rbf/HIPR2/flatjavasrc/Hough.java
	public static final class Hough<T extends Buffer> implements IImageOperation<T, int[]> {
		
		private final int thetaAxisSize;
		private final int radiusAxisSize;
		private final int minContrast;
		
		public Hough(int minContrast) {
			this(0, 0, minContrast);
		}
		
		public Hough(int thetaAxisSize, int radiusAxisSize, int minContrast) {
			this.thetaAxisSize = thetaAxisSize;
			this.radiusAxisSize = radiusAxisSize;
			this.minContrast = minContrast;
		}
		
		@Override
		public final int[] execute(IImageContext<T> context) {
			int stride = context.getStride();
			int sx = context.getX(), ex = sx + context.getWidth();
			int sy = context.getY(), ey = sy + context.getHeight();
			int width = context.getWidth();
			int height = context.getHeight();
			ImagePixelFormat<T> format = context.getPixelFormat();
			int thetaAxisSize = this.thetaAxisSize <= 0 ? width : this.thetaAxisSize;
			int radiusAxisSize = this.radiusAxisSize <= 0 ? height : this.radiusAxisSize;
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
			context.applyActionINT((input, output, i, varStore) -> {
				boolean canApply = false;
				int x = i % stride;
				int y = i / stride;
				int val = format.getARGB(input, i);
				for(int k = 8; k >= 0; --k) {
					if((k == 4)) continue;
					int newx = x + (i % 3) - 1;
					int newy = y + (i / 3) - 1;
					if((newx < sx || newx >= ex || newy < sy || newy >= ey))
						continue;
					int pxv = format.getARGB(input, (newy * stride + newx) * epp);
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
}