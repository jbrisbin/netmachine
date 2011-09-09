package com.jbrisbin.netmachine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class ThreadPool {

	public static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

	public static final ExecutorService ACCEPTOR_POOL = Executors.newCachedThreadPool(
			new NetmachineThreadFactory("netmachine-core-")
	);
	public static final ExecutorService WORKER_POOL = Executors.newFixedThreadPool(
			PROCESSORS,
			new NetmachineThreadFactory("netmachine-worker-")
	);

	private static class NetmachineThreadFactory implements ThreadFactory {
		private AtomicInteger threadNum = new AtomicInteger(0);
		private String prefix = "netmachine-";

		private NetmachineThreadFactory(String prefix) {
			this.prefix = prefix;
		}

		@Override public Thread newThread(Runnable runnable) {
			Thread t = new Thread(runnable, prefix + threadNum.incrementAndGet());
			t.setPriority(Thread.NORM_PRIORITY);
			t.setDaemon(true);
			return t;
		}
	}
}
