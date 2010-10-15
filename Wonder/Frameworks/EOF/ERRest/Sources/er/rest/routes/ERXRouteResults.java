package er.rest.routes;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSMutableDictionary;

import er.rest.ERXRestRequestNode;
import er.rest.format.ERXRestFormat;
import er.rest.format.ERXWORestResponse;
import er.rest.format.IERXRestWriter;

/**
 * ERXRouteResults encapsulates the data necessary to produce a RESTful response. This object exists to defer the
 * generation of the response (so you can chain together rest responders).
 * 
 * @author mschrag
 */
public class ERXRouteResults implements WOActionResults {
	private WOContext _context;
	private ERXRestFormat _format;
	private ERXRestRequestNode _responseNode;
	private NSMutableDictionary<String, String> _headers;

	/**
	 * Constructs an ERXRouteResults.
	 * 
	 * @param context
	 *            the current context
	 * @param format
	 *            the intended format of this response
	 * @param responseNode
	 *            the ERXRestRequestNode to render
	 */
	public ERXRouteResults(WOContext context, ERXRestFormat format, ERXRestRequestNode responseNode) {
		_context = context;
		_format = format;
		_responseNode = responseNode;
		_headers = new NSMutableDictionary<String, String>();
	}
	
	/**
	 * Adds a header for the resulting WOResponse.
	 * 
	 * @param header the header to add
	 * @param key the key to add
	 */
	public void setHeaderForKey(String header, String key) {
		_headers.setObjectForKey(header, key);
	}

	/**
	 * Returns the intended format of this response.
	 * 
	 * @return the intended format of this response
	 */
	public ERXRestFormat format() {
		return _format;
	}

	/**
	 * Returns the ERXRestRequestNode to render.
	 * 
	 * @return the ERXRestRequestNode to render
	 */
	public ERXRestRequestNode responseNode() {
		return _responseNode;
	}

	/**
	 * Generates a WOResponse out of this ERXRouteResults.
	 * 
	 * @return a generated WOResponse
	 */
	public WOResponse generateResponse() {
		WOResponse response = WOApplication.application().createResponseInContext(_context);
		IERXRestWriter writer = _format.writer();
		if (writer == null) {
			throw new IllegalStateException("There is no writer for the format '" + _format.name() + "'.");
		}
		writer.appendToResponse(_responseNode, new ERXWORestResponse(response), _format.delegate());
		if (_headers.count() > 0) {
			for (String key : _headers.keySet()) {
				response.setHeader(_headers.objectForKey(key), key);
			}
		}
		return response;
	}

}
