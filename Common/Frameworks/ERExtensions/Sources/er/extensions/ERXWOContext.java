//
// ERXWOContext.java
// Project armehaut
//
// Created by ak on Mon Apr 01 2002
//
package er.extensions;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODirectAction;
import com.webobjects.appserver.WORequest;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;

/** Replacement of WOContext.
 *  This subclass is installed when the frameworks loads.
 */
public class ERXWOContext extends WOContext implements ERXMutableUserInfoHolderInterface {
    private static Observer observer;
    private boolean _generateCompleteURLs;
    
    public static class Observer {
    	public void applicationDidHandleRequest(NSNotification n) {
    		ERXThreadStorage.removeValueForKey("ERXWOContext.dict");
    	}
    }
    
    /**
     * Public constructor
     * @param context context of request
     */
    public static NSMutableDictionary contextDictionary() {
    	if(observer == null) {
    		synchronized (ERXWOContext.class) {
    			if(observer == null) {
    				observer = new Observer();

    				NSNotificationCenter.defaultCenter().addObserver(observer, 
    						ERXSelectorUtilities.notificationSelector("applicationDidHandleRequest"), 
    						WOApplication.ApplicationDidDispatchRequestNotification, null);
    			}
    		}
    	}
    	NSMutableDictionary result = (NSMutableDictionary)ERXThreadStorage.valueForKey("ERXWOContext.dict"); 
    	if(result == null) {
    		result = new NSMutableDictionary();
    		ERXThreadStorage.takeValueForKey(result, "ERXWOContext.dict");
    	}
     	return result;
    }


    public ERXWOContext(WORequest worequest) {
        super(worequest);
    }
    
    public void _generateCompleteURLs() {
    	super._generateCompleteURLs();
    	_generateCompleteURLs = true;
    }
    
    public void _generateRelativeURLs() {
    	super._generateRelativeURLs();
    	_generateCompleteURLs = false;
    }
    
    public boolean _generatingCompleteURLs() {
    	return _generateCompleteURLs;
    }

    public static WOContext newContext(){
        WOApplication app = WOApplication.application();
        return app.createContextForRequest(app.createRequest("GET", app.cgiAdaptorURL() + "/" + app.name(), "HTTP/1.1", null, null, null));
    }


    public NSMutableDictionary mutableUserInfo() {
        return contextDictionary();
    }
    public void setMutableUserInfo(NSMutableDictionary userInfo) {
        ERXThreadStorage.takeValueForKey(userInfo, "ERXWOContext.dict");
    }
    public NSDictionary userInfo() {
        return mutableUserInfo();
    }
    
    /**
     * Returns a complete URL for the specified action. 
     * Works like {@link WOContext#directActionURLForActionNamed} 
     * but has one extra parameter to specify whether or not to include 
     * the current Session ID (wosid) in the URL. Convenient if you embed
     * the link for the direct action into an email message and don't want 
     * to keep the Session ID in it. 
     * <p>
     * <code>actionName</code> can be either an action -- "ActionName" -- 
     * or an action on a class -- "ActionClass/ActionName". You can also 
     * specify <code>queryDict</code> to be an NSDictionary which contains 
     * form values as key/value pairs. <code>includeSessionID</code> 
     * indicates if you want to include the Session ID (wosid) in the URL.  
     * 
     * @param actionName  String action name
     * @param queryDict   NSDictionary containing query key/value pairs
     * @param includeSessionID	true: to include the Session ID (if has one), <br>
     * 				false: not to include the Session ID
     * @return a String containing the URL for the specified action
     * @see WODirectAction
     */
    public String directActionURLForActionNamed(String actionName, NSDictionary queryDict, boolean includeSessionID) {
        String url = super.directActionURLForActionNamed(actionName, queryDict);
        if (!includeSessionID) { 
            url = stripSessionIDFromURL(url);
        } 
        return url;
    }
    
    /**
     * Removes Session ID (wosid) query key/value pair from the given URL 
     * string. 
     * 
     * @param actionName  String URL
     * @return a String with the Session ID removed
     */ 
    public static String stripSessionIDFromURL(String url) {
	if (url == null)  return null;
        int len = 1;
        int startpos = url.indexOf("?wosid");
        if (startpos < 0) {
        	startpos = url.indexOf("&wosid");
        }
        if (startpos < 0) {
        	startpos = url.indexOf("&amp;wosid");
        	len = 5;
        }

        if (startpos >= 0) {
            int endpos = url.indexOf('&', startpos + len);
            if (endpos < 0)
                url = url.substring(0, startpos);
            else {
            	int endLen = len;
            	if(len == 1 && url.indexOf("&amp;") >= 0) {
            		endLen = 5;
            	}
                url = url.substring(0, startpos + len) + url.substring(endpos + endLen);
            }
        }
        return url;
    }

    public String elementID() {
      String elementID = super.elementID();
      // MS: If you make an element the very first item on a page (i.e. no
      // html tag, no whitespace, etc), elementID will end up being null
      // instead of 0.1 like it would be if you just put a space in front 
      // of it.
      if (elementID == null) {
        appendZeroElementIDComponent();
        incrementLastElementIDComponent();
        elementID = super.elementID();
      }
      return elementID;
    }

    /**
     * Debugging help, returns the path to current component.
     * @param context
     * @return
     */
    public static NSArray componentPath(WOContext context) {
    	NSMutableArray result = new NSMutableArray();
    	WOComponent component = context.component();
    	while(component != null) {
    		result.insertObjectAtIndex(component, 0);
    		component = component.parent();
    	}
    	return result;
    }}
