/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr 
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import java.util.Enumeration;
import java.util.Iterator;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import org.apache.log4j.*;
import org.apache.log4j.spi.*;
import org.apache.log4j.helpers.LogLog;

/**
 * Basic log4j Mail Message Appender<br>
 *	Used for logging log events that will eventually be emailed
 *	out. Logs events using {@link com.webobjects.appserver.WOMailDelivery WOMailDelivery}.
 *  Manditory Fields:<br>
 *      TOAddresses - comma seperated list of email addresses to send the log event
 *		message to.
 *	FromAddress - Who the message is from, if left blank then DomainName is a
 *		manditory field.
 *  Optional Fields:<br>
 *	BccAddresses - comma separated list of email address to bcc on the email
 *	CcAddresses - comma separated list of email address to cc on the email
 * 	ReplyTo - reply to address
 *	DomainName - When generating a from email address, used for the bit after the
 *		@, ie fo@bar.com, the domain name is 'bar.com'.
 *	HostName - When generating an email address from, defaults to name of the
 *		localhost.
 *	ExceptionPage - name of the exception page, is unset, 
 *	Title - Title of the email messages, if not specified the title will be a
 *		truncated version of the log message.
 *	Qualifier - qualifier that defines if the event should be logged.
 */

public class ERXMailAppender extends AppenderSkeleton {

    /** holds the from address */
    protected String fromAddress;

    /** holds the computed from address */
    protected String computedFromAddress;

    /** holds the reply to address */
    protected String replyTo;

    /** holds the to addresses */
    protected String toAddresses;

    /** holds the cc addresses */
    protected String ccAddresses;

    /** holds the bcc addresses */
    protected String bccAddresses;

    /** holds the domain */
    protected String domainName;


    /** holds the qualifier */
    protected String qualifier;

    /** holds the qualifier */
    protected EOQualifier realQualifier;

    /** holds the title */
    protected String title;

    /** holds the exception page name */
    protected String exceptionPageName;

    /** holds the host name */
    protected String hostName;

    protected String formatAsError;

    protected String titleIncludesPriorityLevel;

    protected String titleIncludesApplicationName;

    /** holds the flag if all the conditions for logging have been checked */
    protected boolean conditionsChecked = false;


    /**
     * Public constructor.
     */
    public ERXMailAppender() {
        closed = false;
    }
    
    /**
     * The mail message appender does require
     * a layout.
     * @return true.
     */
    public boolean requiresLayout() { return true; }

    /**
     * When closed set the state to closed. Closes
     * the current appender.
     */
    public void close() { closed = true; }

    /**
     * Gets the qualifier as a string.
     * @return the qualifier as string.
     */
    public String getQualifier() {
        return qualifier;
    }
    /**
     * Sets the qualifier as a string.
     * @param the qualifier as string.
     */
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
        this.realQualifier = null;
    }
    
    /**
     * Gets the qualifier.
     * @return the from address.
     */
    public EOQualifier realQualifier() {
        if(realQualifier == null) {
            if(qualifier == null) {
                return null;
            }
            realQualifier = EOQualifier.qualifierWithQualifierFormat(qualifier, null);
        }
        return realQualifier;
    }

    /**
     * Gets the from address set by the user.
     * @return the from address.
     */
    public String getFromAddress() {
        return fromAddress;
    }

    /**
     * Sets the from address.
     * @param fromAddress to use when generating emails.
     */
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }


    /**
     * Gets the reply to address set by the user.
     * @return the reply to address.
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Sets the reply to address.
     * @param reply to address to use when generating emails.
     */
    public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
    
    /**
     * Gets the from address for the appender.
     * If a from address is not specified then
     * an address is constructed in the form:
     * applicationName-hostName@domainName.
     * @return the from address.     
     */
    public String computedFromAddress() {
        if (computedFromAddress == null) {
            if (getFromAddress() != null) {
                computedFromAddress = getFromAddress();
            } else {
                computedFromAddress = WOApplication.application().name()+"-"+getHostName()+"@" + getDomainName();
            }
        }
        return computedFromAddress;
    }
    
    /**
     * Gets the to addresses as a string.
     * @return to addresses as a string
     */
    public String getToAddresses() {
        return toAddresses;
    }

    /**
     * Sets the to addresses as a string.
     * @param toAddresses comma separated array of email addresses
     */
    public void setToAddresses(String toAddresses) {
        this.toAddresses = toAddresses;
    }

    /**
     * Gets the to addresses as an array.
     * @return the to addresses as an array.
     */
    public NSArray toAddressesAsArray() {
        return toAddresses != null ? NSArray.componentsSeparatedByString(toAddresses, ",") : NSArray.EmptyArray;
    }

    /**
     * Sets the to addresses as a string.
     * @param ccAddresses comma separated array of email addresses
     */
    public void setCcAddresses(String ccAddresses) {
        this.ccAddresses = ccAddresses;
    }

    /**
     * Gets the cc addresses as a String.
     * @return cc addresses as a string
     */
    public String ccAddresses() {
        return ccAddresses;
    }

    /**
     * Gets the cc addresses as an array.
     * @return the cc addresses as an array.
     */
    public NSArray ccAddressesAsArray() {
        return ccAddresses != null ? NSArray.componentsSeparatedByString(ccAddresses, ",") : NSArray.EmptyArray;
    }
    
    /**
     * Sets the bcc addresses as a string.
     * @param bccAddresses comma separated array of email addresses
     */
    public void setBccAddresses(String bccAddresses) {
        this.bccAddresses = bccAddresses;
    }

    /**
     * Gets the bcc addresses as a String.
     * @return bcc addresses as a string
     */
    public String bccAddresses() {
        return bccAddresses;
    }

    /**
     * Gets the bcc addresses as an array.
     * @return the bcc addresses as an array.
     */
    public NSArray bccAddressesAsArray() {
        return bccAddresses != null ? NSArray.componentsSeparatedByString(bccAddresses, ",") : NSArray.EmptyArray;
    }

    /**
     * Gets the exception page name.
     * @return exception page name.
     */
    public String getExceptionPageName() { return exceptionPageName; }

    /**
        * Sets the title.
     * @param title of the mail message
     */
    public void setExceptionPageName(String exceptionPageName) { this.exceptionPageName = exceptionPageName; }
    
    /**
     * Gets the title.
     * @return title.
     */
    public String getTitle() { return title; }

    /**
     * Sets the title.
     * @param title of the mail message
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Gets the host name. If one is not specified then the host
     * name of the machine will be used.
     * @return host name to use when constructing the 'from' email
     *		address.
     */
    public String getHostName() {
        if (hostName == null)
            hostName = ERXConfigurationManager.defaultManager().hostName();
        return hostName;
    }

    /**
     * Sets the host name to use for the given appender.
     * @param hostName for the appender.
     */
    public void setHostName(String hostName) { this.hostName = hostName; }

    /**
     * Gets the domain name.
     * @return domain name.
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Sets the domain name.
     * @param domainName new domain name
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String formatAsError() {
        return formatAsError;
    }
    
    public void setFormatAsError(String value) {
        formatAsError = value;
    }

    public boolean formatAsErrorAsBoolean() {
        return ERXValueUtilities.booleanValueWithDefault(formatAsError(), false);
    }    
    
    public String titleIncludesPriorityLevel() {
        return titleIncludesPriorityLevel;
    }

    public void setTitleIncludesPriorityLevel(String value) {
        titleIncludesPriorityLevel = value;
    }

    public boolean titleIncludesPriorityLevelAsBoolean() {
        return ERXValueUtilities.booleanValueWithDefault(titleIncludesPriorityLevel(), true);
    }

    public String titleIncludesApplicationName() {
        return titleIncludesApplicationName;
    }

    public void setTitleIncludesApplicationName(String value) {
        titleIncludesApplicationName = value;
    }

    public boolean titleIncludesApplicationNameAsBoolean() {
        return ERXValueUtilities.booleanValueWithDefault(titleIncludesApplicationName(), true);
    }


    /**
     * Used to determine if the system is ready to log
     * events with MERCMailDelivery.
     * @return true if all of the conditions are satisfied
     */
    protected boolean checkConditions() {
        if (getFromAddress() == null && getDomainName() == null) {
            LogLog.error("Attempting to log an event with a null domain name and a null from address!");
        } else if (toAddressesAsArray().count() == 0) {
            LogLog.error("Attempting to log with an empty array of toAddresses");
        } else if (this.layout == null) {
            LogLog.warn("Attempting to log an event to an ERCMailMessageAppender without a layout specified.");
        } else {
            conditionsChecked = true;
        }
        return conditionsChecked;
    }

    /**
     * Entry point for logging an event.
     *
     * Reminder: the nesting of calls is:
     *
     *    doAppend()
     *      - check threshold
     *      - filter
     *      - append();
     *        - checkConditions();
     *        - subAppend();
     *
     * @param event current logging event
     */
    public void append(LoggingEvent event) {
        if (conditionsChecked || checkConditions()) {
            EOQualifier q = realQualifier();
            if(q == null || q.evaluateWithObject(event)) {
                subAppend(event);
            }
        } else {
            LogLog.warn("Unable to log event: " + event.getMessage());
        }
    }

    public String composeTitle(LoggingEvent event) {
        String title;
        if (getTitle() != null) {
            title = getTitle();
        } else {
            StringBuffer temp = new StringBuffer();
            if (titleIncludesPriorityLevelAsBoolean()) {
                temp.append(event.getLevel().toString() + ": ");
            }
            if (titleIncludesApplicationNameAsBoolean()) {
                temp.append(WOApplication.application().name() + ": ");
            }
            temp.append(event.getRenderedMessage());
            title = temp.toString();
            int ret = temp.indexOf("\n");
            if(ret > 0) {
            	title = title.substring(0, ret);
            }
        }
        return title;
    }
    
    /**
     * In case we generate a HTML page, we construct a dictionary with the entries that 
     * is later pushed into the page. This is the place to override and extend in case you want to
     * provide your own pages. Current keys are: 
     * extraInfo - NSDictionary of extra info from the application
     * errorMessage - the title for the message
     * formattedMessage - the actual message
     * exception - the throwable given by the event
     * reasonLines - NSArray of strings that give the backtrace
     * @param event logging event
     */
    public NSMutableDictionary composeExceptionPageDictionary(LoggingEvent event) {
        NSMutableDictionary result = new NSMutableDictionary();

        WOContext currentContext = (WOContext)ERXThreadStorage.valueForKey("wocontext");
        NSDictionary extraInformation = null;
        if (currentContext != null) {
            extraInformation = ERXApplication.erxApplication().extraInformationForExceptionInContext(null, currentContext);
            result.setObjectForKey(extraInformation, "extraInfo");
        }

        String title = composeTitle(event);
        if(title != null) {
            result.setObjectForKey(title, "errorMessage");
        }

        String message = this.layout.format(event);
        if(message != null) {
            result.setObjectForKey(message, "formattedMessage");
        }

        if (event.getThrowableInformation() != null && event.getThrowableInformation().getThrowable() != null) {
            result.setObjectForKey(event.getThrowableInformation().getThrowable(), "exception");
        } else {
            NSArray parts = NSArray.componentsSeparatedByString(ERXUtilities.stackTrace(), "\n");
            NSMutableArray subParts = new NSMutableArray();
            boolean first = true;
            for (Enumeration e = parts.reverseObjectEnumerator(); e.hasMoreElements();) {
                String element = (String)e.nextElement();
                if (element.indexOf("org.apache.log4j") != -1)
                    break;
                if (!first)
                    subParts.insertObjectAtIndex(element, 0);
                else
                    first = false;
            }
            result.setObjectForKey(subParts, "reasonLines");
        }
        return result;
    }
    
    /**
     * Where the actual logging event is processed and a
     * mail message is generated.
     * @param event logging event
     */
    public String composeMessage(LoggingEvent event) {
        String result;
        if(getExceptionPageName() != null && ERXValueUtilities.booleanValue(formatAsError())) {
            NSDictionary dict = composeExceptionPageDictionary(event);
            WOComponent page = (WOComponent)ERXApplication.instantiatePage(exceptionPageName);

            for(Enumeration keys = dict.keyEnumerator(); keys.hasMoreElements();) {
                String key = (String) keys.nextElement();
                Object value = dict.objectForKey(key);
                try {
                    page.takeValueForKey(value, key);
                } catch(NSKeyValueCoding.UnknownKeyException e) {
                }
            }
            try {
                result = page.generateResponse().contentString();
            } catch(Exception ex) {
                LogLog.error("Can't create response!", ex);
                result = dict.objectForKey("formattedMessage") + "\n";
                result = dict.objectForKey("extraInfo") + "\n";
                NSArray lines = (NSArray)dict.objectForKey("reasonLines"); 
                if(lines != null) {
                	result = lines.componentsJoinedByString("\n");
                }
            }
        } else {
            result = this.layout.format(event);
            result = result + "\n" + ERXUtilities.stackTrace();
        }
        return result;
    }

    /**
     * Where the actual logging event is processed and a
     * mail message is generated.
     * @param event logging event
     */
    public void subAppend(LoggingEvent event) {
        WOMailDelivery delivery = WOMailDelivery.sharedInstance();
        String title = composeTitle(event);
        String content = composeMessage(event);
        String message = delivery.composePlainTextEmail(computedFromAddress(),
                                                        toAddressesAsArray(), ccAddressesAsArray(), title, content, false);
        if(getExceptionPageName() != null && ERXValueUtilities.booleanValue(formatAsError())) {
            message = "Content-Type: text/html\n" + message;
        }
        if(bccAddresses() != null) {
            message = "BCC: "+bccAddresses()+"\n" + message;
        }
        //LogLog.error(message);
        delivery.sendEmail(message);
    }
}
