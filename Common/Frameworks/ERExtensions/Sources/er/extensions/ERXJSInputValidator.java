package er.extensions;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver._private.*;

/**
 * Client side part of the JavaScript validation.
 * Simply wrap your text-based input component with this component and whenever the user leaves the field, the value is validated via a RPC call on the server side and an error message is displayed if it does not validate.
 *
 * @binding keyName the key to validate against
 * @binding entityName the entity to validate against
 * @binding disabled disable the validation
 *
 * @created ak on Fri May 02 2003
 * @project ERExtensions
 */

public class ERXJSInputValidator extends WOComponent {
    public static class Action extends WODirectAction {
        public Action(WORequest r) { super(r); }
        /**
        * This action is used to validate attributes of eos.
         *
         * @return the error page containing localized error messages if there where any errors.
         */
        public WOActionResults validateValueForKeyInEntityAction() {
            return pageWithName("ERXJSValidationErrors");
        }
    }
    
    /** logging support */
    private static final ERXLogger log = ERXLogger.getLogger(ERXJSInputValidator.class,"components");
    public String _errorSpanID;
    public NSDictionary currentItem;
    /**
     * Public constructor
     * @param context the context
     */
    public ERXJSInputValidator(WOContext context) {
        super(context);
    }

    /** component does not synchronize it's variables */
    public boolean synchronizesVariablesWithBindings() { return false; }

    public void appendToResponse(WOResponse woresponse, WOContext wocontext) {
        super.appendToResponse(woresponse, wocontext);
        if(wocontext instanceof ERXMutableUserInfoHolderInterface) {
            NSMutableArray array = (NSMutableArray)((ERXMutableUserInfoHolderInterface)wocontext).mutableUserInfo().objectForKey("elementArray");
            if(array != null)
                array.removeAllObjects();
        }
    }
    
    public String errorSpanID() {
        if(_errorSpanID == null) {
            _errorSpanID = context().elementID();
        }

        return _errorSpanID;
    }

    public Class classForCurrentItem() {
        String className = (String)currentItem.objectForKey("type");
        Class clazz = null;
        try {
            clazz = Class.forName(className);
            return clazz;
        } catch(Exception ex) {
        }
        return String.class;
    }

    public boolean currentItemCanBlur() {
        Class clazz = classForCurrentItem();
        boolean canBlur = WOText.class.isAssignableFrom(clazz);
        canBlur |= WOTextField.class.isAssignableFrom(clazz);
        return canBlur;
    }
}
