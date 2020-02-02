package sune.lib.sil2;

import java.nio.Buffer;
import java.util.function.Function;

import sune.lib.sil2.IImage.ActionFloat;
import sune.lib.sil2.IImage.ActionINT;
import sune.lib.sil2.IImage.ActionRGB;
import sune.lib.sil2.IImage.Job1D;
import sune.lib.sil2.IImage.Job2D;
import sune.lib.sil2.format.ImagePixelFormat;

public interface IImageContext<T extends Buffer> {
	
	<R> R applyOperation(IImageOperation<T, R> operation);
	
	void applyActionINT(ActionINT<T> action);
	void applyActionRGB(ActionRGB<T> action);
	void applyActionHSL(ActionFloat<T> action);
	void applyActionHCL(ActionFloat<T> action);
	
	void convolute2d(float[] kernel, int iterations, boolean alphaChannel);
	void applyAreaJob(int x, int y, int width, int height, T input, T output, Job2D<T> job);
	void applyLineHJob(int x, int y, int width, T input, T output, Job1D<T> job);
	void applyLineVJob(int x, int y, int height, T input, T output, Job1D<T> job);
	void swapBuffer();
	
	void structureConvolute2d(int[] structure, StructuresConfiguration config);
	
	void opSave();
	T opRemove();
	void opOr();
	void opAnd();
	void opNot();
	int[] opIntegral(Function<Integer, Integer> function);
	
	ImagePixelFormat<T> getPixelFormat();
	InternalChannels<T> getChannels();
	T getPixels();
	T getBuffer();
	int getX();
	int getY();
	int getWidth();
	int getHeight();
	int getStride();
	int getSourceWidth();
	int getSourceHeight();
}