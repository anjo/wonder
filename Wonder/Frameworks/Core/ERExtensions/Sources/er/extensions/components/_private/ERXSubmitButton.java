package er.extensions.components._private;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOAssociation;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver._private.WOConstantValueAssociation;
import com.webobjects.appserver._private.WODynamicElementCreationException;
import com.webobjects.appserver._private.WOInput;
import com.webobjects.foundation.NSDictionary;

import er.extensions.appserver.ERXApplication;
import er.extensions.appserver.ERXBrowser;
import er.extensions.appserver.ERXBrowserFactory;
import er.extensions.appserver.ERXWOContext;
import er.extensions.foundation.ERXKeyValueCodingUtilities;
import er.extensions.foundation.ERXProperties;

/**
 * Clone of WOSubmitButton that should emit <code>&lt;button&gt;</code> tags instead of 
 * <code>&lt;input&gt;</code> tags. This allows you to use HTML content and superior style
 * features. <br />
 * Unfortunately, IE is totally broken and always submits all buttons on a page,
 * which makes it unusable for actions. So this component inserts some javascript to fix this.
 * Of course, this means your button is only usable with scripting turned on in IE.<br />
 * You can add this class via ERXPatcher.setClassForName(ERXSubmitButton.class, "WOSubmitButton");
 * and see how this works out or use it explicitely. If you just patch it into the system,
 * the value is used as the content, otherwise you can put any html you like into it.
 * @author ak
 * 
 * @property 	er.extensions.components._private.ERXSubmitButton.useIEFix		opt-out
 */
public class ERXSubmitButton extends WOInput {

	public static final Logger log = Logger.getLogger(ERXSubmitButton.class);

	public static final String STYLE_PREFIX = "ERXSubmitButton-";
	private static boolean useIEFix = ERXProperties.booleanForKeyWithDefault("er.extensions.components._private.ERXSubmitButton.useIEFix", true);
	
    protected WOAssociation _class;
    protected WOAssociation _id;
    protected WOAssociation _action;
    protected WOAssociation _actionClass;
    protected WOAssociation _directActionName;
    protected WOAssociation _shouldSubmitForm;
    
    private static String ieFix = "<script>window.btnunload = window.onload;\n" +
    "window.onload = function() {\n" +
    "    var btns = document.getElementsByTagName('button');\n" +
    "    for(var i=0;i<btns.length;i++) {\n" +
    "        btns[i].btnonclick = btns[i].onclick;\n" +
    "        btns[i].onclick = function() {\n" +
    "            var bs = document.getElementsByTagName('button');\n" +
    "            var disabledbs = new Array();\n" +
    "            for (var i=0;i<bs.length;i++) {\n" +
    "                if (bs[i] != this && ! bs[i].disabled) { \n" +
    "                    bs[i].disabled = true;\n" +
    "                    disabledbs[disabledbs.length] = bs[i];\n" +
    "                }\n" +
    "            }\n" +
    "            var savedInnerHTML = this.innerHTML;\n" +
    "            this.innerHTML = this.value;\n" +
    "            if(this.btnonclick) {\n" +
    "                var result = this.btnonclick();\n" +
    "                if ( ! result) {\n" +
    "                    this.innerHTML = savedInnerHTML;\n" +
    "                    for (var i=0;i<disabledbs.length;i++) {\n" +
    "                        disabledbs[i].disabled = false;\n" +
    "                    }\n" +
    "                }\n" +
    "                return result;\n" +
    "            }\n" +
    "            return true;\n" +
    "        }\n" +
    "    }\n" +
    "    if(window.btnunload) return window.btnunload();\n" +
    "};</script>";
    
    public static void appendIEButtonFixToResponse(WOContext wocontext, WOResponse woresponse) {
		if(!ERXWOContext.contextDictionary().containsKey("ERXWOSubmit.ieFixed")) {
			ERXBrowser browser = ERXBrowserFactory.factory().browserMatchingRequest(wocontext.request());
			if (browser.isIE()) {
				woresponse._appendContentAsciiString(ieFix);
			}
			ERXWOContext.contextDictionary().setObjectForKey(Boolean.TRUE, "ERXWOSubmit.ieFixed");
		}
    }
    
    public ERXSubmitButton(String arg0, NSDictionary<String, WOAssociation> nsdictionary, WOElement arg2) {
        super("button", nsdictionary, arg2);
        if(_value == null)
            _value = new WOConstantValueAssociation("Submit");
        _shouldSubmitForm = (WOAssociation)_associations.removeObjectForKey("shouldSubmitForm");
        _action = (WOAssociation)_associations.removeObjectForKey("action");
        _actionClass = (WOAssociation)_associations.removeObjectForKey("actionClass");
        _directActionName = (WOAssociation)_associations.removeObjectForKey("directActionName");
        
        // hack for 5.4
        if (ERXApplication.isWO54()) {
        	_class = (WOAssociation) nsdictionary.valueForKey("class");
        }
        else {
        	_class = (WOAssociation)_associations.removeObjectForKey("class");
        }

        // hack for 5.4
        if (ERXApplication.isWO54()) {
        	_id = (WOAssociation) nsdictionary.valueForKey("id");
        }
        else {
        	_id = (WOAssociation)_associations.removeObjectForKey("id");
        }

        if(_action != null && _action.isValueConstant())
            throw new WODynamicElementCreationException("<" + getClass().getName() + ">'action' is a constant.");
        if(_action != null && _directActionName != null || _action != null && _actionClass != null)
            throw new WODynamicElementCreationException("<" + getClass().getName() + "> Either 'action' and 'directActionName' both exist, or 'action' and 'actionClass' both exist ");
    }
    	   
    protected boolean isDisabledInContext(WOContext context) {
    	WOAssociation disabled = (WOAssociation) ERXKeyValueCodingUtilities.privateValueForKey(this, "_disabled");
    	return disabled != null && disabled.booleanValueInComponent(context.component());
    }

    protected String type() {
        return "submit";
    }

    public String toString() {
        return "<ERXSubmitButton  action: " + (_action == null ? "null" : _action.toString()) + " actionClass: " + (_actionClass == null ? "null" : _actionClass.toString()) + ">";
    }


    public void takeValuesFromRequest(WORequest worequest, WOContext wocontext) {
    	//System.out.println(worequest.formValues());
    	/*WOComponent wocomponent = wocontext.component();
    	if(!disabledInComponent(wocomponent) && wocontext._wasFormSubmitted()) {
    		String s1 = nameInContext(wocontext, wocomponent);
    		if(s1 != null) {
    			String s = worequest.stringFormValueForKey(s1);
    			_value.setValue(s, wocomponent);
    		}
    	}*/
    }

    protected String elementName(WOContext wocontext) {
    	return elementName();
    }
    
    protected void _appendOpenTagToResponse(WOResponse woresponse, WOContext wocontext) {
    	if (useIEFix) appendIEButtonFixToResponse(wocontext, woresponse);
    	woresponse.appendContentCharacter('<');
    	woresponse._appendContentAsciiString(elementName(wocontext));   	
    	appendAttributesToResponse(woresponse, wocontext);
    	woresponse.appendContentCharacter('>');
     }
    
    public void appendAttributesToResponse(WOResponse woresponse, WOContext wocontext) {
    	appendConstantAttributesToResponse(woresponse, wocontext);
    	appendNonURLAttributesToResponse(woresponse, wocontext);
    	appendURLAttributesToResponse(woresponse, wocontext);
       	String css = "";
       	
   		if (_class != null) {
			css = (String) _class.valueInComponent(wocontext.component());
		}
       	
    	WOAssociation assoc = _action;

    	if(_action != null) {
    		css += " " + STYLE_PREFIX + _action.keyPath().replaceAll("\\W+", "");
    	} else if(_directActionName != null) {
    		css += " " + STYLE_PREFIX + _directActionName.valueInComponent(wocontext.component());
    	}
    	// log.info(css);
    	if(css.length() > 0) {
    		woresponse._appendTagAttributeAndValue("class", css, false);
    	}
    	if (_id != null) {
    		woresponse._appendTagAttributeAndValue("id", (String) _id.valueInComponent(wocontext.component()), false);
    	}
    	boolean shouldSubmitForm = (_shouldSubmitForm != null ? _shouldSubmitForm.booleanValueInComponent(wocontext.component()) : true);

    	if(isDisabledInContext(wocontext)) {
    		woresponse.appendContentCharacter(' ');
    		woresponse._appendContentAsciiString("disabled=\"disabled\"");
    	}
    	_appendTypeAttributeToResponse(woresponse, wocontext);
    	_appendValueAttributeToResponse(woresponse, wocontext);
    	_appendNameAttributeToResponse(woresponse, wocontext);
    	if(!shouldSubmitForm) {
    		String action = (String) wocontext.componentActionURL();
    		woresponse._appendTagAttributeAndValue("onclick", "document.location.href='" + action + "'; return false;", false);
    	}
    }
    
	protected void _appendTypeAttributeToResponse(WOResponse response, WOContext context) {
		if(type() != null && type().length() > 0)
			response._appendTagAttributeAndValue("type", type(), false);
	}

    protected void _appendCloseTagToResponse(WOResponse woresponse, WOContext wocontext) {
    	woresponse._appendContentAsciiString("</");
    	woresponse._appendContentAsciiString(elementName(wocontext));
    	woresponse.appendContentCharacter('>');
    }

    public WOActionResults invokeAction(WORequest worequest, WOContext wocontext) {
        Object obj = null;
        WOComponent wocomponent = wocontext.component();
        if(!isDisabledInContext(wocontext) && wocontext._wasFormSubmitted()) {
            if(wocontext._isMultipleSubmitForm()) {
                if(worequest.formValueForKey(nameInContext(wocontext, wocomponent)) != null) {
                    wocontext._setActionInvoked(true);
                    if(_action != null)
                        obj = (WOActionResults)_action.valueInComponent(wocomponent);
                    if(obj == null)
                        obj = wocontext.page();
                }
            } else {
                wocontext._setActionInvoked(true);
                if(_action != null)
                    obj = (WOActionResults)_action.valueInComponent(wocomponent);
                if(obj == null)
                    obj = wocontext.page();
            }
        }
        return ((WOActionResults) (obj));
    }

    private String _actionClassAndName(WOContext wocontext) {
        String s = computeActionStringInContext(_actionClass, _directActionName, wocontext);
        return s;
    }
    
    public void appendChildrenToResponse(WOResponse woresponse, WOContext wocontext) {
        if(hasChildrenElements()) {
            super.appendChildrenToResponse(woresponse, wocontext);
        } else {
            String value = (String) _value.valueInComponent(wocontext.component());
            if(value == null) {
                value = "Submit";
            }
            woresponse._appendContentAsciiString(value);
        }
    }

    protected void _appendNameAttributeToResponse(WOResponse woresponse, WOContext wocontext) {
        if(_directActionName != null || _actionClass != null)
            woresponse._appendTagAttributeAndValue("name", _actionClassAndName(wocontext), false);
        else
            super._appendNameAttributeToResponse(woresponse, wocontext);
    }

    public void appendToResponse(WOResponse woresponse, WOContext wocontext) {
    	if(wocontext == null || woresponse == null) {
    		return;
    	}
    	//System.out.println(useButton + ": " + userAgent);
    	// useButton = !useButton;
    	String s = elementName();
    	if(s != null) {
    		_appendOpenTagToResponse(woresponse, wocontext);
    	}
    	appendChildrenToResponse(woresponse, wocontext);
    	if(s != null) {
    		_appendCloseTagToResponse(woresponse, wocontext);
    	}
    	if(_directActionName != null || _actionClass != null) {
    		woresponse._appendContentAsciiString("<input type=\"hidden\" name=\"WOSubmitAction\"");
    		woresponse._appendTagAttributeAndValue("value", _actionClassAndName(wocontext), false);
    		woresponse.appendContentString(" />");
    	}
    }
}