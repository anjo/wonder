//
// ERDDHTMLComponent.java: Class file for WO Component 'ERDDHTMLComponent'
// Project simple
//
// Created by ak on Wed Mar 20 2002
//
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import er.extensions.*;

/**
 * Rich text edit component.<br />
 * 
 */

public class ERDDHTMLComponent extends ERDCustomEditComponent {
    static final ERXLogger log = ERXLogger.getERXLogger(ERDDHTMLComponent.class);

    String varName = null;

    public ERDDHTMLComponent(WOContext context) {
        super(context);
    }

    public boolean isStateless() {
	return false;
    }

    public boolean synchronizesVariablesWithBindings() {
        return false;
    }

    public void reset() {
        super.reset();
        varName = null;
    }
    
    public String varName()  {
	if(varName == null) {
	    varName = ERXExtensions.replaceStringByStringInString("-", "_", "dhtml-" + context().elementID().hashCode() + "-" + key());
	    varName = ERXExtensions.replaceStringByStringInString(".", "_", varName);
	    log.debug(varName);
	}
	return varName;
    }
    
    public void takeValuesFromRequest(WORequest q, WOContext c) throws NSValidation.ValidationException {
        super.takeValuesFromRequest(q,c);
        try {
            object().validateTakeValueForKeyPath(objectKeyPathValue(),key());
        } catch(Throwable e) {
            validationFailedWithException (e, objectKeyPathValue(), key());
        }
    }
}
