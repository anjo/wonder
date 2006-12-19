//
// ERDInspectButton.java: Class file for WO Component 'ERDInspectButton'
// Project ERDirectToWeb
//
// Created by bposokho on Mon Jan 06 2003
//
package er.directtoweb;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.InspectPageInterface;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;

import er.extensions.ERXEC;
import er.extensions.ERXEOControlUtilities;

public class ERDInspectButton extends ERDActionButton {

    public ERDInspectButton(WOContext context) {
        super(context);
    }
    
    public WOComponent inspectObjectAction() {
//    	CHECKME ak: I don't remember why we would use a local instance when we just want to inspect...
    	EOEditingContext context = (ERXEOControlUtilities.isNewObject(object()) ? object().editingContext() : ERXEC.newEditingContext());
    	context.lock();
    	try {
    		EOEnterpriseObject localObject = ERXEOControlUtilities.localInstanceOfObject(context, object());
    		String configuration = (String)valueForBinding("inspectConfigurationName");
    		InspectPageInterface epi = (InspectPageInterface)D2W.factory().pageForConfigurationNamed(configuration, session());
    		epi.setObject(localObject);
    		epi.setNextPage(context().page());
    		context.hasChanges(); // Ensuring it survives.
    		return (WOComponent)epi;
    	} finally {
    		context.unlock();
    	}
    }

}
