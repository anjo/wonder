/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import er.extensions.*;

// A VERY USEFUL COMPONENT - has some crusty stuff in it, but look mainly at valueForBinding and hasBinding.
// 	this guy works in conjunction with D2WCustomComponentWithArgs
public abstract class ERDCustomEditComponent extends WOComponent {

    /** logging support */
    public final static ERXLogger log = ERXLogger.getERXLogger(ERDCustomEditComponent.class);
    
    /**
     * Public constructor
     * @param context current context
     */
   public ERDCustomEditComponent(WOContext context) {
        super(context);
    }

    protected static Integer TRUE = ERXConstant.OneInteger;
    protected static Integer FALSE = ERXConstant.ZeroInteger;

    //////////////////////////////////////// Instance Methods ////////////////////////////////////////////////////////////    
    private EOEnterpriseObject object;
    protected EOEditingContext editingContext;
    private String key;
    protected NSDictionary extraBindings;

    // Validation Support
    public void validationFailedWithException (Throwable e, Object value, String keyPath) {
        parent().validationFailedWithException(e,value,keyPath);
    }
    public void clearValidationFailed() {
        // Since this component can be used stand alone, we might not necessarily
        // have an exception holder as our parent --> testing
        if (parent() instanceof ERXExceptionHolder)
            ((ERXExceptionHolder)parent()).clearValidationFailed();
    }
    
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
    public void setKey(String newKey) { key=newKey; }
    
/*  A tiny bit of performance
    public void appendToResponse(WOResponse r, WOContext c) {
        WOComponent parent=parent();
        if (parent() instanceof D2WCustomComponent &&
            !(parent() instanceof D2WCustomComponentWithArgs)) {
            throw new RuntimeException("Validation for "+key()+" will not properly proceed with D2WCustomComponent -- Use D2WCustomComponentWithArgs");
        }
        super.appendToResponse(r,c);
    }
*/
    
    // Seemless integration of extraBindings into valueForBinding and hasBinding.
    // Note that this should only be used for non-synchronizing components

    public boolean isStateless() { return false; }
    public boolean synchronizesVariablesWithBindings() { return true; }
    // Used by stateless subclasses
    public void reset() {
        super.reset();
        extraBindings = null;
        key = null;
        object = null;
        d2wContext = null;
    }

    public void setExtraBindings(NSDictionary value) { extraBindings = value; }
    public NSDictionary extraBindings() {
        if (extraBindings == null && !synchronizesVariablesWithBindings())
            extraBindings = (NSDictionary)super.valueForBinding("extraBindings");
        return extraBindings;
    }

    public String key() {
        if (key==null && !synchronizesVariablesWithBindings())
            key=(String)super.valueForBinding("key");
        if (key==null && !synchronizesVariablesWithBindings() && d2wContext() != null)
            key=(String)d2wContext().valueForKey("propertyKey");
        return key;
    }

    public EOEnterpriseObject object() {
        if (object==null && !synchronizesVariablesWithBindings())
            object=(EOEnterpriseObject)super.valueForBinding("object");
        return object;
    }


    public void appendToResponse(WOResponse r, WOContext c) {
        try {
            // in the case where we are non-synchronizing but not stateless, make sure we pull again
            if (!synchronizesVariablesWithBindings() && !isStateless()) {
                reset();
            }
            super.appendToResponse(r,c);
        } catch(Exception ex) {
            ERDirectToWeb.reportException(ex, d2wContext());
        }
    }

    
    public boolean hasBinding(String binding) {
        // FIXME:  Turn this check off in production
        if (synchronizesVariablesWithBindings()) {
            throw new RuntimeException("HasBinding being used in a subclass of CustomEditComponent that synchronizesVariablesWithBindings == true");
        }
        return (super.hasBinding(binding) || valueForBinding(binding) != null);
    }

    // Defaults to true when not used in a D2W component.  In the rules the default is false.
    public boolean permissionToEdit() { return hasBinding("permissionToEdit") ? booleanForBinding("permissionToEdit") : true; }

    /**
        deprecated
     **/
    public boolean booleanForBinding(String binding) {        
        return booleanValueForBinding(binding);
    }
    public boolean booleanValueForBinding(String binding) {
        return  ERXUtilities.booleanValue(valueForBinding(binding));
    }

    public Integer integerBooleanForBinding(String binding) {
        return booleanForBinding(binding) ? ERDCustomEditComponent.TRUE : ERDCustomEditComponent.FALSE;
    }

    protected D2WContext d2wContext;
    public void setD2wContext(D2WContext value) {
        d2wContext = value;
    }
    public void setLocalContext(D2WContext value) {
        setD2wContext(value);
    }
    public D2WContext localContext() {
        return d2wContext();
    }
    protected D2WContext d2wContextFromBindings() {
        if (d2wContext == null && !synchronizesVariablesWithBindings()) {
            d2wContext = (D2WContext)super.valueForBinding("localContext");
            if(d2wContext == null) {
                d2wContext = (D2WContext)super.valueForBinding("d2wContext");
            }
        }
        return d2wContext;
    }
    public D2WContext d2wContext() {
        return d2wContextFromBindings();
    }

    public Object valueForBinding(String binding) {
        Object value=null;
        if (log.isDebugEnabled()) {
            log.debug("***** CustomEditComponent.valueForBinding(binding = "+binding+")");
            log.debug("***** CustomEditComponent: parent(): + (" + ((parent() == null) ? "null" : parent().getClass().getName()) + ")");
            log.debug("                           " + parent());
            log.debug("***** CustomEditComponent: parent() instanceof CustomEditComponent == " + (parent() instanceof ERDCustomEditComponent));
            log.debug("***** CustomEditComponent: parent() instanceof D2WCustomComponentWithArgs == " + (parent() instanceof ERD2WCustomComponentWithArgs));
            log.debug("***** CustomEditComponent: parent() instanceof D2WStatelessCustomComponentWithArgs == " + (parent() instanceof ERD2WStatelessCustomComponentWithArgs));
            log.debug("***** CustomEditComponent: parent() instanceof D2WCustomQueryComponentWithArgs == " + (parent() instanceof ERDCustomQueryComponentWithArgs));
        }
        if (super.hasBinding(binding)) {
            log.debug("***** CustomEditComponent: super.hasBinding(binding) == true");
            value = super.valueForBinding(binding);
            log.debug("***** CustomEditComponent: value = " + value);
        } else  if(d2wContextFromBindings() != null) {
            value = d2wContextFromBindings().valueForKey(binding);
        } else {
            WOComponent parent=parent();
            if (parent instanceof ERDCustomEditComponent ||
                parent instanceof ERD2WCustomComponentWithArgs ||
                parent instanceof ERD2WStatelessCustomComponentWithArgs ||
                parent instanceof ERDCustomQueryComponentWithArgs) {
                log.debug("***** CustomEditComponent: inside the parent instanceof branch");
                // this will eventually bubble up to a D2WCustomComponentWithArgs, where it will (depending on the actual binding)
                // go to the d2wContext
                value = parent.valueForBinding(binding);
            }
        }
        if (value == null && binding != null && extraBindings() != null) {
            log.debug("***** CustomEditComponent: inside the extraBindings branch");
            value = extraBindings().objectForKey(binding);
        }
        if (log.isDebugEnabled()) {
            if (value != null)
                log.debug("***** CustomEditComponent: returning value: (" + value.getClass().getName() + ")" + value);
            else
                log.debug("***** CustomEditComponent: returning value: null");
        }
        return value;
    }

}
