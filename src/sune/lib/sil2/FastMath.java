package sune.lib.sil2;

/**
 * Collection of methods for faster mathematical operations. Most of the methods
 * in this class use floats instead of doubles, since float arithmetic should be faster.
 * This class is mostly used for computing image filters and adjustments, however,
 * they can be used for any given purpose where speed is crucial, such as graphics.
 * <br><br>
 * atan2:
 * <a href="http://www.java-gaming.org/index.php?topic=14647.0">
 *     http://www.java-gaming.org/index.php?topic=14647.0
 * </a><br>
 * sin:
 * <a href="http://www.java-gaming.org/index.php?topic=24191.0">
 *     http://www.java-gaming.org/index.php?topic=24191.0
 * </a><br>
 * cos:
 * <a href="http://www.java-gaming.org/index.php?topic=24191.0">
 *     http://www.java-gaming.org/index.php?topic=24191.0
 * </a><br>
 * tan:
 * <a href="http://www.java-gaming.org/index.php?topic=24191.0">
 *     http://www.java-gaming.org/index.php?topic=24191.0
 * </a><br>
 * isqrt:
 * <a href="https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/math/FastMath.java">
 *     https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/math/FastMath.java
 * </a><br>
 * sqrt:
 * <a href="https://github.com/Fishrock123/Optimized-Java/blob/master/src/com/fishrock123/math/RootMath.java">
 *     https://github.com/Fishrock123/Optimized-Java/blob/master/src/com/fishrock123/math/RootMath.java
 * </a><br>
 * pow:
 * <a href="http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/">
 *     http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
 * </a><br>
 * exp:
 * <a href="http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/">
 *     http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
 * </a><br>
 * floor:
 * <a href="http://www.java-gaming.org/index.php?topic=24194.0">
 *     http://www.java-gaming.org/index.php?topic=24194.0
 * </a><br>
 * round:
 * <a href="http://www.java-gaming.org/index.php?topic=24194.0">
 *     http://www.java-gaming.org/index.php?topic=24194.0
 * </a><br>
 * ceil:
 * <a href="http://www.java-gaming.org/index.php?topic=24194.0">
 *     http://www.java-gaming.org/index.php?topic=24194.0
 * </a>
 * @version 1.0.0
 * @author Riven
 * @author jMonkeyEngine
 * @author fishrock123
 * @author Martin Ankerl
 * @author Petr Cipra
 * @see Math*/
public final class FastMath {
	
	// constants
	/**
     * The {@code float} value that is closer than any other to
     * <i>pi</i>, the ratio of the circumference of a circle to its
     * diameter.*/
	public static final float PI = (float) StrictMath.PI;
	/**
     * The {@code float} value that is closer than any other to
     * <i>e</i>, the base of the natural logarithms.*/
	public static final float E  = (float) StrictMath.E;
	
	// angles
	/**
	 * Represents the value of angle {@code 360deg} in radians, that is {@code 2.0*PI}.*/
	public static final float ANGLE_360 = 2.0f * PI;
	/**
	 * Represents the value of angle {@code 270deg} in radians, that is {@code 1.5*PI}.*/
	public static final float ANGLE_270 = 1.5f * PI;
	/**
	 * Represents the value of angle {@code 180deg} in radians, that is {@code 1.0*PI}.*/
	public static final float ANGLE_180 = 1.0f * PI;
	/**
	 * Represents the value of angle {@code 90deg} in radians, that is {@code 0.5*PI}.*/
	public static final float ANGLE_90  = 0.5f * PI;
	
	// atan2
	private static final int     SIZE            = 1024;
	private static final int     EZIS            = -SIZE;
	private static final float   STRETCH         = PI;
	private static final float[] ATAN2_TABLE_PPY = new float[SIZE + 1];
	private static final float[] ATAN2_TABLE_PPX = new float[SIZE + 1];
	private static final float[] ATAN2_TABLE_PNY = new float[SIZE + 1];
	private static final float[] ATAN2_TABLE_PNX = new float[SIZE + 1];
	private static final float[] ATAN2_TABLE_NPY = new float[SIZE + 1];
	private static final float[] ATAN2_TABLE_NPX = new float[SIZE + 1];
	private static final float[] ATAN2_TABLE_NNY = new float[SIZE + 1];
	private static final float[] ATAN2_TABLE_NNX = new float[SIZE + 1];
	
	// sin, cos & tan
	private static final int     TRG_BITS       = 12;
	private static final int     TRG_MASK       = ~(-1 << TRG_BITS);
	private static final int     TRG_COUNT      = TRG_MASK + 1;
	private static final float   TRG_radFull    = PI * 2.0f;
	private static final float   TRG_radToIndex = TRG_COUNT / TRG_radFull;
	private static final float   TRG_degFull    = 360.0f;
	private static final float   TRG_degToIndex = TRG_COUNT / TRG_degFull;
	private static final float[] SIN            = new float[TRG_COUNT];
	private static final float[] COS            = new float[TRG_COUNT];
	private static final float[] TAN            = new float[TRG_COUNT];
	
	// floor, round & ceil
	private static final int   BIG_ENOUGH_INT   = 16 * 1024;
	private static final float BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
	private static final float BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5f;
	
	private static final void initAtan2() {
		for(int i = 0; i <= SIZE; ++i) {
    		float f = (float) i / SIZE;
    		ATAN2_TABLE_PPY[i] = (float) (StrictMath.atan(f) * STRETCH / StrictMath.PI);
    		ATAN2_TABLE_PPX[i] = STRETCH * 0.5f - ATAN2_TABLE_PPY[i];
    		ATAN2_TABLE_PNY[i] = -ATAN2_TABLE_PPY[i];
    		ATAN2_TABLE_PNX[i] = ATAN2_TABLE_PPY[i] - STRETCH * 0.5f;
    		ATAN2_TABLE_NPY[i] = STRETCH - ATAN2_TABLE_PPY[i];
    		ATAN2_TABLE_NPX[i] = ATAN2_TABLE_PPY[i] + STRETCH * 0.5f;
    		ATAN2_TABLE_NNY[i] = ATAN2_TABLE_PPY[i] - STRETCH;
    		ATAN2_TABLE_NNX[i] = -STRETCH * 0.5f - ATAN2_TABLE_PPY[i];
        }
	}
	
	private static final void initTrig() {
		for(int i = 0; i < TRG_COUNT; ++i) {
			SIN[i] = (float) Math.sin((i + 0.5f) / TRG_COUNT * TRG_radFull);
			COS[i] = (float) Math.cos((i + 0.5f) / TRG_COUNT * TRG_radFull);
			TAN[i] = (float) Math.tan((i + 0.5f) / TRG_COUNT * TRG_radFull);
		}
		for(int i = 0; i < 360; i += 90) {
			SIN[(int) (i * TRG_degToIndex) & TRG_MASK] = (float) Math.sin(i * Math.PI / 180.0);
			COS[(int) (i * TRG_degToIndex) & TRG_MASK] = (float) Math.cos(i * Math.PI / 180.0);
			TAN[(int) (i * TRG_degToIndex) & TRG_MASK] = (float) Math.cos(i * Math.PI / 180.0);
		}
	}
	
	static {
		initAtan2();
		initTrig();
	}
	
	// forbid anyone to create an instance of this class
	private FastMath() {
	}
	
	/**
	 * Returns the angle <i>theta</i> from the conversion of rectangular
     * coordinates ({@code x}, {@code y}) to polar coordinates (r, <i>theta</i>).
     * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
     * @param y the y coordinate
     * @param x the x coordinate
     * @see Math#atan2(double, double)
     * @return the <i>theta</i> component of the point (<i>r</i>,&nbsp;<i>theta</i>)
     * in polar coordinates that corresponds to the point (<i>x</i>,&nbsp;<i>y</i>)
     * in Cartesian coordinates.*/
	public static final float atan2(float y, float x) {
		if((x >= 0)) {
			if((y >= 0)) {
				if((x >= y)) return ATAN2_TABLE_PPY[(int) (SIZE * y / x + 0.5)];
				else         return ATAN2_TABLE_PPX[(int) (SIZE * x / y + 0.5)];
			} else {
				if((x >= -y)) return ATAN2_TABLE_PNY[(int) (EZIS * y / x + 0.5)];
				else          return ATAN2_TABLE_PNX[(int) (EZIS * x / y + 0.5)];
			}
		} else {
			if((y >= 0)) {
				if((-x >= y)) return ATAN2_TABLE_NPY[(int) (EZIS * y / x + 0.5)];
				else          return ATAN2_TABLE_NPX[(int) (EZIS * x / y + 0.5)];
			} else {
				if((x <= y)) return ATAN2_TABLE_NNY[(int) (SIZE * y / x + 0.5)];
				else         return ATAN2_TABLE_NNX[(int) (SIZE * x / y + 0.5)];
			}
		}
	}
	
	/**
	 * Returns the trigonometric sine of an angle.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param rad the angle in radians
	 * @return the sine of the argument.
	 * @see Math#sin(double)*/
	public static final float sin(float rad) {
		return SIN[(int) (rad * TRG_radToIndex) & TRG_MASK];
	}
	
	/**
	 * Returns the trigonometric cosine of an angle.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param rad the angle in radians
	 * @return the cosine of the argument.
	 * @see Math#cos(double)*/
	public static final float cos(float rad) {
		return COS[(int) (rad * TRG_radToIndex) & TRG_MASK];
	}
	
	/**
	 * Returns the trigonometric tangent of an angle.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param rad the angle in radians
	 * @return the tangent of the argument.
	 * @see Math#tan(double)*/
	public static final float tan(float rad) {
		return TAN[(int) (rad * TRG_radToIndex) & TRG_MASK];
	}
	
	/**
	 * Returns the trigonometric sine of an angle.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param deg the angle in degrees
	 * @return the sine of the argument.
	 * @see Math#sin(double)
	 * @see Math#toDegrees(double)*/
	public static final float sinDeg(float deg) {
		return SIN[(int) (deg * TRG_degToIndex) & TRG_MASK];
	}
	
	/**
	 * Returns the trigonometric cosine of an angle.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param deg the angle in degrees
	 * @return the cosine of the argument.
	 * @see Math#cos(double)
	 * @see Math#toDegrees(double)*/
	public static final float cosDeg(float deg) {
		return COS[(int) (deg * TRG_degToIndex) & TRG_MASK];
	}
	
	/**
	 * Returns the trigonometric tangent of an angle.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param deg the angle in degrees
	 * @return the tangent of the argument.
	 * @see Math#tan(double)
	 * @see Math#toDegrees(double)*/
	public static final float tanDeg(float deg) {
		return TAN[(int) (deg * TRG_degToIndex) & TRG_MASK];
	}
	
	/**
	 * Returns the inverse of rounded positive square root of a {@code float} value.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
     * @param x the value
     * @return the inverse of rounded positive square root of {@code x}.
	 * @see Math#sqrt(double)*/
	public static final float isqrt(float x) {
		float xhalf = 0.5f * x;
		int i = Float.floatToRawIntBits(x);
		i = 0x5f375a86 - (i >> 1);
		x = Float.intBitsToFloat(i);
		x = x * (1.5f - xhalf * x * x);
		return x;
	}
	
	/**
	 * Returns the rounded positive square root of a {@code float} value.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
     * @param x the value
     * @return the positive square root of {@code x}.
	 * @see Math#sqrt(double)*/
	public static final float sqrt(float x) {
		float xhalf = 0.5f * x;
		int i = Float.floatToRawIntBits(x);
		i = 0x5f375a86 - (i >> 1);
		float y = Float.intBitsToFloat(i);
		y = y * (1.5f - xhalf * y * y);
		y = y * (1.5f - xhalf * y * y);
		return x * y;
	}
	
	/**
	 * Returns the value of the first argument raised to the power of the
     * second argument.
     * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
     * @param a the base
     * @param b the exponent
     * @return the value {@code a}<sup>{@code b}</sup>.
	 * @see Math#pow(double, double)*/
	public static final float pow(float a, float b) {
		return (float) Double.longBitsToDouble(
			(long) (b * (Double.doubleToLongBits(a) - 4606921280493453312L))
				+ 4606921280493453312L);
	}
	
	/**
	 * Returns Euler's number <i>e</i> raised to the power of a {@code float} value.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param val the exponent to raise <i>e</i> to
	 * @return the value <i>e</i><sup>{@code val}</sup>, where <i>e</i> is the base
	 * of the natural logarithms.
	 * @see Math#exp(double)*/
	public static final float exp(float val) {
		return (float) Double.longBitsToDouble(
			((long) (1512775 * val + 1072632447)) << 32);
	}
	
	/**
	 * Returns the largest (closest to positive infinity) {@code float} value
	 * that is less than or equal to the argument and is equal to a mathematical
	 * integer.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param x the value
	 * @return the largest (closest to positive infinity) floating-point value
	 * that is less than or equal to the argument and is equal to a mathematical integer.
	 * @see Math#floor(double)*/
	public static final int floor(float x) {
		return (int) (x + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
	}
	
	/**
	 * Returns the closest {@code long} to the argument, with ties rounding to
	 * positive infinity.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param x the value
	 * @return the value of the argument rounded to the nearest {@code long} value.
	 * @see Math#round(double)*/
	public static final int round(float x) {
		return (int) (x + BIG_ENOUGH_ROUND) - BIG_ENOUGH_INT;
	}
	
	/**
	 * Returns the smallest (closest to negative infinity) {@code float} value
	 * that is greater than or equal to the argument and is equal to a mathematical
	 * integer.
	 * <br><br>
     * <i>Note: Since this method is focused on speed, special cases and results
     * do not have to match the ones in the Math class.</i>
	 * @param x the value
	 * @return the smallest (closest to negative infinity) floating-point value
	 * that is greater than or equal to the argument and is equal to a mathematical integer.
	 * @see Math#ceil(double)*/
	public static final int ceil(float x) {
		return BIG_ENOUGH_INT - (int) (BIG_ENOUGH_FLOOR - x);
	}
	
	/**
	 * Multiplies the given number by {@code 255}.
	 * No overflow and range checks are performed.
	 * @param val the number in range {@code <0, 8421504>}
	 * @return {@code val * 255}*/
	public static final int mul255(int val) {
		return (val << 8) - val;
	}
	
	/**
	 * Divides the given number by {@code 255}.
	 * No overflow and range checks are performed.
	 * @param val the number in range {@code <0, 65789>}
	 * @return {@code val / 255}*/
	public static final int div255(int val) {
		return ((val + 1) + (val >> 8)) >> 8;
	}
}