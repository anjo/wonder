package er.ajax;

import org.apache.log4j.*;

import com.webobjects.appserver.WOApplication;
import com.webobjects.foundation.*;

import er.extensions.*;
import er.extensions.appserver.ajax.ERXAjaxApplication;
import er.extensions.eof.ERXConstant;

public class Ajax extends ERXFrameworkPrincipal {
	public static Class[] REQUIRES = new Class[0];
	public static final Logger log = Logger.getLogger(Ajax.class);
	
	
    static {
        setUpFrameworkPrincipalClass(Ajax.class);
    }

    public Ajax() {
    	NSNotificationCenter center = NSNotificationCenter.defaultCenter();
    	// This is needed when ERXAjaxApplication is sub-classed
    	center.addObserver(this,
    			new NSSelector("finishAjaxInitialization", ERXConstant.NotificationClassArray),
    			WOApplication.ApplicationWillFinishLaunchingNotification,
    			null);
	}
    
    /**
     * This is called directly only for when ERXApplication is sub-classed.
     */
	public void finishInitialization() {
		if ( ! AjaxRequestHandler.useAjaxRequestHandler())
		{
			WOApplication.application().registerRequestHandler(new AjaxRequestHandler(), AjaxRequestHandler.AjaxRequestHandlerKey);
			log.debug("AjaxRequestHandler installed");
		}
		WOApplication.application().registerRequestHandler(new AjaxPushRequestHandler(), AjaxPushRequestHandler.AjaxCometRequestHandlerKey);

		// Register the AjaxResponseDelegate if you're using an ERXAjaxApplication ... This allows us
		// to fix some weird border cases caused by structural page changes.
		WOApplication application = WOApplication.application();
		if (application instanceof ERXAjaxApplication) {
			((ERXAjaxApplication)application).setResponseDelegate(new AjaxResponse.AjaxResponseDelegate());
		}
	}

	/**
	 * The constructor sets this up to receive a notification.  This is used when ERXAjaxApplication is sub-classed.
	 * 
	 * @param notification ApplicationWillFinishLaunchingNotification
	 */
	public void finishAjaxInitialization(NSNotification notification) {
		finishInitialization();
	}
	
	
}
