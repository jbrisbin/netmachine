package com.jbrisbin.netmachine.fsm;

import com.jbrisbin.netmachine.Handler;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public abstract class StateMachine<C> implements Runnable {

  protected final C context;

  protected StateMachine(C context) {
    this.context = context;
  }

  public final C getContext() {
    return context;
  }

  public abstract Handler<C> getStateHandler(String state);

  public abstract String getState();

  @Override public final void run() {
    String state = getState();
    do {
      Handler<C> handler = getStateHandler(state);
      if (null == handler) {
        break;
      } else {
        handler.handle(context);
      }
    } while (null != (state = getState()));
  }

}
