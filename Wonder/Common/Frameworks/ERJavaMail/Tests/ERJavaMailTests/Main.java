//
// Main.java: Class file for WO Component 'Main'
// Project OdaikoJavaMailTests
//
// Created by camille on Thu Jul 04 2002
//
 
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import er.extensions.*;
import er.javamail.*;
import java.util.*;

public class Main extends WOComponent {

    // Accented email
    public static String EMAIL = "Fr�d�ric Dupond <test@domain.com>";
    public String content = "Ceci est le contenu du mail, il n'y a rien de particulier, juste un texte avec des accents:\n\n� � � � � � � � � � � � � � � � � �";

    public Main(WOContext context) {
        super(context);
    }

    public void setAdminEmail (String email) {
        ERJavaMail.sharedInstance ().setAdminEmail (email);
    }

    public String adminEmail () {
        return ERJavaMail.sharedInstance ().adminEmail ();
    }

    public void sendTextOnlyMail () {
        // Create Attachment
        ERMailTextAttachment textAttachment = new ERMailTextAttachment ("R�sultats.txt", "Data goes here! ...");

        // Create an instance of an OFMailDelivery subclass
        ERMailDeliveryPlainText mail = new ERMailDeliveryPlainText ();

        try {
            mail.newMail();
            mail.setTextContent (content);
            mail.addAttachment (textAttachment);
            mail.setFromAddress (EMAIL);
            mail.setReplyToAddress (EMAIL);
            mail.setToAddress (EMAIL);
            mail.setSubject ("Les r�sultats sont arriv�s !");
			mail.sendMail ();
		} catch (Exception e) {
			e.printStackTrace ();
		}
    }

    public void sendHTMLMail () {
		ERMailUtils.sendHTMLMail ("Home", null,
							EMAIL,
							EMAIL,
							EMAIL, "HTML Test");
    }
}
