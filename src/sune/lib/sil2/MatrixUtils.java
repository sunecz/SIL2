package sune.lib.sil2;

import java.util.Locale;

public final class MatrixUtils {
	
	private static final class MatrixRing {
		
		public final float[] values;
		public int index;
		
		public MatrixRing(float[] values) {
			this.values = values;
			this.index = 0;
		}
	}
	
	// Forbid anyone to create an instance of this class
	private MatrixUtils() {
	}
	
	public static final void rotate(float[] matrix, float angle) {
		int side = (int) FastMath.sqrt(matrix.length) + 1;
		int maxDepth = side / 2;
		// 1) Prepare rings to a one-dimensional arrays
		MatrixRing[] rings = new MatrixRing[maxDepth+1];
		for(int i = 0, x = 0, y = 0;; ++i) {
			int _x = x > maxDepth ? maxDepth + maxDepth - x : x;
			int _y = y > maxDepth ? maxDepth + maxDepth - y : y;
			int ring = Math.min(_x, _y);
			MatrixRing _ring = rings[ring];
			if((_ring == null)) {
				int ringSide = side - 2 * ring;
				int len = Math.max(1, 2 * ringSide + 2 * (ringSide - 2));
				_ring = rings[ring] = new MatrixRing(new float[len]);
			}
			_ring.values[_ring.index++] = matrix[i];
			if((++x == side)) {
				x = 0;
				if((++y == side))
					break;
			}
		}
		// 2) Rotate individual rings
		for(MatrixRing ring : rings) {
			int len = ring.values.length;
			float[] newValues = new float[len];
			float step = FastMath.ANGLE_360 / len;
			// Negative because of rotation (+ is CCW, - CW)
			int shift = (int) (-angle / step);
			float ratio = (angle % step) / step;
			// Rotate individual values
			for(int i = 0; i < len; ++i) {
				float value = ring.values[i];
				int k = i + shift;
				k = k < 0 ? len + k : k >= len ? k % len : k;
				// TODO: Better interpolation
				if((ratio != 0.0f)) {
					int n = k + (shift >= 0 ? -1 : 1);
					n = n < 0 ? len + n : n >= len ? n % len : n;
					float valueN = ring.values[n];
					value = (1.0f - ratio) * value + ratio * valueN;
				}
				newValues[k] = value;
			}
			System.arraycopy(newValues, 0, ring.values, 0, len);
		}
		// 3) Put the rings back together into a matrix
		for(int i = 0, l = rings.length; i < l; ++i) {
			MatrixRing ring = rings[i];
			for(int k = 0, x = i, y = i, e = side - i;; ++k) {
				int n = y * side + x;
				matrix[n] = ring.values[k];
				// Left most column of the ring
				if((y != i && y != e -1 && x == i)) {
					x = side - i - 2;
				}
				if((++x == e)) {
					x = i;
					if((++y == e))
						break;
				}
			}
		}
	}
	
	public static final void printMatrix(float[] matrix) {
		System.out.println("[");
		int side = (int) FastMath.sqrt(matrix.length) + 1;
		for(int i = 0, x = side, y = side;; ++i) {
			if((x == side)) System.out.print("\t");
			else            System.out.print(", ");
			System.out.printf(Locale.US, "%+.5f", matrix[i]);
			if((--x == 0)) {
				x = side;
				if((--y == 0)) {
					System.out.println();
					break;
				}
				System.out.println(",");
			}
		}
		System.out.println("]");
	}
	
	public static void main(String[] args) {
		final float[] matrix = {
			-2.0f, -1.0f,  0.0f,
			-1.0f,  1.0f,  1.0f,
			 0.0f,  1.0f,  2.0f
		};
		/*final float[] matrix = {
		     0.1f,  0.1f,  0.1f, 0.1f, 0.1f,
		     0.1f, -2.0f, -1.0f, 0.0f, 0.1f,
		     0.1f, -1.0f,  1.0f, 1.0f, 0.1f,
		     0.1f,  0.0f,  1.0f, 2.0f, 0.1f,
		     0.1f,  0.1f,  0.1f, 0.1f, 0.1f,
		};*/
		float angle = (float) Math.toRadians(1.0f);
		MatrixUtils.rotate(matrix, angle);
		printMatrix(matrix);
	}
}