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
import er.extensions.*;
import java.util.Enumeration;

/**
 * Provides a toOne relationship-like component except the value is pushed in as a string.<br />
 * 
 */

public class ERDEditStringWithChoices extends ERDCustomEditComponent {

    public ERDEditStringWithChoices(WOContext context) {super(context);}
    
    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERDEditStringWithChoices.class);
    
    public String entityForReportName;
    public ERXKeyValuePair currentElement;
    public ERXKeyValuePair selectedElement;

    public boolean synchronizesVariablesWithBindings() { return false; }
    public boolean isStateless() { return true; }
    
    protected NSArray _availableElements;
    public NSArray availableElements(){
        if(_availableElements==null){
            if(log.isDebugEnabled()) log.debug("key ="+key());
            String keyForAvailableObjects = key()+"Available";
            entityForReportName = (String)valueForBinding("entityNameForReport");
            _availableElements =
                ERDirectToWeb.displayableArrayForKeyPathArray((NSArray)object().valueForKeyPath(keyForAvailableObjects),
                                                              entityForReportName, ERXLocalizer.localizerForSession(session()).language());
            if(log.isDebugEnabled()) log.debug("availableElements = "+_availableElements);
        }
        return _availableElements;
    }

    public void reset(){
        super.reset();
        _availableElements = null;
        entityForReportName = null;
        selectedElement = null;
        currentElement = null;
    }

    public void takeValuesFromRequest(WORequest r, WOContext c) {
        super.takeValuesFromRequest(r, c);
        if (selectedElement!=null) {
            object().takeValueForKey(selectedElement.key(), key());
        } else {
            object().takeValueForKey(null, key());
        }
    }

    public void appendToResponse(WOResponse r, WOContext c) {
        entityForReportName = (String)object().valueForKey("entityForReportName");
        String chosenKey = (String)objectPropertyValue();
        if(log.isDebugEnabled()) log.debug("chosenKey = "+chosenKey);
        if(chosenKey!=null){
            for(Enumeration e = availableElements().objectEnumerator(); e.hasMoreElements();){
                ERXKeyValuePair keyValue = (ERXKeyValuePair)e.nextElement();
                if(keyValue.key().equals(chosenKey))
                   selectedElement = keyValue;
            }
            if(log.isDebugEnabled()) log.debug("selectedElement = "+selectedElement.key()+" , "+selectedElement.value());
        }
        super.appendToResponse(r,c);
    }
}
