package sune.lib.sil2.format;

import sune.lib.sil2.FastMath;

final class ImagePixelFormatUtils {
	
	// Forbid anyone to create an instance of this class
	private ImagePixelFormatUtils() {
	}
	
	public static final void linear2premult(int r, int g, int b, int a, byte[] out) {
		a &= 0xff;
		out[3] = (byte) a;
		out[2] = (byte) FastMath.div255((r & 0xff) * a);
		out[1] = (byte) FastMath.div255((g & 0xff) * a);
		out[0] = (byte) FastMath.div255((b & 0xff) * a);
	}
	
	public static final int linear2premult(int argb) {
		int a = argb >>> 24;
		int r = FastMath.div255(((argb >> 16) & 0xff) * a);
		int g = FastMath.div255(((argb >>  8) & 0xff) * a);
		int b = FastMath.div255(((argb)       & 0xff) * a);
		return (argb & 0xff000000) |
			   (r << 16)           |
			   (g <<  8)           |
			   (b);
	}
	
	public static final void premult2linear(byte r, byte g, byte b, byte a, int[] out) {
		if((a == 0x0)) {
			out[0] = out[1] = out[2] = out[3] = 0x0;
		} else {
			int ia = a & 0xff;
			out[3] = ia;
			out[2] = FastMath.mul255(r & 0xff) / ia;
			out[1] = FastMath.mul255(g & 0xff) / ia;
			out[0] = FastMath.mul255(b & 0xff) / ia;
		}
	}
	
	public static final int premult2linear(int argb) {
		int a = argb >>> 24;
		if((a == 0x0)) return 0x0;
		int r = FastMath.mul255((argb >> 16) & 0xff) / a;
		int g = FastMath.mul255((argb >>  8) & 0xff) / a;
		int b = FastMath.mul255((argb)       & 0xff) / a;
		return (argb & 0xff000000) |
			   (r  << 16)          |
			   (g  <<  8)          |
			   (b);
	}
}