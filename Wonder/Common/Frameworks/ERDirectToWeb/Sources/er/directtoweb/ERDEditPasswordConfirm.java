/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOEnterpriseObject;
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
    public static final ERXLogger log = ERXLogger.getERXLogger(ERDEditPasswordConfirm.class);

    public int length;

    public ERDEditPasswordConfirm(WOContext context) { super(context); }
    public boolean passwordExists() { return objectKeyPathValue() != null ? true : false; }
}