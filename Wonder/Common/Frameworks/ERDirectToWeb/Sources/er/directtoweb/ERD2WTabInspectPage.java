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
import java.util.*;

/**
 * Superclass for all tab and wizard pages.<br />
 * 
 */

public abstract class ERD2WTabInspectPage extends ERD2WInspectPage implements ERDTabEditPageInterface {

    public final static String WILL_SWITCH_TAB = "willSwitchTab";
    public static String IMAGE_TAB_COMPONENT_NAME = "ERXImageTabPanel";
    public static String TEXT_TAB_COMPONENT_NAME = "ERXTabPanel";

    public ERD2WTabInspectPage(WOContext c) {
        super(c);
    }

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger("er.directtoweb.templates.ERD2WTabInspectPage");
    public static final ERXLogger validationLog = ERXLogger.getERXLogger("er.directtoweb.validation.ERD2WTabInspectPage");


    public String switchTabActionName() { return isEditing() ? "switchTabAction" : null; }
    public boolean switchTabAction() {
        boolean switchTab = true;
        if (shouldSaveChangesForTab()) {
            if (validationLog.isDebugEnabled()) validationLog.debug("ERTabInspectPage calling tryToSaveChanges");
            switchTab = tryToSaveChanges(true);
        }
        if (switchTab && errorMessages.count()==0 && object().editingContext().hasChanges() && shouldNotSwitchIfHasChanges()) {
            validationFailedWithException(new NSValidation.ValidationException("You currently have changes outstanding.  Please either save or cancel your changes before navigating elsewhere."), null, "editingContextChanges");
        }
        return switchTab && errorMessages.count()==0;
    }

    // Need to set the first tab before the page renders the first time so that rules based on tabKey will fire.
    public void appendToResponse(WOResponse response, WOContext context) {
        if (currentTab() == null && tabSectionsContents() != null && tabSectionsContents().count() > 0) {
            //If firstTab is not null, then try to find the tab named firstTab
            if(tabNumber()!=null && tabNumber().intValue() <= tabSectionsContents().count()){
                setCurrentTab((ERD2WContainer)tabSectionsContents().objectAtIndex(tabNumber().intValue()));
            }
            if(currentTab()==null)
                setCurrentTab((ERD2WContainer)tabSectionsContents().objectAtIndex(0));
        }
        super.appendToResponse(response, context);
    }

    protected Integer _tabNumber;
    public Integer tabNumber(){ return _tabNumber;}
    public void setTabNumber(Integer newTabNumber){ _tabNumber  = newTabNumber;}

    public WOComponent printerFriendlyVersion() {
        WOComponent result=ERDirectToWeb.printerFriendlyPageForD2WContext(d2wContext(),session());
        ((EditPageInterface)result).setObject(object());
        return result;
    }

    public String tabScriptString() {
        int pos=tabSectionsContents().count()-1;
        return "var pos=0;\n if (document.EditForm.elements.length>"+pos+
            ") pos="+pos+";\n var elem = document.EditForm.elements["+pos+
            "];\n if (elem!=null && (elem.type == 'text' || elem.type ==  'area')) elem.focus();";
    }

    // These style rules should definately be restricted to tabKeys if needed.
    public boolean shouldNotSwitchIfHasChanges() { return ERXUtilities.booleanValue(d2wContext().valueForKey("shouldNotSwitchIfHasChanges")); }
    public boolean shouldSaveChangesForTab() { return ERXUtilities.booleanValue(d2wContext().valueForKey("shouldSaveChangesForTab")); }
    public boolean shouldShowNextPreviousButtons() { return ERXUtilities.booleanValue(d2wContext().valueForKey("shouldShowNextPreviousButtons")); }
    public boolean shouldShowPreviousButton() { return ERXUtilities.booleanValue(d2wContext().valueForKey("shouldShowPreviousButton")); }
    public boolean shouldShowNextButton() { return ERXUtilities.booleanValue(d2wContext().valueForKey("shouldShowNextButton")); }
    public boolean useSubmitImages() { return ERXUtilities.booleanValue(d2wContext().valueForKey("useSubmitImages")); }
    
    public boolean useTabImages() { return ERXUtilities.booleanValue(d2wContext().valueForKey("useTabImages")); }
    public boolean useTabSectionImages() { return ERXUtilities.booleanValue(d2wContext().valueForKey("useTabSectionImages")); }

    public WOComponent nextTab() {
        if (switchTabAction()) {
            int currentIndex = tabSectionsContents().indexOfObject(currentTab());
            if (tabSectionsContents().count() >= currentIndex + 2 && currentIndex >= 0) {
                NSNotificationCenter.defaultCenter().postNotification(WILL_SWITCH_TAB, this);
                setCurrentTab((ERD2WContainer)tabSectionsContents().objectAtIndex(currentIndex + 1));
            }
            else
                log.warn("Attempting to move to next tab when current index is: " + currentIndex + " and tab count: " +
                         tabSectionsContents().count());
        }
        return null;
    }

    public WOComponent previousTab() {
        if (switchTabAction()) {
            int currentIndex = tabSectionsContents().indexOfObject(currentTab());
            if (tabSectionsContents().count() >= currentIndex && currentIndex > 0)
                setCurrentTab((ERD2WContainer)tabSectionsContents().objectAtIndex(currentIndex - 1));
            else
                log.warn("Attempting to move to previous tab when current index is: " + currentIndex + " and tab count: " +
                         tabSectionsContents().count());
        }
        return null;
    }

    public boolean currentTabIsFirstTab() {
        return tabSectionsContents() != null && tabSectionsContents().count() > 0 && currentTab() != null ?
        tabSectionsContents().objectAtIndex(0).equals(currentTab()) : false;
    }

    public boolean currentTabIsLastTab() {
        return tabSectionsContents() != null && tabSectionsContents().count() > 0 && currentTab() != null ?
        tabSectionsContents().lastObject().equals(currentTab()) : false;
    }

    /** Used to compute the name of the "tab" component used to render the tabs;
     *  There are basically 2 choices: image tabs or text tabs.  This lets you switch
     *  between them using the d2w rule system: 'useTabImages' => true or false
     *  depending on which component you want.  false = TEXT_TAB_COMPONENT_NAME
     */
    public String tabComponentName() {
	return useTabImages() ? IMAGE_TAB_COMPONENT_NAME : TEXT_TAB_COMPONENT_NAME;
    }
}
