//
// ERD2WEditSortedToManyRelationship.java: Class file for WO Component 'ERD2WEditSortedToManyRelationship'
// Project ERDirectToWeb
//
// Created by bposokho on Thu Sep 19 2002
//
package er.directtoweb;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;

import er.extensions.*;

public class ERD2WEditSortedToManyFault extends D2WEditToManyFault {

    public static final ERXLogger log = ERXLogger.getERXLogger(ERD2WEditSortedToManyFault.class);

    public ERD2WEditSortedToManyFault(WOContext context) {
        super(context);
    }

    /**
     * Computes the destination entity that we're editing.  Hits the "destinationEntityName"
     * rule.
     *
     * @return destination entity
     */
    public EOEntity destinationEntity() {
        final String destinationEntityName = (String)d2wContext().valueForKey("destinationEntityName");
        EOEntity result = null;

        if ( destinationEntityName != null )
            result = EOUtilities.entityNamed(object().editingContext(), destinationEntityName);

        return result;
    }

    public String indexKey(){
        final EOEntity destinationEntity = destinationEntity();
        String indexKey = null;

        if ( destinationEntity != null ) {
            final String isSortedJoinValue = (String)destinationEntity.userInfo().valueForKey("isSortedJoinEntity");

            if ( "true".equals(isSortedJoinValue) ) {
                synchronized (_context) {
                    _context.setEntity(destinationEntity);
                    indexKey = (String)_context.valueForKey("indexKey");
                }
            }
        }

        return indexKey;
    }


    private static D2WContext _context=ERD2WContext.newContext();
    public NSArray sortedBrowserList() {
        NSArray result = browserList();
        if (indexKey()!=null)
            result = ERXArrayUtilities.sortedArraySortedWithKey(result,
                                                                indexKey(),
                                                                null);

        return result;
    }

    public String browserStringForItem(){
        String result = super.browserStringForItem();
        if(showIndex()){
            Integer index = (Integer)browserItem.valueForKey(indexKey());
            if(index != null){
                result = index.intValue() + ". " + result;
            }
        }
        return result;
    }

    public boolean showIndex(){
        return ERXValueUtilities.booleanValueWithDefault(d2wContext().valueForKey("showIndex"), false);
    }

    public int browserSize() {
        int browserSize = 10;  // reasonable default value
        int maxBrowserSize = 20;

        String contextSize = (String)d2wContext().valueForKey("browserSize");
        if(contextSize != null) {
            try {
                browserSize = Integer.parseInt(contextSize);
            } catch(NumberFormatException nfe) {
                log.error("browserSize not a number: " + browserSize);
            }
        }
        String maxContextSize = (String)d2wContext().valueForKey("maxBrowserSize");
        if(maxContextSize != null) {
            try {
                maxBrowserSize = Integer.parseInt(maxContextSize);
            } catch(NumberFormatException nfe) {
                log.error("maxBrowserSize not a number: " + maxBrowserSize);
            }
        }

        NSArray sortedBrowserList = sortedBrowserList();
        if(sortedBrowserList != null) {
            int count = sortedBrowserList.count();
            browserSize = (count > browserSize && count < maxBrowserSize) ? count : browserSize;
        }
        return browserSize;
    }
}
