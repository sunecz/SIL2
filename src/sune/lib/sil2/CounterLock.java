package sune.lib.sil2;

import java.util.concurrent.atomic.AtomicInteger;

final class CounterLock {
	
	private final AtomicInteger counter;
	private final Object lock = new Object();
	
	public CounterLock() {
		this(0);
	}
	
	public CounterLock(int value) {
		this.counter = new AtomicInteger(value);
	}
	
	public final void increment() {
		synchronized(lock) {
			counter.getAndIncrement();
		}
	}
	
	public final void decrement() {
		synchronized(lock) {
			counter.getAndDecrement();
			if((counter.get() <= 0)) {
				lock.notifyAll();
			}
		}
	}
	
	public final boolean await() {
		while(counter.get() > 0) {
			synchronized(lock) {
				try {
					lock.wait();
				} catch(InterruptedException ex) {
					return false;
				}
			}
		}
		return true;
	}
}