package er.modern.directtoweb.components.buttons;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.EditPageInterface;
import com.webobjects.directtoweb.SelectPageInterface;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;

import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXGuardedObjectInterface;
import er.extensions.foundation.ERXValueUtilities;

/**
 * Edit button for repetitions
 * 
 * @binding object
 * 
 * @d2wKey isEntityEditable
 * @d2wKey editConfigurationName
 * @d2wKey classForEditObjButton
 * @d2wKey editButtonLabel
 * 
 * @author davidleber
 * @project ERModernDirectToWeb
 */
public class ERMDEditButton extends ERMDActionButton {
	
	private static final Logger log = Logger.getLogger(ERMDEditButton.class);
	
	public interface Keys extends ERMDActionButton.Keys {
		public static final String editButtonLabel = "editButtonLabel";
		public static final String classForEditObjButton = "classForEditObjButton";
		public static final String editConfigurationName = "editConfigurationName";
	}
	
	public ERMDEditButton(WOContext context) {
        super(context);
    }

	/**
	 * Label for the edit button
	 * <p>
     * Defaults to "Edit"
     * 
	 * @return
	 */
	public String buttonLabel() {
		if (_buttonLabel == null) {
			_buttonLabel = stringValueForBinding(Keys.editButtonLabel, "Edit");
		}
		return _buttonLabel;
	}
    
	/**
	 * CSS class for the edit button.
	 * <p>
	 * Defaults to "Button ObjButton EditObjButton"
	 * 
	 * @return
	 */
	public String buttonClass() {
		if (_buttonClass == null) {
			_buttonClass = stringValueForBinding(Keys.classForEditObjButton, "Button ObjButton EditObjButton");
		}
		return _buttonClass;
	}
	
	/**
	 * Action performed by the edit button
	 * 
	 * @return
	 */
	public WOComponent editObjectAction() {
		WOComponent result = null;
		if (shouldAllowInlineEditing()) {
	    	EOEditingContext ec = ERXEC.newEditingContext(object().editingContext());
	    	EOEnterpriseObject localObj = ERXEOControlUtilities.localInstanceOfObject(ec, object());
	    	SelectPageInterface parent = parentSelectPage();
	        if(parent != null) {
	        	d2wContext().takeValueForKey("edit", Keys.inlineTask);
	            parent.setSelectedObject(localObj);
	        } else {
	        	throw new IllegalStateException("This page is not an instance of SelectPageInterface. I can't select here.");
	        }
	    } else {
			result = editObjectInPageAction();
		}
		return result;
	}
	
	/**
	 * Determines whether the entity is editable
	 * 
	 * @return
	 */
    public boolean isEditable() {
        boolean result = ERXValueUtilities.booleanValue(d2wContext().valueForKey(Keys.isEntityEditable));
        Object o = object();
        if (o instanceof ERXGuardedObjectInterface) {
            result = result && ((ERXGuardedObjectInterface)o).canUpdate();
        }
        return result;
    }

    /**
     * Action performed by edit button if in-line editing is disabled
     * 
     * @return
     */
    public WOComponent editObjectInPageAction() {
        EOEnterpriseObject localObject = localInstanceOfObject();
        String configuration = (String)valueForBinding(Keys.editConfigurationName);
        if(log.isDebugEnabled()){
           log.debug("configuration = "+configuration);
        }
        EditPageInterface epi = (EditPageInterface)D2W.factory().pageForConfigurationNamed(configuration, session());
        epi.setObject(localObject);
        epi.setNextPage(context().page());
        localObject.editingContext().hasChanges(); // Ensuring it survives.
        return (WOComponent)epi;
    }
   
}