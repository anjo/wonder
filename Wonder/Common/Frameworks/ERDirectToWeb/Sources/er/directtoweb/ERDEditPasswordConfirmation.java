package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.directtoweb.*;
import er.extensions.*;

/**
 * Provides a "confirm password" service. Can
 * only be used in conjunction with {$link ERDEditPassword}, because
 * the original password value is grabbed from the <code>context.mutableUserInfo</code>.
 * As all the names in a displayPropertyRepetition must be distinct, you must provide a
 * dummy name for this component to show. The corresponding rules for a key named <code>passwordConfirmation</code> have already been set up.
 * @binding object the object to edit
 * @binding length the length of the text field
 * @d2wKey length the length of the text field
 *
 * @created ak on Sun Aug 17 2003
 * @project ERDirectToWeb
 */

public class ERDEditPasswordConfirmation extends ERDCustomEditComponent {

    /** logging support */
    private static final ERXLogger log = ERXLogger.getLogger(ERDEditPasswordConfirmation.class,"components");

    public int length;
    /**
     * Public constructor
     * @param context the context
     */
    public ERDEditPasswordConfirmation(WOContext context) {
        super(context);
    }

    public void fail(String errorCode) {
        if(log.isDebugEnabled())
            log.debug("fail:<object:" + object() + "; key:" + key() + ";  password: " + password() + "; code:" + errorCode + ";>");
        validationFailedWithException(ERXValidationFactory.defaultFactory().createException(object(), key(), password(), errorCode), password(), key());
    }

    public String passwordPropertyKey() {
        String passwordPropertyKey = null;
        if(context() instanceof ERXMutableUserInfoHolderInterface) {
            NSDictionary userInfo = ((ERXMutableUserInfoHolderInterface)context()).mutableUserInfo();
            passwordPropertyKey = (String) userInfo.valueForKey(ERDEditPassword.passwordPropertyKey);
        }
        return passwordPropertyKey;
    }

    public Object objectKeyPathValue() {
        return passwordPropertyKey() == null || object() == null ? null : object().valueForKeyPath(passwordPropertyKey()); 
    }

    public String password() {
        String password = null;
        if(context() instanceof ERXMutableUserInfoHolderInterface) {
            NSDictionary userInfo = ((ERXMutableUserInfoHolderInterface)context()).mutableUserInfo();
            password = (String) userInfo.valueForKey("ERDEditPassword." + passwordPropertyKey() + ".value");
        }
        return password;
    }

    protected String _passwordConfirm;
    public String passwordConfirm() {
        /*if(_passwordConfirm == null) {
            _passwordConfirm = (String)valueForBinding("passwordConfirm");
        }*/
        return _passwordConfirm;
    }
    public void setPasswordConfirm(String value) {
        _passwordConfirm = value;
    }

    public void setObject(EOEnterpriseObject newObject) {
        if (newObject!=object()) {
            _passwordConfirm = null;
        }
        super.setObject(newObject);
    }

    protected void checkPasswords() {
        String password = password();
        String passwordPropertyKey = passwordPropertyKey();
        String passwordConfirm = passwordConfirm();

        if (password==null || password.equals("") || passwordConfirm==null || passwordConfirm.equals("")) {
            // one or both is null or empty
            if (objectKeyPathValue() == null) {
                fail("PasswordsFillBothFieldsException");
            } else {
                // if we already have a value, then they need to both be null||empty
                if (!(password==null || password.equals("")) && (passwordConfirm==null || passwordConfirm.equals(""))) {
                    fail("PasswordsFillBothFieldsException");
                }
            }
        } else {
            // they are both non-null
            if(!password.equals(passwordConfirm)) {
                fail("PasswordsDontMatchException");
            } else {
                if (object() instanceof NSValidation) {
                    try {
                        object().validateTakeValueForKeyPath(password, passwordPropertyKey);
                    } catch (NSValidation.ValidationException e) {
                        validationFailedWithException(e, password, passwordPropertyKey);
                    }
                } else {
                    object().validateTakeValueForKeyPath(password, passwordPropertyKey);
                }
            }
        }
    }
    
    public void takeValuesFromRequest(WORequest r, WOContext c) {
        super.takeValuesFromRequest(r,c);
        checkPasswords();
    }
}
