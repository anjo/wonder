package er.ajax;

import java.util.Collection;

import org.jabsorb.JSONSerializer;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOAssociation;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.appserver.ERXResourceManager;
import er.extensions.appserver.ERXResponseRewriter;
import er.extensions.appserver.ERXWOContext;
import er.extensions.appserver.ajax.ERXAjaxApplication;
import er.extensions.appserver.ajax.ERXAjaxSession;
import er.extensions.foundation.ERXProperties;
import er.extensions.foundation.ERXStringUtilities;

public class AjaxUtils {
	private static final String SECURE_RESOURCES_KEY = "er.ajax.secureResources";

	/**
	 * If the value is null, this returns "null", otherwise it returns '[value]'.
	 * @param value the value to quote
	 * @return the quoted value or "null"
	 */
	public static String quote(String value) {
		return value == null ? "null" : "'" + value + "'";
	}

	/**
	 * Return whether or not the given request is an Ajax request.
	 * @param request the request the check
	 */
	public static boolean isAjaxRequest(WORequest request) {
		return ERXAjaxApplication.isAjaxRequest(request);
	}

	public static void setPageReplacementCacheKey(WOContext _context, String _key) {
		_context.response().setHeader(_key, ERXAjaxSession.PAGE_REPLACEMENT_CACHE_LOOKUP_KEY);
	}

	/**
	 * Creates a response for the given context (which can be null), sets the charset to UTF-8, the connection to
	 * keep-alive and flags it as a Ajax request by adding an AJAX_REQUEST_KEY header. You can check this header in the
	 * session to decide if you want to save the request or not.
	 * 
	 * @param context
	 */
	public static AjaxResponse createResponse(WORequest request, WOContext context) {
		AjaxResponse response = null;
		if (context != null) {
			WOResponse existingResponse = context.response();
			if (existingResponse instanceof AjaxResponse) {
				response = (AjaxResponse) existingResponse;
			}
			else {
				response = new AjaxResponse(request, context);
				response.setHeaders(existingResponse.headers());
				response.setUserInfo(existingResponse.userInfo());
				response.appendContentString(existingResponse.contentString());
			}
		}
		if (response == null) {
			response = new AjaxResponse(request, context);
		}
		if (context != null) {
			context._setResponse(response);
		}
		// Encode using UTF-8, although We are actually ASCII clean as all
		// unicode data is JSON escaped using backslash u. This is less data
		// efficient for foreign character sets but it is needed to support
		// naughty browsers such as Konqueror and Safari which do not honour the
		// charset set in the response
		response.setHeader("text/plain; charset=utf-8", "content-type");
		response.setHeader("Connection", "keep-alive");
		response.setHeader(ERXAjaxSession.DONT_STORE_PAGE, ERXAjaxSession.DONT_STORE_PAGE);
		return response;
	}

	/**
	 * Returns the userInfo dictionary if the supplied message and replaces it with a mutable version if it isn't
	 * already one.
	 * 
	 * @param message
	 */
	public static NSMutableDictionary mutableUserInfo(WOMessage message) {
		return ERXWOContext.contextDictionary();
	}

	/**
	 * Adds a script tag with a correct resource url in the html head tag if it isn't already present in the response.
	 * 
	 * @param response
	 * @param fileName
	 */
	public static void addScriptResourceInHead(WOContext context, WOResponse response, String framework, String fileName) {
		String processedFileName = fileName;
		// PROTOTYPE MISC
		if (ERXProperties.booleanForKey("er.ajax.compressed") && ("prototype.js".equals(fileName) || "scriptaculous.js".equals(fileName))) {
			processedFileName = "sc-17-proto-15-compressed.js";
		}
		ERXResponseRewriter.addScriptResourceInHead(response, context, framework, processedFileName);
	}

	/**
	 * Calls ERXWOContext.addScriptResourceInHead with "Ajax" framework
	 */
	public static void addScriptResourceInHead(WOContext context, WOResponse response, String fileName) {
		AjaxUtils.addScriptResourceInHead(context, response, "Ajax", fileName);
	}

	/**
	 * Calls ERXWOContext.addStylesheetResourceInHead
	 */
	public static void addStylesheetResourceInHead(WOContext context, WOResponse response, String framework, String fileName) {
		ERXResponseRewriter.addStylesheetResourceInHead(response, context, framework, fileName);
	}

	/**
	 * Calls ERXWOContext.addStylesheetResourceInHead with "Ajax" framework
	 */
	public static void addStylesheetResourceInHead(WOContext context, WOResponse response, String fileName) {
		AjaxUtils.addStylesheetResourceInHead(context, response, "Ajax", fileName);
	}

	/**
	 * Adds a reference to an arbitrary file with a correct resource url wrapped between startTag and endTag in the html
	 * head tag if it isn't already present in the response.
	 * 
	 * @param response
	 * @param fileName
	 * @param startTag
	 * @param endTag
	 * @deprecated this is not called by anything anymore and does not use the new support for loading-on-demand
	 */
	public static void addResourceInHead(WOContext context, WOResponse response, String framework, String fileName, String startTag, String endTag) {
		ERXResponseRewriter.addResourceInHead(response, context, framework, fileName, startTag, endTag, ERXResponseRewriter.TagMissingBehavior.Top);

		// MS: OK ... Sheesh.  If you're not using Wonder's ERXResourceManager #1, you're a bad person, but #2 in development mode
		// you have a lame resource URL that does not act like a path (wr/wodata=/path/to/your/resource), rather it acts like a query string
		// (wr?wodata=/path/to/your/resource).  This means that relative resource references won't work and also only previously cached resources
		// will load (i.e. ones coming from something that made an explicit WOResourceURL, etc, reference).  This explodes when scriptaculous tries 
		// to load its required resources dynamically (like builder.js, effects.js, etc).
		//
		// So we have to check for this condition -- you asked to load scriptaculous.js from Ajax framework and you don't have ERXResourceManager
		// and you're in development mode (as far as your lame WOResourceManager is concerned), so we need to do Scriptaculous' job and manually
		// load the dependent js files on its behalf.  You really should just suck it up and use ERXResourceManager because it really is just
		// better.  But if you're holding out and scared like a child, then we'll do this for you. 
		// PROTOTYPE MISC
		if (!(WOApplication.application().resourceManager() instanceof ERXResourceManager) && "Ajax".equals(framework) && "scriptaculous.js".equals(fileName) && !(context.request() == null || context.request() != null && context.request().isUsingWebServer() && !WOApplication.application()._rapidTurnaroundActiveForAnyProject())) {
			boolean enqueueIfTagMissing = !AjaxUtils.isAjaxRequest(context.request());
			ERXResponseRewriter.addResourceInHead(response, context, framework, "builder.js", startTag, endTag, ERXResponseRewriter.TagMissingBehavior.Top);
			ERXResponseRewriter.addResourceInHead(response, context, framework, "effects.js", startTag, endTag, ERXResponseRewriter.TagMissingBehavior.Top);
			ERXResponseRewriter.addResourceInHead(response, context, framework, "dragdrop.js", startTag, endTag, ERXResponseRewriter.TagMissingBehavior.Top);
			ERXResponseRewriter.addResourceInHead(response, context, framework, "controls.js", startTag, endTag, ERXResponseRewriter.TagMissingBehavior.Top);
			ERXResponseRewriter.addResourceInHead(response, context, framework, "slider.js", startTag, endTag, ERXResponseRewriter.TagMissingBehavior.Top);
		}
	}

	/**
	 * Calls ERXWOContext.addScriptCodeInHead.
	 */
	public static void addScriptCodeInHead(WOResponse response, WOContext context, String script) {
		ERXResponseRewriter.addScriptCodeInHead(response, context, script);
	}

	/**
	 * @deprecated replaced by ERXStringUtilities.safeIdentifierName
	 */
	public static String toSafeElementID(String elementID) {
		return ERXStringUtilities.safeIdentifierName(elementID);
	}

	public static boolean shouldHandleRequest(WORequest request, WOContext context, String containerID) {
		String elementID = context.elementID();
		String senderID = context.senderID();
		String updateContainerID = null;
		if (containerID != null) {
			if (AjaxResponse.isAjaxUpdatePass(request)) {
				updateContainerID = AjaxUpdateContainer.updateContainerID(request);
			}
		}
		boolean shouldHandleRequest = elementID != null && (elementID.equals(senderID) || (containerID != null && containerID.equals(updateContainerID)) || elementID.equals(ERXAjaxApplication.ajaxSubmitButtonName(request)));
		return shouldHandleRequest;
	}

	public static void updateMutableUserInfoWithAjaxInfo(WOContext context) {
		AjaxUtils.updateMutableUserInfoWithAjaxInfo(context.response());
	}

	public static void updateMutableUserInfoWithAjaxInfo(WOMessage message) {
		NSMutableDictionary dict = AjaxUtils.mutableUserInfo(message);
		dict.takeValueForKey(ERXAjaxSession.DONT_STORE_PAGE, ERXAjaxSession.DONT_STORE_PAGE);
	}

	/**
	 * Returns an AjaxResponse with the given javascript as the body of the response.
	 * 
	 * @param context the WOContext
	 * @param javascript the javascript to send
	 * @return a new response
	 */
	public static WOResponse javascriptResponse(String javascript, WOContext context) {
		WORequest request = context.request();
		AjaxResponse response = AjaxUtils.createResponse(request, context);
		AjaxUtils.appendScriptHeaderIfNecessary(request, response);
		response.appendContentString(javascript);
		AjaxUtils.appendScriptFooterIfNecessary(request, response);
		return response;
	}

	/**
	 * Shortcut for appendScript.
	 * 
	 * @param context the context
	 * @param script the script to append
	 */
	public static void appendScript(WOContext context, String script) {
		AjaxUtils.appendScript(context.request(), context.response(), script);
	}

	/**
	 * Appends the given javascript to the response, surrounding it in a script header/footer if necessary.
	 * 
	 * @param request the request
	 * @param response the response
	 * @param script the script to append
	 */
	public static void appendScript(WORequest request, WOResponse response, String script) {
		AjaxUtils.appendScriptHeaderIfNecessary(request, response);
		response.appendContentString(script);
		AjaxUtils.appendScriptFooterIfNecessary(request, response);
	}
	
	public static void appendScriptHeaderIfNecessary(WORequest request, WOResponse response) {
		if (AjaxUpdateContainer.hasUpdateContainerID(request)) {
			AjaxUtils.appendScriptHeader(response);
		}
		else {
			response.setHeader("text/javascript", "Content-Type");
		}
	}

	public static void appendScriptHeader(WOResponse response) {
		boolean appendTypeAttribute = ERXProperties.booleanForKeyWithDefault("er.extensions.ERXResponseRewriter.javascriptTypeAttribute", false);
		if (appendTypeAttribute) {
			response.appendContentString("<script type=\"text/javascript\">");
		}
		else {
			response.appendContentString("<script>");
		}
	}

	public static void appendScriptFooterIfNecessary(WORequest request, WOResponse response) {
		if (AjaxUpdateContainer.hasUpdateContainerID(request)) {
			AjaxUtils.appendScriptFooter(response);
		}
	}

	public static void appendScriptFooter(WOResponse response) {
		response.appendContentString("</script>");
	}

	public static Object valueForBinding(String name, Object defaultValue, NSDictionary associations, WOComponent component) {
		Object value = AjaxUtils.valueForBinding(name, associations, component);
		if (value != null) {
			return value;
		}
		return defaultValue;
	}

	public static String stringValueForBinding(String name, NSDictionary associations, WOComponent component) {
		WOAssociation association = (WOAssociation) associations.objectForKey(name);
		if (association != null) {
			return (String) association.valueInComponent(component);
		}
		return null;
	}

	public static Object valueForBinding(String name, NSDictionary associations, WOComponent component) {
		WOAssociation association = (WOAssociation) associations.objectForKey(name);
		if (association != null) {
			return association.valueInComponent(component);
		}
		return null;
	}

	public static boolean booleanValueForBinding(String name, boolean defaultValue, NSDictionary associations, WOComponent component) {
		WOAssociation association = (WOAssociation) associations.objectForKey(name);
		if (association != null) {
			return association.booleanValueInComponent(component);
		}
		return defaultValue;
	}

	public static void setValueForBinding(Object value, String name, NSDictionary associations, WOComponent component) {
		WOAssociation association = (WOAssociation) associations.objectForKey(name);
		if (association != null) {
			association.setValue(value, component);
		}
	}

	/**
	 * Returns the array bound to the given association.
	 * 
	 * @param <T> the array type
	 * @param component the component to resolve against
	 * @param association the association to retrieve a value for
	 * @return an array (or null)
	 */
	public static <T> NSArray<T> arrayValueForAssociation(WOComponent component, WOAssociation association) {
		NSArray<T> array = null;
		if (association != null) {
			array = AjaxUtils.arrayValueForObject(association.valueInComponent(component));
		}
		return array;
	}

	/**
	 * Returns the array bound to the given binding name.
	 * 
	 * @param <T> the array type
	 * @param component the component to resolve against
	 * @param bindingName the name of the binding
	 * @return an array (or null)
	 */
	public static <T> NSArray<T> arrayValueForBinding(WOComponent component, String bindingName) {
		return AjaxUtils.arrayValueForObject(component.valueForBinding(bindingName));
	}

	/**
	 * Returns the array for the given object.  If the object is a string, it will be parsed as a
	 * JSON value.
	 * 
	 * @param <T> the array type
	 * @param value the object value
	 * @return an array (or null)
	 */
	@SuppressWarnings("unchecked")
	public static <T> NSArray<T> arrayValueForObject(Object value) {
		NSArray arrayValue;
		if (value == null) {
			arrayValue = null;
		}
		else if (value instanceof NSArray) {
			arrayValue = (NSArray<T>) value;
		}
		else if (value instanceof String) {
			try {
				String strValue = ((String) value).trim();
				if (!strValue.startsWith("[")) {
					strValue = "[" + strValue + "]";
				}
				JSONSerializer serializer = new JSONSerializer();
				serializer.registerDefaultSerializers();
				Object objValue = serializer.fromJSON(strValue);
				if (objValue.getClass().isArray()) {
					arrayValue = new NSArray((Object[]) objValue);
				}
				else if (objValue instanceof Collection) {
					arrayValue = new NSArray((Collection) objValue);
				}
				else {
					arrayValue = new NSArray(objValue);
				}
			}
			catch (Throwable e) {
				throw new IllegalArgumentException("Failed to convert String to array.", e);
			}
		}
		else {
			throw new IllegalArgumentException("Unable to convert '" + value + "' to an array.");
		}
		return arrayValue;
	}

	/**
	 * Returns an Ajax component action url. Using an ajax component action urls guarantees that caching during your
	 * ajax request will be handled appropriately.
	 * 
	 * @param context
	 *            the context of the request
	 * @return an ajax request url.
	 */
	public static String ajaxComponentActionUrl(WOContext context) {
		String actionUrl = context.componentActionURL();
		if (AjaxRequestHandler.useAjaxRequestHandler()) {
			actionUrl = actionUrl.replaceFirst("/" + WOApplication.application().componentRequestHandlerKey() + "/", "/" + AjaxRequestHandler.AjaxRequestHandlerKey + "/");
		}
		return actionUrl;
	}

	public static void appendTagAttributeAndValue(WOResponse response, WOContext context, WOComponent component, NSDictionary associations, String name) {
		AjaxUtils.appendTagAttributeAndValue(response, context, component, associations, name, null);
	}

	public static void appendTagAttributeAndValue(WOResponse response, WOContext context, WOComponent component, NSDictionary associations, String name, String appendValue) {
		AjaxUtils.appendTagAttributeAndValue(response, context, component, name, (WOAssociation) associations.objectForKey(name), appendValue);
	}

	public static void appendTagAttributeAndValue(WOResponse response, WOContext context, WOComponent component, String name, WOAssociation association) {
		AjaxUtils.appendTagAttributeAndValue(response, context, component, name, association, null);
	}

	public static void appendTagAttributeAndValue(WOResponse response, WOContext context, WOComponent component, String name, WOAssociation association, String appendValue) {
		if (association != null || appendValue != null) {
			String value = null;
			if (association != null) {
				value = (String) association.valueInComponent(component);
			}
			if (value == null || value.length() == 0) {
				value = appendValue;
			}
			else if (appendValue != null && appendValue.length() > 0) {
				if (!value.endsWith(";")) {
					value += ";";
				}
				value += appendValue;
			}
			if (value != null) {
				response._appendTagAttributeAndValue(name, value, true);
			}
		}
	}

}
