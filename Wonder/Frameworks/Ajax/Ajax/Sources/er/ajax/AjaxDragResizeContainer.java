package er.ajax;

// Generated by the WOLips Templateengine Plug-in at 09.08.2006 12:23:46

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSNumberFormatter;

import er.extensions.appserver.ERXWOContext;

/**
 * 
 * @binding minWidth
 * @binding minHeight
 * @binding minLeft
 * @binding minTop
 * @binding maxLeft
 * @binding maxTop
 * @binding ondragfocus
 * @binding ondragblur
 * @binding ondragstart
 * @binding ondragmove
 * @binding ondragend
 * @binding dimensionsFromElementWithID
 * @binding id
 * @binding class
 * @binding drsElement
 * @binding drsMoveHandle
 * @binding report
 * @binding action
 * @binding skipContainer When set to true, the container div including nested
 *          content is omitted. Use this if you want to use an existing element
 *          as container. Make sure, this element is "position:relative" and
 *          specify it's dom id using the id binding.
 */
public class AjaxDragResizeContainer extends AjaxComponent {

	public static final String EVENT_BLUR = "dragblur";

	public static final String EVENT_FOCUS = "dragfocus";

	public static final String EVENT_DRAG_END = "dragend";

	private String _dragResizeContainerID;

	private String _actionUrl;

	public AjaxDragResizeContainer(WOContext context) {
		super(context);
	}

	public boolean synchronizesVariablesWithBindings() {
		return false;
	}

	public void appendToResponse(WOResponse response, WOContext context) {
		_dragResizeContainerID = (String) valueForBinding("id", ERXWOContext.safeIdentifierName(context(), true) + "_DragResizeContainer");
		_actionUrl = AjaxUtils.ajaxComponentActionUrl(context);
		super.appendToResponse(response, context);
	}

	public String dragResizeContainerID() {
		return _dragResizeContainerID;
	}

	public String style() {
		StringBuffer sb = new StringBuffer();
		sb.append("position: relative;");
		if (canGetValueForBinding("style")) {
			sb.append(valueForBinding("style"));
		}
		return sb.toString();
	}

	public NSDictionary createAjaxOptions() {
		NSMutableDictionary options = new NSMutableDictionary();

		// defaults
		options.takeValueForKey(jsForElementOrHandle(drsElementClass()), "isElement");
		options.takeValueForKey(jsForElementOrHandle(drsMoveHandleClass()), "isHandle");

		options.takeValueForKey(jsForEvent(EVENT_DRAG_END), "ondragend");
		options.takeValueForKey(jsForEvent(EVENT_FOCUS), "ondragfocus");
		options.takeValueForKey(jsForEvent(EVENT_BLUR), "ondragblur");

		// bindings
		NSMutableArray ajaxOptionsArray = new NSMutableArray();
		ajaxOptionsArray.addObject(new AjaxOption("minWidth", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("minHeight", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("minLeft", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("minTop", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("maxLeft", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("maxTop", AjaxOption.NUMBER));

		ajaxOptionsArray.addObject(new AjaxOption("ondragfocus", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("ondragstart", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("ondragend", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("ondragmove", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("ondragblur", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("enabled", AjaxOption.BOOLEAN));

		// overwrite defaults with values recieved from bindings if any
		options.addEntriesFromDictionary(AjaxOption.createAjaxOptionsDictionary(ajaxOptionsArray, this));

		return options;
	}

	private String jsForElementOrHandle(String cssClass) {
		return "function(elm){ if (elm.className && elm.className.indexOf('" + cssClass + "') > -1) return true; }";
	}

	private String drsElementClass() {
		return (String) valueForBinding("drsElement", "drsElement");
	}

	private String drsMoveHandleClass() {
		return (String) valueForBinding("drsMoveHandle", drsElementClass());
	}

	private String jsForEvent(String event) {
		// PROTOTYPE FUNCTIONS
		return "function(isResize){ " + "this.reqNum += 1; " // Make sure that every request is unique. Otherwise
																// requests get dropped somewhere.
				+ "new Ajax.Request('" + _actionUrl + "', {method: 'get', parameters: " + "'id='+this.element.id+'&" + "event=" + event + "&" + "w='+this.elmW+'&" + "h='+this.elmH+'&" + "x='+this.elmX+'&" + "y='+this.elmY+'&" + "resized='+isResize+'&" + "reqNum='+this.reqNum" + " })}";
	}

	protected void addRequiredWebResources(WOResponse response) {
		addScriptResourceInHead(response, "prototype.js");
		addScriptResourceInHead(response, "dragresize.js");
	}

	public WOActionResults handleRequest(WORequest request, WOContext context) {
		WOResponse result = AjaxUtils.createResponse(request, context);
		result.setHeader("text/javascript", "content-type");

		if (canSetValueForBinding("report")) {
			Object o = AjaxDragResize.resizableObjectForPage(context.page(), request.stringFormValueForKey("id"));
			NSNumberFormatter formatter = new NSNumberFormatter("0");
			ResizeReport report = new ResizeReport(dragResizeContainerID(), request.stringFormValueForKey("event"), request.stringFormValueForKey("id"), request.numericFormValueForKey("x", formatter).intValue(), request.numericFormValueForKey("y", formatter).intValue(), request.numericFormValueForKey("w", formatter).intValue(), request.numericFormValueForKey("h", formatter).intValue(), "true".equals(request.stringFormValueForKey("resized")), o);
			setValueForBinding(report, "report");
		}

		if (canGetValueForBinding("action")) {
			WOActionResults results = (WOActionResults) valueForBinding("action");
			if (results != null) {
				System.out.println("AjaxDragResize.handleRequest: Not quite sure what to do with non-null results yet ...");
			}
		}
		return result;
	}

	public class ResizeReport {
		private int _x, _y, _width, _height;

		private boolean _resized;

		private Object _object;

		private String _id, _event, _container;

		public ResizeReport(String container, String event, String id, int x, int y, int width, int height, boolean resized, Object object) {
			super();
			this._x = x;
			this._y = y;
			this._width = width;
			this._height = height;
			this._resized = resized;
			this._object = object;
			this._id = id;
			this._event = event;
			this._container = container;
		}

		public int height() {
			return _height;
		}

		public boolean resize() {
			return _resized;
		}

		public Object object() {
			return _object;
		}

		public int width() {
			return _width;
		}

		public int x() {
			return _x;
		}

		public int y() {
			return _y;
		}

		public String id() {
			return _id;
		}

		public String event() {
			return _event;
		}

		public String container() {
			return _container;
		}

		public String toString() {
			return "ResizeReport: {container: " + _container + ", event: " + _event + ", id: " + _id + ", geometry: " + _width + "x" + _height + "+" + _x + "+" + _y + ", resized: " + _resized + ", " + (_object == null ? "" : "objectClass: " + _object.getClass().getName() + ", ") + "object: " + _object + "}";
		}

		public boolean isBlur() {
			return AjaxDragResizeContainer.EVENT_BLUR.equals(event());
		}

		public boolean isDragEnd() {
			return AjaxDragResizeContainer.EVENT_DRAG_END.equals(event());
		}

		public boolean isFocus() {
			return AjaxDragResizeContainer.EVENT_FOCUS.equals(event());
		}

	}
}
