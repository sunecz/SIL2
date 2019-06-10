package sune.lib.sil2;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * This class is used to access the Unsafe instance.<br><br>
 * <em>Note:</em> this class throws an {@linkplain IllegalStateException}
 * when the Unsafe instance cannot be obtained. This can happen on systems
 * where the sun.misc.Unsafe class was disabled/removed, possibly causing
 * the application to crash, however a simple try-catch block can be used
 * to ignore the exception.
 * <h2>Usage:</h2>
 * <pre>
 * Unsafe unsafe = UnsafeInstance.get();
 * // ... operations with the Unsafe instance</pre>
 * @author Sune
 * @since 2.0.2
 * @see sun.misc.Unsafe*/
final class UnsafeInstance {
	
	/**
	 * The Unsafe instance, initialized on the first class access.*/
	private static final Unsafe unsafe;
	
	static {
		Unsafe _unsafe = null;
		try {
			Class<?> clazz = Class.forName("sun.misc.Unsafe");
			Field    field = clazz.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			_unsafe = (Unsafe) field.get(null);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to obtain the Unsafe instance");
		}
		unsafe = _unsafe;
	}
	
	/**
	 * Gets the Unsafe instance.
	 * @return The Unsafe instance.*/
	public static final Unsafe get() {
		return unsafe;
	}
}