//
// ERXNavigationMenuItem.java: Class file for WO Component 'ERXNavigationMenuItem'
// Project ERExtensions
//
// Created by max on Wed Oct 30 2002
//
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.util.Enumeration;

/** Please read "Documentation/Navigation.html" to fnd out how to use the navigation components.*/

public class ERXNavigationMenuItem extends ERXStatelessComponent {

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXNavigationMenuItem.class);
    
    protected ERXNavigationItem _navigationItem;
    protected ERXNavigationState _navigationState;

    protected boolean _linkDirectlyToDirectActions = true;
    
    protected int _level=-1;
    protected Boolean _isDisabled;
    protected Boolean _meetsDisplayConditions;
    protected Boolean _isSelected;
    protected Boolean _hasActivity;
    protected WOComponent _redirect;
    
    public ERXNavigationMenuItem(WOContext context) {
        super(context);
    }

    public void reset() {
        _navigationItem = null;
        _navigationState = null;
        _meetsDisplayConditions=null;
        _level=-1;
        _hasActivity=null;
        _isDisabled=null;
        _isSelected=null;
        super.reset();
    }

    public ERXNavigationState navigationState() {
        if (_navigationState == null)
            _navigationState = ERXNavigationManager.manager().navigationStateForSession(session());
        return _navigationState;
    }

    /** AK This is only an experiment: when calling up a DA, we use a component action and redirect to the actual DA  */
    public WOComponent directActionRedirect() {
        WOComponent page = pageWithName("WORedirect");
        String url = context().directActionURLForActionNamed(navigationItem().directActionName(), navigationItem().queryBindings());
        ((WORedirect)page).setUrl(url);
        
        return page;
    }
    
    public String contextComponentActionURL() {
        // If the navigation should be disabled return null
        if (navigationState().isDisabled() || meetsDisplayConditions() == false) {
            return null;
        }

        // If the user specified an action or pageName, return the source URL
        if ((navigationItem().action() != null) || (navigationItem().pageName() != null)) {
            // Return the URL to the action or page placed in the context by invokeAction
            return context().componentActionURL();
        }
        if (navigationItem().directActionName() != null) {
            if(_linkDirectlyToDirectActions) {
                return context().directActionURLForActionNamed(navigationItem().directActionName(), navigationItem().queryBindings());
            } else {
                return context().componentActionURL();
            }
        }

        // If the user specified some javascript, put that into the HREF and return it
        if (canGetValueForBinding("javascriptFunction")) {

            // Make sure there are no extra quotations marks - replace them with apostrophes
            String theFunction = (String)valueForBinding("javascriptFunction");
            return ERXStringUtilities.replaceStringByStringInString("\"", "'", theFunction);
        }

        return null;
    }

    public WOComponent menuItemSelected() {
        WOComponent anActionResult = null;

        if ((navigationItem().action() != null) && (navigationItem().action() != "")) {
            anActionResult = (WOComponent)valueForKeyPath(navigationItem().action());
        } else if ((navigationItem().pageName() != null) && (navigationItem().pageName() != "")) {
            anActionResult = (WOComponent)(pageWithName(navigationItem().pageName()));
        } else if ((navigationItem().directActionName() != null) && (navigationItem().directActionName() != "")) {
            // FIXME: Need to support directAction classes
            if(_linkDirectlyToDirectActions) {
                ERXDirectAction da = new ERXDirectAction(context().request());
                anActionResult = (WOComponent)(da.performActionNamed(navigationItem().directActionName()));
            } else {
                anActionResult = (WOComponent)valueForKeyPath("directActionRedirect");
            }
        }
        return anActionResult;
    }

    /**
     * Decides whether the item gets displayed at all.
     * This is done by evaluating the boolean value of a "conditions" array in the definition file.
     * eg: conditions = ("session.user.canEditThisStuff", "session.user.isEditor")
     * will display the item only if the user can edit this stuff *and* is an editor. 
     */
    public boolean meetsDisplayConditions() {
        if (_meetsDisplayConditions == null) {
            _meetsDisplayConditions = Boolean.TRUE;
            if (navigationItem().conditions().count() != 0) {
                Enumeration enumerator = navigationItem().conditions().objectEnumerator();
                while (enumerator.hasMoreElements()) {
                    String anObject = (String)enumerator.nextElement();
                    Object value = valueForKeyPath(anObject);
                    _meetsDisplayConditions = ERXValueUtilities.booleanValue(value) ? Boolean.TRUE : Boolean.FALSE;
                    if (log.isDebugEnabled()) {
                        log.debug(navigationItem().name() + " testing display condition: "+ anObject + " --> " + value  + ":" + _meetsDisplayConditions);
                    }
                    if (!_meetsDisplayConditions.booleanValue()) break;
                }
            }
            if(_meetsDisplayConditions.booleanValue() && navigationItem().qualifier() != null) {
                _meetsDisplayConditions = navigationItem().qualifier().evaluateWithObject(this) ? Boolean.TRUE : Boolean.FALSE;
            }
        }
        return _meetsDisplayConditions.booleanValue();
    }

    public ERXNavigationItem navigationItem() {
        if (_navigationItem==null) {
            _navigationItem = (ERXNavigationItem)valueForBinding("navigationItem");
        }
        return _navigationItem;
    }

    public boolean isDisabled() {
        if (_isDisabled == null) {
            _isDisabled=navigationState().isDisabled() || !meetsDisplayConditions() ? Boolean.TRUE : Boolean.FALSE;
        }
        return _isDisabled.booleanValue();
    }

    public boolean isSelected() {
        if (_isSelected == null) {
            NSArray navigationState = navigationState().state();
            _isSelected=!isDisabled() && navigationState != null && navigationState.containsObject(navigationItem().name()) ? Boolean.TRUE : Boolean.FALSE;

        }
        return _isSelected.booleanValue();
    }

    public int level() {
        if (_level==-1) {
            Integer l=(Integer)valueForBinding("level");
            _level=l!=null ? l.intValue() : 0;
        }
        return _level;
    }

    public String linkClass() {
        if(level() == 0) {
            return "";
        }
        return "Nav" + level() + (isSelected() ? "Selected" : (isDisabled() ? "Disabled" : ""));
    }

    private final static String[] COLOR=new String[] { "", "#EEEEEE", "#111111", "#EEEEEE", "#111111" };
    private final static String[] TD_BGCOLOR=new String[] { "", "#003366", "#d0d0d0", "#ff6600", "#ff6600" };
    private final static String[] DISABLED_TD_BGCOLOR=new String[] { "", "#003366", "#EFEFEF", "#ff9966", "#ff9966" };

    public String tdColor() {
        return !isDisabled()  ? TD_BGCOLOR[level()+(isSelected()? 1 : 0)] : DISABLED_TD_BGCOLOR[level()];
    }

    public Object resolveValue(String key) {
        return key!=null && key.startsWith("^") ? valueForKeyPath(key.substring(1)) : key;
    }

    public boolean hasActivity() {
        if (_hasActivity == null)
            _hasActivity = ERXValueUtilities.booleanValue(resolveValue(navigationItem().hasActivity())) ?
                Boolean.TRUE : Boolean.FALSE;
        return _hasActivity.booleanValue();
    }

    public boolean hasActivityAndIsEnabled(){
        return hasActivity() && !isDisabled();
    }

    public String displayName() { return (String)resolveValue(navigationItem().displayName()); }
    
}
