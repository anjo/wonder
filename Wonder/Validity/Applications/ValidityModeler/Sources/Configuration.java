import com.gammastream.validity.*;
import com.webobjects.appserver.*;
import com.webobjects.appserver.xml.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.io.*;
//import org.xml.sax.InputSource.*;

public final class Configuration extends Object implements WOXMLCoding {

    private NSMutableArray paths = new NSMutableArray();
    private NSMutableArray quickRules = new NSMutableArray();
    private String password;

    public static Configuration configurationWithPath(java.lang.String path){
        return (Configuration)WOXMLDecoder.decoder().decodeRootObject(path);
    }
    
    public Configuration(String s){
        this.setPassword(s);
    }
    
    public void addPath(String path){
        if(!paths.containsObject(path))
            paths.addObject(path);     
    }
    
    public void removePath(String path){
        if(paths.containsObject(path))
            paths.removeObject(path);
    }

    public void removeRule(GSVRule rule){
        if(quickRules.containsObject(rule))
            quickRules.removeObject(rule);
    }
    
    public void addRule(GSVRule rule){
        if(!quickRules.containsObject(rule))
            quickRules.addObject(rule);
    }

    public void setQuickRules(NSMutableArray rules){
            quickRules=rules;
    }

    public NSMutableArray paths(){
        return paths;
    }
    
    public NSMutableArray quickRules(){
        return quickRules;
    }
    
    public String password(){
        return password;
    }
    
    public void setPassword(String s){
        password = s;
    }
    
    public boolean hasRules() {
        return (quickRules().count()>0);
    }

    public boolean hasRecent() {
        return (paths().count()>0);
    }


    




    
    // xml interfaced methods
    public void encodeWithWOXMLCoder(WOXMLCoder coder) {
        coder.encodeObjectForKey((NSArray)paths, "Paths");
        coder.encodeObjectForKey(password, "Password");
        coder.encodeObjectForKey((NSArray)quickRules, "QuickRules");
    }

    public Configuration(WOXMLDecoder decoder) {
        paths = new NSMutableArray((NSArray)decoder.decodeObjectForKey("Paths"));
        password = (String)decoder.decodeObjectForKey("Password");
        quickRules = new NSMutableArray((NSArray)decoder.decodeObjectForKey("QuickRules"));
    }

    public Class classForCoder() {
        try{
        return Class.forName("Configuration");
        }catch(ClassNotFoundException e){
        System.out.println(e);
        return null;
        }
    }

}
