package poke.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.ServerConf;
import poke.server.managers.HeartbeatManager;



/**
 * Anuradha's changes
 * A thread provides time out service, it calls listener's timeout() method on every timeout.
 * If the reset method is called before timeout, the timer will be restart.
 *
 *
 */
public class RaftNodeTimer implements Runnable {
  protected static Logger logger = LoggerFactory.getLogger("RaftNodeTimer");
  private TimeoutHandler listener = null;
  private int timeout = 0;
  private final Object sleepLock = new Object();
  private volatile boolean reset = false;
  private volatile boolean isStopped = false;
  private Thread thread = null;
  
  // It is not allowed to create new instance with new operator, can only be created
  // from create method.
  private RaftNodeTimer() {
  }
  
  //creates and sets values for existing server
  public static RaftNodeTimer create(String name, int timeout, TimeoutHandler listener) {
    RaftNodeTimer worker = new RaftNodeTimer();
    worker.setTimeout(timeout);
    worker.setListener(listener);
    
    worker.setThread(new Thread(worker));
    worker.getThread().setName(name);
    worker.getThread().start();
    return worker;
  }
  
  /**
   * @return the listener
   */
  private TimeoutHandler getListener() {
    return listener;
  }

  /**
   * @param listener the listener to set
   */
  private void setListener(TimeoutHandler listener) {
    this.listener = listener;
  }

  /**
   * @return the timeout
   */
  private int getTimeout() {
    return timeout;
  }

  /**
   * @param timeout the timeout to set
   */
  private void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  /**
   * @return the thread
   */
  private Thread getThread() {
    return thread;
  }

  /**
   * @param thread the thread to set
   */
  private void setThread(Thread thread) {
    this.thread = thread;
  }
  
  public void reset() {
    logger.debug(thread.getName() + " RESET");
    synchronized (sleepLock) {
      reset = true;
      sleepLock.notifyAll();
    }   
  }
  
  public void reset(int timeout) {
    setTimeout(timeout);
    reset();
  }
  
  public boolean isStopped() {
    return isStopped;
  }

  public void stop() {
    logger.info(thread.getName() + " STOP");
    this.isStopped = true;
    reset();
    try {
      this.thread.join();
    } catch(Exception e) {
      logger.error("exception", e);
    }
  }

  private void sleep() {
    logger.debug("entering sleep");
    try {
      while(!isStopped()) {
        reset = false;
        synchronized (sleepLock) {
          sleepLock.wait(getTimeout());
        }
        if(reset)
          continue;
        break;
      }
    } catch(InterruptedException iex) {
      logger.debug("interrupted");
    }
  }

  @Override
  public void run() {
//    logger.debug(thread.getName() + " started, timeout=" + this.timeout);
    try {
      while (!isStopped()) {
        sleep();
        if(isStopped())
          break;
        try {
           doTimeOut(listener.getConf(), listener.getServer(),listener.getHeartbeatMgr());
        } catch (Exception e) {
          if (isStopped()) {
            break;
          }
        }        
      }
    } catch (Throwable t) {
      logger.error("RaftNodeTimer exception", t);
    } finally {
      logger.info(thread.getName() + " STOPPED");
    }
  }

  private void doTimeOut(ServerConf conf, Server server, HeartbeatManager hbMgr) {
    logger.debug(thread.getName() + " TIMEOUT");
    if(listener != null) {
      getListener().timeout(conf, server, hbMgr);
      logger.debug(thread.getName() + " listener timeout is called");
    } else {
      logger.warn(thread.getName() + " listener is null");
    }
  }
}
