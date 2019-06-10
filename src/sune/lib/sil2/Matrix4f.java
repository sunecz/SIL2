package sune.lib.sil2;

/**
 * Represents a matrix with 4 rows of 5-dimensional vectors with float values.*/
public final class Matrix4f {
	
	/**
	 * The value at [0, 0].*/
	public final float m00;
	/**
	 * The value at [0, 1].*/
	public final float m01;
	/**
	 * The value at [0, 2].*/
	public final float m02;
	/**
	 * The value at [0, 3].*/
	public final float m03;
	/**
	 * The value at [0, 4].*/
	public final float m04;
	/**
	 * The value at [1, 0].*/
	public final float m10;
	/**
	 * The value at [1, 1].*/
	public final float m11;
	/**
	 * The value at [1, 2].*/
	public final float m12;
	/**
	 * The value at [1, 3].*/
	public final float m13;
	/**
	 * The value at [1, 4].*/
	public final float m14;
	/**
	 * The value at [2, 0].*/
	public final float m20;
	/**
	 * The value at [2, 1].*/
	public final float m21;
	/**
	 * The value at [2, 2].*/
	public final float m22;
	/**
	 * The value at [2, 3].*/
	public final float m23;
	/**
	 * The value at [2, 4].*/
	public final float m24;
	/**
	 * The value at [3, 0].*/
	public final float m30;
	/**
	 * The value at [3, 1].*/
	public final float m31;
	/**
	 * The value at [3, 2].*/
	public final float m32;
	/**
	 * The value at [3, 3].*/
	public final float m33;
	/**
	 * The value at [3, 4].*/
	public final float m34;
	
	/**
	 * Creates a new instance from the given array of floats.
	 * @param data The data of length {@code 20}*/
	public Matrix4f(float... data) {
		if((data == null))
			throw new IllegalArgumentException("Data cannot be null");
		if((data.length != 20))
			throw new IllegalArgumentException("Data must have the length equal to 20");
		this.m00 = data[0];
		this.m01 = data[1];
		this.m02 = data[2];
		this.m03 = data[3];
		this.m04 = data[4];
		this.m10 = data[5];
		this.m11 = data[6];
		this.m12 = data[7];
		this.m13 = data[8];
		this.m14 = data[9];
		this.m20 = data[10];
		this.m21 = data[11];
		this.m22 = data[12];
		this.m23 = data[13];
		this.m24 = data[14];
		this.m30 = data[15];
		this.m31 = data[16];
		this.m32 = data[17];
		this.m33 = data[18];
		this.m34 = data[19];
	}
	
	/**
	 * Multiply a 4-dimensional vector, given by the given ints, with {@code this} matrix.
	 * @param a0 The first value of the vector
	 * @param a1 The secobd value of the vector
	 * @param a2 The third value of the vector
	 * @param a3 The fourth value of the vector
	 * @return The multiplication result, a 4-dimensional vector, as a float array*/
	public final float[] multiply(int a0, int a1, int a2, int a3) {
		float[] result = new float[4];
		multiply((float) a0, (float) a1, (float) a2, (float) a3, result);
		return result;
	}
	
	/**
	 * Multiply a 4-dimensional vector, given by the given ints, with {@code this} matrix
	 * and stores it in the given float array.
	 * @param a0 The first value of the vector
	 * @param a1 The secobd value of the vector
	 * @param a2 The third value of the vector
	 * @param a3 The fourth value of the vector
	 * @param result The output array of length {@code 4}*/
	public final void multiply(int a0, int a1, int a2, int a3, float[] result) {
		multiply((float) a0, (float) a1, (float) a2, (float) a3, result);
	}
	
	/**
	 * Multiply a 4-dimensional vector, given by the given floats, with {@code this} matrix.
	 * @param a0 The first value of the vector
	 * @param a1 The secobd value of the vector
	 * @param a2 The third value of the vector
	 * @param a3 The fourth value of the vector
	 * @return The multiplication result, a 4-dimensional vector, as a float array*/
	public final float[] multiply(float a0, float a1, float a2, float a3) {
		float[] result = new float[4];
		multiply(a0, a1, a2, a3);
		return result;
	}
	
	/**
	 * Multiply a 4-dimensional vector, given by the given floats, with {@code this} matrix
	 * and stores it in the given float array.
	 * @param a0 The first value of the vector
	 * @param a1 The secobd value of the vector
	 * @param a2 The third value of the vector
	 * @param a3 The fourth value of the vector
	 * @param result The output array of length {@code 4}*/
	public final void multiply(float a0, float a1, float a2, float a3, float[] result) {
		result[0] = a0 * m00 + a1 * m01 + a2 * m02 + a3 * m03 + m04;
		result[1] = a0 * m10 + a1 * m11 + a2 * m12 + a3 * m13 + m14;
		result[2] = a0 * m20 + a1 * m21 + a2 * m22 + a3 * m23 + m24;
		result[3] = a0 * m30 + a1 * m31 + a2 * m32 + a3 * m33 + m34;
	}
}