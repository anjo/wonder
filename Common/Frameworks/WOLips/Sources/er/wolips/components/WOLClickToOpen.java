package er.wolips.components;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSBundle;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.ERXProperties;
import er.extensions.ERXResponseRewriter;
import er.wolips.WOLipsUtilities;

/**
 * WOLClickToOpen provide support for opening components on your page
 * based on metadata that is added by ERXComponent (or from your component
 * base class).
 * 
 * @author mschrag
 */
public class WOLClickToOpen extends WOComponent {
  public WOLClickToOpen(WOContext context) {
    super(context);
  }

  @Override
  public boolean isStateless() {
    return true;
  }

  public boolean canClickToOpen() {
    return ERXProperties.booleanForKeyWithDefault("er.component.clickToOpen", false);
  }

  public String clickToOpenUrl() {
    String app = (String) valueForBinding("app");
    if (app == null) {
      app = NSBundle.mainBundle().name();
    }
    NSMutableDictionary params = new NSMutableDictionary();
    params.setObjectForKey(app, "app");
    params.setObjectForKey("REPLACEME", "component");
    return WOLipsUtilities.wolipsUrl("openComponent", params);
  }

  @Override
  public void appendToResponse(WOResponse woresponse, WOContext wocontext) {
    WOLipsUtilities.includePrototype(woresponse, wocontext);
    ERXResponseRewriter.addScriptResourceInHead(woresponse, wocontext, "WOLips", "wolips.js");
    super.appendToResponse(woresponse, wocontext);
  }
}