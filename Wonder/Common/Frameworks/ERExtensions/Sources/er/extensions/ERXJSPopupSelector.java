/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

/**
 * Nice guy for performing actions when a popup item is selected.<br />
 * 
 * @binding string
 * @binding list
 * @binding selectsItem
 * @binding popupName
 * @binding doNotAddOneToComputedIndex" defaults="Boolean
 */

public class ERXJSPopupSelector extends WOComponent {

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXJSPopupSelector.class);
    
    
    public ERXJSPopupSelector(WOContext aContext) {
        super(aContext);
    }

    public boolean isStateless() { return true; }
    
    public String onClickString() {
        String result=null;
        Object item=valueForBinding("selectsItem");
        NSArray list=(NSArray)valueForBinding("list");
        String popupName=(String)valueForBinding("popupName");
        if (list!=null && item!=null) {
            int index=list.indexOfObject(item);
            if (index==-1) {
                log.info(item+" could not be found in "+list);
            }
            // by default we assume that there is one more item on top of the list (i.e. - none - or - pick one -)
            // when the relationship is mandatory, this is not the case
            boolean doNotAddOne=ERXValueUtilities.booleanValueForBindingOnComponentWithDefault("doNotAddOneToComputedIndex",
                                                                                          this,
                                                                                          false);
            if (!doNotAddOne) index++;
            String formName = null;
            if(context() instanceof ERXMutableUserInfoHolderInterface) {
                formName = (String)((ERXMutableUserInfoHolderInterface)context()).mutableUserInfo().valueForKey("formName");
            }
            if(formName == null)
                formName = "forms[2]";
            result="javascript:window.document."+formName+"."+popupName+".selectedIndex="+index+"; return false;";
        }
        return result;
    }
}
