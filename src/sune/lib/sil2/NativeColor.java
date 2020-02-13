package sune.lib.sil2;

import sune.lib.sil2.format.ImagePixelFormats;

public final class NativeColor {
	
	private static FormatColor INSTANCE;
	
	public static final FormatColor get() {
		return INSTANCE == null ? INSTANCE = new FormatColor(ImagePixelFormats.getNativeFormat()) : INSTANCE;
	}
}