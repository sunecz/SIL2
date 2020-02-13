package sune.lib.sil2;

import java.io.PrintStream;
import java.util.Locale;

public final class MatrixUtils {
	
	@FunctionalInterface
	static interface QuadriConsumer<A, B, C, D> {
		void accept(A a, B b, C c, D d);
	}
	
	static final int min(int x, int y) {
		return y - (((x - y) >>> 31) & 1) * (y - x);
	}
	
	static final int max(int x, int y) {
		return x - (((x - y) >>> 31) & 1) * (x - y);
	}
	
	// Forbid anyone to create an instance of this class
	private MatrixUtils() {
	}
	
	/**
	 * Utility class for rotating matricies.
	 * @author Sune
	 */
	public static final class MatrixRotator {
		
		private static final float PI = (float) StrictMath.PI;
		private static final float ANGLE_360 = 2.0f * PI;
		
		private static final QuadriConsumer<Integer, float[], Integer, float[]>
			CALLBACK_UNFOLD = ((mi, ring, ri, matrix) -> ring[ri] = matrix[mi]);
		private static final QuadriConsumer<Integer, float[], Integer, float[]>
			CALLBACK_FOLD   = ((mi, ring, ri, matrix) -> matrix[mi] = ring[ri]);
		
		private final float[] matrix;
		private final float[][] rings;
		private final float[] temp;
		
		public MatrixRotator(float[] matrix) {
			this.matrix = matrix;
			int side = matrixSide(matrix.length);
			this.rings = createRings(side);
			this.temp  = new float[ringLength(side, 0)];
		}
		
		private static final int ringLength(int side, int ring) {
			return max(1, ((side - (ring << 1)) + (side - ((ring + 1) << 1))) << 1);
		}
		
		private static final float[][] createRings(int side) {
			float[][] rings = new float[(side >> 1) + 1][];
			for(int i = 0, l = rings.length; i < l; ++i)
				rings[i] = new float[ringLength(side, i)];
			return rings;
		}
		
		/**
		 * Computes the index of a matrix ring given by the index of a point
		 * in its diagonal line and that diagonal's length. The {@code maxd}
		 * argument is simply {@code side << 1} and is used just not to calculate
		 * that value over and over.
		 * @param i The index of a point in its diagonal line
		 * @param maxd The maximum depth of the diagonal line ({@code side << 1})
		 * @param side The length of the diagonal line
		 * @return The index of a matrix ring where the point lies.
		 */
		private static final int diagonalRing(int i, int maxd, int side) {
			// (i < m ? 0 : 2 * (m - i)) + i
			return (((maxd - i) >>> 31) & 1) * (((maxd - i) << 1) - (1 - (side & 1))) + i
						// if side is even: i == maxd ? -1 : 0
						- (1 - ((((maxd - i) >>> 31) & 1) + (((i - maxd) >>> 31) & 1))) * (1 - (side & 1));
		}
		
		/**
		 * Computes the index of a point {@code [x, y]} in its diagonal line.
		 * @param x The x-coordinate of a point
		 * @param y The y-coordinate of a point
		 * @param side The side length of a matrix
		 * @return The index of a point in its diagonal line.
		 */
		private static final int diagonalIdx(int x, int y, int side) {
			// (y > side - x - 1 ? 1 : 0) * (side - y - 1 - x) + x
			return (((side - 1 - x - y) >>> 31) & 1) * (side - y - 1 - x) + x;
		}
		
		/**
		 * Computes the length of a diagonal line where a point {@code [x, y]} lies.
		 * @param x The x-coordinate of a point
		 * @param y The y-coordinate of a point
		 * @param side The side length of a matrix
		 * @return The length of a diagonal line.
		 */
		private static final int diagonalLen(int x, int y, int side) {
			// (y > side - x - 1 ? 1 : 0) * (2 * (side - 1 - (x + y))) + (x + y)
			return (((side - 1 - x - y) >>> 31) & 1) * ((side - 1 - (x + y)) << 1) + (x + y);
		}
		
		private static final int matrixSide(int length) {
			// Must use Math.sqrt due to arithmetic accuracy
			return (int) Math.sqrt(length);
		}
		
		private static final float lerp(float x, float y, float t) {
			return t * y + (1.0f - t) * x;
		}
		
		private static final int index(int i, int l) {
			return (((i >>> 31) & 1) * l + i) % l;
		}
		
		private final void ringMatrixLoop(QuadriConsumer<Integer, float[], Integer, float[]> callback) {
			int side = matrixSide(matrix.length);
			// Prepare auxiliary array for computing inner ring index
			int[] idxs = new int[((side >> 1) + 1) << 1];
			for(int i = 1, l = idxs.length; i < l; i += 2)
				idxs[i] = ringLength(side, i >> 1) - 1;
			// Fold one-dimensional rings to the matrix
			for(int i = 0, x = 0, y = 0;; ++i) {
				// Calculate index of current position's ring
				int dlen = diagonalLen(x, y, side) + 1;
				int didx = diagonalIdx(x, y, side);
				int ring = diagonalRing(didx, dlen >> 1, dlen);
				// Calculate index of a ring index counter
				int ridx = (ring << 1) + (((x - y) >>> 31) & 1);
				// Calculate decrement for current index
				// Top    row and right column -> +1
				// Bottom row and left  column -> -1
				int rdec = ((((x - y) >>> 31) & 1) << 1) - 1;
				callback.accept(i, rings[ring], idxs[ridx], matrix);
				idxs[ridx] -= rdec;
				if((++x == side)) {
					x = 0;
					if((++y == side))
						break;
				}
			}
		}
		
		private final void rotate(float[] ring, float rad) {
			int len = ring.length;
			System.arraycopy(ring, 0, temp, 0, len);
			float stp = (ANGLE_360 / len);
			float rat = (Math.abs(rad) % stp) / stp;
			int inc = rad < 0.0f ? 1 : -1;
			int shf = (int) (rad / stp);
			for(int i = 0; i < len; ++i)
				ring[index(i+shf, len)] = lerp(temp[i], temp[index(i+inc, len)], rat);
		}
		
		public final void unfold() {
			ringMatrixLoop(CALLBACK_UNFOLD);
		}
		
		public final void fold() {
			ringMatrixLoop(CALLBACK_FOLD);
		}
		
		public final void rotate(float rad) {
			for(int i = 0, l = rings.length; i < l; ++i)
				rotate(rings[i], rad);
		}
	}
	
	public static final void rotate(float[] matrix, float rad) {
		MatrixRotator rotator = new MatrixRotator(matrix);
		rotator.unfold();
		rotator.rotate(rad);
		rotator.fold();
	}
	
	public static final void printMatrix(float[] matrix) {
		printMatrix(matrix, System.out);
	}
	
	public static final void printMatrix(float[] matrix, PrintStream printer) {
		printer.println("[");
		// Must use Math.sqrt due to arithmetic accuracy
		int side = (int) Math.sqrt(matrix.length);
		for(int i = 0, x = side, y = side;; ++i) {
			if((x == side)) printer.print("\t");
			else            printer.print(", ");
			printer.printf(Locale.US, "%+.5f", matrix[i]);
			if((--x == 0)) {
				x = side;
				if((--y == 0)) {
					printer.println();
					break;
				}
				printer.println(",");
			}
		}
		printer.println("]");
	}
}