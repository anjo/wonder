/*
  $Id$
 
  ERMailDeliveryHTML.java - Camille Troillard - tuscland@mac.com
*/

package er.javamail;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

import java.util.*;
import java.io.*;
import javax.mail.*;
import javax.activation.*;
import javax.mail.internet.*;

/** This ERMailDelivery subclass is specifically crafted for HTML messages
    using a WOComponent as redering device.
    @author Camille Troillard <tuscland@mac.com> */
public class ERMailDeliveryHTML extends ERMailDeliveryComponentBased
{
    /** Holds the HTML content */
    protected String _htmlContent;
    
    /** Plain text preamble set in top of HTML source so that non-HTML compliant mail readers
        can at least display this message. */
    private String _hiddenPlainTextContent = "";

    /** True if this the current message has a plain text preamble. */
    private boolean _hasHiddenPlainTextContent = false;

    /** Sets the Plain text preamble that will be displayed set in top of HTML source.
        Non-HTML compliant mail readers can at least display this message. */
    public void setHiddenPlainTextContent (String content) {
        _hiddenPlainTextContent = content + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n";
        _hasHiddenPlainTextContent = true;
    }

    /**
     * Sets the HTML content. Note that if you set the
     * WOComponent to be used when rendering the message
     * this content will be ignored.
     * @param content HTML content to be used
     */
    public void setHTMLContent (String content) {
        _htmlContent = content;
    }

    /** Creates a new mail instance within ERMailDelivery.  Sets hasHiddenPlainTextContent to false. */
    public void newMail () {
        super.newMail ();
        _hasHiddenPlainTextContent = false;
        _htmlContent = null;
    }

    protected String htmlContent () {
	String htmlContent = null;
	if (this.component () != null)
	    htmlContent = this.componentContentString ();
        else
	    htmlContent = _htmlContent;

	return htmlContent;
    }

    /** Pre-processes the mail before it gets sent.
        @see ERMailDelivery#prepareMail */
    protected DataHandler prepareMail () throws MessagingException {
        MimeMultipart multipart = null;
        MimeBodyPart textPart = null;
        MimeBodyPart htmlPart = null;

	this.mimeMessage ().setSentDate (new Date ());
	multipart = new MimeMultipart ("alternative");
		
	// set the plain text part
	if (_hasHiddenPlainTextContent) {
	    textPart = new MimeBodyPart ();
	    textPart.setText (_hiddenPlainTextContent);
	    multipart.addBodyPart (textPart);
	}

	// create and fill the html message part
	htmlPart = new MimeBodyPart ();

	// Set the content of the html part
	htmlPart.setContent (this.htmlContent (), "text/html");
	multipart.addBodyPart (htmlPart);

	// Inline attachements
	if (this.inlineAttachments ().count () > 0) {
	    // Create a "related" MimeMultipart
	    MimeMultipart relatedMultiparts = new MimeMultipart ("related");
			
	    // add each inline attachments to the message
	    Enumeration en = this.inlineAttachments ().objectEnumerator ();
	    while (en.hasMoreElements ()) {
		ERMailAttachment attachment = (ERMailAttachment)en.nextElement ();
		BodyPart bp = attachment.getBodyPart ();
		relatedMultiparts.addBodyPart (bp);
	    }
	    
	    // Add this multipart to the main multipart as a compound BodyPart
	    BodyPart relatedAttachmentsBodyPart = new MimeBodyPart ();
	    relatedAttachmentsBodyPart.setDataHandler (new DataHandler (relatedMultiparts, relatedMultiparts.getContentType ()));
	    multipart.addBodyPart (relatedAttachmentsBodyPart);
	}

        return new DataHandler (multipart, multipart.getContentType ());
    }

}
