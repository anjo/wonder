//
//  ERXNavigationManager.java
//  ERExtensions
//
//  Created by Max Muller on Wed Oct 30 2002.
//
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

import java.util.Enumeration;

public class ERXNavigationManager {

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXNavigationManager.class);
    
    protected static ERXNavigationManager manager;

    public static ERXNavigationManager manager() {
        if (manager == null)
            manager = new ERXNavigationManager();
        return manager;
    }

    protected NSDictionary navigationItemsByName = NSDictionary.EmptyDictionary;
    protected ERXNavigationItem rootNavigationItem;
    protected String navigationMenuFileName;
    protected boolean hasRegistered = false;
    
    public ERXNavigationState navigationStateForSession(WOSession session) {
        ERXNavigationState state = (ERXNavigationState)session.objectForKey(navigationStateSessionKey());
        if (state == null) {
            state = new ERXNavigationState();
            session.setObjectForKey(state, navigationStateSessionKey());
        }
        return state;
    }

    public String navigationStateSessionKey() {
        return "NavigationSate";
    }

    public String navigationMenuFileName() {
        if (navigationMenuFileName == null) {
            navigationMenuFileName = System.getProperty("er.extensions.ERXNavigationManager.NavigationMenuFileName");
        }
        return navigationMenuFileName;
    }

    public void setNavigationMenuFileName(String name) {
        navigationMenuFileName = name;
    }
    
    public NSDictionary navigationItemsByName() {
        return navigationItemsByName;
    }
    
    public ERXNavigationItem rootNavigationItem() {
        return rootNavigationItem;
    }

    public ERXNavigationItem navigationItemForName(String name) {
        return (ERXNavigationItem)navigationItemsByName.objectForKey(name);
    }

    protected void setNavigationItems(NSArray items) {
        NSMutableDictionary itemsByName = new NSMutableDictionary();
        if (items != null && items.count() > 0) {
            for (Enumeration e = items.objectEnumerator(); e.hasMoreElements();) {
                ERXNavigationItem item = (ERXNavigationItem)e.nextElement();
                if (itemsByName.objectForKey(item.name()) != null) {
                    log.warn("Attempting to register multiple navigation items for the same name: " + item.name());
                } else {
                    itemsByName.setObjectForKey(item, item.name());
                    if (item.name().equals("Root"))
                        rootNavigationItem = item;
                }
            }
        }
        if (rootNavigationItem == null)
            log.warn("No root navigation item set. You need one.");
        navigationItemsByName = itemsByName.immutableClone();
    }
    
    public void configureNavigation() {
        loadNavigationMenu();
        hasRegistered = true;
    }
    
    public void loadNavigationMenu() {
        NSMutableArray navigationMenus = new NSMutableArray();
        // First load the nav_menu from application.
        NSArray appNavigationMenu = (NSArray)ERXExtensions.readPropertyListFromFileinFramework(navigationMenuFileName(),null);
        if (appNavigationMenu != null) {
            if (log.isDebugEnabled())
                log.debug("Found navigation menu in application: " + WOApplication.application().name());
            navigationMenus.addObjectsFromArray(createNavigationItemsFromDictionaries(appNavigationMenu));
            registerObserverForFramework(null);
        }
        for (Enumeration e = ERXUtilities.allFrameworkNames().objectEnumerator(); e.hasMoreElements();) {
            String frameworkName = (String)e.nextElement();
            NSArray aNavigationMenu = (NSArray)ERXExtensions.readPropertyListFromFileinFramework(navigationMenuFileName(), frameworkName);
            if (aNavigationMenu != null && aNavigationMenu.count() > 0) {
                if (log.isDebugEnabled()) log.debug("Found navigation menu in framework: " + frameworkName);
                navigationMenus.addObjectsFromArray(createNavigationItemsFromDictionaries(aNavigationMenu));
                registerObserverForFramework(frameworkName);
            }
        }
        setNavigationItems(navigationMenus);
        if (log.isDebugEnabled())
            log.debug("Navigation Menu Configured");
    }

    public void registerObserverForFramework(String frameworkName) {
        if (!WOApplication.application().isCachingEnabled() && !hasRegistered) {
            String filePath = ERXFileUtilities.pathForResourceNamed(navigationMenuFileName(),                                                                                                 frameworkName, null);
            if (log.isDebugEnabled())
                log.debug("Registering observer for filePath: " + filePath);
            ERXFileNotificationCenter.defaultCenter().addObserver(this,
                                                                  new NSSelector("reloadNavigationMenu", ERXConstant.NotificationClassArray),
                                                                  filePath);
        }
    }

    protected NSArray createNavigationItemsFromDictionaries(NSArray navItems) {
        NSMutableArray navigationItems = null;
        if (navItems != null && navItems.count() > 0) {
            navigationItems = new NSMutableArray();
            for (Enumeration e = navItems.objectEnumerator(); e.hasMoreElements();) {
                navigationItems.addObject(new ERXNavigationItem((NSDictionary)e.nextElement()));
            }
        }
        return navigationItems != null ? navigationItems : NSArray.EmptyArray;
    }
    
    public void reloadNavigationMenu(NSNotification notification) {
        log.info("Reloading Navigation Menu");
        loadNavigationMenu();
    }
}
