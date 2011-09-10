package com.jbrisbin.netmachine;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public abstract class WriteHandler<T> implements Handler<T> {

  @Override public void handle(T obj) {
    write(obj, null);
  }

  public abstract void write(T obj, Handler<Void> completionHandler);

}
