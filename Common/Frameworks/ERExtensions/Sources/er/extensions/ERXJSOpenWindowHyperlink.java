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
import java.lang.*;
import com.webobjects.directtoweb.*;
import org.apache.log4j.Category;

public class ERXJSOpenWindowHyperlink extends WOComponent {

    public ERXJSOpenWindowHyperlink(WOContext aContext) {
        super(aContext);
    }

    ///////////////////////////////////////  log4j category  //////////////////////////////////////////
    public static final Category cat = Category.getInstance(ERXJSOpenWindowHyperlink.class);

    public boolean isStateless() { return true; }
    public boolean synchronizesVariablesWithBindings() { return false; }

    public boolean isDirectAction() {
        return valueForBinding("directActionName") != null;
    }

    // see EROpenJSWindowSubmitButton for the purpose of this method
    public String contextComponentActionURL() {
        return context().componentActionURL();
    }
    
    public String openWindow() {
        StringBuffer result = new StringBuffer("javascript:win=window.open('");
        if (valueForBinding("href")!=null) {
            result.append(valueForBinding("href"));
        } else if (valueForBinding("directActionName") == null) {
            result.append(contextComponentActionURL());
        } else {
            String anActionName;
            if (valueForBinding("actionClass") == null) {
                anActionName = (String)valueForBinding("directActionName");
            } else {
                anActionName = (String)valueForBinding("actionClass") + "/" + (String)valueForBinding("directActionName");
            }
            result.append(context().directActionURLForActionNamed(anActionName, (NSDictionary)valueForBinding("queryDictionary")));
            ERXExtensions.addRandomizeDirectActionURL(result);
        }
        String fragment=(String)valueForBinding("fragment");
        if (fragment!=null)
            result.append("#"+fragment);
        result.append("','"+valueForBinding("target"));
        result.append("','width="+valueForBinding("width"));
        result.append(",height="+valueForBinding("height"));
        result.append(",location=no");
        result.append(",scrollbars="+valueForBinding("scrollbars"));
        result.append(",menubar="+valueForBinding("menubar"));
        result.append(",toolbar="+valueForBinding("toolbar"));
        result.append(",titlebar="+valueForBinding("titlebar"));
        result.append(",resizable="+valueForBinding("resizable"));
        result.append(",dependant=yes");
        result.append("'); win.focus(); return false;");
        return result.toString();
    }

    public WOActionResults action() {
        return (WOActionResults)valueForBinding("action");
    }
}
