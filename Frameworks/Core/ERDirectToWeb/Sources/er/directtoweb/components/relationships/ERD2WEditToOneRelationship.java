/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb.components.relationships;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WEditToOneRelationship;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEnterpriseObject;

import er.extensions.foundation.ERXUtilities;

/**
 * Improves superclass by adding restrictions on the choices and uses ERXToOneRelationship, thus can handle localization
 * and has better layout options.
 * @d2wKey restrictedChoiceKey keypath off the component that returns the list of objects to display
 * @d2wKey restrictingFetchSpecification name of the fetchSpec to use for the list of objects.
 */

public class ERD2WEditToOneRelationship extends D2WEditToOneRelationship {

    public ERD2WEditToOneRelationship(WOContext context) {
        super(context);
    }
    
    // Validation Support
    public void validationFailedWithException (Throwable e, Object value, String keyPath) {
        parent().validationFailedWithException(e,value,keyPath);
    }

    public Object restrictedChoiceList() {
        String restrictedChoiceKey=(String)d2wContext().valueForKey("restrictedChoiceKey");
        if( restrictedChoiceKey!=null &&  restrictedChoiceKey.length()>0 )
            return valueForKeyPath(restrictedChoiceKey);
        String fetchSpecName=(String)d2wContext().valueForKey("restrictingFetchSpecification");
        if(fetchSpecName != null && object().editingContext() != null) {
            EORelationship relationship = ERXUtilities.relationshipWithObjectAndKeyPath((EOEnterpriseObject)object(),
                                                                                        (String)d2wContext().valueForKey("propertyKey"));
            return EOUtilities.objectsWithFetchSpecificationAndBindings(object().editingContext(), relationship.destinationEntity().name(),fetchSpecName,null);
        }
        return null;
    }
}
