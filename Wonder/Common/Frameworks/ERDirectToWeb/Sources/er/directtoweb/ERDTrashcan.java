/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.directtoweb.*;
import er.extensions.*;

/**
 * A better trashcan.  As this component is named inconsistently with the
 * other buttons that operate on lists, {@see ERDDeleteButton} should be used instead.<br />
 * 
 * @binding object
 * @binding dataSource
 * @binding d2wContext
 * @binding trashcanExplanation
 * @binding noTrashcanExplanation
 */

public class ERDTrashcan extends ERDDeleteButton {

    public ERDTrashcan(WOContext context) { super(context); }
}
