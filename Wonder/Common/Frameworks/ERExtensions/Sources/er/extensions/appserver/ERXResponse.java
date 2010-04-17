package er.extensions.appserver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSMutableData;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSRange;

import er.extensions.foundation.ERXThreadStorage;

/**
 * ERXResponse provides a place to override methods of WOResponse. This is
 * returned by default from ERXApplication. Also has support for "partials",
 * i.e. in your render tree, you can define a new "partial", where the content
 * will actually get rendered.
 * 
 * @author mschrag
 * @author ak
 */
public class ERXResponse extends WOResponse {
	public static final String ContentDispositionHeaderKey = "content-disposition";
	public static final String ContentTypeHeaderKey = "content-type";
	public static final String DisablePageCachingKey = "com.webobjects.appserver.Response.DisablePageCaching";

	public static class Context {
		protected LinkedHashMap<String, ERXResponse> partials = new LinkedHashMap<String, ERXResponse>();

		protected Stack<ERXResponse> stack = new Stack<ERXResponse>();
	}

	private LinkedHashMap<String, Integer> marks;
	private Stack<Object> _contentStack;
	private WOContext _context;

	public ERXResponse() {
	}

	/**
	 * Convenience constructor for direct actions.
	 * @param content text content of the response
	 * @param status HTTP status code of the response
	 */
	public ERXResponse(String content, int status) {
		this(content);
		setStatus(status);
	}

	/**
	 * Convenience constructor for direct actions.
	 * @param content text content of the response
	 */
	public ERXResponse(String content) {
		setContent(content);
	}

	public ERXResponse(WOContext context) {
		_context = context;
	}
	
	protected void __setContent(Object appendable) {
		try {
			WOMessage.class.getDeclaredField("_content").set(this, appendable);
		}
		catch (Throwable e) {
			throw new NSForwardException(e);
		}
	}
	
	/**
	 * Pushes a new _content onto the stack, so you can write to this response
	 * and capture the output.
	 */
	public void pushContent() {
		if (_contentStack == null) {
			_contentStack = new Stack<Object>();
		}
		_contentStack.push(_content);
		Object newContent;
		try {
			newContent = _content.getClass().newInstance();
		}
		catch (Throwable e) {
			throw new NSForwardException(e);
		}
		__setContent(newContent);
	}

	/**
	 * Pops the last _content off the stack, optionally appending the current
	 * content to it.
	 * 
	 * @param append
	 */
	public void popContent(boolean append) {
		if (_contentStack == null || _contentStack.size() == 0) {
			throw new IllegalStateException("You attempted to popContent off of an empty stack.");
		}
		Object oldAppendable = _content;
		Object appendable = _contentStack.pop();
		__setContent(appendable);
		if (append) {
			appendContentString(oldAppendable.toString());
		}
	}

	/**
	 * Call this to mark the place where a partial should get rendered.
	 * 
	 * @param key
	 */
	public void mark(String key) {
		if (marks == null) {
			marks = new LinkedHashMap<String, Integer>();
		}
		marks.put(key, _contentLength());
	}

	/**
	 * Overridden to insert the partials in the respective area.
	 */
	@Override
	public void _finalizeInContext(WOContext originalContext) {
		super._finalizeInContext(originalContext);
		if (marks != null && marks.size() > 0) {
			Context context = currentContext();
			NSMutableData content = new NSMutableData();
			int last = 0;
			for (Map.Entry<String, Integer> entry : marks.entrySet()) {
				String key = entry.getKey();
				Integer offset = entry.getValue();
				NSRange range = new NSRange(last, offset - last);
				NSData data = content().subdataWithRange(range);
				content.appendData(data);
				ERXResponse partial = context.partials.get(key);
				if (partial != null) {
					NSData partialData = partial.content();
					content.appendData(partialData);
				}
				last = offset;
			}
			NSRange range = new NSRange(last, _contentLength() - last);
			NSData data = content().subdataWithRange(range);
			content.appendData(data);
			setContent(content);
		}
	}

	private static Context currentContext() {
		Context context = (Context) ERXThreadStorage.valueForKey("ERXResponse.Context");
		if (context == null) {
			context = new Context();
			ERXThreadStorage.takeValueForKey(context, "ERXResponse.Context");
		}
		return context;
	}

	/**
	 * Returns the associated response for the supplied key. Creates it if
	 * needed.
	 * 
	 * @param key
	 *            the key to push the partial as
	 * @return the new ERXResponse to write to
	 */
	public static ERXResponse pushPartial(String key) {
		Context context = currentContext();
		WOContext wocontext = ERXWOContext.currentContext();
		context.stack.push((ERXResponse) wocontext.response());
		ERXResponse response = context.partials.get(key);
		if (response == null) {
			response = new ERXResponse(wocontext);
			context.partials.put(key, response);
		}
		wocontext._setResponse(response);
		return response;
	}

	/**
	 * Returns the top-most response after this one has been pulled from the
	 * stack.
	 * 
	 * @return the previous partial
	 */
	public static ERXResponse popPartial() {
		Context context = currentContext();
		ERXResponse response = context.stack.pop();
		WOContext wocontext = ERXWOContext.currentContext();
		wocontext._setResponse(response);
		return response;
	}

	/**
	 * The original _appendTagAttributeAndValue would skip null values, but not
	 * blank values, which would produce html like &lt;div style = ""&gt;. This
	 * implementation also skips blank values.
	 */
	@Override
	public void _appendTagAttributeAndValue(String name, String value, boolean escape) {
		if (value != null) {
			super._appendTagAttributeAndValue(name, value, escape);
		}
	}

	/**
	 * Overridden to <b>not</b> call super if trying to download an attachment
	 * to IE.
	 * 
	 * @see com.webobjects.appserver.WOResponse#disableClientCaching()
	 * 
	 */
	@Override
	public void disableClientCaching() {
		boolean isIEDownloadingAttachment = isIE() && isAttachment() && !isHTML();
		if (!isIEDownloadingAttachment) {
			//NSLog.out.appendln("Disabling client caching");
			super.disableClientCaching();
		}
		else {
			//NSLog.out.appendln("Allowing IE client caching");
		}
	}

	/**
	 * @see #disablePageCaching()
	 * @return <code>true</code> if disablePageCaching() has been called for
	 *         this response
	 */
	public boolean isPageCachingDisabled() {
		return userInfoForKey(DisablePageCachingKey) != null;
	}

	/**
	 * WO 5.4 API Sets the value for key in the user info dictionary.
	 * 
	 * @param value
	 *            value to add to userInfo()
	 * @param key
	 *            key to add value under
	 */
	public void setUserInfoForKey(Object value, String key) {
		/**
		 * require [valid_value] value != null; [valid_key] key != null;
		 **/
		NSMutableDictionary newUserInfo = new NSMutableDictionary(value, key);
		if (userInfo() != null) {
			newUserInfo.addEntriesFromDictionary(userInfo());
		}
		setUserInfo(newUserInfo);
		/** ensure [value_set] userInfoForKey(key).equals(value); **/
	}

	/**
	 * WO 5.4 API
	 * 
	 * @param key
	 *            key to return value from userInfo() for
	 * @return value from userInfo() for key, or null if not available
	 */
	public Object userInfoForKey(String key) {
		/** require [valid_key] key != null; **/
		return userInfo() != null ? userInfo().objectForKey(key) : null;
	}

	public boolean isAttachment() {
		String contentDisposition = contentDisposition();
		return contentDisposition != null && (contentDisposition.indexOf("inline") > -1 || contentDisposition.indexOf("attachment") > -1);
	}

	/**
	 * @return <code>true</code> if the content type of this response indicates
	 *         HTML
	 */
	public boolean isHTML() {
		return contentType() != null && contentType().toLowerCase().indexOf("text/html") > -1;
	}

	/**
	 * @return header value for ContentDispositionHeaderKey
	 */
	public String contentDisposition() {
		return headerForKey(ContentDispositionHeaderKey);
	}

	/**
	 * @return header value for ContentTypeHeaderKey
	 */
	public String contentType() {
		return headerForKey(ContentTypeHeaderKey);
	}

	/**
	 * @return <code>true</code> if the Request this Response is for has a user
	 *         agent that indicates and IE browser
	 */
	public boolean isIE() {
		boolean isIE = false;
		if (_context != null && _context.request() instanceof ERXRequest) {
			isIE = ((ERXRequest) _context.request()).browser().isIE();
		}
		return isIE;
	}

}
