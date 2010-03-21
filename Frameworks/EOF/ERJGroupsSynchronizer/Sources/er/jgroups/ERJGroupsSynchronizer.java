package er.jgroups;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.ExtendedReceiverAdapter;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;

import com.webobjects.appserver.WOApplication;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSSelector;

import er.extensions.eof.ERXDatabase;
import er.extensions.eof.ERXObjectStoreCoordinatorSynchronizer.IChangeListener;
import er.extensions.eof.ERXObjectStoreCoordinatorSynchronizer.RemoteChange;
import er.extensions.foundation.ERXProperties;
import er.extensions.remoteSynchronizer.ERXRemoteSynchronizer;

/**
 * A multicast synchronizer built on top of the JGroups library.  This is a much
 * more robust implementation than the default synchronizer used by ERXObjectStoreCoordinatorSynchronizer.
 * 
 * @property er.extensions.ERXObjectStoreCoordinatorPool.maxCoordinators you should set this property to at least "1" to trigger ERXObjectStoreCoordinatorSynchronizer to turn on
 * @property er.extensions.jgroupsSynchronizer.properties an XML JGroups configuration file (defaults to jgroups-default.xml in this framework) 
 * @property er.extensions.remoteSynchronizer.enabled if true, remote synchronization is enabled
 * @property er.extensions.remoteSynchronizer "er.jgroups.ERJGroupsSynchronizer" for this implementation
 * @property er.extensions.jgroupsSynchronizer.multicastAddress the multicast address to use (defaults to 230.0.0.1, and only necessary if you are using multicast)
 * @property er.extensions.jgroupsSynchronizer.multicastPort the multicast port to use (defaults to 9753, and only necessary if you are using multicast)
 * @property er.extensions.jgroupsSynchronizer.groupName the JGroups group name to use (defaults to WOApplication.application.name)
 * @property er.extensions.remoteSynchronizer.includeEntities the list of entities to synchronize (all by default)
 * @property er.extensions.remoteSynchronizer.excludeEntities the list of entities to NOT synchronize (none by default)
 * @property er.extensions.jgroupsSynchronizer.autoReconnect whether to auto reconnect when shunned (defaults to false)
 * @property er.extensions.jgroupsSynchronizer.applicationWillTerminateNotificationName the name of the NSNotification that is sent when the application is terminating. Leave blank to disable this feature.
 * @property er.extensions.jgroupsSynchronizer.useShutdownHook whether to register a JVM shutdown hook to clean up the JChannel (defaults to true)
 * 
 * @author mschrag
 */
public class ERJGroupsSynchronizer extends ERXRemoteSynchronizer {
  private String _groupName;
  private JChannel _channel;

  public ERJGroupsSynchronizer(IChangeListener listener) throws ChannelException {
    super(listener);
    String jgroupsPropertiesFile = ERXProperties.stringForKey("er.extensions.jgroupsSynchronizer.properties");
    String jgroupsPropertiesFramework = null;
    if (jgroupsPropertiesFile == null) {
      jgroupsPropertiesFile = "jgroups-default.xml";
      jgroupsPropertiesFramework = "ERJGroupsSynchronizer";
    }
    _groupName = ERXProperties.stringForKeyWithDefault("er.extensions.jgroupsSynchronizer.groupName", WOApplication.application().name());

    String localBindAddressStr = ERXProperties.stringForKey("er.extensions.jgroupsSynchronizer.localBindAddress");
    if (localBindAddressStr == null) {
      System.setProperty("bind.address", WOApplication.application().hostAddress().getHostAddress());
    }
    else {
      System.setProperty("bind.address", localBindAddressStr);
    }

    URL propertiesUrl = WOApplication.application().resourceManager().pathURLForResourceNamed(jgroupsPropertiesFile, jgroupsPropertiesFramework, (NSArray<String>)null);
    _channel = new JChannel(propertiesUrl);
    _channel.setOpt(Channel.LOCAL, Boolean.FALSE);
    if (ERXProperties.booleanForKeyWithDefault("er.extensions.jgroupsSynchronizer.autoReconnect", true)) {
      _channel.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
    }

    _registerForCleanup();
  }

  //@Override
  public void join() throws ChannelException {
    _channel.connect(_groupName);
  }

  //@Override
  public void leave() {
    _channel.disconnect();
  }

  //@Override
  public void listen() {
    _channel.setReceiver(new ExtendedReceiverAdapter() {
      //@Override
      public void receive(Message message) {
        try {
          byte[] buffer = message.getBuffer();
          ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
          DataInputStream dis = new DataInputStream(bais);
          int transactionCount = dis.readInt();
          RemoteChange remoteChange = new RemoteChange("AnotherInstance", -1, transactionCount);
          for (int transactionNum = 0; transactionNum < transactionCount; transactionNum++) {
            _readCacheChange(remoteChange, dis);
          }
          addChange(remoteChange);
          if (ERXRemoteSynchronizer.log.isInfoEnabled()) {
            ERXRemoteSynchronizer.log.info("Received " + transactionCount + " changes from " + message.getSrc());
          }
          if (ERXRemoteSynchronizer.log.isDebugEnabled()) {
            ERXRemoteSynchronizer.log.info("  Changes = " + remoteChange.remoteCacheChanges());
          }
        }
        catch (IOException e) {
          ERXRemoteSynchronizer.log.error("Failed to apply remote changes.  This is bad.", e);
        }
      }

      //@Override
      public void viewAccepted(View view) {
        // System.out.println(".viewAccepted: " + view);
      }
    });
  }

  //@Override
  protected void _writeCacheChanges(int transactionID, NSArray cacheChanges) throws ChannelNotConnectedException, ChannelClosedException, IOException {
    if (!_channel.isConnected()) {
      if (ERXRemoteSynchronizer.log.isInfoEnabled()) {
        ERXRemoteSynchronizer.log.info("Channel not connected: Not Sending " + cacheChanges.count() + " changes.");
      }
      if (ERXRemoteSynchronizer.log.isDebugEnabled()) {
        ERXRemoteSynchronizer.log.info("Channel not connected: Changes = " + cacheChanges);
      }
      return;
    }
    if (cacheChanges.count() == 0) {
    	if (ERXRemoteSynchronizer.log.isInfoEnabled()) {
    		ERXRemoteSynchronizer.log.info("No changes to send!");
    	}
    	return;
    }
    RefByteArrayOutputStream baos = new RefByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    dos.writeInt(cacheChanges.count());
    for (Enumeration cacheChangesEnum = cacheChanges.objectEnumerator(); cacheChangesEnum.hasMoreElements();) {
      ERXDatabase.CacheChange cacheChange = (ERXDatabase.CacheChange) cacheChangesEnum.nextElement();
      _writeCacheChange(dos, cacheChange);
    }
    dos.flush();
    dos.close();
    if (ERXRemoteSynchronizer.log.isInfoEnabled()) {
      ERXRemoteSynchronizer.log.info("Sending " + cacheChanges.count() + " changes.");
    }
    if (ERXRemoteSynchronizer.log.isDebugEnabled()) {
      ERXRemoteSynchronizer.log.info("  Changes = " + cacheChanges);
    }
    Message message = new Message(null, null, baos.buffer(), 0, baos.size());
    _channel.send(message);
  }

  private void _registerForCleanup() {
    String notificationName = ERXProperties.stringForKey("er.extensions.jgroupsSynchronizer.applicationWillTerminateNotificationName");
    if (notificationName != null && notificationName.length() > 0) {
      NSSelector applicationLaunchedNotification = new NSSelector("_applicationWillTerminateNotification", new Class[] { NSNotification.class });
      NSNotificationCenter.defaultCenter().addObserver(this, applicationLaunchedNotification, notificationName, null);
    }

    if (ERXProperties.booleanForKeyWithDefault("er.extensions.jgroupsSynchronizer.useShutdownHook", true)) {
      Runtime.getRuntime().addShutdownHook(new Thread(new JGroupsCleanupTask(_channel), "ERJGroupsCleanupThread"));
    }
  }

  private static void cleanUpJChannel(JChannel channel) {
    try {
      if (channel == null || !channel.isOpen()) {
        return;
      }

      if (channel.isConnected()) {
        channel.disconnect();
      }

      channel.close();
    }
    catch (Throwable e) {
      ERXRemoteSynchronizer.log.error("Error closing JChannel: " + channel, e);
    }
  }

  public void _applicationWillTerminateNotification(NSNotification notification) {
    try {
      cleanUpJChannel(_channel);
    }
    catch (Throwable e) {
      ERXRemoteSynchronizer.log.error("Error cleaning up ERJgroupsSynchronizer JChannel", e);
    }
  }

  private static class JGroupsCleanupTask implements Runnable {
    private final JChannel channel;

    public JGroupsCleanupTask(JChannel channel) {
      this.channel = channel;
    }

    public void run() {
      cleanUpJChannel(channel);
    }
  }
}
