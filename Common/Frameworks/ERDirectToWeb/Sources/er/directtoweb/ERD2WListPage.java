/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import er.extensions.*;

public abstract class ERD2WListPage extends ERD2WPage implements ERDListPageInterface, SelectPageInterface, ERXComponentActionRedirector.Restorable  {

    /** logging support */
    public final static ERXLogger log = ERXLogger.getERXLogger(ERD2WListPage.class);

    /**
     * Public constructor
     * @param c current context
     */
    public ERD2WListPage(WOContext c) {
        super(c);
        NSNotificationCenter.defaultCenter().addObserver(this, new NSSelector("savedChanges", ERXConstant.NotificationClassArray), EOEditingContext.EditingContextDidSaveChangesNotification, null);
    }

    public void finalize() throws Throwable {
        NSNotificationCenter.defaultCenter().removeObserver(this);
        super.finalize();
    }
    
    /** reimplementation of D2WList stuff */
    
    protected WODisplayGroup _displayGroup;
    public boolean _hasToUpdate = false;
    protected boolean _rowFlip = false;

    public WODisplayGroup displayGroup() {
        if(_displayGroup == null) {
            _displayGroup = new WODisplayGroup();
            if (ERD2WFactory.erFactory().defaultListPageDisplayGroupDelegate() != null) {
                _displayGroup.setDelegate(ERD2WFactory.erFactory().defaultListPageDisplayGroupDelegate());
            }
        }
        return _displayGroup;
    }

    public void savedChanges(NSNotification nsnotification) {
        _hasToUpdate = true;
    }

    public boolean isEntityReadOnly() {
        boolean flag = super.isEntityReadOnly();
        flag = !ERXValueUtilities.booleanValueWithDefault(d2wContext().valueForKey("isEntityEditable"), !flag);
        flag = ERXValueUtilities.booleanValueWithDefault(d2wContext().valueForKey("readOnly"), flag);
        return flag;
    }

    public boolean isSelecting() {
        return task().equals("select");
    }

    public boolean isListEmpty() {
        return listSize() == 0;
    }

    public int listSize() {
        return displayGroup().allObjects().count();
    }

    public String alternatingColorForRow() {
        _rowFlip = !_rowFlip;
        if(_rowFlip || !alternateRowColor())
           return backgroundColorForTable();
        else
           return backgroundColorForTableDark();
    }

    public String backgroundColorForRow() {
       return !isSelecting() || object() != displayGroup().selectedObject() ? alternatingColorForRow() : "#FFFF00";
    }

    public void setBackgroundColorForRow(String value) {
        // just for KVC reasons
    }

    public EOEnterpriseObject selectedObject() {
        return (EOEnterpriseObject)displayGroup().selectedObject();
    }

    public void setSelectedObject(EOEnterpriseObject eo) {
        if(eo != null)
            displayGroup().selectObject(eo);
        else
            displayGroup().clearSelection();
    }

    public WOComponent selectObjectAction() {
        setSelectedObject(object());
        if(nextPageDelegate() != null)
            return nextPageDelegate().nextPage(this);
        else
            return null;
    }

    public WOComponent backAction() {
        return nextPageDelegate() == null ? nextPage() == null ? (WOComponent)D2W.factory().queryPageForEntityNamed(entity().name(), session()) : nextPage() : nextPageDelegate().nextPage(this);
    }

    /*** end of reimplementation */

    public String urlForCurrentState() {
        return context().directActionURLForActionNamed(d2wContext().dynamicPage(), null);
    }

    public static WOComponent printerFriendlyVersion(D2WContext d2wContext, WOSession session, EODataSource dataSource, WODisplayGroup displayGroup) {
        ListPageInterface result=(ListPageInterface)ERDirectToWeb.printerFriendlyPageForD2WContext(d2wContext,session);
        result.setDataSource(dataSource);
        WODisplayGroup dg = null;
        if(result instanceof D2WListPage) {
            dg = ((D2WListPage)result).displayGroup();
        } else if(result instanceof ERDListPageInterface) {
            dg = ((ERDListPageInterface)result).displayGroup();
        } else {
            try {
                dg = (WODisplayGroup)((WOComponent)result).valueForKey("displayGroup");
            } catch(Exception ex) {
                log.warn("Can't get displayGroup from page of class: " + result.getClass().getName());
            }
        }
        if(dg != null) {
            dg.setSortOrderings(displayGroup.sortOrderings());
            dg.setNumberOfObjectsPerBatch(displayGroup.allObjects().count());
            dg.updateDisplayedObjects();
        }
        return (WOComponent)result;
    }
    
    public WOComponent printerFriendlyVersion() {
        return ERD2WListPage.printerFriendlyVersion(d2wContext(), session(), dataSource(), displayGroup());
    }

    // This will allow d2w pages to be listed on a per configuration basis in stats collecting.
    public String descriptionForResponse(WOResponse aResponse, WOContext aContext) {
        String descriptionForResponse = (String)d2wContext().valueForKey("pageConfiguration");
        /*
        if (descriptionForResponse == null)
            log.info("Unable to find pageConfiguration in d2wContext: " + d2wContext());
         */
        return descriptionForResponse != null ? descriptionForResponse : super.descriptionForResponse(aResponse, aContext);
    }
    
    // for SmartAttributeAssignment we need object in the context
    public void setObject(EOEnterpriseObject eo) {
        super.setObject(eo);
        d2wContext().takeValueForKey(eo,"object");
    }

    private boolean _hasBeenInitialized=false;

    private Integer _batchSize = null;
    
    public int numberOfObjectsPerBatch() {
        if (_batchSize == null) {
            NSKeyValueCoding userPreferences=(NSKeyValueCoding)d2wContext().valueForKey("userPreferences");
            if (userPreferences!=null) {
                String key=ERXExtensions.userPreferencesKeyFromContext("batchSize", d2wContext());
                // batchSize prefs are expected in the form vfk batchSize.<pageConfigName>
                Number batchSizePref = (Number)userPreferences.valueForKey(key);
                if (log.isDebugEnabled()) log.debug("batchSize User Prefererence: " + batchSizePref);
                if (batchSizePref!=null) _batchSize = ERXConstant.integerForInt(batchSizePref.intValue());
            }
            if (_batchSize == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No userPrefs...  Using default values: batchSize = " + d2wContext().valueForKey("defaultBatchSize"));
                }
                _batchSize = ERXConstant.integerForString((String)d2wContext().valueForKey("defaultBatchSize"));
            }
        }
        return _batchSize.intValue();
    } 

    // this can be overridden by subclasses for which sorting has to be fixed (i.e. Grouping Lists)
    public boolean userPreferencesCanSpecifySorting() {
        return true && !"printerFriendly".equals(d2wContext().valueForKey("subTask"));
    }

    public NSArray sortOrderings() {
        NSArray sortOrderings=null;
        if (userPreferencesCanSpecifySorting()) {
            NSKeyValueCoding userPreferences=(NSKeyValueCoding)d2wContext().valueForKey("userPreferences");
            if (userPreferences!=null) {
                String key=ERXExtensions.userPreferencesKeyFromContext("sortOrdering", d2wContext());
                // sort ordering prefs are expected in the form vfk sortOrdering.<pageConfigName>
                sortOrderings=(NSArray)userPreferences.valueForKey(key);
                if (log.isDebugEnabled()) log.debug("Found sort Orderings in user prefs "+ sortOrderings);
            }
        }
        if (sortOrderings==null) {
            NSArray sortOrderingDefinition=(NSArray)d2wContext().valueForKey("defaultSortOrdering");
            if (sortOrderingDefinition!=null) {
                NSMutableArray so=new NSMutableArray();
                NSArray displayPropertyKeys = (NSArray)d2wContext().valueForKey("displayPropertyKeys");
                for (int i=0; i< sortOrderingDefinition.count();) {
                    String sortKey=(String)sortOrderingDefinition.objectAtIndex(i++);
                    String sortSelectorKey=(String)sortOrderingDefinition.objectAtIndex(i++);
                    if(displayPropertyKeys.containsObject(sortKey)) {
                        EOSortOrdering sortOrdering=new EOSortOrdering(sortKey,
                                                                       ERXUtilities.sortSelectorWithKey(sortSelectorKey));
                        so.addObject(sortOrdering);
                    } else {
                        log.warn("Sort key '"+sortKey+"' is not in display keys");
                    }
                }
                sortOrderings=so;
                if (log.isDebugEnabled()) log.debug("Found sort Orderings in rules "+ sortOrderings);
            }
        }
        return sortOrderings;
    }


    public String defaultSortKey() {
        // the default D2W mechanism is completely disabled
        return null;
    }
    
    public void takeValuesFromRequest(WORequest r, WOContext c) {
        setupPhase();
        super.takeValuesFromRequest(r, c);
    }

    public WOActionResults invokeAction(WORequest r, WOContext c) {
        setupPhase();
        return super.invokeAction(r, c);
    }

    public void appendToResponse(WOResponse r, WOContext c) {
        setupPhase();
        _rowFlip = true;
        if(_hasToUpdate) {
            displayGroup().fetch();
            _hasToUpdate = false;
        }
        super.appendToResponse(r,c);
    }
    
    public void setDataSource(EODataSource eodatasource) {
        NSArray sortOrderings=sortOrderings();
        displayGroup().setDataSource(eodatasource);
        displayGroup().setSortOrderings(sortOrderings!=null ? sortOrderings : NSArray.EmptyArray);
        displayGroup().fetch();
    }
    
    protected void setupPhase() {
        WODisplayGroup dg=displayGroup();
        if (dg!=null) {
            if (!_hasBeenInitialized) {
                log.debug("Initializing display group");
                String fetchspecName = (String)d2wContext().valueForKey("restrictingFetchSpecification");
                if(fetchspecName != null) {
                    EODataSource ds = dataSource();
                    if(ds instanceof EODatabaseDataSource)
                        ((EODatabaseDataSource)ds).setFetchSpecificationByName(fetchspecName);
                }
                NSArray sortOrderings=sortOrderings();
                dg.setSortOrderings(sortOrderings!=null ? sortOrderings : NSArray.EmptyArray);
                dg.setNumberOfObjectsPerBatch(numberOfObjectsPerBatch());
                dg.fetch();
                dg.updateDisplayedObjects();
                _hasBeenInitialized=true;
            }
            // this will have the side effect of resetting the batch # to sth correct, in case
            // the current index if out of range
            log.debug("dg.currentBatchIndex() "+dg.currentBatchIndex());
            dg.setCurrentBatchIndex(dg.currentBatchIndex());
            if (listSize() > 0)
                d2wContext().takeValueForKey(dg.allObjects().objectAtIndex(0), "object");
        }
    }

    public boolean isEntityInspectable() {
        return ERXValueUtilities.booleanValueWithDefault(d2wContext().valueForKey("isEntityInspectable"), isEntityReadOnly());
        // return isEntityReadOnly() && (isEntityInspectable!=null && isEntityInspectable.intValue()!=0);
    }

    public WOComponent deleteObjectAction() {
        String confirmDeleteConfigurationName=(String)d2wContext().valueForKey("confirmDeleteConfigurationName");
        ConfirmPageInterface nextPage;
        if(confirmDeleteConfigurationName==null) {
            log.warn("Using default delete template: ERD2WConfirmPageTemplate, set the 'confirmDeleteConfigurationName' key to something more sensible");
            nextPage = (ConfirmPageInterface)pageWithName("ERD2WConfirmPageTemplate");
        } else {
            nextPage = (ConfirmPageInterface)D2W.factory().pageForConfigurationNamed(confirmDeleteConfigurationName,session());
        }
        nextPage.setConfirmDelegate(new ERDDeletionDelegate(object(),dataSource(),context().page()));
        nextPage.setCancelDelegate(new ERDDeletionDelegate(null,null,context().page()));
        if(nextPage instanceof InspectPageInterface) {
            ((InspectPageInterface)nextPage).setObject(object());
        } else {
            nextPage.setMessage("Are you sure you want to delete the following "+d2wContext().valueForKey("displayNameForEntity")+":<br> "+object().userPresentableDescription()+ " ?");
        }
        return (WOComponent) nextPage;
    }

    public WOComponent editObjectAction() {
        EditPageInterface epi;
        String editConfigurationName=(String)d2wContext().valueForKey("editConfigurationName");
        log.debug("editConfigurationName: " + editConfigurationName);
        if(editConfigurationName != null) {
            epi = (EditPageInterface)D2W.factory().pageForConfigurationNamed(editConfigurationName,session());
        } else {
            epi = D2W.factory().editPageForEntityNamed(object().entityName(), session());            
        }
        epi.setObject(localInstanceOfObject());
        epi.setNextPage(context().page());
        return (WOComponent)epi;
    }

    public WOComponent inspectObjectAction() {
        InspectPageInterface ipi;
        String inspectConfigurationName=(String)d2wContext().valueForKey("inspectConfigurationName");
        log.debug("inspectConfigurationName: " + inspectConfigurationName);
        if(inspectConfigurationName!=null) {
            ipi=(InspectPageInterface)D2W.factory().pageForConfigurationNamed(inspectConfigurationName,session());
        } else {
            ipi = D2W.factory().inspectPageForEntityNamed(object().entityName(), session());
        }
        ipi.setObject(object());
        ipi.setNextPage(context().page());
        return (WOComponent)ipi;
    }

    
    protected EOEnterpriseObject localInstanceOfObject() {
        return ERD2WUtilities.localInstanceFromObjectWithD2WContext(object(), d2wContext());
    }
    
    public boolean showCancel() { return nextPage()!=null; }

    public boolean isSelectingNotTopLevel(){
        boolean result = false;
        if(isSelecting()&&(parent()!=null))
            result = true;
        return result;
    }

    private String _formTargetJavaScriptUrl;
    public String formTargetJavaScriptUrl() {
        if (_formTargetJavaScriptUrl==null) {
            _formTargetJavaScriptUrl= application().resourceManager().urlForResourceNamed("formTarget.js", "ERDirectToWeb", null, context().request());
        }
        return _formTargetJavaScriptUrl;
    }

    public String targetString(){
        String result = "";
        NSDictionary targetDictionary = (NSDictionary)d2wContext().valueForKey("targetDictionary");
        if(targetDictionary != null){
            StringBuffer buffer = new StringBuffer();
            buffer.append( targetDictionary.valueForKey("targetName")!=null ?
                           targetDictionary.valueForKey("targetName") : "foobar");
            buffer.append(":width=");
            buffer.append( targetDictionary.valueForKey("width")!=null ?
                           targetDictionary.valueForKey("width") : "{window.screen.width/2}");
            buffer.append(", height=");
            buffer.append( targetDictionary.valueForKey("height")!=null ?
                           targetDictionary.valueForKey("height") : "{myHeight}");
            buffer.append(",");
            buffer.append( (targetDictionary.valueForKey("scrollbars")!=null && targetDictionary.valueForKey("scrollbars")== "NO")?
                           " " : "scrollbars");
            buffer.append(", {(isResizable)?'resizable':''}, status");
            result = buffer.toString();
        }else{
            result = "foobar:width={window.screen.width/2}, height={myHeight}, scrollbars, {(isResizable)?'resizable':''}, status";
        }
        return result;
    }

    public boolean shouldShowSelectAll() {
        return listSize()>10;
    }
/*
// FIXME: This needs to be generalized.
public String pageTitle() {
    return "NetStruxr - "+d2wContext().valueForKey("displayNameForEntity")+" List";
}
*/
    public void warmUpForDisplay(){
        //default implementation does nothing
    }

    public String colorForRow(){
        String result = null;
        if(d2wContext().valueForKey("referenceRelationshipForBackgroupColor")!=null){
            String path = (String)d2wContext().valueForKey("referenceRelationshipForBackgroupColor")+".backgroundColor";
            result = (String)object().valueForKeyPath(path);
        }
        return result;
    }

    public EOEnterpriseObject referenceEO;
    private NSArray _referenceEOs;
    public NSArray referenceEOs(){
        if(_referenceEOs==null){
            String relationshipName = (String)d2wContext().valueForKey("referenceRelationshipForBackgroupColor");
            if(relationshipName!=null){
                EOEntity entity = EOModelGroup.defaultGroup().entityNamed(entityName());
                EORelationship relationship = entity.relationshipNamed(relationshipName);
                _referenceEOs = EOUtilities.objectsForEntityNamed( EOSharedEditingContext.defaultSharedEditingContext(),
                                                                   relationship.destinationEntity().name());
                _referenceEOs = ERXArrayUtilities.sortedArraySortedWithKey(_referenceEOs, "ordering", EOSortOrdering.CompareAscending);
            }
        }
        return _referenceEOs;
    }

}
