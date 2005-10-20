package er.extensions;

import com.webobjects.appserver.*;
import com.webobjects.appserver._private.*;
import com.webobjects.foundation.*;

/**
 * Contains one case of a ERXWOSwitch.
 * @author ak (Java port)
 * @author Charles Lloyd
 */
public class ERXWOCase extends WODynamicElement {

    protected Object _value;
    protected WOElement _children;
    
    public ERXWOCase(String name, NSDictionary associations, WOElement woelement) {
        super(name, associations, woelement);
        WOAssociation assoc = (WOAssociation) associations.objectForKey("case");
        if(!(assoc instanceof WOConstantValueAssociation)) {
            throw new IllegalStateException("You must bind 'case' to a constant value");
        }
        _value = assoc.valueInComponent(null);
        _children = woelement;
    }
    
    public Object caseValue() {
        return _value;
    }
    
    public void appendToResponse(WOResponse woresponse, WOContext wocontext) {
        _children.appendToResponse(woresponse, wocontext);
    }
    
    public WOActionResults invokeAction(WORequest worequest, WOContext wocontext) {
        return _children.invokeAction(worequest, wocontext);
    }
    
    public void takeValuesFromRequest(WORequest worequest, WOContext wocontext) {
        _children.takeValuesFromRequest(worequest, wocontext);
    }
}
