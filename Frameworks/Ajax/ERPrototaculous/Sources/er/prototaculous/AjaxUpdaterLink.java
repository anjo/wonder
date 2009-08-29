package er.prototaculous;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSDictionary;

import er.extensions.appserver.ERXWOContext;


/**
 * An Ajax.Updater as a link
 * @see AjaxUpdater
 * 
 * @binding queryDictionary		The form values used in the directAction that replaced $('container')
 * @binding confirmMessage		The message you wish to display in a confirm panel before executing the update
 * 
 * @author mendis
 *
 * TODO: Convert to dynamic element (for DA form elements support)
 */
public class AjaxUpdaterLink extends AjaxUpdater {	
    public AjaxUpdaterLink(WOContext context) {
        super(context);
    }
    
    @Override
    public boolean synchronizesVariablesWithBindings() {
    	return false;
    }
    
    @Override
    public boolean isStateless() {
    	return true;
    }
    
    /*
     * API/Bindings
     */
    public static interface Bindings extends AjaxUpdater.Bindings {
    	public static final String queryDictionary = "queryDictionary";
    	public static final String confirmMessage = "confirmMessage";
    }
    
    // accessors   
    @Override
    public String onClick() {
    	if (hasBinding(Bindings.confirmMessage)) {
    		return "if (confirm('" + confirmMessage() + "')) {new Ajax.Updater('" + container() + "', " + url() + ", {" + options() + "}); } return false;";
    	} else return super.onClick();
    }
    
    public String confirmMessage() {
    	return (String) valueForBinding(Bindings.confirmMessage);
    }
    
    @Override
    protected String url() {
    	if (hasBinding(Bindings.action) || hasBinding(Bindings.directActionName))
    		return "this.href";
    	else return super.url();
    }
    
    public String href() {
    	if (hasBinding(Bindings.action)) {
    		return ERXWOContext.ajaxActionUrl(context());
    	} else if (hasBinding(Bindings.directActionName)) {
    		NSDictionary queryDictionary = hasBinding(Bindings.queryDictionary) ? queryDictionary() : null;
    		return context().directActionURLForActionNamed(directActionName(), queryDictionary);
    	} else return "#";
    }

    public NSDictionary queryDictionary() {
    	return (NSDictionary) valueForBinding(Bindings.queryDictionary);
    }
    
    // actions
    public WOActionResults invokeAction() {
    	context()._setActionInvoked(true);
		if (hasBinding(Bindings.action))  {
			WOActionResults action = action();
			if (action instanceof WOComponent)  ((WOComponent) action)._setIsPage(true);	// cache is pageFrag cache
			return action;
		} else return context().page();
    }
}