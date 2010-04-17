package er.ajax;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;

import er.extensions.appserver.ERXWOContext;
import er.extensions.appserver.ajax.ERXAjaxApplication;
import er.extensions.foundation.ERXValueUtilities;

/**
 * This abstract (by design) superclass component isolate general utility methods.
 * 
 * @author Jean-Francois Veillette <jean.francois.veillette@gmail.com>
 * @version $Revision $, $Date $ <br>
 *          &copy; 2006 OS communications informatiques, inc. http://www.os.ca
 *          Tous droits réservés.
 */

public abstract class AjaxComponent extends WOComponent implements IAjaxElement {
    /** logging */
    protected Logger log = Logger.getLogger(getClass());

    public AjaxComponent(WOContext context) {
        super(context);
    }

    /**
     * Utility to get the value of a binding or a default value if none is
     * supplied.
     * @param name name of the binding
     * @param defaultValue value to return if unbound
     * @return value for binding or defaultValue value if unbound
     */
    public Object valueForBinding(String name, Object defaultValue) {
        Object value = defaultValue;
        if(hasBinding(name)) {
            value = valueForBinding(name);
        }
        return value;
    }
    
    /**
     * Utility to get the boolean value of a binding or a default value if none is
     * supplied.  Handles non-boolean bindings Numbers, NSArray, String, NSKeyValueCoding.
     * @param name name of the binding
     * @param defaultValue value to return if unbound
     * @return value for binding or defaultValue value if unbound
     */
    public boolean booleanValueForBinding(String name, boolean defaultValue) {
    	boolean value = defaultValue;
        if (hasBinding(name)) {
			Object boundValue = valueForBinding(name);
			value = ERXValueUtilities.booleanValue(boundValue);
		}
        return value;
    }
    
    public Object valueForBinding(String name, WOComponent component) {
    	return valueForBinding(name, (Object)null);
    }
    
	public Object valueForBinding(String name, Object defaultValue, WOComponent component) {
		return valueForBinding(name, (Object)defaultValue);
	}

    protected void addScriptResourceInHead(WOResponse _response, String _fileName) {
    	AjaxUtils.addScriptResourceInHead(context(), _response, _fileName);
    }

    protected void addScriptResourceInHead(WOResponse _response, String _framework, String _fileName) {
	AjaxUtils.addScriptResourceInHead(context(), _response, _framework, _fileName);
    }
    
    protected void addStylesheetResourceInHead(WOResponse _response, String _fileName) {
      AjaxUtils.addStylesheetResourceInHead(context(), _response, _fileName);
    }
    
    protected void addStylesheetResourceInHead(WOResponse _response, String _framework, String _fileName) {
	AjaxUtils.addStylesheetResourceInHead(context(), _response, _framework, _fileName);
    }

    /**
     * Execute the request, if it's coming from our action, then invoke the
     * ajax handler and put the key <code>AJAX_REQUEST_KEY</code> in the
     * request userInfo dictionary (<code>request.userInfo()</code>).
     */
    public WOActionResults invokeAction(WORequest request, WOContext context) {
        Object result;
        if (shouldHandleRequest(request, context)) {
            result = handleRequest(request, context);
            AjaxUtils.updateMutableUserInfoWithAjaxInfo(context());
            if (result == null && !ERXAjaxApplication.isAjaxReplacement(request)) {
            	result = AjaxUtils.createResponse(request, context);
            }
        } else {
            result = super.invokeAction(request, context);
        }
        return (WOActionResults) result;
    }

    /**
     * Returns the ID that represents this container for the purposes of Ajax updates. In common cases,
     * this corresponds to your updateContainerID.
     * 
     * @param context the current context
     * @return your container ID
     */
    protected String _containerID(WOContext context) {
    	return null;
    }
	
    protected boolean shouldHandleRequest(WORequest request, WOContext context) {
    	return AjaxUtils.shouldHandleRequest(request, context, _containerID(context));
	}
	
    public String safeElementID() {
    	String id = (String)valueForBinding("id");
    	if(id == null) {
    		return ERXWOContext.safeIdentifierName(context(), false);
    	}
    	return id;
    }

    /**
     * Overridden to call {@link #addRequiredWebResources(WOResponse)}.
     */
    public void appendToResponse(WOResponse res, WOContext ctx) {
        super.appendToResponse(res, ctx);
        addRequiredWebResources(res);
    }


	public void appendTagAttributeToResponse(WOResponse response, String name, Object object) {
		if (object != null) {
			response._appendTagAttributeAndValue(name, object.toString(), true);
		}
	}
	
    /**
     * Override this method to append the needed scripts for this component.
     * @param res
     */
    protected abstract void addRequiredWebResources(WOResponse res);
    
    /**
     * Override this method to return the response for an Ajax request.
     * @param request
     * @param context
     */
    public abstract WOActionResults handleRequest(WORequest request, WOContext context);

}
