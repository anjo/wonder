// ERCMailMessage.java
// (c) by Anjo Krank (ak@kcmedia.ag)
package er.corebusinesslogic;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.util.*;
import java.math.BigDecimal;
import er.extensions.*;

public class ERCMailMessage extends _ERCMailMessage {

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERCMailMessage.class);

    /** holds the address separator */
    public static final String AddressSeparator = ",";
    
    /**
     * Clazz object used to hold all clazz related methods.
     */
    public static class ERCMailMessageClazz extends _ERCMailMessageClazz {

        /**
         * Gets an iterator for batching through un sent messages.
         * @return batch iterator for messages to be sent
         */
        public ERXFetchSpecificationBatchIterator batchIteratorForUnsentMessages() {
            EOFetchSpecification fetchSpec = EOFetchSpecification.fetchSpecificationNamed("messagesToBeSent",
                                                                                          "ERCMailMessage");
            return new ERXFetchSpecificationBatchIterator(fetchSpec);
        }
    }

    public static ERCMailMessageClazz mailMessageClazz() { return (ERCMailMessageClazz)EOGenericRecordClazz.clazzForEntityNamed("ERCMailMessage"); }


    /**
     * Public constructor.
     */
    public ERCMailMessage() {
        super();
    }

    /**
     * Default state of the mail message is
     * 'Ready To Be Sent'.
     * @param anEditingContext inserted into
     */
    public void awakeFromInsertion(EOEditingContext anEditingContext) {
        super.awakeFromInsertion(anEditingContext);
        setState(ERCMailState.READY_TO_BE_SENT_STATE);
    }
        
    /** log entry support is disabled */
    public String relationshipNameForLogEntry() {  return null; }
    public EOEnterpriseObject logEntryType() 	{  return null; }

    // State Methods
    public boolean isReadyToSendState() 	{ return state() == ERCMailState.READY_TO_BE_SENT_STATE; }
    public boolean isSentState() 		{ return state() == ERCMailState.SENT_STATE; }
    public boolean isExceptionState() 		{ return state() == ERCMailState.EXCEPTION_STATE; }
    public boolean isReceivedState() 		{ return state() == ERCMailState.RECEIVED_STATE; }

    // IMPLEMENTME: MarkReadInterface
    public void markReadBy(EOEnterpriseObject by) {
        // this will be useful for marketing to track who opens the emails
       setReadAsBoolean(true);
    }

    public void setReadAsBoolean(boolean read) {
        setIsRead(read ? ERXConstant.OneInteger : ERXConstant.ZeroInteger);
    }
    public boolean isReadAsBoolean() {
        return ERXUtilities.booleanValue(isRead());
    }

    public NSArray toAddressesAsArray() {
        return toAddresses() != null ? NSArray.componentsSeparatedByString(toAddresses(), ",") : NSArray.EmptyArray;
    }

    public void setToAddressesAsArray(NSArray toAddresses) {
        if (toAddresses != null && toAddresses.count() > 0) {
            setToAddresses(toAddresses.componentsJoinedByString(AddressSeparator));
        }
    }

    public NSArray ccAddressesAsArray() {
        return ccAddresses() != null ? NSArray.componentsSeparatedByString(ccAddresses(), ",") : NSArray.EmptyArray;
    }

    public void setCcAddressesAsArray(NSArray ccAddresses) {
        if (ccAddresses != null && ccAddresses.count() > 0) {
            setCcAddresses(ccAddresses.componentsJoinedByString(AddressSeparator));
        }
    }

    public NSArray bccAddressesAsArray() {
        return bccAddresses() != null ? NSArray.componentsSeparatedByString(bccAddresses(), ",") : NSArray.EmptyArray;
    }

    public void setBccAddressesAsArray(NSArray bccAddresses) {
        if (bccAddresses != null && bccAddresses.count() > 0) {
            setBccAddresses(bccAddresses.componentsJoinedByString(AddressSeparator));
        }
    }
    
    /**
     * Long description of the mail message.
     * @return very verbose description of the mail message.
     */
    public String longDescription() {
        StringBuffer sb=new StringBuffer();
        sb.append("To: ");
        sb.append(toAddresses());
        sb.append("\n");
        sb.append("cc: ");
        sb.append(ccAddresses());
        sb.append("\n");
        sb.append("Created: ");
        sb.append(created());
        sb.append("\n");
        sb.append("Title: ");
        sb.append(title());
        sb.append("\n");
        sb.append("Text: ");
        sb.append(text());
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Appends test to the currently stored text.
     * Useful for nested mime messages or multi-part messages.
     * @param text to be appended
     */
    public void appendText(String text) {
        String storedText = text();
        setText((storedText == null ? "" : storedText) + " " + text);
    }

    public Object validateEmptyStringForKey(Object value, String field) {
        if(value == null || "".equals(value) || ((String)value).length() == 0) {
            throw ERXValidationFactory.defaultFactory().createCustomException(this, field, value, "null");
        }
        return value;
    }
    
    // Validation Methods
    public Object validateFromAddress(String newValue) {
        return validateEmptyStringForKey(newValue, "fromAddress");
    }

    public Object validateTitle(String newValue) {
        return validateEmptyStringForKey(newValue, "title");
    }
    
    public Object validateToAddresses(String newValue) {
        return validateEmptyStringForKey(newValue, "toAddresses");
    }

    public Object validateText(String newValue) {
        return validateEmptyStringForKey(newValue, "text");
    }

    public void attachFileWithMimeType(String filePath, String mimeType) {
        ERCMessageAttachment attachment = (ERCMessageAttachment)ERXUtilities.createEO("ERCMessageAttachment", editingContext());
        attachment.setFilePath(filePath);
        if(mimeType != null)
            attachment.setMimeType(mimeType);
        addToBothSidesOfAttachments(attachment);
    }    
}
