package sune.lib.sil2.format;

import java.nio.Buffer;
import java.nio.IntBuffer;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritablePixelFormat;

public interface ImagePixelFormat<T extends Buffer> {
	
	int getShiftR();
	int getShiftG();
	int getShiftB();
	int getShiftA();
	
	WritablePixelFormat<T> getReadFormat();
	PixelFormat<T> getWriteFormat();
	
	T newBuffer(int length);
	IntBuffer toIntBuffer(T buffer);
	T fromIntBuffer(IntBuffer buffer);
	
	void set(T dst, int i, int value);
	void setPixel(T dst, int i, int r, int g, int b, int a);
	void setPixel(T dst, int i, T src, int k);
	void setPixel(T dst, int i, int argb);
	
	int get(T src, int i);
	int getARGB(T src, int i);
	
	int getElementsPerPixel();
	
	T toValidBuffer(Buffer buffer);
	
	boolean isPremultiplied();
}