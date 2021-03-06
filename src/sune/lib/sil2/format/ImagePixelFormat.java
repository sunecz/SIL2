package sune.lib.sil2.format;

import java.nio.Buffer;

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
	
	void set(T dst, int i, int value);
	void set(T dst, int i, T src, int k);
	
	void setPixel(T dst, int i, int r, int g, int b, int a);
	void setPixelPre(T dst, int i, int r, int g, int b, int a);
	
	void setARGB(T dst, int i, int argb);
	void setARGBPre(T dst, int i, int argb);
	void setARGB(T dst, int i, T src, int k);
	
	int get(T src, int i);
	
	int getARGB(T src, int i);
	int getARGBPre(T src, int i);
	
	int getElementsPerPixel();
	boolean isPremultiplied();
}