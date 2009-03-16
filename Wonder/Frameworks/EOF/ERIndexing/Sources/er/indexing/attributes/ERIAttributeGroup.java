package er.indexing.attributes;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

import er.indexing.ERAttributeIndex;
import er.indexing.ERIndex;

public class ERIAttributeGroup extends _ERIAttributeGroup {

    @SuppressWarnings("unused")
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ERIAttributeGroup.class);

    public static final ERIAttributeGroupClazz clazz = new ERIAttributeGroupClazz();

    public static class ERIAttributeGroupClazz extends _ERIAttributeGroup._ERIAttributeGroupClazz {
        public ERIAttributeGroup attributeGroupForName(EOEditingContext ec, String name) {
            return objectMatchingKeyAndValue(ec, Key.NAME, name);
        }
    }

    public interface Key extends _ERIAttributeGroup.Key {
    }

    public void init(EOEditingContext ec) {
        super.init(ec);
    }

    public NSArray<ERIAttributeGroup> groups() {
        NSMutableArray<ERIAttributeGroup> result = new NSMutableArray<ERIAttributeGroup>();
        ERIAttributeGroup current = this;

        result.addObject(current);
        while (current.parent() != null) {
            result.insertObjectAtIndex(current, 0);
            current = current.parent();
        }
        return result;
    }

    public NSArray<ERIAttribute> allAttributes() {
        NSMutableArray<ERIAttribute> result = new NSMutableArray<ERIAttribute>();
        for (ERIAttributeGroup group : groups()) {
            result.addObjectsFromArray(attributes());
        }
        return result;
    }

    public ERIAttribute attributeForName(String name) {
        for (ERIAttribute attribute : allAttributes()) {
            if (attribute.name().equals(name)) {
                return attribute;
            }
        }
        return null;
    }

    public ERIDocument documentForGlobalID(EOEditingContext editingContext, EOKeyGlobalID permanentGlobalID) {
        ERIDocument document = new ERIDocument(this, permanentGlobalID);
        return document;
    }

    public synchronized ERIndex index() {
        ERAttributeIndex index = ERAttributeIndex.indexNamed(name());
        return index;
    }
}
