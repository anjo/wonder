package er.directtoweb;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.ConfirmPageInterface;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.D2WPage;

import er.extensions.ERXGuardedObjectInterface;
import er.extensions.ERXLocalizer;

/**
 * Delete button for repetitions. 
 *
 * @binding object
 * @binding dataSource
 * @binding d2wContext
 * @binding trashcanExplanation
 * @binding noTrashcanExplanation
 *
 * @created ak on Mon Sep 01 2003
 * @project ERDirectToWeb
 */

public class ERDDeleteButton extends ERDActionButton {

    /** logging support */
    private static final Logger log = Logger.getLogger(ERDDeleteButton.class);
	
    /**
     * Public constructor
     * @param context the context
     */
    public ERDDeleteButton(WOContext context) {
        super(context);
    }

    public boolean canDelete() {
        return object() != null && object() instanceof ERXGuardedObjectInterface ? ((ERXGuardedObjectInterface)object()).canDelete() : true;
    }

    public WOComponent deleteObjectAction() {
        ConfirmPageInterface nextPage = (ConfirmPageInterface)D2W.factory().pageForConfigurationNamed((String)valueForBinding("confirmDeleteConfigurationName"), session());
        nextPage.setConfirmDelegate(new ERDDeletionDelegate(object(), dataSource(), context().page()));
        nextPage.setCancelDelegate(new ERDPageDelegate(context().page()));
        D2WPage d2wPage = ((D2WPage)nextPage);
        
        String message = ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject("ERDTrashcan.confirmDeletionMessage", d2wContext()); 
        nextPage.setMessage(message);
        d2wPage.setObject(object());
        return (WOComponent) nextPage;
    }

    public String onMouseOverTrashcan() {
        return hasBinding("trashcanExplanation") ? "self.status='" + valueForBinding("trashcanExplanation") + "'; return true" : "";
    }

    public String onMouseOverNoTrashcan() {
        return hasBinding("noTrashcanExplanation") ? "self.status='" + valueForBinding("noTrashcanExplanation") + "'; return true" : "";
    }    
}
