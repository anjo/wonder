/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb.components.relationships;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WQueryToOneRelationship;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;

import er.extensions.eof.ERXEC;
import er.extensions.eof.qualifiers.ERXPrimaryKeyListQualifier;
import er.extensions.foundation.ERXValueUtilities;

/**
 * Enhanced relationship query component to to-one relationships.
 * @d2wKey multiple when true, the user can choose multiple items
 * @d2wKey restrictedChoiceKey keypath off the component that returns the list of objects to display
 * @d2wKey restrictingFetchSpecification name of the fetchSpec to use for the list of objects.
 */

public class ERD2WQueryToOneRelationship extends D2WQueryToOneRelationship {
    static final Logger log = Logger.getLogger(ERD2WQueryToOneRelationship.class);

    public ERD2WQueryToOneRelationship(WOContext context) {
        super(context);
    }

    public boolean hasMultipleSelection() {
        return ERXValueUtilities.booleanValue(d2wContext().valueForKey("multiple"));
    }
    
    public String componentName() {
        return !hasMultipleSelection() ? "ERXToOneRelationship" :  "ERXToManyRelationship";
    }
    
    public WOComponent self() {
        return this;
    }

    public void setValue(Object newValue) {
        if(hasMultipleSelection()) {
            if (newValue instanceof NSArray) {
                NSArray array = (NSArray) newValue;
                if(array.count() == 0) {
                    newValue = null;
                }
            }
            String operator = ERXPrimaryKeyListQualifier.IsContainedInArraySelectorName;
            displayGroup().queryOperator().takeValueForKey(operator, propertyKey());
        }
        super.setValue(newValue);
    }

    public Object restrictedChoiceList() {
        String restrictedChoiceKey = (String) d2wContext().valueForKey("restrictedChoiceKey");
        if (restrictedChoiceKey != null && restrictedChoiceKey.length() > 0) {
            return valueForKeyPath(restrictedChoiceKey);
        }
        String fetchSpecName = (String) d2wContext().valueForKey("restrictingFetchSpecification");
        if (fetchSpecName != null) {
            EOEditingContext ec = ERXEC.newEditingContext();
            EOEntity entity = d2wContext().entity();
            EORelationship relationship = entity.relationshipNamed((String) d2wContext().valueForKey("propertyKey"));
            ec.lock();
            try {
                return EOUtilities.objectsWithFetchSpecificationAndBindings(ec,
                        relationship.destinationEntity().name(), fetchSpecName, null);
            } finally {
                ec.unlock();
            }
        }
        return null;
    }
}
