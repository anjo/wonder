package er.indexing.example.eof;

import com.webobjects.eocontrol.*;

public class AssetGroup extends _AssetGroup {

    @SuppressWarnings("unused")
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AssetGroup.class);

    public static final AssetGroupClazz clazz = new AssetGroupClazz();
    public static class AssetGroupClazz extends _AssetGroup._AssetGroupClazz {
        /* more clazz methods here */
    }

    public interface Key extends _AssetGroup.Key {}

    /**
     * Initializes the EO. This is called when an EO is created, not when it is 
     * inserted into an EC.
     */
    public void init(EOEditingContext ec) {
        super.init(ec);
    }
    
    // more EO methods here
}
