/*
 $Id$

 ERMessage.java - Camille Troillard - tuscland@mac.com
*/


package er.javamail;

import javax.mail.*;
import javax.mail.internet.*;

public class ERMessage extends Object {

    private MimeMessage message;
    private Object anObject;
    
    public ERMessage() {};
    
    public ERMessage(MimeMessage mimeMessage, Object callbackObject ) {
    	super();
    	message = mimeMessage;
    	anObject = callbackObject;
    }

    public void setMimeMessage (MimeMessage m) {
        message = m;
    }
    
    public MimeMessage mimeMessage () {
        return message;
    }
    
    public void setCallbackObject (Object obj) {
        anObject = obj;
    }
    
    public Object callbackObject () {
        return anObject;
    }
    
    /**
     * @param recipientType which can be: 
     * <code>Message.RecipientType.To</code>, 
     * <code>Message.RecipientType.CC</code>, or
     * <code>Message.RecipientType.BCC</code>
     */
    public Address[] recipients (Message.RecipientType recipientType) throws MessagingException{
        return message == null ?  null : message.getRecipients (recipientType);
    }
    
    public String recipientsAsString (Message.RecipientType recipientType) 
            throws MessagingException, AddressException {
        return recipientsAsString (recipientType, -1);
    }
    
    public String recipientsAsString (Message.RecipientType recipientType, int maxAddresses) 
            throws MessagingException, AddressException {
        Address[] allAddresses = recipients (recipientType);
        Address[] limitteredAddresses = null;

        if (allAddresses == null  ||  allAddresses.length == 0)  return null;

        if (maxAddresses > 0) {
            limitteredAddresses = new Address[maxAddresses];
            System.arraycopy(allAddresses, 0, 
                    limitteredAddresses, 0, Math.min (allAddresses.length, maxAddresses));
        } else {
            limitteredAddresses = allAddresses;
        }
        
        StringBuffer result = new StringBuffer(); 
        result.append (ERMailUtils.convertInternetAddressesToNSArray((InternetAddress[]) limitteredAddresses)
                                                        .componentsJoinedByString (", "));
        if (0 < maxAddresses  &&  maxAddresses < allAddresses.length) {
            result.append (", and ");
            result.append (allAddresses.length - maxAddresses);
            result.append (" other recipients");
        }
        return result.toString ();
    }

    public String allRecipientsAsString () throws MessagingException {
        return allRecipientsAsString (true, -1);
    } 
    
    public String allRecipientsAsString (boolean includeBcc) throws MessagingException {
        return allRecipientsAsString (includeBcc, -1);
    }

    public String allRecipientsAsString (boolean includeBcc, int maxAddresses) throws MessagingException {
        StringBuffer recipients = new StringBuffer ();
        String addresses = recipientsAsString(Message.RecipientType.TO, maxAddresses);
        if (addresses != null  &&  addresses.length() > 0) 
            recipients.append ("To: ").append (addresses);

        addresses = recipientsAsString(Message.RecipientType.CC, maxAddresses);
        if (addresses != null  &&  addresses.length() > 0) 
            recipients.append ("CC: ").append (addresses);
        
        if (includeBcc) {
            addresses = recipientsAsString (Message.RecipientType.BCC, maxAddresses);
            if (addresses != null  &&  addresses.length () > 0) 
                recipients.append ("BCC: ").append (addresses);
        }
        return recipients.toString ();
    }
    
    public String toString () {
        StringBuffer sbuf = new StringBuffer ();
        sbuf.append ("<").append(getClass ().getName ()).append (" ");
        if (message == null) {
            sbuf.append("No mime message is set.");
        } else {
            try {
                sbuf.append(allRecipientsAsString ());
            } catch (MessagingException ex) {
                ;
            }
        }
        sbuf.append (">");
        return sbuf.toString ();
    }

}
