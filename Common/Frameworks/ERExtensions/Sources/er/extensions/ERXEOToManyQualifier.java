/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import java.util.*;

/** @deprecated use ERXToManyQualifier instead */

public class ERXEOToManyQualifier extends ERXToManyQualifier implements Cloneable {

     public ERXEOToManyQualifier(EOEntity e,
                                String toManyKey,
                                NSArray elements) {
        super(e,toManyKey,elements);
    }

    /**
     * @deprecated use ERXEOAccessUtilities.primaryKeysForObjects(NSArray) instead
     */
    public static NSArray primaryKeysForObjectsFromSameEntity(NSArray eos) {
        return ERXEOAccessUtilities.primaryKeysForObjects(eos);           
    }

    /**
     * @deprecated use ERXEOAccessUtilities.snapshotsForRelationshipNamed(NSArray,String) instead
     */
    public static NSArray primaryKeysForObjectsFromSameEntity(String relKey, NSArray eos) {
        return ERXEOAccessUtilities.snapshotsForObjectsFromRelationshipNamed(eos,relKey);
    }
}