/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb.components.relationships;

import com.webobjects.appserver.WOContext;

/**
 * Allows custom components to be used to display the eos from a toMany.<br />
 * @d2wKey customComponentName
 * @d2wKey componentBorder
 * @d2wKey numCols
 */
public class ERD2WDisplayToManyCustom extends ERD2WDisplayToManyTable {

     public ERD2WDisplayToManyCustom(WOContext context) { super(context); }
}
