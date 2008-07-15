package er.ajax;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;

/**
 * AjaxBusyIndicator provides various ways of performing operations when an Ajax requst is in process.
 * 
 * @binding busyClass (optional) the CSS class to apply to the updating element during the request
 * @binding divID (optional) the id of the div to show and hide during the request
 * @binding onCreate (optional) the function to execute when the request starts
 * @binding onComplete (optional) the function to execute when the request ends
 * @binding busyImage (optional) if set, a busy div will be automatically created with this image in it
 * @binding busyImageFramework (optional) the framework that contains the busy image
 * @binding watchContainerID (optional) if set, the other bindings will only apply when this container ID is being
 *          updated, which provides for per-element busy controls
 * @binding id (optional) if bound, you can provide a custom style for the generated busy image div
 * @binding class (optional) if bound, you can provide a custom style for the generated busy image div 
 * @binding style (optional) if bound, you can provide a custom style for the generated busy image div 
 * @author mschrag
 */
public class AjaxBusyIndicator extends AjaxComponent {
	public AjaxBusyIndicator(WOContext context) {
		super(context);
	}

	public boolean isStateless() {
		return true;
	}

	public boolean synchronizesVariablesWithBindings() {
		return false;
	}

	protected void addRequiredWebResources(WOResponse res) {
		addScriptResourceInHead(res, "prototype.js");
		addScriptResourceInHead(res, "effects.js");
	}

	public boolean customStyle() {
		return hasBinding("id") || hasBinding("class") || hasBinding("style");
	}

	public String style() {
		String style = (String) valueForBinding("style", "display: none");
		return style;
	}

	public String divID() {
		String id = (String) valueForBinding("divID");
		if (id == null) {
			id = (String) valueForBinding("id");
			if (id == null) {
				id = "busy";
			}
		}
		return id;
	}

	public String onCreate() {
		return (String) valueForBinding("onCreate", "null");
	}

	public String onComplete() {
		return (String) valueForBinding("onComplete", "null");
	}

	public WOActionResults handleRequest(WORequest request, WOContext context) {
		return null;
	}

}
