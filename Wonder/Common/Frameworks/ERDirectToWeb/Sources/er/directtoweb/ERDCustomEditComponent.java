/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

import er.extensions.ERXArrayUtilities;

/**
 * Superclass for most of the custom edit components.  <br />
 * 
 */

public abstract class ERDCustomEditComponent extends ERDCustomComponent {

    /** logging support */
    public final static Logger log = Logger.getLogger(ERDCustomEditComponent.class);

    /** interface for all the keys used in this pages code */
    public static interface Keys {
        public static final String object = "object";
        public static final String localContext = "localContext";
        public static final String permissionToEdit = "permissionToEdit";
    }

    private NSArray _defaultSortOrderingsForDestinationEntity;

    /**
     * Public constructor
     * @param context current context
     */
    public ERDCustomEditComponent(WOContext context) {
        super(context);
    }

    //////////////////////////////////////// Instance Methods ////////////////////////////////////////////////////////////    
    private EOEnterpriseObject object;
    protected EOEditingContext editingContext;
    
    public Object objectPropertyValue() {
        return objectKeyPathValue();
    }
    public void setObjectPropertyValue(Object newValue) {
        setObjectKeyPathValue(newValue);
    }
    public Object objectKeyPathValue() {
        return key() != null && object() != null ? object().valueForKeyPath(key()) : null;
    }
    public void setObjectKeyPathValue(Object newValue) {
        if (key() != null && object() != null) object().takeValueForKeyPath(newValue,key());
    }

    public void setObject(EOEnterpriseObject newObject) {
        object=newObject;
        if (object!=null) // making sure the editing context stays alive
            editingContext=object.editingContext();
    }
    public EOEnterpriseObject object() {
        if (object==null && !synchronizesVariablesWithBindings())
            object=(EOEnterpriseObject)valueForBinding(Keys.object);
        return object;
    }
    
    public boolean isStateless() { return false; }
    public boolean synchronizesVariablesWithBindings() { return true; }

    /** Used by stateful but non-synching subclasses */
    public void resetCachedBindingsInStatefulComponent() {
        super.resetCachedBindingsInStatefulComponent();
        object = null;
        editingContext = null;
        _defaultSortOrderingsForDestinationEntity = null;
    }

    // Used by stateless subclasses
    public void reset() {
        super.reset();
        object = null;
        editingContext = null;
        _defaultSortOrderingsForDestinationEntity = null;
    }

    // Defaults to true when not used in a D2W component.  In the rules the default is false.
    public boolean permissionToEdit() {
        return hasBinding(Keys.permissionToEdit) ? booleanValueForBinding(Keys.permissionToEdit) : true;
    }

    /**
     * Computes the destination entity that we're editing.  Hits the "destinationEntityName"
     * rule.
     *
     * @return destination entity
     */
    public EOEntity destinationEntity() {
        final String destinationEntityName = (String)valueForBinding("destinationEntityName");
        return destinationEntityName != null ? EOUtilities.entityNamed(object().editingContext(), destinationEntityName) : null;
    }

    /**
     * Hits the destinationEntityName rule to get the destination entity.  Grabs a private D2WContext, sets the
     * destination entity and asks for the defaultSortOrdering rule.
     *
     * @return an array of sort orderings for the destination entity.
     */
    public NSArray defaultSortOrderingsForDestinationEntity() {
        if (_defaultSortOrderingsForDestinationEntity == null) {
            final D2WContext context = new D2WContext();
            final NSArray sortOrderingDefinition;
            final int sortOrderingDefinitionCount;
            NSMutableArray sortOrderings = null;
            int i = 0;

            context.setEntity(destinationEntity());
            sortOrderingDefinition = (NSArray)context.valueForKey("defaultSortOrdering");
            sortOrderingDefinitionCount = sortOrderingDefinition != null ? sortOrderingDefinition.count() : 0;

            while (i < sortOrderingDefinitionCount) {
                final String key = (String)sortOrderingDefinition.objectAtIndex(i++);
                final String selectorKey = (String)sortOrderingDefinition.objectAtIndex(i++);
                final EOSortOrdering sortOrdering = new EOSortOrdering(key, ERXArrayUtilities.sortSelectorWithKey(selectorKey));
                (sortOrderings != null ? sortOrderings : (sortOrderings = new NSMutableArray())).addObject(sortOrdering);
            }

            _defaultSortOrderingsForDestinationEntity = sortOrderings != null ? sortOrderings.immutableClone() : NSArray.EmptyArray;
        }

        return _defaultSortOrderingsForDestinationEntity;
    }

}