package er.prototaculous.widgets;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

import er.extensions.appserver.ERXWOContext;

import com.webobjects.appserver.WOActionResults;

/**
 * Encapsulates http://www.stickmanlabs.com/lightwindow 2.0
 *
 * Extending the api of WOSubmitButton
 *
 * 
 * @author mendis
 */

public class LightWindowButton extends LightWindow {

    public LightWindowButton(WOContext context) {
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
    
    // accessors
    public String onClick() {
    	return "myLightWindow.activateWindow({ " + options() + " }); return false;";
    }
    
    private NSArray<String> _options() {
    	NSMutableArray<String> _options = new NSMutableArray<String>();
    	
    	_options.add("href: '" + href() + "'");
    	if (hasBinding(Bindings.formID)) _options.add("form: '" + formID() + "'");
    	_options.add("type: '" + type + "'");
    	_options.add("rel: 'submitForm'");
    	if (hasBinding(Bindings.height) && valueForBinding(Bindings.height) != null) _options.add("height: " + valueForBinding(Bindings.height));
    	if (hasBinding(Bindings.width) && valueForBinding(Bindings.width) != null) _options.add("width: " + valueForBinding(Bindings.width));
    	if (hasBinding(Bindings.top) && valueForBinding(Bindings.top) != null) _options.add("top: " + valueForBinding(Bindings.top));
    	if (hasBinding(Bindings.left) && valueForBinding(Bindings.left) != null) _options.add("left: " + valueForBinding(Bindings.left));
    	if (hasBinding(Bindings.title) && valueForBinding(Bindings.left) != null) _options.add("title: '" + valueForBinding(Bindings.title) + "'");
    	
    	return _options.immutableClone();
    }
    
    public String options() {
    	return _options().componentsJoinedByString(",");
    }
    
    @SuppressWarnings("unchecked")
	public String href() {
    	if (hasBinding(Bindings.action))
    		return ERXWOContext.ajaxActionUrl(context());
    	else if (hasBinding(Bindings.directActionName)) {
    		String directActionName = (String) valueForBinding(Bindings.directActionName);
    		NSDictionary queryDictionary = (NSDictionary) valueForBinding(Bindings.queryDictionary);
    		
    		return context().directActionURLForActionNamed(directActionName, queryDictionary);
    	} else return null;
    }
    
    // actions
    public WOActionResults invokeAction() {
		if (hasBinding(Bindings.action)) {
			WOActionResults action = (WOActionResults) valueForBinding(Bindings.action);
			if (action instanceof WOComponent)  ((WOComponent) action)._setIsPage(true);	// cache is pageFrag cache
			return action;
		} else return context().page();
    }
}