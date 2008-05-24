package er.corebusinesslogic.audittrail;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOQualifier;

import er.extensions.ERXEOControlUtilities;
import er.extensions.ERXGenericRecord;
import er.extensions.ERXKeyGlobalID;
import er.extensions.ERXQ;

/**
 * Bracket for all single audit trail actions. It serves as a "shadow" of the
 * object in question.
 * 
 * @author ak
 * 
 */
public class ERCAuditTrail extends _ERCAuditTrail {

    @SuppressWarnings("unused")
    static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ERCAuditTrail.class);

    public static final ERCAuditTrailClazz clazz = new ERCAuditTrailClazz();

    public static class ERCAuditTrailClazz extends _ERCAuditTrail._ERCAuditTrailClazz {

        public ERCAuditTrail auditTrailForObject(EOEditingContext ec, EOEnterpriseObject eo) {
            EOKeyGlobalID gid = null;
            if (eo instanceof ERXGenericRecord) {
                gid = ((ERXGenericRecord) eo).permanentGlobalID();
            } else {
                throw new IllegalArgumentException("Can't handle non ERXGenericRecord");
            }

            ERCAuditTrail trail = (ERCAuditTrail) ERXEOControlUtilities.objectWithQualifier(ec, ENTITY_NAME, ERXQ.equals(Key.GID, ERXKeyGlobalID.globalIDForGID(gid)));
            if (trail == null) {
                trail = (ERCAuditTrail) EOQualifier.filteredArrayWithQualifier(ec.insertedObjects(),
                        ERXQ.equals("entityName", ENTITY_NAME).and(ERXQ.equals(Key.GID + ".globalID", gid))).lastObject();
            }
            return trail;
        }

        public ERCAuditTrail auditTrailForGlobalID(EOEditingContext ec, EOGlobalID gid) {
            throw new IllegalArgumentException("Can't handle non ERXGenericRecord");
        }

        public ERCAuditTrail createAuditTrailForObject(EOEditingContext ec, EOEnterpriseObject eo) {
            ERCAuditTrail trail = createAndInsertObject(ec);

            trail.setObject(eo);
            return trail;
        }
    }

    public interface Key extends _ERCAuditTrail.Key {

    }

    public ERCAuditTrail() {
        super();
    }
    
    public void init(EOEditingContext ec) {
        super.init(ec);
        setIsDeleted(false);
    }

    public void setObject(EOEnterpriseObject eo) {
        EOGlobalID gid;
        if (gid() == null) {
            if (eo instanceof ERXGenericRecord) {
                gid = ((ERXGenericRecord) eo).permanentGlobalID();
            } else {
                throw new IllegalArgumentException("Can't handle non ERXGenericRecord");
            }
            ERXKeyGlobalID keyGID = ERXKeyGlobalID.globalIDForGID((EOKeyGlobalID) gid);
            setGid(keyGID);
        }
    }

    public EOEnterpriseObject object() {
        EOKeyGlobalID gid = gid().globalID();
        return editingContext().faultForGlobalID(gid, editingContext());
    }

    public void createEntry(ERCAuditTrailType type, String keyPath, Object oldValue, Object newValue) {
        ERCAuditTrailEntry entry = ERCAuditTrailEntry.clazz.createAndInsertObject(editingContext());
        entry.setTrail(this);
        addToEntries(entry);
        entry.setKeyPath(keyPath);
        entry.setType(type);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        if(type == ERCAuditTrailType.DELETED) {
            setIsDeleted(true);
        }
    }
}
