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
import java.util.*;

public class ERXGroupingRepetition extends WOComponent {

    public ERXGroupingRepetition(WOContext aContext) {
        super(aContext);
    }

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXGroupingRepetition.class);

    private NSMutableArray _sections;
    private Object _sectionItem;
    private NSMutableDictionary _itemsPerSection=new NSMutableDictionary();
    private final static Object NULL="N/A";
    
    public boolean synchronizesVariablesWithBindings() { return false; }
    public boolean isStateless() { return true; }
    
    public NSArray sections() {
        if (_sections==null) {
            _sections= new NSMutableArray();
            NSArray list=(NSArray)valueForBinding("list");
            if (list!=null) {
                for (Enumeration e=list.objectEnumerator(); e.hasMoreElements();) {
                    Object item=e.nextElement();
                    if(log.isDebugEnabled()) log.debug("item = "+item);
                    setValueForBinding(item,"item");
                    // Sections have to be copiable objects -- no EOs!!
                    Object section=valueForBinding("sectionForItem");
                    if(log.isDebugEnabled()) log.debug("section = "+section);
                    if (section==null) section=NULL;
                    Object sectionKey=copiableKeyForSection(section);
                    if(log.isDebugEnabled()) log.debug("copiableKeyForSection = "+sectionKey);
                    NSMutableArray itemsForSection=null;
                    if (_sections.containsObject(section))
                        itemsForSection=(NSMutableArray)_itemsPerSection.objectForKey(sectionKey);
                    else {
                        _sections.addObject(section);
                        itemsForSection=new NSMutableArray();
                        _itemsPerSection.setObjectForKey(itemsForSection,sectionKey);
                    }
                    itemsForSection.addObject(item);
                }
            }
        }
        return _sections;
    }

    private String _sectionKey=null;
    public Object copiableKeyForSection(Object section) {
        Object result=section;
        if (section!=null && section!=NULL) {
            if(log.isDebugEnabled()) log.debug("_sectionKey = "+_sectionKey);
            if (_sectionKey==null) {
                _sectionKey=(String)valueForBinding("sectionKey");
            }
            if (_sectionKey!=null && section instanceof EOEnterpriseObject) {
                result = ((EOEnterpriseObject)section).valueForKey(_sectionKey);
            } else if (_sectionKey!=null && section instanceof NSArray) {
                result = ((NSArray)((NSArray)section).valueForKey(_sectionKey)).componentsJoinedByString(",");
            }	
        }
        return result!=null ? result : NULL;        
    }
    
    public void setSectionItem(Object section) {
        _sectionItem=section;
        setValueForBinding((_sectionItem!=NULL && (_sectionItem ==null || !_sectionItem.equals(NULL))) ?
                           _sectionItem : null, "subListSection");
        setValueForBinding(_itemsPerSection.objectForKey(copiableKeyForSection(_sectionItem)),"subList");
    }
    
    public void reset() {
        _sections=null;
        _sectionItem=null;
        _sectionKey=null;
    }
}
