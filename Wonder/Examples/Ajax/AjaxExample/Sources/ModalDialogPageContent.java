
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

import er.ajax.*;


/**
 * Simple component used as content of an AjaxModalDialog via pageName binding.
 */
public class ModalDialogPageContent extends WOComponent {
	
	private boolean isSecondConfirmation;
	
	
    public ModalDialogPageContent(WOContext context) {
        super(context);
    }
    
    /**
     * Binding for WOString that is in AjaxUpdateContainer "ConfirmationMessage". Returns an initial or
     * repeated confirmation message.
     */
    public String confirmationMessage() {
    	NSLog.out.appendln("confirmationMessage called");
    	return isSecondConfirmation ? "Are you really, really, really sure you want to delete this?" : "Are you sure you want to delete this?";
    }
    
    
    /** 
     * Ajax method that is called when deletion is confirmed in the Ajax Dialog with the Yes2 hyperlink.
     * This shows how to update the dialog contents and how to close the box from Java.
     */
    public WOActionResults deleteIt() {
    	NSLog.out.appendln("deleteIt in ModalDialogPageContent called");
    	isSecondConfirmation = ! isSecondConfirmation;
    	
    	if (isSecondConfirmation) {
    		AjaxModalDialog.update(context(), "Think again...");
    	} else {
    		AjaxModalDialog.close(context());
    	}

    	return null;
    }
}