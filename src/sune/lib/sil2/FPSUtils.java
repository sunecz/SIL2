package sune.lib.sil2;

import java.lang.reflect.Method;

/**
 * A utility class for getting current FPS in the JavaFX Platform.
 * @since 2.0
 * @author Sune*/
public final class FPSUtils {
	
	private static Object toolkit;
	private static final Object ensureToolkit() throws Exception {
		return toolkit == null ? (toolkit = getToolkit()) : toolkit;
	}
	
	private static Object performanceTracker;
	private static final Object ensurePerformanceTracker() throws Exception {
		return performanceTracker == null
					? performanceTracker = getPerformanceTracker(ensureToolkit())
					: performanceTracker;
	}
	
	private static final Method method_getInstantFPS;
	private static final Method method_calcFPS;
	static {
		Method _method_getInstantFPS = null;
		Method _method_calcFPS = null;
		try {
			Class<?> clazz = Class.forName("com.sun.javafx.perf.PerformanceTracker");
			_method_getInstantFPS = clazz.getMethod("getInstantFPS");
			_method_calcFPS = clazz.getDeclaredMethod("calcFPS");
			Reflection.setAccessible(_method_getInstantFPS, true);
			Reflection.setAccessible(_method_calcFPS, true);
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to initialize FPS Utils", ex);
		}
		method_getInstantFPS = _method_getInstantFPS;
		method_calcFPS = _method_calcFPS;
	}
	
	// Forbid anyone to create an instance of this class
	private FPSUtils() {
	}
	
	private static final Object getToolkit() throws Exception {
		Class<?> clazz = Class.forName("com.sun.javafx.tk.Toolkit");
		Method method = clazz.getMethod("getToolkit");
		Reflection.setAccessible(method, true);
		return method.invoke(null);
	}
	
	private static final Object getPerformanceTracker(Object toolkit) throws Exception {
		Class<?> clazz = Class.forName("com.sun.javafx.tk.Toolkit");
		Method method = clazz.getMethod("getPerformanceTracker");
		Reflection.setAccessible(method, true);
		return method.invoke(toolkit);
	}
	
	private static final void performanceTrackerPulse() throws Exception {
		method_calcFPS.invoke(ensurePerformanceTracker());
	}
	
	private static final float getInstantFPS() throws Exception {
		performanceTrackerPulse();
		return (float) method_getInstantFPS.invoke(ensurePerformanceTracker());
	}
	
	/**
	 * Gets the current FPS.<br>
	 * Note that this method uses an internal API of the JavaFX Toolkit.
	 * @return The FPS*/
	public static final float getFPS() {
		try {
			return getInstantFPS();
		} catch(Exception ex) {
			// Ignore
		}
		return 0.0f;
	}
}