package er.coolcomponents;

import com.webobjects.appserver.WOContext;

/**
 * Tab panel that uses ajax update when switching tabs. Allows denial of tab switching. Useful when validation failures occur.<br />
 * 
 * @binding tabs a list of objects representing the tabs
 * @binding tabNameKey a string containing a key to apply to tabs to get the title of the tab
 * @binding selectedTab contains the selected tab
 * @binding tabClass CSS class to use for the selected tab
 * @binding nonSelectedTabClass CSS class to use for the unselected tabs
 * @binding submitActionName if this binding is non null, tabs will contain a submit button instead of a regular hyperlink and the action
 * @binding useFormSubmit true, if the form should be submitted before switching, allows denial of switches
 * @binding id CSS id for the wrapper div
 * 
 */
public class CCAjaxTabPanel extends CCTabPanel {
	
    public CCAjaxTabPanel(WOContext context) {
        super(context);
    }
    
}