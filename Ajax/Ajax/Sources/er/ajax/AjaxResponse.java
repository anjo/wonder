package er.ajax;

import java.util.Enumeration;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.ERXResponse;

/**
 * AjaxResponse provides support for performing an AjaxUpdate in the same response
 * as an ajax action.
 * 
 * @author mschrag
 */
public class AjaxResponse extends ERXResponse {
	public static final String AJAX_UPDATE_PASS = "_ajaxUpdatePass";
	private static NSMutableArray _responseAppenders; 
	
	/**
	 * Add a response appender to the list of response appender.  At the end of
	 * every AjaxResponse, the AjaxResponseAppenders are given an opportunity to
	 * tag along. For instance, if you have an area at the top of your pages that
	 * show errors or notifications, you may want all of your ajax responses to have
	 * a chance to trigger an update of this area, so you could register an 
	 * AjaxResponseAppender that renders a javascript block that calls 
	 * MyNotificationsUpdate() only if there are notifications to be shown. Without
	 * response appenders, you would have to include a check in all of your 
	 * components to do this. 
	 * 
	 * @param responseAppender the appender to add
	 */
	public static void addAjaxResponseAppender(AjaxResponseAppender responseAppender) {
		if (_responseAppenders == null) {
			_responseAppenders = new NSMutableArray();
		}
		_responseAppenders.addObject(responseAppender);
	}
	
	private WORequest _request;
	private WOContext _context;

	public AjaxResponse(WORequest request, WOContext context) {
		_request = request;
		_context = context;
	}

	public WOResponse generateResponse() {
		if (AjaxUpdateContainer.hasUpdateContainerID(_request)) {
			String originalSenderID = _context.senderID();
			_context._setSenderID("");
			try {
				StringBuffer content = _content;
				_content = new StringBuffer();
				NSMutableDictionary userInfo = AjaxUtils.mutableUserInfo(_request);
				userInfo.setObjectForKey(Boolean.TRUE, AjaxResponse.AJAX_UPDATE_PASS);
				WOActionResults woactionresults = WOApplication.application().invokeAction(_request, _context);
				_content.append(content);
				if (_responseAppenders != null) {
					Enumeration responseAppendersEnum = _responseAppenders.objectEnumerator();
					while (responseAppendersEnum.hasMoreElements()) {
						AjaxResponseAppender responseAppender = (AjaxResponseAppender) responseAppendersEnum.nextElement();
						responseAppender.appendToResponse(this, _context);
					}
				}
			}
			finally {
				_context._setSenderID(originalSenderID);
			}
		}
		return this;
	}
	
	public static boolean isAjaxUpdatePass(WORequest request) {
		NSDictionary userInfo = AjaxUtils.mutableUserInfo(request);
		return userInfo != null && userInfo.valueForKey(AjaxResponse.AJAX_UPDATE_PASS) != null;
	}
}
