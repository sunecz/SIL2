package sune.lib.sil2;

import java.nio.Buffer;

/**
 * Provides fast blurring of image pixels. It offers two most used blurs:
 * Box blur and Gaussian blur. Note that none of these are computed exactly;
 * these methods are made for speed, not for accuracy.<br><br>
 * @version 1.0
 * @author Ivan Kutskir
 * @author Sune
 * @see
 * <a href="http://blog.ivank.net/fastest-gaussian-blur.html">
 * 	http://blog.ivank.net/fastest-gaussian-blur.html
 * </a>
 */
public final class FastBlur {
	
	// Forbid anyone to create an instance of this class
	private FastBlur() {
	}
	
	public static final <T extends Buffer> void gaussianBlur(T input, T output, int x, int y, int w, int h, int r, int s,
			InternalChannels<T> channels, boolean premultiply) {
		final CounterLock lock   = new CounterLock(4);
		final int         length = input.capacity();
		final float[]     boxes  = generateBoxes(r, 3);
		final byte[] dstR = new byte[length];
		final byte[] dstG = new byte[length];
		final byte[] dstB = new byte[length];
		final byte[] dstA = new byte[length];
		final byte[] srcR = new byte[length];
		final byte[] srcG = new byte[length];
		final byte[] srcB = new byte[length];
		final byte[] srcA = new byte[length];
		channels.separate(input, srcR, srcG, srcB, srcA, false);
		Threads.execute(() -> {
			gaussianBlur(srcR, dstR, x, y, w, h, r, s, boxes);
			lock.decrement();
		});
		Threads.execute(() -> {
			gaussianBlur(srcG, dstG, x, y, w, h, r, s, boxes);
			lock.decrement();
		});
		Threads.execute(() -> {
			gaussianBlur(srcB, dstB, x, y, w, h, r, s, boxes);
			lock.decrement();
		});
		Threads.execute(() -> {
			gaussianBlur(srcA, dstA, x, y, w, h, r, s, boxes);
			lock.decrement();
		});
		lock.await();
		channels.join(dstR, dstG, dstB, dstA, output, premultiply);
	}
	
	public static final <T extends Buffer> void boxBlur(T input, T output, int x, int y, int w, int h, int r, int s,
			InternalChannels<T> channels, boolean premultiply) {
		final CounterLock lock   = new CounterLock(4);
		final int         length = input.capacity();
		final byte[] dstR = new byte[length];
		final byte[] dstG = new byte[length];
		final byte[] dstB = new byte[length];
		final byte[] dstA = new byte[length];
		final byte[] srcR = new byte[length];
		final byte[] srcG = new byte[length];
		final byte[] srcB = new byte[length];
		final byte[] srcA = new byte[length];
		channels.separate(input, srcR, srcG, srcB, srcA, false);
		Threads.execute(() -> {
			boxBlur(srcR, dstR, x, y, w, h, r, s);
			lock.decrement();
		});
		Threads.execute(() -> {
			boxBlur(srcG, dstG, x, y, w, h, r, s);
			lock.decrement();
		});
		Threads.execute(() -> {
			boxBlur(srcB, dstB, x, y, w, h, r, s);
			lock.decrement();
		});
		Threads.execute(() -> {
			boxBlur(srcA, dstA, x, y, w, h, r, s);
			lock.decrement();
		});
		lock.await();
		channels.join(dstR, dstG, dstB, dstA, output, premultiply);
	}
	
	private static final float[] generateBoxes(int sigma, int amount) {
		int sg = 12*sigma*sigma;
		int k0 = (int) Math.sqrt(sg / amount + 1);
		if((k0 & 1) == 0) --k0;
		int k1 = k0+2;
		int mk = (int) Math.round((sg-amount*k0*k0-4*amount*k0-3*amount)/(-4*k0-4));
		float[] sizes = new float[amount];
		for(int i = 0; i < amount; ++i)
			sizes[i] = i < mk ? k0 : k1;
		return sizes;
	}
	
	private static final void gaussianBlur(byte[] input, byte[] output, int x, int y, int w, int h,
			int r, int s, float[] bxs) {
		gboxBlur(input, output, x, y, w, h, (int) ((bxs[0] - 1.0f) * 0.5f), s);
		gboxBlur(output, input, x, y, w, h, (int) ((bxs[1] - 1.0f) * 0.5f), s);
		gboxBlur(input, output, x, y, w, h, (int) ((bxs[2] - 1.0f) * 0.5f), s);
	}
	
	private static final void boxBlur(byte[] input, byte[] output, int x, int y, int w, int h, int r, int s) {
		System.arraycopy(input, 0, output, 0, input.length);
		boxBlurHorizontal(output, input,  x, y, w, h, r, s);
		boxBlurVertical  (input,  output, x, y, w, h, r, s);
		boxBlurHorizontal(output, input,  x, y, w, h, r, s);
		boxBlurVertical  (input,  output, x, y, w, h, r, s);
	}
	
	private static final void gboxBlur(byte[] input, byte[] output, int x, int y, int w, int h, int r, int s) {
		System.arraycopy(input, 0, output, 0, input.length);
		boxBlurHorizontal(output, input, x, y, w, h, r, s);
		boxBlurVertical  (input, output, x, y, w, h, r, s);
	}
	
	private static final void boxBlurHorizontal(byte[] input, byte[] output, int x, int y, int w, int h, int r, int s) {
		float iarr = 1.0f / (r+r+1);
		for(int i = 0, k = y * s; i < h; ++i, k+=s) {
			int ti = k+x, li = ti, ri = ti+r, fv = input[ti] & 0xff,
				lv = input[ti+w-1] & 0xff, val = (r+1) * fv;
			for(int j = ti, e = ti+r; j < e; ++j)
				val += input[j] & 0xff;
			for(int j = 0; j <= r; ++j, ++ri, ++ti)
				output[ti] = (byte) FastMath.round((val += (input[ri] & 0xff) - fv) * iarr);
			for(int j = r+1, e = w-r; j < e; ++j, ++ri, ++li, ++ti)
				output[ti] = (byte) FastMath.round((val += (input[ri] & 0xff) - (input[li] & 0xff)) * iarr);
			for(int j = 0; j < r; ++j, ++li, ++ti)
				output[ti] = (byte) FastMath.round((val += lv - (input[li] & 0xff)) * iarr);
		}
	}
	
	private static final void boxBlurVertical(byte[] input, byte[] output, int x, int y, int w, int h, int r, int s) {
		float iarr = 1.0f / (r+r+1);
		for(int i = 0, f = x + y * s, l = r * s, c = s * (h-1); i < w; ++i) {
			int ti = i+f, li = ti, ri = ti+l, fv = input[ti] & 0xff,
				lv = input[ti+c] & 0xff, val = (r+1) * fv;
			for(int j = ti, e = ti+r*s; j < e; j+=s)
				val += input[j] & 0xff;
			for(int j = 0; j <= r; ++j, ri+=s, ti+=s)
				output[ti] = (byte) FastMath.round((val += (input[ri] & 0xff) - fv) * iarr);
			for(int j = r+1, e = h-r; j < e; ++j, li+=s, ri+=s, ti+=s)
				output[ti] = (byte) FastMath.round((val += (input[ri] & 0xff)-(input[li] & 0xff)) * iarr);
			for(int j = 0; j < r; ++j, li+=s, ti+=s)
				output[ti] = (byte) FastMath.round((val += lv-(input[li] & 0xff)) * iarr);
		}
	}
}