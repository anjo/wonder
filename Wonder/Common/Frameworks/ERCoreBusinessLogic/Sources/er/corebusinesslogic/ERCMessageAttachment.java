// ERCMessageAttachment.java
// (c) by Anjo Krank (ak@kcmedia.ag)
package er.corebusinesslogic;

import org.apache.log4j.Logger;

import com.webobjects.eocontrol.*;

import er.extensions.*;

public class ERCMessageAttachment extends _ERCMessageAttachment {
    static final Logger log = Logger.getLogger(ERCMessageAttachment.class);

    public ERCMessageAttachment() {
        super();
    }

    public void awakeFromInsertion(EOEditingContext ec) {
        super.awakeFromInsertion(ec);
    }
    
    
    // Class methods go here
    
    public static class ERCMessageAttachmentClazz extends _ERCMessageAttachmentClazz {
        
    }

    public static ERCMessageAttachmentClazz messageAttachmentClazz() { return (ERCMessageAttachmentClazz)EOEnterpriseObjectClazz.clazzForEntityNamed("ERCMessageAttachment"); }
}
