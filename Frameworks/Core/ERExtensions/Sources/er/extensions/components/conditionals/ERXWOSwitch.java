package er.extensions.components.conditionals;

import java.util.Enumeration;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOAssociation;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODynamicElement;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver._private.WODynamicGroup;
import com.webobjects.appserver._private.WOHTMLBareString;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;


/**
 * Adds a "multiple if" element to the WO templates. You'd use them to wrap "ERXWOCase" elements with
 * their case bound to a value.
 <code><pre>
 ==========================
 Example.wo/Example.html
 ==========================
 &lt;webobject name=Switch&gt;
    &lt;webobject name=CaseOne&gt;
        &lt;h2&gt;One: &lt;webobject name=ChosenCaseString&gt;&lt;/webobject&gt;&lt;/h2&gt;
    &lt;/webobject&gt;

    &lt;webobject name=CaseTwo&gt;
        &lt;h2&gt;Two: &lt;webobject name=ChosenCaseString&gt;&lt;/webobject&gt;&lt;/h2&gt;
    &lt;/webobject&gt;

    &lt;webobject name=CaseThree&gt;
        &lt;h2&gt;Three: &lt;webobject name=ChosenCaseString&gt;&lt;/webobject&gt;&lt;/h2&gt;
    &lt;/webobject&gt;

    &lt;webobject name=CaseFour&gt;
        &lt;h2&gt;Four: &lt;webobject name=ChosenCaseString&gt;&lt;/webobject&gt;&lt;/h2&gt;
    &lt;/webobject&gt;    

    &lt;webobject name=DefaultCase&gt;
        &lt;h2&gt;Default: &lt;webobject name=ChosenCaseString&gt;&lt;/webobject&gt;&lt;/h2&gt;
    &lt;/webobject&gt;    
&lt;/webobject&gt;
 or
    &lt;wo:ERXWOSwitch case="$case"&gt;
        &lt;wo:ERXWOCase case="caseOne"&gt;
                Case One!
        &lt;/wo:ERXWOCase&gt;
        &lt;wo:ERXWOCase case="caseTwo"&gt;
                Case Two!
        &lt;/wo:ERXWOCase&gt;
        &lt;wo:WXCase case="default"&gt;
                OTHER
        &lt;/wo:WXCase&gt;
    &lt;/wo:ERXWOSwitch&gt;

 ==========================
 Example.wo/Example.wod
 ==========================

Switch: ERXWOSwitch {
    case = chosenCase;
}

CaseFour: ERXWOCase {
    case = "Fourth";
}

CaseOne: ERXWOCase {
    case = "First";
}

CaseThree: ERXWOCase {
    case = "Third";
}

CaseTwo: ERXWOCase {
    case = 2;
}

DefaultCase: WXCase {
    case = "default";
}

ChosenCaseString: WOString {
    value = chosenCase;
}

 ==========================
 Example.java
 ==========================

public Object chosenCase() {
    Object objects = new Object[] {"Fourth", "Third", "First", new Integer(2), "dummy"};
    return objects[(new Random()).nextInt(objects.length)];
}

 </code></pre>
 * @author ak (Java port)
 * @author Charles Lloyd
 * @binding case the ivar that holds the value to be switched on
 */
public class ERXWOSwitch extends WODynamicElement {
    
    private NSDictionary _childCases;
    private WOAssociation _case;
    
    public ERXWOSwitch(String name, NSDictionary associations, WOElement template) {
        super(name, associations, template);
        NSMutableDictionary dict = new NSMutableDictionary();
        _case = (WOAssociation) associations.objectForKey("case");
        for(Enumeration e = ((WODynamicGroup)template).childrenElements().objectEnumerator(); e.hasMoreElements(); ) {
            WOElement child = (WOElement)e.nextElement();
            if(child instanceof ERXWOCase) {
                Object value = ((ERXWOCase)child).caseValue();
                dict.setObjectForKey(child, value);
            } else if(!(child instanceof WOHTMLBareString)) {
                throw new IllegalStateException("Direct children must be ERXWOCase");
            }
        }
        _childCases = dict.immutableClone();
    }

    protected WOElement childCaseInContext(WOContext context) {
        Object value = _case.valueInComponent(context.component());
        value = (value == null ? "default" : value);
        
        WOElement result = (WOElement) _childCases.objectForKey(value);
        
        if(result == null) {
            result = (WOElement) _childCases.objectForKey("default");
        }
        if(result == null) {
            result = new WOHTMLBareString("");
        }
        return result;
    }
    
    public void appendToResponse(WOResponse woresponse, WOContext wocontext) {
        childCaseInContext(wocontext).appendToResponse(woresponse, wocontext);
    }
    
    public WOActionResults invokeAction(WORequest worequest, WOContext wocontext) {
        return childCaseInContext(wocontext).invokeAction(worequest, wocontext);
    }
    
    public void takeValuesFromRequest(WORequest worequest, WOContext wocontext) {
        childCaseInContext(wocontext).takeValuesFromRequest(worequest, wocontext);
    }
    
}
