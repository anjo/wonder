package er.directtoweb.components.attachments._ajax;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WComponent;

import er.attachment.model.ERAttachment;

/**
 * D2W component for editing ERAttachments
 * 
 * @author mendis
 *
 * @bindings object
 * @bindings propertyKey
 */
public class D2WEditAttachment extends D2WComponent {
    public D2WEditAttachment(WOContext context) {
        super(context);
    }
    
    // accessors
    public String configurationName() {
    	return object().entityName() + "." + propertyKey();
    }
    
    public ERAttachment attachment() {
    	return (ERAttachment) objectPropertyValue();
    }
    
    public void setAttachment(ERAttachment attachment) {
    	object().takeValueForKeyPath(attachment, propertyKey());
    }
    
    public String container() {
    	return (String) d2wContext().valueForKey("id") + "_container";
    }
    
    
    public String onComplete() {
    	return "function() { new Ajax.Updater('" + container() + "', $('" + container() + "').getAttribute('ref'), {evalScripts:true}); }";
    }

    // actions    
    public void removeAttachment() {
    	//ERAttachment attachment = (ERAttachment) objectPropertyValue();
    	//attachment.delete();
    	object().takeValueForKeyPath(null, propertyKey());
    }
}