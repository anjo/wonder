/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.util.Enumeration;
import er.extensions.ERXLogger;
import er.extensions.ERXConstant;

// For now this simply 
public class ERDConfirmMessage extends ERDCustomEditComponent {

    public ERDConfirmMessage(WOContext context) { super(context); }
    
    /** logging support */
    public final static ERXLogger log = ERXLogger.getERXLogger(ERDConfirmMessage.class);
    
    public String message;
    
    public boolean isStateless() { return true; }
    public boolean synchronizesVariablesWithBindings() { return false; }

    // Ok to do this, if they enter text then everything is A OK.
    public void awake() { message = hasBinding("defaultMessage") ? (valueForBinding("defaultMessage") == null ? "" : (String)valueForBinding("defaultMessage")) : ""; }
    
    public void reset() {
        super.reset();
        _confirmMessageKey = null;
        _list = null;
        _confirmMessageManditoryErrorMessage = null;
        _confirmMessageTextfieldSize = null;
        _confirmMessageTextfieldMaxlength = null;
        _confirmMessageExplanation = null;
    }
    
    private NSArray _list;
    public NSArray list() {
        if (_list == null) {
            if (object() != null && key() != null) {
                _list = (NSArray)objectKeyPathValue();
            } else if (object() != null) {
                _list = new NSArray(object());
            } else if (hasBinding("dataSource")) {
                _list = er.extensions.ERXExtensions.arrayFromDataSource((EODataSource)valueForBinding("dataSource"));
            } else {
                log.warn("ERConfirmMessage being used without the proper bindings");
                _list = ERXConstant.EmptyArray;
            }
            if (_list.count() == 0)
                log.warn("ERConfirmMessage: list set to zero");
        }
        return _list;
    }

    private String _confirmMessageKey = null;
    public String confirmMessageKey() {
        if (_confirmMessageKey == null)
            _confirmMessageKey = (String)valueForBinding("confirmMessageKey");
        return _confirmMessageKey;
    }

    private String _confirmMessageManditoryErrorMessage = null;
    public String  confirmMessageManditoryErrorMessage() {
        if (_confirmMessageManditoryErrorMessage == null) {
            _confirmMessageManditoryErrorMessage = (String)valueForBinding("confirmMessageManditoryErrorMessage");
            _confirmMessageManditoryErrorMessage = (_confirmMessageManditoryErrorMessage == null) ?
                "You must enter a <b>confirmation message</b>.":
                _confirmMessageManditoryErrorMessage;
        }
        return _confirmMessageManditoryErrorMessage;
    }
    private String _confirmMessageExplanation = null;
    public String confirmMessageExplanation() {
        if (_confirmMessageExplanation == null) {
            _confirmMessageExplanation = (String)valueForBinding("confirmMessageExplanation");
        }
        return _confirmMessageExplanation;
    }

    public boolean confirmMessageIsTextfield() {
        return booleanForBinding("confirmMessageIsTextfield");
    }

    private String _confirmMessageTextfieldSize = null;
    public String confirmMessageTextfieldSize() {
        if (_confirmMessageTextfieldSize == null) {
            _confirmMessageTextfieldSize = (String)valueForBinding("confirmMessageTextfieldSize");
            _confirmMessageTextfieldSize = (_confirmMessageTextfieldSize == null) ?
                "60":
                _confirmMessageTextfieldSize;
        }
        return _confirmMessageTextfieldSize;
    }
    
    private String _confirmMessageTextfieldMaxlength = null;
    public String confirmMessageTextfieldMaxlength() {
        if (_confirmMessageTextfieldMaxlength == null) {
            _confirmMessageExplanation = (String)valueForBinding("confirmMessageTextfieldMaxlength");
        }
        return _confirmMessageTextfieldMaxlength;
    }
    
    public void takeValuesFromRequest(WORequest r, WOContext c) {
        super.takeValuesFromRequest(r, c);
        if (list().count() > 0) {
            if ((message == null || message.equals("")) && booleanForBinding("confirmMessageManditory")) {
                validationFailedWithException(new NSValidation.ValidationException(confirmMessageManditoryErrorMessage()), list().objectAtIndex(0),
                                              confirmMessageKey());
            } else if (message != null && !message.equals("")){
                if (confirmMessageKey() == null)
                    throw new RuntimeException("You must specify a confirmMessageKey for this pageConfiguration!");
                if (log.isDebugEnabled())
                    log.debug("Setting message: " + message + " for key: " + confirmMessageKey() + " on eos: " + list());
                for (Enumeration e = list().objectEnumerator(); e.hasMoreElements();) {
                    EOEnterpriseObject eo = (EOEnterpriseObject)e.nextElement();
                    eo.takeValueForKeyPath(message, confirmMessageKey());
                }
            }
        } else {
            log.warn("List is zero.  If used in a confirm page template, need to set the object or datasource");
        }
    }
}
