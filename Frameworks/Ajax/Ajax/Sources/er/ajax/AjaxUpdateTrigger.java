package er.ajax;

import java.util.Enumeration;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODynamicElement;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.association.WOAssociation;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;

/**
 * AjaxUpdateTrigger is useful if you have multiple containers on a page
 * that are controlled by a central parent component.  AjaxUpdateTrigger
 * allows you to pass in an array of containers that need to be updated.  An
 * example of this is if you have multiple editable areas on a page and only
 * one should be in edit mode at a time. If you put an AjaxUpdateTrigger 
 * inside the edit view, you can set the other components to not be in
 * edit mode and trigger all of the other update containers to update, 
 * reflecting their new non-editable status.
 * 
 * @binding updateContainerIDs an array of update container IDs to update
 * @binding resetAfterUpdate if true, the array of IDs will be cleared after appendToResponse
 * 
 * @author mschrag
 */
public class AjaxUpdateTrigger extends WODynamicElement {
	private NSDictionary _associations;
	private WOAssociation _updateContainerIDs;
	private WOAssociation _resetAfterUpdate;

	public AjaxUpdateTrigger(String name, NSDictionary associations, WOElement template) {
		super(name, associations, template);
		_associations = associations;
		_updateContainerIDs = (WOAssociation) associations.objectForKey("updateContainerIDs");
		_resetAfterUpdate = (WOAssociation) associations.objectForKey("resetAfterUpdate");
	}

	public void appendToResponse(WOResponse response, WOContext context) {
		super.appendToResponse(response, context);
		WOComponent component = context.component();
		NSArray updateContainerIDs = (NSArray) _updateContainerIDs.valueInComponent(component);
		if (updateContainerIDs != null && updateContainerIDs.count() > 0) {
			AjaxUtils.appendScriptHeader(response);
			Enumeration updateContainerIDEnum = updateContainerIDs.objectEnumerator();
			while (updateContainerIDEnum.hasMoreElements()) {
				String updateContainerID = (String) updateContainerIDEnum.nextElement();
				// PROTOTYPE FUNCTIONS
				Object evalScripts = AjaxUtils.valueForBinding("evalScripts", "true", _associations, component);
				response.appendContentString("if ($wi('" + updateContainerID + "')) { ");
				response.appendContentString("new Ajax.Updater('" + updateContainerID + "', $wi('" + updateContainerID + "').getAttribute('updateUrl'), {" + " evalScripts: " + evalScripts + ", insertion: Element.update });\n");
				response.appendContentString(" }");
			}
			AjaxUtils.appendScriptFooter(response);
	
			if (_resetAfterUpdate != null && _resetAfterUpdate.booleanValueInComponent(component)) {
				((NSMutableArray) updateContainerIDs).removeAllObjects();
			}
		}
	}

}
