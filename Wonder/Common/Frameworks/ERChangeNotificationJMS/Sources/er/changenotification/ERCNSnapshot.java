//
// ERCNSnapshot.java
// Project ERChangeNotificationJMS
//
// Created by tatsuya on Sat Aug 31 2002
//
package er.changenotification;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.util.Enumeration;
import java.io.Serializable;
import er.extensions.ERXLogger;

/** 
 * ERCNSnapshot encapsulates changes in enterprise objects for single 
 * saveChanges operation. It implements java.io.Serializable interface so that 
 * it can be transmitted between application instances as a JMS object message. 
 * <p>
 * Its constructor is called by ERCNPublisher object. It processes the 
 * change notification posted by the default EOObjectStoreCoordinator and 
 * populate the dictionaries with the snapshots of updated enterprise objects. 
 */
public class ERCNSnapshot implements Serializable {

    private final String _senderHost;
    private final Number _senderPort;
    private final String _senderAppName;

    private final NSDictionary _shapshotsForInsertionGroupedByEntity;
    private final NSDictionary _shapshotsForUpdateGroupedByEntity;
    private final NSDictionary _globalIDsForDeletionGroupedByEntity;
    
    private transient boolean _shouldPostChange = false;
    private transient String _toString;

    public ERCNSnapshot(NSNotification notification) {
        WOApplication app = WOApplication.application();
        _senderHost = app.host();
        _senderPort = app.port(); // Don't forget to apply Max's change
        _senderAppName = app.name();

        NSDictionary userInfo = (NSDictionary)notification.userInfo();

        ERCNNotificationCoordinator coordinator = ERCNNotificationCoordinator.coordinator();
        if (coordinator.changeTypesToTrack().containsObject("inserted")) 
            _shapshotsForInsertionGroupedByEntity = snapshotsGroupedByEntity((NSArray)userInfo.objectForKey("inserted"));
        else 
            _shapshotsForInsertionGroupedByEntity = NSDictionary.EmptyDictionary;

        if (coordinator.changeTypesToTrack().containsObject("updated")) 
            _shapshotsForUpdateGroupedByEntity = snapshotsGroupedByEntity((NSArray)userInfo.objectForKey("updated"));
        else 
            _shapshotsForUpdateGroupedByEntity = NSDictionary.EmptyDictionary;

        if (coordinator.changeTypesToTrack().containsObject("deleted")) 
            _globalIDsForDeletionGroupedByEntity = globalIDsGroupedByEntity((NSArray)userInfo.objectForKey("deleted"));
        else 
            _globalIDsForDeletionGroupedByEntity = NSDictionary.EmptyDictionary;
    }

    public NSDictionary shapshotsForInsertionGroupedByEntity() {
        return _shapshotsForInsertionGroupedByEntity;
    }
    
    public NSDictionary shapshotsForUpdateGroupedByEntity() {
        return _shapshotsForUpdateGroupedByEntity;
    }
    
    public NSDictionary globalIDsForDeletionGroupedByEntity() {
        return _globalIDsForDeletionGroupedByEntity;
    }

    public boolean shouldPostChange() { 
        return _shouldPostChange; 
    }

    public boolean shouldSynchronizeEntity(String entityName) {
        return ! ERCNNotificationCoordinator.coordinator().entitiesNotToSynchronize().containsObject(entityName);
    }

    public String senderHost() {
        return _senderHost;
    }
    
    public Number senderPort() {
        return _senderPort;
    }
    
    public String senderAppName() {
        return _senderAppName;
    }

    public NSDictionary snapshotsGroupedByEntity(NSArray objects) {
        if (objects == null)  return NSDictionary.EmptyDictionary;
    
        NSMutableDictionary result = new NSMutableDictionary();
        EOEditingContext ec = new EOEditingContext();
        ec.lock();
        
        Enumeration e = objects.objectEnumerator();
        while (e.hasMoreElements()) {
            EOKeyGlobalID globalID = (EOKeyGlobalID) e.nextElement();
            String entityName = globalID.entityName();

            if (shouldSynchronizeEntity(entityName)) {
                _shouldPostChange = true;
                EODatabaseContext dbContext = ERCNNotificationCoordinator.databaseContextForEntityNamed(entityName, ec);
                NSMutableArray snapshotsForEntity = (NSMutableArray)result.objectForKey(entityName);
                if (snapshotsForEntity == null) {
                    snapshotsForEntity = new NSMutableArray();
                    result.setObjectForKey(snapshotsForEntity, entityName);
                }
                snapshotsForEntity.addObject(dbContext.snapshotForGlobalID(globalID));
            }
        }
        ec.unlock();
        return result.immutableClone();
    }

    public NSDictionary globalIDsGroupedByEntity(NSArray objects) {
        if (objects == null)  return NSDictionary.EmptyDictionary;
    
        NSMutableDictionary result = new NSMutableDictionary();
        
        Enumeration e = objects.objectEnumerator();
        while (e.hasMoreElements()) {
            EOKeyGlobalID globalID = (EOKeyGlobalID) e.nextElement();
            String entityName = globalID.entityName();

            if (shouldSynchronizeEntity(entityName)) {
                _shouldPostChange = true;
                NSMutableArray globalIDsForEntity = (NSMutableArray)result.objectForKey(entityName);
                if (globalIDsForEntity == null) {
                    globalIDsForEntity = new NSMutableArray();
                    result.setObjectForKey(globalIDsForEntity, entityName);
                }
                globalIDsForEntity.addObject(globalID);
            }
        }
        return result.immutableClone();
    }

    public String toString() {
        if (_toString == null) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append("<").append(getClass().getName()).append("\n");
            
            sbuf.append(" sender: ").append(senderHost()).append(":")
                .append(senderPort()).append("/").append(senderAppName()).append("\n");
            
            sbuf.append(" insertion: ").append(_summaryForChangeType(_shapshotsForInsertionGroupedByEntity));
            sbuf.append(" update: ").append(_summaryForChangeType(_shapshotsForUpdateGroupedByEntity));
            sbuf.append(" deletion: ").append(_summaryForChangeType(_globalIDsForDeletionGroupedByEntity));
            
            sbuf.append(">");
            _toString = sbuf.toString();
        }
        return _toString;
    }
    
    private String _summaryForChangeType(NSDictionary objectsGroupedByEntity) {
        StringBuffer sbuf = new StringBuffer();
        if (objectsGroupedByEntity.allKeys().count() == 0) {
            sbuf.append("none \n");
        } else {
            sbuf.append("\n");
            Enumeration entityNames = objectsGroupedByEntity.keyEnumerator();
            while (entityNames.hasMoreElements()) {
                String entityName = (String)entityNames.nextElement();
                sbuf.append("    ").append(entityName).append(": ");
                NSArray objects = (NSArray)objectsGroupedByEntity.objectForKey(entityName);
                sbuf.append(objects.count()).append(" objects \n");
            }
        }
        return sbuf.toString();
    }
}
