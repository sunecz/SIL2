package sune.lib.sil2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class used for thread management. Contains methods used for executing
 * parallel tasks as runnables.
 * <br><br>
 * Note that the {@linkplain #destroy()} static method has to be called
 * when an application should close in order to stop all the running threads.
 * @author Petr Cipra*/
public final class Threads {
	
	private static final ExecutorService THREADS = newCachedDeamonThreadPool();
	private static final ExecutorService newCachedDeamonThreadPool() {
		return Executors.newCachedThreadPool((r) -> {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			return thread;
		});
	}
	
	// forbid anyone to create an instance of this class
	private Threads() {
	}
	
	/**
	 * Submits a runnable to be run in a thread in the future.
	 * @param run the runnable*/
	public static final void execute(Runnable run) {
		if((isRunning())) {
			THREADS.execute(run);
		}
	}
	
	/**
	 * Attempts to stop forcibly all running threads.*/
	public static final void destroy() {
		THREADS.shutdownNow();
	}
	
	/**
	 * Checks wheter the threads executor service is running or not.
	 * @return {@code true}, if the threads executor service is running,
	 * otherwise {@code false}.*/
	public static final boolean isRunning() {
		return !THREADS.isShutdown();
	}
}