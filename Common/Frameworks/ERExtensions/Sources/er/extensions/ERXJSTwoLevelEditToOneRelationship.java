/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

public class ERXJSTwoLevelEditToOneRelationship extends WOComponent {

    public ERXJSTwoLevelEditToOneRelationship(WOContext aContext) {
        super(aContext);
    }

    private NSArray _parentList=null;
    
/*
 type --> category --> defaultType

    sourceEntityName = Gift;
    relationshipKey = type;
    destinationDisplayKey = textDescription;
    restrictingRelationshipKey = category;
    restrictingDestinationDisplayKey = textDescription;
    defaultRestrictedRelationshipKey = defaultType;


 */
    public boolean synchronizesVariablesWithBindings() { return false; }

    public NSArray parentList() {
        if (_parentList==null) {
            // FIXME this list should be shared with all other JSTwoLevel.. sharing the same sourceEntityName!
            String entityName=(String)valueForBinding("sourceEntityName");
            EOEditingContext ec=((EOEnterpriseObject)valueForBinding("sourceObject")).editingContext();
            EOEntity sourceEntity=EOUtilities.entityNamed(ec,entityName);
            EORelationship r1=sourceEntity.relationshipNamed((String)valueForBinding("relationshipKey"));
            EOEntity childEntity=r1.destinationEntity();
            EORelationship r2= childEntity.relationshipNamed((String)valueForBinding("restrictingRelationshipKey"));
            EOEntity parentEntity=r2.destinationEntity();
            NSArray unsortedList=EOUtilities.objectsForEntityNamed(ec,parentEntity.name());
            EOSortOrdering sortOrdering=new EOSortOrdering((String)valueForBinding("restrictingRelationshipSortKey"),
                                                           EOSortOrdering.CompareAscending);
            NSMutableArray sortArray=new NSMutableArray(sortOrdering);
            String secondarySortKey=(String)valueForBinding("restrictingSecondarySortKey");
            if (secondarySortKey!=null && secondarySortKey.length()>0) {
                sortOrdering=new EOSortOrdering(secondarySortKey,
                                                EOSortOrdering.CompareAscending);
                sortArray.addObject(sortOrdering);
            }
            _parentList=EOSortOrdering.sortedArrayUsingKeyOrderArray(unsortedList, sortArray);
        }
        return _parentList;
    }

    public EOEnterpriseObject selectedParent() {
        EOEnterpriseObject selectedChild=selectedChild();
        return selectedChild!=null ?
            (EOEnterpriseObject)selectedChild.valueForKey((String)valueForBinding("restrictingRelationshipKey")) : null;
    }
    public void setSelectedParent(EOEnterpriseObject newValue) { // FIXME
        // do nothing, there is always a setSelectedChild with the JS subcomponent
    }
    public EOEnterpriseObject selectedChild() {
        return (EOEnterpriseObject)
        ((EOEnterpriseObject)valueForBinding("sourceObject")).valueForKey((String)valueForBinding("relationshipKey"));
    }
    public void setSelectedChild(EOEnterpriseObject newValue) { 
        ((EOEnterpriseObject)valueForBinding("sourceObject")).takeValueForKey(newValue,
                                                                              (String)valueForBinding("relationshipKey"));   
    }
}
