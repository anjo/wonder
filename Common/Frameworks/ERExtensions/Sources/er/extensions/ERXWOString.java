package er.extensions;

import java.text.*;

import com.webobjects.appserver.*;
import com.webobjects.appserver._private.*;
import com.webobjects.foundation.*;


/**
 * Reimplementation of WOString that can resolve localized format strings. 
 * This is very useful as most of the components in DirectToWeb use a "format"-String
 * binding - so to use localized patterns, you'd need to re-implement all these
 * components.
 * @author ak
 */
public class ERXWOString extends WODynamicElement {
	
    public static ERXLogger log = ERXLogger.getERXLogger(ERXWOString.class);
    
    protected WOAssociation _dateFormat;
    protected WOAssociation _numberFormat;
    protected WOAssociation _formatter;
    protected WOAssociation _value;
    protected WOAssociation _escapeHTML;
    protected WOAssociation _valueWhenEmpty;
    
    boolean _shouldFormat;
	
    public ERXWOString(String s, NSDictionary nsdictionary, WOElement woelement)
	{
    	super(null, null, null);
    	_value = (WOAssociation)nsdictionary.objectForKey("value");
    	if(_value == null) {
    		throw new WODynamicElementCreationException("<" + getClass().getName() + "> ( no 'value' attribute specified.");
    	}
    	_valueWhenEmpty = (WOAssociation)nsdictionary.objectForKey("valueWhenEmpty");
    	_escapeHTML = (WOAssociation)nsdictionary.objectForKey("escapeHTML");
    	_dateFormat = (WOAssociation)nsdictionary.objectForKey("dateformat");
    	_numberFormat = (WOAssociation)nsdictionary.objectForKey("numberformat");
    	_formatter = (WOAssociation)nsdictionary.objectForKey("formatter");
    	
    	if(_dateFormat != null || _numberFormat != null || _formatter != null)
    		_shouldFormat = true;
    	else
    		_shouldFormat = false;
    	
    	if((_dateFormat != null && _numberFormat != null)|| 
    			(_formatter != null && _dateFormat != null) || 
				(_formatter != null && _numberFormat != null)) {
    		throw new WODynamicElementCreationException("<" + getClass().getName() + "> ( cannot have 'dateFormat' and 'numberFormat' or 'formatter' attributes at the same time.");
    	}
	}
	
    public void appendToResponse(WOResponse woresponse, WOContext wocontext) {
    	WOComponent component = wocontext.component();
    	Object valueInComponent = null;
    	
    	if(_value != null) {
    		valueInComponent = _value.valueInComponent(component);
    		if(_shouldFormat) {
    			Format format = null;
    			if(_formatter != null) {
    				format = (Format)_formatter.valueInComponent(component);
    			}
    			if(format == null) {
    				if(_dateFormat != null) {
    					String formatString = (String)_dateFormat.valueInComponent(component);
    					if(formatString == null) {
    						format = ERXTimestampFormatter.defaultDateFormatterForObject(formatString);
    					} else {
    						format = ERXTimestampFormatter.dateFormatterForPattern(formatString);
    					}
    				} else if(_numberFormat != null) {
    					String formatString = (String)_numberFormat.valueInComponent(component);
    					if(formatString == null) {
        					format = ERXNumberFormatter.defaultNumberFormatterForObject(valueInComponent);
    					} else {
        					format = ERXNumberFormatter.numberFormatterForPattern(formatString);
    					}
    				}
    			}
    			if(format != null) {
    				try {
    					valueInComponent = format.format(valueInComponent);
    				} catch(IllegalArgumentException ex) {
    					log.debug("Exception while formatting", ex);
    					valueInComponent = null;
    				}
    			} else {
    				if(valueInComponent != null) {
    					log.debug("no formatter found!" + valueInComponent);
    				}
    			}
    		}
    	} else {
    		log.warn("value binding is null !");
    	}
    	String stringValue = null;
    	
    	if(valueInComponent != null)
    		stringValue = valueInComponent.toString();
    	if((stringValue == null || stringValue.length() == 0) && _valueWhenEmpty != null) {
    		stringValue = (String)_valueWhenEmpty.valueInComponent(component);
    		woresponse.appendContentString(stringValue);
    	} else if(stringValue != null) {
    		boolean escapeHTML = true;
    		if(_escapeHTML != null)
    			escapeHTML = _escapeHTML.booleanValueInComponent(component);
    		if(escapeHTML)
    			woresponse.appendContentHTMLString(stringValue);
    		else
    			woresponse.appendContentString(stringValue);
    	}
    }
}
