package sune.lib.sil2;

import java.util.Arrays;

import sune.lib.sil2.format.ImagePixelFormat;

/**
 * Represents a color channel.*/
final class InternalColorChannel {
	
	private final int shift;
	private final int mask;
	
	private InternalColorChannel(int shift) {
		this.shift = shift;
		this.mask  = 0xff << shift;
	}
	
	public static final InternalColorChannel from(ImagePixelFormat<?> format, ColorChannel channel) {
		switch(channel) {
			case RED:   return new InternalColorChannel(format.getShiftR());
			case GREEN: return new InternalColorChannel(format.getShiftG());
			case BLUE:  return new InternalColorChannel(format.getShiftB());
			case ALPHA: return new InternalColorChannel(format.getShiftA());
			default:
				throw new UnsupportedOperationException("Unsupported color channel: " + channel);
		}
	}
	
	public static final InternalColorChannel[] fromMany(ImagePixelFormat<?> format, ColorChannel... channels) {
		return Arrays.asList(channels).parallelStream().map((c) -> from(format, c)).toArray(InternalColorChannel[]::new);
	}
	
	public final int getShift() {
		return shift;
	}
	
	public final int getMask() {
		return mask;
	}
}