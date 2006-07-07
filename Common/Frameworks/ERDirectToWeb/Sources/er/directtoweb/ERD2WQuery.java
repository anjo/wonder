package er.directtoweb;

import org.apache.log4j.Logger;

import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;

import er.extensions.*;

/**
 * Same as D2WQuery, except that you can specify the queryBindings in advance.
 * 
 * @created ak on Fri Jan 09 2004
 * @project ERDirectToWeb
 */

public class ERD2WQuery extends D2WQuery {

    /** logging support */
    private static final Logger log = Logger.getLogger(ERD2WQuery.class);
	
    /**
     * Public constructor
     * @param context the context
     */
    public ERD2WQuery(WOContext context) {
        super(context);
    }
    
    static {
    	D2WSwitchComponent.addToPossibleBindings("queryBindings");
    }
}
