package er.extensions;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

/**
 * Provides better error checking.
 * When it is embedded in a WOForm that does not have enctype=multipart/form-data,
 * it throws an IllegalArgumentException. 
 *
 * @created ak on Wed Oct 09 2002
 * @project ERExtensions
 */

public class ERXWOFileUpload extends com.webobjects.appserver._private.WOFileUpload {

    /** logging support */
    private static final ERXLogger log = ERXLogger.getLogger(ERXWOFileUpload.class,"components");
	
    /**
     * Public constructor
     * @param context the context
     */
    public ERXWOFileUpload(String string, NSDictionary nsdictionary,
                        WOElement woelement) {
        super(string, nsdictionary, woelement);
    }

    public void checkEnctype(WOContext context) {
        if(context != null && context instanceof ERXMutableUserInfoHolderInterface) {
            NSDictionary ui = ((ERXMutableUserInfoHolderInterface)context).mutableUserInfo();
            if(!("multipart/form-data".equals(ui.objectForKey("enctype")))) {
                throw new IllegalArgumentException("This form is missing a 'enctype=multipart/form-data' attribute. It is required for WOFileUpload to work.");
            }
        }
    }

    public void appendToResponse(WOResponse response, WOContext context) {
        checkEnctype(context);
        super.appendToResponse(response, context);
    }
}
