package er.ajax;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver._private.WODynamicGroup;
import com.webobjects.foundation.NSDictionary;

public abstract class AjaxDynamicElement extends WODynamicGroup implements IAjaxElement {
  protected Logger log = Logger.getLogger(getClass());
  private WOElement _children;
  private NSDictionary _associations;

  public AjaxDynamicElement(String name, NSDictionary associations, WOElement children) {
    super(name, associations, children);
    _children = children;
    _associations = associations;
  }

  public NSDictionary associations() {
    return _associations;
  }

  public Object valueForBinding(String name, Object defaultValue, WOComponent component) {
    return AjaxUtils.valueForBinding(name, defaultValue, _associations, component);
  }

  public Object valueForBinding(String name, WOComponent component) {
    return AjaxUtils.valueForBinding(name, _associations, component);
  }

  public boolean booleanValueForBinding(String name, boolean defaultValue, WOComponent component) {
    return AjaxUtils.booleanValueForBinding(name, defaultValue, _associations, component);
  }

  public void setValueForBinding(Object value, String name, WOComponent component) {
    AjaxUtils.setValueForBinding(value, name, _associations, component);
  }

  protected void addScriptResourceInHead(WOContext context, WOResponse response, String framework, String fileName) {
    AjaxUtils.addScriptResourceInHead(context, response, framework, fileName);
  }

  protected void addScriptResourceInHead(WOContext context, WOResponse response, String fileName) {
    AjaxUtils.addScriptResourceInHead(context, response, fileName);
  }

  protected void addStylesheetResourceInHead(WOContext context, WOResponse response, String fileName) {
    AjaxUtils.addStylesheetResourceInHead(context, response, fileName);
  }

  protected void addStylesheetResourceInHead(WOContext context, WOResponse response, String framework, String fileName) {
    AjaxUtils.addStylesheetResourceInHead(context, response, framework, fileName);
  }

  /**
   * Execute the request, if it's comming from our action, then invoke the
   * ajax handler and put the key <code>AJAX_REQUEST_KEY</code> in the
   * request userInfo dictionary (<code>request.userInfo()</code>).
   */
  public WOActionResults invokeAction(WORequest request, WOContext context) {
    Object result = null;
    if (AjaxUtils.shouldHandleRequest(request, context, _containerID(context))) {
      Object childrenResult = null;
      if (_invokeChildrenBeforeHandleRequest()) {
        childrenResult = super.invokeAction(request, context);
      }
      AjaxResponse response = AjaxUtils.createResponse(request, context);
      if (_isDelayedElement()) {
    	  response.addDelayedElement(this, context.component(), context.elementID());
      }
      else {
    	  result = handleRequest(request, context);
      }
      if (_invokeChildrenAfterHandleRequest()) {
        childrenResult = super.invokeAction(request, context);
      }
      if (result == null) {
        result = childrenResult;
      }
      AjaxUtils.updateMutableUserInfoWithAjaxInfo(context);
    }
    else if (hasChildrenElements()) {
      result = super.invokeAction(request, context);
    }
    return (WOActionResults) result;
  }
  
  protected boolean _isDelayedElement() {
	  return false;
  }

  protected boolean _invokeChildrenBeforeHandleRequest() {
    return false;
  }

  protected boolean _invokeChildrenAfterHandleRequest() {
    return false;
  }
  
  protected String _containerID(WOContext context) {
	  return null;
  }
  
  /**
   * Overridden to call {@see #addRequiredWebResources(WOResponse)}.
   */
  public void appendToResponse(WOResponse response, WOContext context) {
    addRequiredWebResources(response, context);
  }

  public void appendTagAttributeToResponse(WOResponse response, String name, Object object) {
    if (object != null) {
      response._appendTagAttributeAndValue(name, object.toString(), true);
    }
  }

  public void takeValuesFromRequest(WORequest request, WOContext context) {
    takeChildrenValuesFromRequest(request, context);
  }

  /**
   * Override this method to append the needed scripts for this component.
   * @param res
   */
  protected abstract void addRequiredWebResources(WOResponse response, WOContext context);

  /**
   * Override this method to return the response for an Ajax request.
   * @param request
   * @param context
   * @return
   */
  public abstract WOActionResults handleRequest(WORequest request, WOContext context);

}
