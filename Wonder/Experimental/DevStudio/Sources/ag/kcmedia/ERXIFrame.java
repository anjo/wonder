//
// Sources/ag/kcmedia/ERXIFrame.java: Class file for WO Component 'ERXIFrame'
// Project DevStudio
//
// Created by ak on Thu Jul 25 2002
//
package ag.kcmedia;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import er.extensions.*;

public class ERXIFrame extends WOComponent {
    static final ERXLogger log = ERXLogger.getERXLogger(ERXIFrame.class,"components");

    public ERXIFrame(WOContext context) {
        super(context);
    }

    //public boolean isStateless() {return true;}
    public boolean synchronizesVariablesWithBindings() {return false;}
    
    public String iframe = "iframe";
    
    boolean isInvoking = false;
    public void awake() {
        super.awake();
        isInvoking = false;
        log.info("awake");
    }
    
    public String srcUrl()  {
        isInvoking = false;
        log.info("srcUrl");
        if (hasBinding("src")) {
            return (String)valueForBinding("src");
        }
        if (hasBinding("pageName") || hasBinding("value") || hasBinding("useContent")) {
            return context().componentActionURL();
        }
        return "ERROR_URL_NOT_FOUND";
    }

    public WOElement frameContent()  {
        isInvoking = false;
        log.info("frameContent");
        WOElement aContentElement = null;
        if (hasBinding("pageName")) {
            String  aPageName = (String)valueForBinding("pageName");
            aContentElement = pageWithName(aPageName);
        } else if(hasBinding("value")) {
            aContentElement = (WOElement)valueForBinding("value");
        } else if(hasBinding("useContent")) {
            isInvoking = true;
            aContentElement = this;
        }
        return aContentElement;
    }

    public boolean isInvoking() {
        return isInvoking;
    }
}
