package sune.lib.sil2;

import sune.lib.sil2.format.ImagePixelFormat;

/**
 * Represents a color channel.*/
final class InternalColorChannel {
	
	private final int shift;
	private final int mask;
	
	private InternalColorChannel(int shift, int mask) {
		this.shift = shift;
		this.mask  = mask;
	}
	
	public static final InternalColorChannel from(ImagePixelFormat<?> format, ColorChannel channel) {
		switch(channel) {
			case RED:   return new InternalColorChannel(format.getShiftR(), 0xff << format.getShiftR());
			case GREEN: return new InternalColorChannel(format.getShiftG(), 0xff << format.getShiftG());
			case BLUE:  return new InternalColorChannel(format.getShiftB(), 0xff << format.getShiftB());
			case ALPHA: return new InternalColorChannel(format.getShiftA(), 0xff << format.getShiftA());
			default:
				throw new UnsupportedOperationException("Unsupported color channel: " + channel);
		}
	}
	
	public static final InternalColorChannel[] fromMany(ImagePixelFormat<?> format, ColorChannel... channels) {
		InternalColorChannel[] array = new InternalColorChannel[channels.length];
		for(int i = 0, l = channels.length; i < l; ++i) array[i] = from(format, channels[i]);
		return array;
	}
	
	public final int getShift() {
		return shift;
	}
	
	public final int getMask() {
		return mask;
	}
}