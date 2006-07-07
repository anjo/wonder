/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import org.apache.log4j.Logger;

import com.webobjects.appserver.*;

import er.extensions.*;

/**
 * Used to edit passwords where when changed the changed value must be confirmed.<br />
 *
 * @binding errorMessage
 * @binding password
 * @binding passwordConfirm
 * @binding extraBindings
 * @binding key
 * @binding object
 */

public class ERDEditPasswordConfirm extends ERDCustomEditComponent {

    /** logging support */
    public static final Logger log = Logger.getLogger(ERDEditPasswordConfirm.class);

    public int length;

    public ERDEditPasswordConfirm(WOContext context) { super(context); }
    public boolean passwordExists() { return objectKeyPathValue() != null ? true : false; }
}