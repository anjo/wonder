//Check path to Config File.
//Check the Expire Date.

import com.webobjects.appserver.*;
import com.webobjects.appserver.xml.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.io.*;

public class Application extends WOApplication {

    private Configuration config;
    private String configPath;
    public String fontList = "Arial,Helvetica";
    
    public static void main(String argv[]) {
        WOApplication.main(argv, Application.class);
    }

    public Application() {
        super();
        configPath = NSPathUtilities.stringByAppendingPathComponent(this.path(), "Configuration.xml");
        try {
            config = Configuration.configurationWithPath(configPath);
        } catch(Exception e){
            System.out.println(e);
            config = new Configuration("");
            this.saveConfiguration();
        }
    }

    public boolean saveConfiguration(){
        String codedString = WOXMLCoder.coder().encodeRootObjectForKey(config, "Configuration");
        try{
            File configurationFile = new File(configPath);
            FileOutputStream fos = new FileOutputStream(configurationFile);
            fos.write(codedString.getBytes());
            fos.close();
            return true;
        }catch(IOException e){
            System.out.println(e);
            return false;
        }
    }

    public Configuration config(){
        return config;
    }
}
