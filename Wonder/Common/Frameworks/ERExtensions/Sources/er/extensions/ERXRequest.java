package er.extensions;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.appserver._private.*;
import com.webobjects.eocontrol.*;

/** Subclass of WORequest that fixes several Bugs.
 * The ID's are #2924761 and #2961017. It can also be extended to handle
 * #2957558 ("de-at" is converted to "German" instead of "German_Austria").
 * The request is created via ${link ERXApplication$createRequest()}.
 */
public  class ERXRequest extends WORequest {

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXRequest.class);

    protected static Boolean isBrowserFormValueEncodingOverrideEnabled;
    public boolean isBrowserFormValueEncodingOverrideEnabled() {
        if (isBrowserFormValueEncodingOverrideEnabled == null) {
            isBrowserFormValueEncodingOverrideEnabled = ERXProperties.booleanForKeyWithDefault("er.extensions.ERXRequest.BrowserFormValueEncodingOverrideEnabled", false) ? Boolean.TRUE : Boolean.FALSE;
        }
        return isBrowserFormValueEncodingOverrideEnabled.booleanValue();
    }
    
    /** Simply call superclass constructor */
    public ERXRequest(String string, String string0, String string1,
                      NSDictionary nsdictionary, NSData nsdata,
                      NSDictionary nsdictionary2) {
        super(string, string0, string1, nsdictionary,
              nsdata, nsdictionary2);
        if (isBrowserFormValueEncodingOverrideEnabled() && browser().formValueEncoding() != null) {
            setDefaultFormValueEncoding(browser().formValueEncoding());
        }
    }

    /** NSArray to keep browserLanguages in. */
    protected  NSArray _browserLanguages;

    /** holds a reference to the browser object */
    protected ERXBrowser _browser;
    
    /** Returns a cooked version of the languages the user has set in his Browser.
     * Adds "Nonlocalized" and {link ERXLocalizer$defaultLanguage()} if not
     * already present.
     * @returns cooked version of user's languages
     */
    public NSArray browserLanguages() {
        if (_browserLanguages == null) {
            NSArray fixedLanguages = null;
            String string = this.headerForKey("accept-language");
            if (string != null) {
                NSArray rawLanguages
                = NSArray.componentsSeparatedByString(string, ",");
                fixedLanguages = fixAbbreviationArray(rawLanguages);
            }
            NSMutableArray languageKeys =
                WOProperties.TheLanguageDictionary.objectsForKeys(fixedLanguages,
                                                                  null).mutableClone();
            languageKeys.addObject("Nonlocalized");
            if(!languageKeys.containsObject(ERXLocalizer.defaultLanguage()))
                languageKeys.addObject(ERXLocalizer.defaultLanguage());
            _browserLanguages = languageKeys.immutableClone();
        }
        return _browserLanguages;
    }

    /**
     * Gets the ERXBrowser associated with the user-agent of
     * the request.
     * @return browser object for the request
     */
    public ERXBrowser browser() {
        if (_browser == null) {
            ERXBrowserFactory browserFactory = ERXBrowserFactory.factory();
            _browser = browserFactory.browserMatchingRequest(this);
            browserFactory.retainBrowser(_browser);            
        }
        return _browser;
    }

    /**
     * Cleaning up retian count on the browser.
     */
    public void finalize() throws Throwable {
        if (_browser != null)
            ERXBrowserFactory.factory().releaseBrowser(_browser);
        super.finalize();
    }
    
    private static class _LanguageComparator extends NSComparator {
        
        private static float q(String languageString) {
            float result=0f;
            if (languageString!=null) {
                int semicolumn=languageString.indexOf(';');
                if (semicolumn!=-1 &&
                    languageString.length()>semicolumn+2) {
                    result=Float.parseFloat(languageString.substring(semicolumn+3));
                } else
                    result=1.0f;
            }
            return result;
        }
        public int compare(Object o1, Object o2) {
            float f1=q((String)o1);
            float f2=q((String)o2);
            return f1<f2 ? OrderedDescending : ( f1==f2 ? OrderedSame : OrderedAscending ); // we want DESCENDING SORT!!
        }
        
    }

    
    /** Translates ("de", "en-us;q=0.33", "en", "en-gb;q=0.66") to ("de", "en_gb", "en-us", "en").
     * @param languages NSArray of Strings
        * @returns sorted NSArray of normalized Strings
        */
    private final static NSComparator COMPARE_Qs=new _LanguageComparator();
    protected NSArray fixAbbreviationArray(NSArray languages) {
        try {
            languages=languages.sortedArrayUsingComparator(COMPARE_Qs);
        } catch (NSComparator.ComparisonException e) {
            log.error("Couldn't sort language array "+languages+": "+e);
        } catch (NumberFormatException e2) {
            log.error("Couldn't sort language array "+languages+": "+e2);
        }
        NSMutableArray nsmutablearray = new NSMutableArray(languages.count());
        int cnt = languages.count();
        for (int i = cnt - 1; i >= 0; i--) {
            String string = (String) languages.objectAtIndex(i);
            int offset;
            string = string.trim();
            offset = string.indexOf(';');
            if (offset > 0)
                string = string.substring(0, offset);
            offset = string.indexOf('-');
            if (offset > 0) {
                String langPrefix = string.substring(0, 2);  //  "en" part of "en-us"
                if (!nsmutablearray.containsObject(langPrefix)) 
                    nsmutablearray.insertObjectAtIndex(langPrefix, 0);
                // converts "en-us" into "en_us";
                StringBuffer cooked = new StringBuffer(string.length());
                cooked.append(langPrefix)
                    .append("_")
                    .append(string.substring(offset+1, offset+3));
                string = cooked.toString();
            }
            nsmutablearray.insertObjectAtIndex(string, 0);
        }
        return nsmutablearray;
    }

    public NSDictionary cookieValues() {
        try {
            return super.cookieValues();
        } catch (Throwable t) {
            log.warn(t + ":" + this);
            log.warn(t);
            return NSDictionary.EmptyDictionary;
        }
    }    

    public boolean isSessionIDInRequest() {
        ERXApplication app = (ERXApplication)WOApplication.application();
        
        if (app.isStreamingRequestHandlerKey(requestHandlerKey())) {
            return false;
        } else {
            return super.isSessionIDInRequest();
        }
    }

    protected String _getSessionIDFromValuesOrCookie(boolean inCookiesFirst) {
        ERXApplication app = (ERXApplication)WOApplication.application();

        boolean wis = WOApplication.application().streamActionRequestHandlerKey().equals(requestHandlerKey());
        boolean alternateStreaming = app.isStreamingRequestHandlerKey(requestHandlerKey());
        boolean streaming = wis || alternateStreaming;
        
        String sessionID = null;
        if(inCookiesFirst) {
            sessionID = cookieValueForKey("wosid");
            if(sessionID == null && !streaming) {
                sessionID = stringFormValueForKey("wosid");
            }
        } else {
            if(!streaming) {
                sessionID = stringFormValueForKey("wosid");
            }
            if(sessionID == null) {
                sessionID = cookieValueForKey("wosid");
            }
        }
        return sessionID;
    }
}
