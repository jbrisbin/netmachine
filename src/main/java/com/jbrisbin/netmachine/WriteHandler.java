package com.jbrisbin.netmachine;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface WriteHandler<T> {

	void write(T obj, Handler<Void> completionHandler);

}
