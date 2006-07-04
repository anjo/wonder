package er.imadaptor;

public class IMConnectionTester implements Runnable, IMessageListener {
  private static final String PING_MESSAGE = "~Ping~";
  private static final String PONG_MESSAGE = "~Pong~";

  private IInstantMessenger _watcher;
  private IInstantMessenger _watched;

  private Object _pingPongMessageLock;
  private int _failureCount;
  private boolean _pinged;
  private boolean _ponged;
  private long _pingPongFrequencyMillis;
  private long _timeoutMillis;
  private long _lastConnectionAttempt;

  private boolean _running;

  public IMConnectionTester(IInstantMessenger watcher, IInstantMessenger watched, long pingPongFrequencyMillis, long timeoutMillis) {
    _running = true;

    _pingPongMessageLock = new Object();

    _watcher = watcher;
    _watched = watched;

    _pingPongFrequencyMillis = pingPongFrequencyMillis;
    _timeoutMillis = timeoutMillis;

    synchronized (_pingPongMessageLock) {
      _watched.addMessageListener(this);
      _watcher.addMessageListener(this);
    }
  }

  public void stop() {
    _running = false;
  }

  public void messageReceived(IInstantMessenger instantMessenger, String buddyName, String message) {
    //System.out.println("IMConnectionTester.messageReceived: " + buddyName + ", " + message);
    if (instantMessenger == _watched && _watcher.getScreenName().equals(buddyName) && IMConnectionTester.PING_MESSAGE.equals(message)) {
      synchronized (_pingPongMessageLock) {
        try {
          //System.out.println("IMConnectionTester.testConnection: Sending PONG to " + myWatcher.getScreenName());
          _watched.sendMessage(buddyName, IMConnectionTester.PONG_MESSAGE);
        }
        catch (MessageException e) {
          // We failed to pong!
          e.printStackTrace();
        }
      }
    }
    else if (instantMessenger == _watcher && _watched.getScreenName().equals(buddyName) && IMConnectionTester.PONG_MESSAGE.equals(message)) {
      synchronized (_pingPongMessageLock) {
        _ponged = true;
        //System.out.println("IMConnectionTester.testConnection: Recevied PONG from " + myWatched.getScreenName());
        _pingPongMessageLock.notifyAll();
      }
    }
  }

  protected void testConnection() throws IMConnectionException {
    if (_running && !_watched.isConnected()) {
      _watched.connect();
      _failureCount = 0;
    }

    if (_running && !_watcher.isConnected()) {
      _watcher.connect();
      _failureCount = 0;
    }

    synchronized (_pingPongMessageLock) {
      try {
        //System.out.println("IMConnectionTester.testConnection: Sending PING to " + myWatched.getScreenName());
        _watcher.sendMessage(_watched.getScreenName(), IMConnectionTester.PING_MESSAGE);
        _ponged = false;
        _pingPongMessageLock.wait(_timeoutMillis);
        if (!_ponged) {
          //System.out.println("IMConnectionTester.testConnection: " + myWatcher.getScreenName() + " did not respond to PING");
          _failureCount++;
          if (_running && _failureCount > 5) {
            //System.out.println("IMConnectionTester.reconnect: Reconnecting " + myWatched.getScreenName());
            _watched.connect();
            _failureCount = 0;
          }
        }
      }
      catch (MessageException e) {
        e.printStackTrace();
      }
      catch (InterruptedException e) {
        // ignore
      }
      finally {
        _pinged = false;
        _ponged = false;
      }
    }
  }

  public void run() {
    while (_running) {
      try {
        Thread.sleep(_pingPongFrequencyMillis);
      }
      catch (InterruptedException e) {
        // who cares
      }

      if (_running) {
        //System.out.println("IMConnectionTester.run: Testing " + myWatched.getScreenName());
        try {
          testConnection();
        }
        catch (Throwable t) {
          t.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) throws IMConnectionException {
    IInstantMessenger watchedInstantMessenger = new AimBotInstantMessenger.Factory().createInstantMessenger("iykwias", "wec1kl");
    watchedInstantMessenger.connect();

    IInstantMessenger watcherInstantMessenger = new AimBotInstantMessenger.Factory().createInstantMessenger("iykwiasjr", "tc2001");
    watcherInstantMessenger.connect();

    Thread watcherThread = new Thread(new IMConnectionTester(watcherInstantMessenger, watchedInstantMessenger, 60000, 30000));
    watcherThread.start();

    //myWatchedThread = new Thread(new IMConnectionTester(myInstantMessenger, myWatcherInstantMessenger, 60000, 30000));
    //myWatchedThread.start();
  }
}
