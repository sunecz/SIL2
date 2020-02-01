package sune.lib.sil2.operation;

final class IImageUtils {
	
	public static final float F2I = 255.0f;
	public static final float I2F = 1.0f / 255.0f;
	
	// Forbid anyone to create an instance of this class
	private IImageUtils() {
	}
	
	public static final float clamp01(float val) {
		if((val <= 0.0f)) return 0.0f;
		if((val >= 1.0f)) return 1.0f;
		return val;
	}
	
	public static final float clamp11(float val) {
		if((val <= -1.0f)) return -1.0f;
		if((val >= +1.0f)) return +1.0f;
		return val;
	}
	
	public static final float clamp00(float val) {
		if((val <= 0.0f)) return 0.0f;
		return val;
	}
	
	public static final int clamp02(int val) {
		if((val <= 0x00)) return 0x00;
		if((val >= 0xff)) return 0xff;
		return val;
	}
	
	public static final int clamp(int val, int min, int max) {
		return val <= min ? min : val >= max ? max : val;
	}
}