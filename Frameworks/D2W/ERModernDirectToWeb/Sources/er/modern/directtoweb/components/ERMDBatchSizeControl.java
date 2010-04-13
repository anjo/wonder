package er.modern.directtoweb.components;

import com.webobjects.appserver.WOContext;

import er.ajax.AjaxUpdateContainer;
import er.directtoweb.components.ERDCustomComponent;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.localization.ERXLocalizer;

/**
 * A modern batch size controll that uses an AjaxInplaceEditor to edit
 * the batch size.
 * 
 * @d2wKey itemString
 * @d2wKey showString
 * @d2wKey separatorString
 * @d2wKey updateContainerID
 * @d2wKey localizer
 * 
 * @author davidleber
 *
 */
public class ERMDBatchSizeControl extends ERDCustomComponent {
	
	public static interface Keys {
		 public static final String itemString = "itemString";
		 public static final String showString = "showString";
		 public static final String separatorString = "separatorString";
		 public static final String updateContainerID = "updateContainerID";
		 public static final String localizer = "localizer";
	}
	
	private String _itemsString;
	private String _showString;
	private ERXLocalizer _localizer;
	private String _updateContainerID;
	private String _separatorString;
	private String _batchSizeFieldID;
	
	public ERMDBatchSizeControl(WOContext context) {
        super(context);
    }
	
	@Override
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}

	/**
	 * The string displayed for 100 [item]s : Show 10
	 * <p>
	 * Default is "item"
	 * 
	 * @return
	 */
	public String itemsString() {
		if (_itemsString == null) {
			String key = stringValueForBinding(Keys.itemString, "item");
			_itemsString = localizer().localizedStringForKey("ERMBatchSizeControl." + key);
			if (_itemsString == null ) {
				_itemsString = localizer().localizedStringForKeyWithDefault(key);
			}
		}
		return _itemsString;
	}

	/**
	 * The string displayed for: 100 items : [Show] 10
	 * <p>
	 * Defaults to "Show"
	 * 
	 * @return
	 */
	public String showString() {
		if (_showString == null) {
			String key = stringValueForBinding(Keys.showString, "Show");
			_showString = localizer().localizedStringForKey("ERMBatchSizeControl." + key);
			if (_showString == null) {
				_showString = localizer().localizedStringForKeyWithDefault(key);
			}
		}
		return _showString;
	}
	
	/**
	 * The string displayed for: 100 items [:] Show 10
	 * <p>
	 * Defaults to ":"
	 * 
	 * @return
	 */
	public String separatorString() {
		if (_separatorString == null) {
			String key = stringValueForBinding(Keys.separatorString, ":");
			_separatorString = localizer().localizedStringForKey("ERMBatchSizeControl." + key);
			if (_separatorString == null) {
				_separatorString = localizer().localizedStringForKeyWithDefault(key);
			}
		}
		return _separatorString;
	}
	
	/**
	 * Localizer. 
	 * <p>
	 * Defaults to ERXLocalizer.currentLocalizer()
	 * 
	 * @return
	 */
    public ERXLocalizer localizer() {
		if (_localizer == null) {
			_localizer = (ERXLocalizer)objectValueForBinding(Keys.localizer, ERXLocalizer.currentLocalizer());
		}
		return _localizer;
	}
	
    /**
     * Update container id for the displayGroup's list.
     * <p>
     * Defaults to the first parent update container id.
     * 
     * @return
     */
	public String updateContainerID() {
		if (_updateContainerID == null) {
			_updateContainerID = (String) valueForBinding(Keys.updateContainerID);
			if (_updateContainerID == null) {
				_updateContainerID = AjaxUpdateContainer.currentUpdateContainerID();
			}
		}
		return _updateContainerID;
	}

	/**
	 * Returns a unique id for this batch size control
	 * 
	 * @return
	 */
	public String batchSizeFieldID() {
		if (_batchSizeFieldID == null) {
			_batchSizeFieldID = "BSIF" + ERXStringUtilities.safeIdentifierName(context().contextID());;
		}
		return _batchSizeFieldID;
	}
	
	public void setBatchSizeFieldID(String fieldID) {
		_batchSizeFieldID = fieldID;
	}

	/**
	 * Returns the js function to update the updateContainerID
	 * 
	 * @return
	 */
	public String updateFunction() {
		return "function(e) { " + updateContainerID() + "Update() }";
	}
	
}