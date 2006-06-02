/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;


/**
 * Default editing context delegate. This delegate
 * augments the regular transaction process by adding
 * the calling of willInsert, willUpdate or willDelete
 * on enterprise objects that are of type ERXEnterpriseObject
 * after saveChanges is called on the editing context, but
 * before validateForSave is called on the object. These
 * methods can give the object a last chance to modify itself
 * before validation occurs. The second enhancement is a built
 * in flushing of caches on subclasses of ERXEnterpriseObject
 * when objects have changes merged in or are invalidated.
 * Being able to maintain caches on enterprise objects that
 * are flushed when the underlying values change can be very
 * handy.
 */
public class ERXDefaultEditingContextDelegate extends ERXEditingContextDelegate {

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXDefaultEditingContextDelegate.class);

    /**
     * Constructor needed for Serialable interface
     */
    public ERXDefaultEditingContextDelegate() {}
}
