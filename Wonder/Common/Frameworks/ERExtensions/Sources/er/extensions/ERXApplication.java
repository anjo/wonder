/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
//import com.webobjects.appserver._private.ERXSubmitButton;
import java.util.*;
import org.apache.log4j.*;

/**
 *  ERXApplication is the abstract superclass of WebObjects applications
 *  built with the ER frameworks.<br/>
 *  <br/>
 *  Useful enhancements include the ability to change the deployed name of
 *  the application, support for automatic application restarting at given intervals
 *  and more context information when handling exceptions.
 */

public abstract class ERXApplication extends WOApplication {

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXApplication.class);

    /** request logging support */
    public static final ERXLogger requestHandlingLog = ERXLogger.getERXLogger("er.extensions.ERXApplication.RequestHandling");
    
    // FIXME: Should correct all references to WORequestHandler.DidHandleRequestNotification and then delete this ivar.
    public static final String WORequestHandlerDidHandleRequestNotification = WORequestHandler.DidHandleRequestNotification;

    private static boolean _wasERXApplicationMainInvoked = false;

    /** 
     * Called when the application starts up and saves the command line 
     * arguments for {@link ERXConfigurationManger}. 
     * 
     * @see com.webobjects.appserver.WOApplication#main(String[], Class)
     */
    public static void main(String argv[], Class applicationClass) {
        _wasERXApplicationMainInvoked = true;
        ERXConfigurationManager.defaultManager().setCommandLineArguments(argv);
        WOApplication.main(argv, applicationClass);
    }

    /**
     * Installs several bufixes and enhancements to WODynamicElements.
     * Sets the Context class name to "er.extensions.ERXWOContext" if
     * it is "WOContext". Patches ERXWOForm, ERXWOFileUpload, ERXWOText
     * to be used instead of WOForm, WOFileUpload, WOText.
     */
    public void installPatches() {
        ERXPatcher.installPatches();
        if(contextClassName().equals("WOContext"))
            setContextClassName("er.extensions.ERXWOContext");
        ERXPatcher.setClassForName(ERXWOForm.class, "WOForm");
        ERXPatcher.setClassForName(ERXAnyField.class, "WOAnyField");
        //ERXPatcher.setClassForName(ERXSubmitButton.class, "WOSubmitButton");

        // Fix for 3190479 URI encoding should always be UTF8
        // See http://www.w3.org/International/O-URL-code.html
        // For WO 5.1.x users, please comment this statement to compile.
        com.webobjects.appserver._private.WOURLEncoder.WO_URL_ENCODING = "UTF8";
        
        // WO 5.1 specific patches
        if (ERXProperties.webObjectsVersionAsDouble() < 5.2d) {
            // ERXWOText contains a patch for WOText to not include the value 
            // attribute (#2948062). Fixed in WO 5.2
            ERXPatcher.setClassForName(ERXWOText.class, "WOText");

            // ERXWOFileUpload returns a better warning than throwing a ClassCastException. 
            // Fixed in WO 5.2
            ERXPatcher.setClassForName(ERXWOFileUpload.class, "WOFileUpload");
        }
    }
    
    /**
     * The ERXApplication contructor.
     */
    public ERXApplication() {
        super();
        if (! ERXConfigurationManager.defaultManager().isDeployedAsServlet()  &&  
            ! _wasERXApplicationMainInvoked) {
            _displayMainMethodWarning();
        }        
        installPatches();

        ERXModelGroup.setDefaultGroup(ERXModelGroup.modelGroupForLoadedBundles());
        registerRequestHandler(new ERXDirectActionRequestHandler(), directActionRequestHandlerKey());

        Long timestampLag=Long.getLong("EOEditingContextDefaultFetchTimestampLag");
        if (timestampLag!=null)
            EOEditingContext.setDefaultFetchTimestampLag(timestampLag.longValue());

        String defaultMessageEncoding = System.getProperty("er.extensions.ERXApplication.DefaultMessageEncoding");
        if (defaultMessageEncoding != null) {
            log.debug("Setting WOMessage default encoding to \"" + defaultMessageEncoding + "\"");
            WOMessage.setDefaultEncoding(defaultMessageEncoding);
        }
        
        NSNotificationCenter.defaultCenter().addObserver(
                        this,
                        new NSSelector("finishInitialization",  ERXConstant.NotificationClassArray),
                        WOApplication.ApplicationWillFinishLaunchingNotification,
                        null);

        NSNotificationCenter.defaultCenter().addObserver(this,
                                                         new NSSelector("didFinishLaunching",
                                                                        ERXConstant.NotificationClassArray),
                                                         WOApplication.ApplicationDidFinishLaunchingNotification,
                                                         null);        
    }

    /**
     * Notification method called when the application posts
     * the notification {@link WOApplicaiton#ApplicationWillFinishLaunchingNotification}. 
     * This method calls subclasse's {@link #finishInitialization} method. 
     * 
     * @param n notification that is posted after the WOApplication
     *      has been constructed, but before the application is
     *      ready for accepting requests.
     */
    public final void finishInitialization(NSNotification n) {
        finishInitialization();
    }

    /**
     * Notification method called when the application posts
     * the notification {@link WOApplicaiton#ApplicationDidFinishLaunchingNotification}.
     * This method calls subclasse's {@link #didFinishLaunching} method.
     *
     * @param n notification that is posted after the WOApplication
     *      has finished launching and is ready for accepting requests.
     */    
    public final void didFinishLaunching(NSNotification n) {
        didFinishLaunching();
    }
    
    /**
     * Called when the application posts {@link WOApplicaiton#ApplicationWillFinishLaunchingNotification}.  
     * Override this to perform application initialization. (optional)
     */
    public void finishInitialization() {
        // empty
    }

    /**
     * Called when the application posts {@link WOApplicaiton#ApplicationDidFinishLaunchingNotification}.
     * Override this to perform application specific tasks after the application has been initialized.
     * THis is a good spot to perform batch application tasks.
     */
    public void didFinishLaunching() {
        // empty
    }    
    
    /**
     * The ERXApplication singleton.
     * @return returns the <code>WOApplication.application()</code> cast as an ERXApplication
     */
    public static ERXApplication erxApplication() { return (ERXApplication)WOApplication.application(); }
    
    /**
     *  Adds support for automatic application cycling. Applications can be configured
     *  to cycle in two ways:<br/>
     *  <br/>
     *  The first way is by setting the System property <b>ERTimeToLive</b> to the number
     *  of seconds that the application should be up before terminating. Note that when
     *  the application's time to live is up it will quit calling the method <code>killInstance</code>.<br/>
     *  <br/>
     *  The second way is by setting the System property <b>ERTimeToDie</b> to the number
     *  of seconds that the application should be up before starting to refuse new sessions.
     *  In this case when the application starts to refuse new sessions it will also register
     *  a kill timer that will terminate the application between 30 minutes and 1:30 minutes.<br/>
     */
    public void run() {
        int timeToLive=ERXProperties.intForKey("ERTimeToLive");
        if (timeToLive > 0) {
            log.info("Instance will live "+timeToLive+" seconds.");
            NSLog.out.appendln("Instance will live "+timeToLive+" seconds.");
            NSTimestamp now=new NSTimestamp();
            NSTimestamp exitDate=(new NSTimestamp()).timestampByAddingGregorianUnits(0, 0, 0, 0, 0, timeToLive);
            WOTimer t=new WOTimer(exitDate, 0, this, "killInstance", null, null, false);
            t.schedule();
        }
        int timeToDie=ERXProperties.intForKey("ERTimeToDie");
        if (timeToDie > 0) {
            log.info("Instance will not live past "+timeToDie+":00.");
            NSLog.out.appendln("Instance will not live past "+timeToDie+":00.");
            NSTimestamp now=new NSTimestamp();
            int s=(timeToDie-ERXTimestampUtility.hourOfDay(now))*3600-ERXTimestampUtility.minuteOfHour(now)*60;
            if (s<0) s+=24*3600; // how many seconds to the deadline

            // deliberately randomize this so that not all instances restart at the same time
            // adding up to 1 hour
            s+=(new Random()).nextFloat()*3600;

            NSTimestamp stopDate=now.timestampByAddingGregorianUnits(0, 0, 0, 0, 0, s);
            WOTimer t=new WOTimer(stopDate, 0, this, "startRefusingSessions", null, null, false);
            t.schedule();        }
        super.run();
    }

    /**
     * Creates the request object for this loop.
     * Overridden to use an {@link ERXRequest} object that fixes a bug
     * with localization.
     * @param aMethod the HTTP method object used to send the request, must be one of "GET", "POST" or "HEAD"
     * @param aURL - must be non-null
     * @param anHTTPVersion - the version of HTTP used
     * @param someHeaders - dictionary whose String keys correspond to header names
     * @param aContent - the HTML content of the receiver
     * @param someInfo - an NSDictionary that can contain any kind of information related to the current response.
     * @returns a new WORequest object
     */
    public WORequest createRequest(String aMethod, String aURL,
                                   String anHTTPVersion,
                                   NSDictionary someHeaders, NSData aContent,
                                   NSDictionary someInfo) {
        WORequest worequest = new ERXRequest(aMethod, aURL, anHTTPVersion, someHeaders, aContent, someInfo);
        return worequest;
    }

    /** Used to instanciate a WOComponent when no context is available,
        * typically ouside of a session
        *
        * @param pageName - The name of the WOComponent that must be instanciated.
        */
    public static WOComponent instantiatePage (String pageName) {
        // Create a context from a fake request
        WORequest fakeRequest = new ERXRequest("GET", "", "HTTP/1.1", null, null, null);
        WOContext context = application().createContextForRequest( fakeRequest );
        return application().pageWithName(pageName, context);
    }

    /**
     *  Stops the application from handling any new requests. Will still handle
     *  requests from existing sessions. Also registers a kill timer that will
     *  terminate the application thirty minutes from the time this method is
     *  called
     */
    public void startRefusingSessions() {
        log.info("Refusing new sessions");
        NSLog.out.appendln("Refusing new sessions");
        refuseNewSessions(true);
        log.info("Registering kill timer");
        NSTimestamp now=new NSTimestamp();
        NSTimestamp exitDate=(new NSTimestamp()).timestampByAddingGregorianUnits(0, 0, 0, 0, 0, 1800);
        WOTimer t=new WOTimer(exitDate, 0, this, "killInstance", null, null, false);
        t.schedule();
    }

    /**
     *  Killing the instance will log a 'Forcing exit' message and then call <code>System.exit(1)</code>
     */
    public void killInstance() {
        log.info("Forcing exit");
        NSLog.out.appendln("Forcing exit");
        System.exit(1);
    }
    /** cached name suffix */
    private String _nameSuffix;
    /** has the name suffix been cached? */
    private boolean _nameSuffixLookedUp=false;
    /**
     *  The name suffix is appended to the current name of the application. This adds the ability to
     *  add a useful suffix to differentuate between different sets of applications on the same machine.<br/>
     *  <br/>
     *  The name suffix is set via the System property <b>ERApplicationNameSuffix</b>.<br/>
     *  <br/>
     *  For example if the name of an application is Buyer and you want to have a training instance
     *  appear with the name BuyerTraining then you would set the ERApplicationNameSuffix to Training.<br/>
     *  <br/>
     *  @return the System property <b>ERApplicationNameSuffix</b> or <code>null</code>
     */
    public String nameSuffix() {
        if (!_nameSuffixLookedUp) {
            _nameSuffix=System.getProperty("ERApplicationNameSuffix");
            _nameSuffix=_nameSuffix==null ? "" : _nameSuffix;
            _nameSuffixLookedUp=true;
        }
        return _nameSuffix;
    }
    /** cached computed name */
    private String _userDefaultName;
    /**
     *  Adds the ability to completely change the applications name by setting the System property
     *  <b>ERApplicationName</b>. Will also append the <code>nameSuffix</code> if one is set.<br/>
     *  <br/>
     *  @return the computed name of the application.
     */
    public String name() {
        if (_userDefaultName==null) {
            _userDefaultName=System.getProperty("ERApplicationName");
            if (_userDefaultName==null) _userDefaultName=super.name();
            if (_userDefaultName!=null) {
                String suffix=nameSuffix();
                if (suffix!=null && suffix.length()>0)
                    _userDefaultName+=suffix;
            }
        }
        return _userDefaultName;
    }

    /**
     *  This method returns {@link WOApplication}'s <code>name</code> method.<br/>
     *  @return the name of the application executable. 
     */
    public String rawName() { return super.name(); }

    /**
     *  Puts together a dictionary with a bunch of useful information relative to the current state when the exception
     *  occurred. Potentially added information:<br/>
     * <ol>
     * <li>the current page name</li>
     * <li>the current component</li>
     * <li>the complete hierarchy of nested components</li>
     * <li>the requested uri</li>
     * <li>the D2W page configuration</li>
     * <li>the previous page list (from the WOStatisticsStore)</li>
     * </ol>
     * <br/>
     * @return dictionary containing extra information for the current context.
     */
    public NSMutableDictionary extraInformationForExceptionInContext(Exception e, WOContext context) {
        NSMutableDictionary extraInfo=new NSMutableDictionary();
        if (context!=null && context.page()!=null) {
            extraInfo.setObjectForKey(context.page().name(), "CurrentPage");
            if (context.component() != null) {
                extraInfo.setObjectForKey(context.component().name(), "CurrentComponent");
                if (context.component().parent() != null) {
                    WOComponent component = context.component();
                    NSMutableArray hierarchy = new NSMutableArray(component.name());
                    while (component.parent() != null) {
                        component = component.parent();
                        hierarchy.addObject(component.name());
                    }
                    extraInfo.setObjectForKey(hierarchy, "CurrentComponentHierarchy");
                }
            }
            extraInfo.setObjectForKey(context.request().uri(), "uri");
            /* Nice information to have if you are a d2w application,
                however ERExtensions does not link D2W.
                if (context.page() instanceof D2WComponent) {
                    D2WContext c=((D2WComponent)context.page()).d2wContext();
                    String pageConfiguration=(String)c.valueForKey("pageConfiguration");
                    if (pageConfiguration!=null)
                        extraInfo.setObjectForKey(pageConfiguration, "D2W-PageConfiguration");
                }
            */
            if (context.hasSession())
                if (context.session().statistics() != null)
                    extraInfo.setObjectForKey(context.session().statistics(), "PreviousPageList");
        }
        return extraInfo;
    }


    /**
     * Reports an exception. This method only logs the error and could be
     * overriden to return a valid error page.
     * @param exception to be reported
     * @param extraInfo dictionary of extra information about what was
     *		happening when the exception was thrown.
     * @return a valid response to display or null. In that case the superclasses
     *         {@link #handleException(Exception, WOContext)} is called
     */
    public WOResponse reportException(Throwable exception, NSDictionary extraInfo) {
        log.error("Exception caught, " + exception.getMessage() + " extra info: "
                  + extraInfo, exception instanceof NSForwardException ?
                  ((NSForwardException) exception).originalException() : exception);
        return null;
    }


    /**
     * Workaround for WO 5.2 DirectAction lock-ups.
     * As the super-implementation is empty, it is fairly safe to override here to call
     * the normal exception handling earlier than usual.
     * @see com.webobjects.appserver.WOApplication#handleActionRequestError(WORequest, Exception, String, WORequestHandler, String, String, Class, WOAction)
     */
    // NOTE: if you use WO 5.1, comment out this method, otherwise it won't compile. 
    public WOResponse handleActionRequestError(WORequest aRequest, Exception exception, String reason, WORequestHandler aHandler, String actionClassName, String actionName, Class actionClass, WOAction actionInstance) {
        return handleException(exception, actionInstance.context());
    }
    
    /**
     * Logs extra information about the current state.
     * @param exception to be handled
     * @param context current context
     * @return the WOResponse of the generated exception page.
     */
    public WOResponse handleException(Exception exception, WOContext context) {
        // We first want to test if we ran out of memory. If so we need to quite ASAP.
        handlePotentiallyFatalException(exception);

        // Not a fatal exception, business as usual.
        NSDictionary extraInfo = extraInformationForExceptionInContext(exception, context);
        WOResponse response = reportException(exception, extraInfo);
        if(response == null)
            response = super.handleException(exception, context);
        return response;
    }

    /**
     * Standard exception page. Also logs error to standard out.
     * @param exception to be handled
     * @param context current context
     * @return the WOResponse of the generic exception page.
     */
    public WOResponse genericHandleException(Exception exception, WOContext context) {
        return super.handleException(exception, context);
    }
    
    /**
     * Handles the potentially fatal OutOfMemoryError by quiting the
     * application ASAP. Broken out into a separate method to make
     * custom error handling easier, ie generating your own error
     * pages in production, etc.
     * @param exception to check if it is a fatal exception.
     */
    public void handlePotentiallyFatalException(Exception exception) {
        if( exception instanceof NSForwardException ) {
            Throwable t = ((NSForwardException) exception).originalException();
            if( t instanceof OutOfMemoryError ) {
                // We first log just in case the log4j call puts us in a bad state.
                NSLog.err.appendln("Ran out of memory, killing this instance");
                log.error("Ran out of memory, killing this instance");
                Runtime.getRuntime().exit( 1 );
            }
        }
    }

    /**
     * Simple hook to null out the thread local storage so
     * we don't hold a reference to the context object.
     * @param request object
     * @return response
     */
    public WOResponse dispatchRequest(WORequest request) {
        WOResponse response = null;
        if (requestHandlingLog.isDebugEnabled()) {
            requestHandlingLog.debug("Dispatching request: " + request);
        }
        try {
            response = super.dispatchRequest(request);
        } finally {
            // We always want to get rid of the wocontext key.
            ERXThreadStorage.removeValueForKey("wocontext");
        }
        if (requestHandlingLog.isDebugEnabled()) {

            requestHandlingLog.debug("Returning, encoding: " + response.contentEncoding() + " response: " + response);
        }        
        return response;
    }

    /**
     * When a context is created we push it into thread local storage.
     * This handles the case for direct actions.
     * @param request the request
     * @return the newly created context
     */
    public WOContext createContextForRequest(WORequest request) {
        WOContext context = super.createContextForRequest(request);
        // We only want to push in the context the first time it is
        // created, ie we don't want to loose the current context
        // when we create a context for an error page.
        if (ERXThreadStorage.valueForKey("wocontext") == null) {
            ERXThreadStorage.takeValueForKey(context, "wocontext");
        }
        return context;
    }

    
    /** 
     * Logs the warning message if the main method was not called 
     * during the startup.
     */
    private void _displayMainMethodWarning() {
        log.warn("\n\nIt seems that your applicaiton class " 
            + application().getClass().getName() + " did not call "   
            + ERXApplication.class.getName()
            + ".main(argv[], applicationClass) method. "
            + "Please modify your Application.java as the followings so that "
            + ERXConfigurationManager.class.getName() + " can provide its "
            + "rapid turnaround feature completely. \n\n"
            + "Please change Application.java like this: \n" 
            + "public static void main(String argv[]) { \n"
            + "    ERXApplication.main(argv, Application.class); \n"
            + "}\n\n");
    }
}
