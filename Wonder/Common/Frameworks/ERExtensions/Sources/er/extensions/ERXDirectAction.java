/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr 
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WODirectAction;
import com.webobjects.appserver.WORedirect;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.woextensions.WOEventDisplayPage;
import com.webobjects.woextensions.WOEventSetupPage;
import com.webobjects.woextensions.WOStatsPage;

/**
 * Basic collector for direct action additions. All of the actions are password protected, 
 * you need to give an argument "pw" that matches the corresponding system property for the action.
 */
public class ERXDirectAction extends WODirectAction {

    /** logging support */
    public final static Logger log = Logger.getLogger(ERXDirectAction.class);

    /** holds a reference to the current browser used for this session */
    private ERXBrowser browser;

    /** Public constructor */
    public ERXDirectAction(WORequest r) { super(r); }


    /**
     * Checks if the action can be executed.
     * @param passwordKey
     */
    protected boolean canPerformActionWithPasswordKey(String passwordKey) {
    	if(ERXApplication.isDevelopmentModeSafe()) {
    		return true;
    	}
    	String password = ERXProperties.decryptedStringForKey(passwordKey);
    	if(password == null || password.length() == 0) {
    		log.error("Attempt to use action when key is not set: " + passwordKey);
    		return false;
    	}
    	String requestPassword = request().stringFormValueForKey("pw");
    	if(requestPassword == null) {
    		requestPassword = (String) context().session().objectForKey("ERXDirectAction." + passwordKey);
    	} else {
    		context().session().setObjectForKey(requestPassword, "ERXDirectAction." + passwordKey);
    	}
    	if(requestPassword == null || requestPassword.length() == 0) {
    		return false;
    	}
    	return password.equals(requestPassword);
    }

    /**
     * Action used for junit tests. This method is only active when WOCachingEnabled is
     * disabled (we take this to mean that the application is not in production).<br/>
     * <br/>
     * Synopsis:<br/>
     * pw=<i>aPassword</i>&case=<i>classNameOfTestCase</i>
     * <br/>
     * Form Values:<br/>
     * <b>pw</b> password to be checked against the system property <code>er.extensions.ERXJUnitPassword</code>.
     * <b>case</b> class name for unit test to be performed.<br/>
     * <br/>
     * @return {@link er.testrunner.ERXWOTestInterface ERXWOTestInterface} 
     * with the results after performing the given test.
     */
    public WOComponent testAction() {
        WOComponent result=null;
        if (canPerformActionWithPasswordKey("er.extensions.ERXJUnitPassword")) {
            
            result=pageWithName("ERXWOTestInterface");
            String testCase = request().stringFormValueForKey("case");
            if(testCase != null) {
                result.takeValueForKey(testCase, "theTest");
                // (ak:I wish we could return a direct test result...)
                // return (WOComponent)result.valueForKey("performTest");
            }
        }             
        return result;
    }


    /**
     * Action used for changing logging settings at runtime. This method is only active
     * when WOCachingEnabled is disabled (we take this to mean that the application is
     *                                    not in production).<br/>
     * <br/>
     * Synopsis:<br/>
     * pw=<i>aPassword</i>
     * <br/>
     * Form Values:<br/>
     * <b>pw</b> password to be checked against the system property <code>er.extensions.ERXLog4JPassword</code>.
     * <br/>
     * @return {@link ERXLog4JConfiguration} for modifying current logging settings.
     */
    public WOComponent log4jAction() {
        WOComponent result=null;
        if (canPerformActionWithPasswordKey("er.extensions.ERXLog4JPassword")) {
		result=pageWithName("ERXLog4JConfiguration");
		session().setObjectForKey(Boolean.TRUE, "ERXLog4JConfiguration.enabled");
	}
        return result;
    }

    /**
     * Action used for forcing garbage collection. If WOCachingEnabled is true (we take this to mean 
     * that the application is in production) you need to give a password to access it.<br/>
     * <br/>
     * Synopsis:<br/>
     * pw=<i>aPassword</i>
     * <br/>
     * Form Values:<br/>
     * <b>pw</b> password to be checked against the system property <code>er.extensions.ERXGCPassword</code>.
     * <br/>
     * @return short info about free and used memory before and after GC.
     */
    public WOComponent forceGCAction() {
        ERXStringHolder result=(ERXStringHolder)pageWithName("ERXStringHolder");
        if (canPerformActionWithPasswordKey("er.extensions.ERXGCPassword")) {
            Runtime runtime = Runtime.getRuntime();
            ERXUnitAwareDecimalFormat decimalFormatter = new ERXUnitAwareDecimalFormat(ERXUnitAwareDecimalFormat.BYTE);
            decimalFormatter.setMaximumFractionDigits(2);
           
            String info = "Before: ";
            info += decimalFormatter.format(runtime.maxMemory()) + " max, ";
            info += decimalFormatter.format(runtime.totalMemory()) + " total, ";
            info += decimalFormatter.format(runtime.totalMemory()-runtime.freeMemory()) + " used, ";
            info += decimalFormatter.format(runtime.freeMemory()) + " free\n";
            
            ERXExtensions.forceGC(5);
  
            info += "After: ";
            info += decimalFormatter.format(runtime.maxMemory()) + " max, ";
            info += decimalFormatter.format(runtime.totalMemory()) + " total, ";
            info += decimalFormatter.format(runtime.totalMemory()-runtime.freeMemory()) + " used, ";
            info += decimalFormatter.format(runtime.freeMemory()) + " free\n";

            result.setValue(info);
        }
        return result;
    }

    public WOActionResults logoutAction() {
        if (existingSession()!=null) {
            existingSession().terminate();
        }
        WORedirect r=(WORedirect)pageWithName("WORedirect");
        r.setUrl(context().directActionURLForActionNamed("default", null));
        return r;
    }
    
    /**
     * Returns the browser object representing the web 
     * browser's "user-agent" string. You can obtain 
     * browser name, version, platform and Mozilla version, etc. 
     * through this object. <br>
     * Good for WOConditional's condition binding to deal 
     * with different browser versions. 
     * @return browser object
     */
    public ERXBrowser browser() { 
        if (browser == null  &&  request() != null) {
            ERXBrowserFactory browserFactory = ERXBrowserFactory.factory();
            browser = browserFactory.browserMatchingRequest(request());
            browserFactory.retainBrowser(browser);
        }
        return browser; 
    }

    public WOActionResults performActionNamed(String actionName) {
        WOActionResults actionResult = super.performActionNamed(actionName);
        if (browser != null) 
            ERXBrowserFactory.factory().releaseBrowser(browser);
        return actionResult;
    }
    
    /**
        * Sets a System property. This is also active in deployment mode because one might want to change a System property
     * at runtime.
     * Synopsis:<br/>
     * pw=<i>aPassword</i>&key=<i>someSystemPropertyKey</i>&value=<i>someSystemPropertyValue</i>
     *
     * @return either null when the password is wrong or the key is missing or a new page showing the System properties
     */
    public WOActionResults systemPropertyAction() {
    	WOResponse r = null;
    	if (canPerformActionWithPasswordKey("er.extensions.ERXDirectAction.ChangeSystemPropertyPassword")) {
        String key = request().stringFormValueForKey("key");
        String value = request().stringFormValueForKey("value");
    		r = new WOResponse();
        if (ERXStringUtilities.stringIsNullOrEmpty(key) ) {
            r.appendContentString("key cannot be null or empty old System properties:\n"+System.getProperties());
        } else {
            value = ERXStringUtilities.stringIsNullOrEmpty(value) ? "" : value;
            java.util.Properties p = System.getProperties();
            p.put(key, value);
            System.setProperties(p);
                ERXLogger.configureLoggingWithSystemProperties();
            r.appendContentString("<html><body>New System properties:<br>");
            for (java.util.Enumeration e = p.keys(); e.hasMoreElements();) {
                Object k = e.nextElement();
                if (k.equals(key)) {
                    r.appendContentString("<b>'"+k+"="+p.get(k)+"'     <= you changed this</b><br>");
                } else {
                    r.appendContentString("'"+k+"="+p.get(k)+"'<br>");
                }
            }
            r.appendContentString("</body></html>");
        }
    	}
		return r;
    }
    
    /**
     * Opens the localizer edit page if the app is in development mode.
     */
    public WOActionResults editLocalizedFilesAction() {
    	WOResponse r = null;
    	if (ERXApplication.isDevelopmentModeSafe()) {
    		return pageWithName("ERXLocalizationEditor");
    	}
		return r;
    }
    
    
    public WOActionResults dumpCreatedKeysAction() {
    	WOResponse r = new WOResponse();
    	if (ERXApplication.isDevelopmentModeSafe()) {
    		session();
            ERXLocalizer.currentLocalizer().dumpCreatedKeys();
    	}
		return r;
    }
    
    /**
     * Returns an empty response.
     * 
     * @return nothing
     */
    public WOActionResults emptyAction() {
    	WOResponse response = new WOResponse();
    	return response;
    }

    @SuppressWarnings("unchecked")
    public <T extends WOComponent> T pageWithName(Class<T> componentClass) {
      return (T) super.pageWithName(componentClass.getName());
    }

}