/*
 $Id$

 ERMailFileAttachment.java - Camille Troillard - tuscland@mac.com
*/


package er.javamail;

import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;

public class ERMailFileAttachment extends ERMailAttachment {

    protected String _fileName;
    protected String _contentID;

    protected ERMailFileAttachment (Object content) {
        super (content);
    }
	
    public ERMailFileAttachment (String fileName, String id, File content) {
        super (content);
        this.setFileName (fileName);
        this.setContentID (id);
    }

    public String fileName () {
	if (_fileName == null)
	    _fileName = "attachement.txt";
	return _fileName;
    }

    public void setFileName (String name) {
        _fileName = name;
    }

    public String contentID () {
	return _contentID;
    }

    public void setContentID (String id) {
        _contentID = id;
    }

    protected BodyPart getBodyPart () throws MessagingException {
        MimeBodyPart bp = new MimeBodyPart ();
        DataSource ds = new FileDataSource ((File)this.content ());
        bp.setDataHandler (new DataHandler (ds));

        if (this.contentID () != null)	bp.setHeader ("Content-ID", this.contentID ());
        bp.setFileName (this.fileName ());

        return bp;
    }
}
