package er.extensions.components.javascript;
import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOClassDescription;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSValidation;
import er.extensions.components.ERXStatelessComponent;
import er.extensions.validation.ERXValidationException;

/**
 * Server side part of the JavaScript validation
 *
 * @binding sample sample binding explanation
 *
 * @author ak on Fri May 02 2003
 * @project ERExtensions
 */

public class ERXJSValidationErrors extends ERXStatelessComponent {

    /** logging support */
    private static final Logger log = Logger.getLogger(ERXJSValidationErrors.class);

    public String _errors;
    public String _callback;

    /**
     * Public constructor
     * @param context the context
     */
    public ERXJSValidationErrors(WOContext context) {
        super(context);
    }

    public String callback() { return "parent." + _callback; }
    public void awake() {
        String key = context().request().stringFormValueForKey("_vkey");
        String value = context().request().stringFormValueForKey("_vvalue");
        String entity = context().request().stringFormValueForKey("_ventityName");

        String contextID = context().request().stringFormValueForKey("contextID");

        _callback = context().request().stringFormValueForKey("callback");

        if(value != null && value.length() == 0)
            value = null;
        
        Object newValue = value;

        log.debug("validateKeyAndValueInEntityAction: key="+key+", value="+value+", entity="+entity + ", contextID "+contextID+ ", callback=" + _callback);

        EOEnterpriseObject eo = null;
        WOComponent page = null;
        try {
            if(contextID != null)
                page = session().restorePageForContextID(contextID);
            log.debug("Page: " + (page != null ? "Yes" : "No"));
            if(page != null && true) {
                eo = (EOEnterpriseObject)page.valueForKey("object");
                eo.editingContext().lock();
                int dotOffset = key.lastIndexOf('.');
                if(dotOffset > 0) {
                    Object otherEO = eo.valueForKeyPath(key.substring(0, dotOffset));
                    if(otherEO != null) {
                        NSValidation.Utility.validateValueForKey(otherEO, value, key.substring(dotOffset+1));
                    }
                } else {
                    eo.validateValueForKey(value, key);
                }
             } else {
                EOClassDescription cd = EOClassDescription.classDescriptionForEntityName(entity);
                if(cd != null)
                    newValue = cd.validateValueForKey(value, key);
            }
        } catch (ERXValidationException ex) {
            try {
                log.info(ex);
                NSKeyValueCoding d2wContext = (NSKeyValueCoding)page.valueForKey("d2wContext");
                d2wContext.takeValueForKey(key, "propertyKey");
                ex.setContext(d2wContext);
                ex.setTargetLanguage((String)session().valueForKeyPath("language"));
                _errors = ex.getMessage();
            } catch(Exception ex1) {
                _errors = ex1.toString();
            }
        } catch (NSValidation.ValidationException ex1) {
            _errors = ex1.getMessage();
        } finally {        
            if(eo != null && eo.editingContext() != null)
                eo.editingContext().unlock();
            if(page != null) {
                // we cheat here because calling sleep() is not enough...
                page._sleepInContext(page.context());
            }
        }
    }
    
    public void reset() { _errors = null; _callback = null;}
}
