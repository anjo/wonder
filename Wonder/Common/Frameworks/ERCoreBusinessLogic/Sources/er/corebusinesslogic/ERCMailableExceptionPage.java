/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.corebusinesslogic;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

import er.extensions.ERXConfigurationManager;

public class ERCMailableExceptionPage extends WOComponent {

    public String errorMessage;
    public EOEnterpriseObject actor;
    public Throwable exception;
    public NSArray _reasonLines;
    public String currentReasonLine;
    public String formattedMessage;
    public NSDictionary extraInfo;

    public String currentUserInfoKey;
    public Object currentUserInfoValue;    
    
    public ERCMailableExceptionPage(WOContext aContext) {
        super(aContext);
    }

    public boolean isEventLoggingEnabled() {
        return false;
    }

    public void setException(Throwable value) {
        exception = value;
    }

    public void setActor(EOEnterpriseObject value) {
        actor = value;
    }

    public void setExtraInfo(NSDictionary value) {
        extraInfo = value;
    }

    public void setFormattedMessage(String value) {
        formattedMessage = value;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String shortErrorMessage() {
        return exception != null ? exception.getClass().getName() : errorMessage;
    }
    
    public void setReasonLines(NSArray reasonLines) {
        _reasonLines = reasonLines;
    }
    
    public NSArray reasonLines() {
        if (_reasonLines==null && exception!=null && exception.getMessage()!=null) {
            _reasonLines=NSArray.componentsSeparatedByString(exception.getMessage(), "\n");
        }
        return _reasonLines;
    }

    public NSTimestamp now() {
        return new NSTimestamp();
    }
    
    public String hostName() {
        return ERXConfigurationManager.defaultManager().hostName();
    }
}
