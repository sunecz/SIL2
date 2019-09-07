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
 * </a><br>
 * getExponent, abs, scalb, hypot:
 * <a href="https://github.com/apache/commons-math/blob/master/src/main/java/org/apache/commons/math4/util/FastMath.java">
 *     https://github.com/apache/commons-math/blob/master/src/main/java/org/apache/commons/math4/util/FastMath.java
 * </a>
 * @version 1.0
 * @author Riven
 * @author jMonkeyEngine
 * @author fishrock123
 * @author Martin Ankerl
 * @author Apache
 * @author Sune
 * @see Math*/
public final class FastMath {
	
	// Constants
	/**
     * The {@code float} value that is closer than any other to
     * <i>pi</i>, the ratio of the circumference of a circle to its
     * diameter.*/
	public static final float PI = (float) StrictMath.PI;
	/**
     * The {@code float} value that is closer than any other to
     * <i>e</i>, the base of the natural logarithms.*/
	public static final float E  = (float) StrictMath.E;
	
	// Angles
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
	
	// Function: atan2
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
	
	// Function: sin, cos & tan
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
	
	// Function: floor, round & ceil
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
			TAN[(int) (i * TRG_degToIndex) & TRG_MASK] = (float) Math.tan(i * Math.PI / 180.0);
		}
	}
	
	static {
		initAtan2();
		initTrig();
	}
	
	// Forbid anyone to create an instance of this class
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
	
	/**
     * Returns the unbiased exponent used in the representation of a
     * {@code float}.  Special cases:
     *
     * <ul>
     * <li>If the argument is NaN or infinite, then the result is
     * {@link Float#MAX_EXPONENT} + 1.
     * <li>If the argument is zero or subnormal, then the result is
     * {@link Float#MIN_EXPONENT} -1.
     * </ul>
     * @param f a {@code float} value
     * @return the unbiased exponent of the argument
     * @see Math#getExponent(float)
     */
	public static final int getExponent(float f) {
		// NaN and Infinite will return the same exponent anywho so can use raw bits
		return ((Float.floatToRawIntBits(f) >>> 23) & 0xff) - 127;
	}
	
	/**
     * Returns the absolute value of a {@code float} value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     * Special cases:
     * <ul><li>If the argument is positive zero or negative zero, the
     * result is positive zero.
     * <li>If the argument is infinite, the result is positive infinity.
     * <li>If the argument is NaN, the result is NaN.</ul>
     *
     * @apiNote As implied by the above, one valid implementation of
     * this method is given by the expression below which computes a
     * {@code float} with the same exponent and significand as the
     * argument but with a guaranteed zero sign bit indicating a
     * positive value:<br>
     * {@code Float.intBitsToFloat(0x7fffffff & Float.floatToRawIntBits(a))}
     *
     * @param   a   the argument whose absolute value is to be determined
     * @return  the absolute value of the argument.
     * @see Math#abs(float)
     */
	public static final float abs(float a) {
		return Float.intBitsToFloat(0x7fffffff & Float.floatToRawIntBits(a));
	}
	
	/**
     * Returns {@code f} &times;
     * 2<sup>{@code scaleFactor}</sup> rounded as if performed
     * by a single correctly rounded floating-point multiply to a
     * member of the float value set.  See the Java
     * Language Specification for a discussion of floating-point
     * value sets.  If the exponent of the result is between {@link
     * Float#MIN_EXPONENT} and {@link Float#MAX_EXPONENT}, the
     * answer is calculated exactly.  If the exponent of the result
     * would be larger than {@code Float.MAX_EXPONENT}, an
     * infinity is returned.  Note that if the result is subnormal,
     * precision may be lost; that is, when {@code scalb(x, n)}
     * is subnormal, {@code scalb(scalb(x, n), -n)} may not equal
     * <i>x</i>.  When the result is non-NaN, the result has the same
     * sign as {@code f}.
     *
     * <p>Special cases:
     * <ul>
     * <li> If the first argument is NaN, NaN is returned.
     * <li> If the first argument is infinite, then an infinity of the
     * same sign is returned.
     * <li> If the first argument is zero, then a zero of the same
     * sign is returned.
     * </ul>
     *
     * @param f number to be scaled by a power of two.
     * @param scaleFactor power of 2 used to scale {@code f}
     * @return {@code f} &times; 2<sup>{@code scaleFactor}</sup>
     * @see Math#scalb(float, int)
     */
	public static final float scalb(float f, int scaleFactor) {
		if((scaleFactor > -127) && (scaleFactor < 128))
			return f * Float.intBitsToFloat((scaleFactor + 127) << 23);
		if((Float.isNaN(f) || Float.isInfinite(f) || (f == 0.0f)))
			return f;
		if((scaleFactor < -277))
			return (f > 0) ? 0.0f : -0.0f;
		if((scaleFactor > 276))
			return (f > 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
		final int bits = Float.floatToIntBits(f);
		final int sign = bits & 0x80000000;
		int exponent   = (bits >>> 23) & 0xff;
		int mantissa   = bits & 0x007fffff;
		int scaledExponent = exponent + scaleFactor;
		if((scaleFactor < 0)) {
			if((scaledExponent > 0))
				return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
			if((scaledExponent > -24)) {
				mantissa |= 1 << 23;
				final int mostSignificantLostBit = mantissa & (1 << (-scaledExponent));
				mantissa >>>= 1 - scaledExponent;
				if((mostSignificantLostBit != 0)) 
					mantissa++;
				return Float.intBitsToFloat(sign | mantissa);
			}
			return (sign == 0) ? 0.0f : -0.0f;
		}
		if((exponent == 0)) {
			while((mantissa >>> 23) != 1) {
				mantissa <<= 1;
				--scaledExponent;
			}
			++scaledExponent;
			mantissa &= 0x007fffff;
			if((scaledExponent < 255)) {
				return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
            }
			return (sign == 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }
		if((scaledExponent < 255))
			return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
		return (sign == 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
	}
	
	/**
     * Returns sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)
     * without intermediate overflow or underflow.
     *
     * <p>Special cases:
     * <ul>
     *
     * <li> If either argument is infinite, then the result
     * is positive infinity.
     *
     * <li> If either argument is NaN and neither argument is infinite,
     * then the result is NaN.
     *
     * </ul>
     *
     * <p>The computed result must be within 1 ulp of the exact
     * result.  If one parameter is held constant, the results must be
     * semi-monotonic in the other parameter.
     *
     * @param x a value
     * @param y a value
     * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)
     * without intermediate overflow or underflow
     * @see Math#hypot(double, double)
     */
	public static final float hypot(float x, float y) {
		if((Float.isInfinite(x) || Float.isInfinite(y)))
			return Float.POSITIVE_INFINITY;
		if((Float.isNaN(x) || Float.isNaN(y)))
			return Float.NaN;
		final int expX = getExponent(x);
		final int expY = getExponent(y);
		if((expX > expY + 27))
			return abs(x);
		if((expY > expX + 27))
			return abs(y);
		final int middleExp = (expX + expY) / 2;
		final float scaledX = scalb(x, -middleExp);
		final float scaledY = scalb(y, -middleExp);
		final float scaledH = sqrt(scaledX * scaledX + scaledY * scaledY);
		return scalb(scaledH, middleExp);
	}
}