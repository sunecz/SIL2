package sune.lib.sil2;

import java.lang.reflect.Method;

public final class FPSUtils {
	
	private static Object toolkit;
	private static final Object ensureToolkit() throws Exception {
		return toolkit == null ? (toolkit = getToolkit()) : toolkit;
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
	
	private static final void performanceTrackerPulse(Object performanceTracker) throws Exception {
		Class<?> clazz = Class.forName("com.sun.javafx.perf.PerformanceTracker");
		Method method = clazz.getDeclaredMethod("calcFPS");
		Reflection.setAccessible(method, true);
		method.invoke(performanceTracker);
	}
	
	private static final float getInstantFPS(Object toolkit) throws Exception {
		Object performanceTracker = getPerformanceTracker(toolkit);
		Class<?> clazz = Class.forName("com.sun.javafx.perf.PerformanceTracker");
		performanceTrackerPulse(performanceTracker);
		Method method = clazz.getMethod("getInstantFPS");
		Reflection.setAccessible(method, true);
		return (float) method.invoke(performanceTracker);
	}
	
	public static final float getFPS() {
		try {
			return getInstantFPS(ensureToolkit());
		} catch(Exception ex) {
			// Ignore
		}
		return 0.0f;
	}
}