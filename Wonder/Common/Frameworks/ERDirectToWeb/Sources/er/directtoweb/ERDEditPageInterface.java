/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.directtoweb.*;
import com.webobjects.eocontrol.*;

/**
 * Small improvements to the EditPageInterface.<br />
 * 
 */

public interface ERDEditPageInterface extends EditPageInterface {
    // Not having this one is very annoying.
    public EOEnterpriseObject object();    
}
