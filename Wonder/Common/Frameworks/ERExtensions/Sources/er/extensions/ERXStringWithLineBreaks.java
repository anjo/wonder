/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

/**
 * Simple component that can convert a string that has
 * line breaks and tabs in it into an html string that
 * has <BR> and &nbsp instead. Very useful for preserving
 * line breaks that are typed into a WOTextBox.
 * <br/>
 * Synopsis:<br/>
 * value=<i>aString</i>;[valueWhenEmpty=<i>aString</i>;]
 * 
 * @binding value string to be converted
 * @binding valueWhenEmpty if null or length of zero what to
 *		display
 */
public class ERXStringWithLineBreaks extends ERXStatelessComponent {

    /** holds the html-ified string */
    public String _value;

    /**
     * Public constructor
     * @param context current context
     */
    public ERXStringWithLineBreaks(WOContext context) {
        super(context);
    }
    
    /**
     * Nulls out cached instance variable: _value
     */
    public void reset() {
        super.reset();
        _value = null;
    }
    /**
     * Converts '\r\n', '\n', '\r' into "<BR>" and
     * converts '\t' into five non-breaking spaces.
     * @return converts string bound to binding: <b>value</b>
     * 		into html-ified line breaks.
     */
    // FIXME: Should use ERXSimpleHTMLFormatter
    public String value() {
        if (_value == null) {
            Object value = valueForObjectBinding("value");
            String result = null;
            if (value != null) {
                result = (value instanceof String) ? (String)value : value.toString();
                result = WOResponse.stringByEscapingHTMLString(result);
                // FIXME: This could be optimized
                result = ERXExtensions.replaceStringByStringInString("\r\n", "\r", result);
                result = ERXExtensions.replaceStringByStringInString("\n", "\r", result);
                result = ERXExtensions.replaceStringByStringInString("\r", "<BR>", result);
                result = ERXExtensions.replaceStringByStringInString("\t",
                                                                    "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;",
                                                                    result);
            }
            _value = result;
        }
        return _value;
    }

    /**
     * Returns binding <b>valueWhenEmpty</b>.
     * @return value to display when the string is empty
     */
    public Object valueWhenEmpty() {
        return valueForObjectBinding("valueWhenEmpty");
    }
}
