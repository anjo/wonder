package er.rest.format;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import com.webobjects.appserver.WORequest;

import er.rest.ERXRestRequestNode;
import er.rest.ERXRestUtils;

/**
 * ERXJSONRestRequestParser is an implementation of the IERXRestRequestParser interface that supports JSON document
 * requests.
 * 
 * @author mschrag
 */
public class ERXJSONRestParser implements IERXRestParser {
	public static ERXRestRequestNode createRequestNodeForJSON(String name, JSON json, boolean rootNode, ERXRestFormat.Delegate delegate) {
		ERXRestRequestNode requestNode = new ERXRestRequestNode(name, rootNode);

		if (json instanceof JSONNull) {
			// just leave the value null
		}
		else if (json instanceof JSONArray) {
			requestNode.setArray(true);
			JSONArray jsonArray = (JSONArray) json;
			for (Object obj : jsonArray) {
				if (ERXRestUtils.isPrimitive(obj)) {
					ERXRestRequestNode primitiveChild = new ERXRestRequestNode(null, obj, false);
					requestNode.addChild(primitiveChild);
					delegate.nodeDidParse(primitiveChild);
				}
				else {
					requestNode.addChild(ERXJSONRestParser.createRequestNodeForJSON(null, (JSON) obj, true, delegate));
				}
			}
		}
		else if (json instanceof JSONObject) {
			JSONObject jsonObject = (JSONObject) json;
			for (Object key : jsonObject.keySet()) {
				String strKey = (String) key;
				Object value = jsonObject.get(key);
				if (ERXRestUtils.isPrimitive(value)) {
					ERXRestRequestNode primitiveChild = new ERXRestRequestNode(strKey, value, false);
					requestNode.addChild(primitiveChild);
					delegate.nodeDidParse(primitiveChild);
				}
				else {
					requestNode.addChild(ERXJSONRestParser.createRequestNodeForJSON(strKey, (JSON) value, false, delegate));
				}
			}
		}
		else {
			throw new IllegalArgumentException("Unknown JSON value '" + json + "'.");
		}

		delegate.nodeDidParse(requestNode);

		return requestNode;
	}
	
	@Deprecated
	public ERXRestRequestNode parseRestRequest(WORequest request, ERXRestFormat.Delegate delegate) {
		return parseRestRequest(request.contentString(), delegate);
	}

	@Deprecated
	public ERXRestRequestNode parseRestRequest(String contentStr, ERXRestFormat.Delegate delegate) {
		return parseRestRequest(new ERXStringRestRequest(contentStr), delegate);
	}

	public ERXRestRequestNode parseRestRequest(IERXRestRequest request, ERXRestFormat.Delegate delegate) {
		ERXRestRequestNode rootRequestNode = null;
		String contentString = request.stringContent();
		if (contentString != null) {
			contentString = contentString.trim();
		}
		if (contentString != null && contentString.length() > 0 && !"undefined".equals(contentString)) {
			
			// MS: Support direct updating of primitive type keys -- so if you don't want to
			// wrap your request in XML, this will allow it
			// if (!contentStr.trim().startsWith("<")) {
			// contentStr = "<FakeWrapper>" + contentStr.trim() + "</FakeWrapper>";
			// }
			JSON rootJSON = JSONSerializer.toJSON(contentString, ERXJSONRestWriter._config);
			rootRequestNode = createRequestNodeForJSON(null, rootJSON, true, delegate);
		}
		else {
			rootRequestNode = new ERXRestRequestNode(null, true);
			rootRequestNode.setNull(true);
		}

		return rootRequestNode;
	}
}
