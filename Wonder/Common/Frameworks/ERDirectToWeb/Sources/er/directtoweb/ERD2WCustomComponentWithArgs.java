/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import er.extensions.ERXExceptionHolder;
import org.apache.log4j.Category;

// Useful component and important bug fix
// Fixes validation failures being propogated
// Adds valueForBinding that resolves in the d2wContext.
public class ERD2WCustomComponentWithArgs extends D2WCustomComponent implements ERXExceptionHolder {

   public ERD2WCustomComponentWithArgs(WOContext context) {
        super(context);
    }
    ///////////////////////////  log4j category  ///////////////////////////
    public final static Category cat = Category.getInstance(ERD2WCustomComponentWithArgs.class);

    private Object _extraBindings;
    public Object extraBindings() {
        _extraBindings=d2wContext().valueForKey("extraBindings");
        if (_extraBindings!=null) {
            if (_extraBindings instanceof String && ((String)_extraBindings).length()>0) {
                // assume it's a dict and parse it
                _extraBindings=NSPropertyListSerialization.propertyListFromString((String)_extraBindings);
            }
        }
        return _extraBindings;
    }

    // Done this way so that subClasses can always get the original valueForBinding.
    public Object originalValueForBinding(String binding) { return super.valueForBinding(binding); }
    
    public Object valueForBinding(String binding) {
        return hasBinding(binding) ? originalValueForBinding(binding) : nonCachingContext().valueForKey(binding);
    }

    public void validationFailedWithException (Throwable e, Object value, String keyPath) {
        parent().validationFailedWithException(e,value,keyPath);
    }

    public void clearValidationFailed(){
        if (parent() instanceof ERXExceptionHolder)
            ((ERXExceptionHolder)parent()).clearValidationFailed();
    }

    public boolean d2wDebuggingEnabled() {
        return ERDirectToWeb.d2wDebuggingEnabled(session());
    }
    public boolean d2wComponentNameDebuggingEnabled() {
        return ERDirectToWeb.d2wComponentNameDebuggingEnabled(session());
    }
    public boolean d2wPropertyKeyDebuggingEnabled() {
        return ERDirectToWeb.d2wPropertyKeyDebuggingEnabled(session());
    }
    
    // Needed to fix a caching bug.
    public D2WContext nonCachingContext() { return (D2WContext)super.valueForBinding("localContext"); }
}
