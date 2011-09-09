package com.jbrisbin.netmachine;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface Handler<T> {

  void handle(T obj);

}
