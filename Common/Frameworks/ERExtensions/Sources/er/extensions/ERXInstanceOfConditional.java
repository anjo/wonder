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
import java.lang.*;
import org.apache.log4j.Category;

/**
 * Conditional component that tests if an object is an instance of a given
 * class or interface
 * <br/>
 * Synopsis:<br/>
 * object=<i>anObject</i>;className=<i>aClassName2</i>;[negate=<i>aBoolean</i>;]
 * <br/>
 * Bindings:<br/>
 * <b>object</b> object to test
 * <b>className</b> class or interface name
 * <b>negate</b><br/> Inverts the sense of the conditional.
 * <br/>
 */
public class ERXInstanceOfConditional extends WOComponent {

    /** Public constructor */
    public ERXInstanceOfConditional(WOContext aContext) {
        super(aContext);
    }

    /** logging support */
    public final static Category log = Category.getInstance(ERXInstanceOfConditional.class);

    /** component is stateless */
    public boolean isStateless() { return true; }

    /** resets cached ivars */
    public void reset() {
        super.reset();
        _instanceOf = null;
    }
    /** cached value of comparison */
    private Boolean _instanceOf;

    /**
     * Tests if the bound object is an instance of the class.
     * Note: If the class is not found a ClassNotFoundException
     * will be thrown via an NSForwardException.
     * @return the boolean result of the <code>isInstance</code> test.
     */
    public boolean instanceOf() {
        if (_instanceOf == null) {
            Class instance = null;
            String className = (String)valueForBinding("className");
            if (log.isDebugEnabled())
                log.debug("Resolving class: " + className);
            instance = _NSUtilities.classWithName(className);
            if (instance == null)
                throw new NSForwardException(new ClassNotFoundException((String)valueForBinding("className")));
            _instanceOf = instance.isInstance(valueForBinding("object")) ? Boolean.TRUE : Boolean.FALSE;
        }
        return _instanceOf.booleanValue();
    }
}
