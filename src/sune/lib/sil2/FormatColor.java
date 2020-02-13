package sune.lib.sil2;

import javafx.scene.paint.Color;
import sune.lib.sil2.format.ImagePixelFormat;

public final class FormatColor {
	
	private static final float F2I = 255.0f;
	private final ImagePixelFormat<?> format;
	
	public FormatColor(ImagePixelFormat<?> format) {
		this.format = format;
	}
	
	private static final void linear2premult(int r, int g, int b, int a, byte[] result) {
		result[0] = (byte) FastMath.div255(r * a);
		result[1] = (byte) FastMath.div255(g * a);
		result[2] = (byte) FastMath.div255(b * a);
		result[3] = (byte) a;
	}
	
	private static final void premult2linear(int r, int g, int b, int a, byte[] result) {
		if((a == 0x0)) {
			result[0] = result[1] = result[2] = result[3] = 0;
			return;
		}
		result[0] = (byte) (FastMath.mul255(r) / a);
		result[1] = (byte) (FastMath.mul255(g) / a);
		result[2] = (byte) (FastMath.mul255(b) / a);
		result[3] = (byte) a;
	}
	
	// ----- Non-premultiplied colors
	
	private final int rgba2int(int r, int g, int b, int a) {
		return ((a & 0xff) << format.getShiftA()) |
			   ((r & 0xff) << format.getShiftR()) |
			   ((g & 0xff) << format.getShiftG()) |
			   ((b & 0xff) << format.getShiftB());
	}
	
	public final int rgb(int r, int g, int b) {
		return rgba(r, g, b, 0xff);
	}
	
	public final int rgba(int r, int g, int b, int a) {
		return rgba2int(r, g, b, a);
	}
	
	public final int hsl(float h, float s, float l) {
		return hsla(h, s, l, 1.0f);
	}
	
	public final int hsla(float h, float s, float l, float a) {
		int[] rgb = new int[3];
		Colors.hsl2rgb(h, s, l, rgb);
		return rgba(rgb[0], rgb[1], rgb[2], FastMath.round(a * F2I));
	}
	
	public final int hcl(float h, float c, float l) {
		return hcla(h, c, l, 1.0f);
	}
	
	public final int hcla(float h, float c, float l, float a) {
		int[] rgb = new int[3];
		Colors.hcl2rgb(h, c, l, rgb);
		return rgba(rgb[0], rgb[1], rgb[2], FastMath.round(a * F2I));
	}
	
	public final int xyz(float x, float y, float z) {
		float[] rgb = new float[3];
		Colors.xyz2rgb(x, y, z, rgb);
		return rgba(FastMath.round(rgb[0] * F2I),
		            FastMath.round(rgb[1] * F2I),
		            FastMath.round(rgb[2] * F2I),
		            0xff);
	}
	
	public final int lab(float l, float a, float b) {
		float[] xyz = new float[3];
		Colors.lab2xyz(l, a, b, xyz);
		return xyz(xyz[0], xyz[1], xyz[2]);
	}
	
	public final int color(Color color) {
		if((color == null)) throw new NullPointerException("Invalid color");
		int r = FastMath.round((float) color.getRed()     * F2I);
		int g = FastMath.round((float) color.getGreen()   * F2I);
		int b = FastMath.round((float) color.getBlue()    * F2I);
		int a = FastMath.round((float) color.getOpacity() * F2I);
		return rgba(r, g, b, a);
	}
	
	public final int fromARGB(int argb) {
		int a = (argb >> 24) & 0xff;
		int r = (argb >> 16) & 0xff;
		int g = (argb >>  8) & 0xff;
		int b = (argb)       & 0xff;
		return rgba(r, g, b, a);
	}
	
	public final int toARGB(int color) {
		int a = (color >> format.getShiftA()) & 0xff;
		int r = (color >> format.getShiftR()) & 0xff;
		int g = (color >> format.getShiftG()) & 0xff;
		int b = (color >> format.getShiftB()) & 0xff;
		return (a << 24) | (r << 16) | (g << 8) | (b);
	}
	
	// -----
	
	// ----- Premultiplied colors
	
	private final int rgba2intPre(int r, int g, int b, int a) {
		byte[] premult = new byte[4];
		linear2premult(r, g, b, a, premult);
		r = premult[0];
		g = premult[1];
		b = premult[2];
		a = premult[3];
		return ((a & 0xff) << format.getShiftA()) |
			   ((r & 0xff) << format.getShiftR()) |
			   ((g & 0xff) << format.getShiftG()) |
			   ((b & 0xff) << format.getShiftB());
	}
	
	public final int rgbPre(int r, int g, int b) {
		return rgbaPre(r, g, b, 0xff);
	}
	
	public final int rgbaPre(int r, int g, int b, int a) {
		return rgba2intPre(r, g, b, a);
	}
	
	public final int hslPre(float h, float s, float l) {
		return hslaPre(h, s, l, 1.0f);
	}
	
	public final int hslaPre(float h, float s, float l, float a) {
		int[] rgb = new int[3];
		Colors.hsl2rgb(h, s, l, rgb);
		return rgbaPre(rgb[0], rgb[1], rgb[2], FastMath.round(a * F2I));
	}
	
	public final int hclPre(float h, float c, float l) {
		return hclaPre(h, c, l, 1.0f);
	}
	
	public final int hclaPre(float h, float c, float l, float a) {
		int[] rgb = new int[3];
		Colors.hcl2rgb(h, c, l, rgb);
		return rgbaPre(rgb[0], rgb[1], rgb[2], FastMath.round(a * F2I));
	}
	
	public final int xyzPre(float x, float y, float z) {
		float[] rgb = new float[3];
		Colors.xyz2rgb(x, y, z, rgb);
		return rgbaPre(FastMath.round(rgb[0] * F2I),
		               FastMath.round(rgb[1] * F2I),
		               FastMath.round(rgb[2] * F2I),
		               0xff);
	}
	
	public final int labPre(float l, float a, float b) {
		float[] xyz = new float[3];
		Colors.lab2xyz(l, a, b, xyz);
		return xyzPre(xyz[0], xyz[1], xyz[2]);
	}
	
	public final int colorPre(Color color) {
		if((color == null)) throw new NullPointerException("Invalid color");
		int r = FastMath.round((float) color.getRed()     * F2I);
		int g = FastMath.round((float) color.getGreen()   * F2I);
		int b = FastMath.round((float) color.getBlue()    * F2I);
		int a = FastMath.round((float) color.getOpacity() * F2I);
		return rgbaPre(r, g, b, a);
	}
	
	public final int fromARGBPre(int argb) {
		int a = (argb >> 24) & 0xff;
		int r = (argb >> 16) & 0xff;
		int g = (argb >>  8) & 0xff;
		int b = (argb)       & 0xff;
		byte[] linear = new byte[4];
		premult2linear(r, g, b, a, linear);
		r = linear[0];
		g = linear[1];
		b = linear[2];
		a = linear[3];
		return rgba(r, g, b, a);
	}
	
	public final int toARGBPre(int color) {
		int a = (color >> format.getShiftA()) & 0xff;
		int r = (color >> format.getShiftR()) & 0xff;
		int g = (color >> format.getShiftG()) & 0xff;
		int b = (color >> format.getShiftB()) & 0xff;
		byte[] premult = new byte[4];
		linear2premult(r, g, b, a, premult);
		r = premult[0];
		g = premult[1];
		b = premult[2];
		a = premult[3];
		return (a << 24) | (r << 16) | (g << 8) | (b);
	}
	
	// -----
}