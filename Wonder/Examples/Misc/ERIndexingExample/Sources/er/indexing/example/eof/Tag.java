package er.indexing.example.eof;

import com.webobjects.eocontrol.*;

public class Tag extends _Tag {

    @SuppressWarnings("unused")
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Tag.class);

    public static final TagClazz clazz = new TagClazz();
    public static class TagClazz extends _Tag._TagClazz {
        /* more clazz methods here */
    }

    public interface Key extends _Tag.Key {}

    /**
     * Initializes the EO. This is called when an EO is created, not when it is 
     * inserted into an EC.
     */
    public void init(EOEditingContext ec) {
        super.init(ec);
    }
    
    // more EO methods here
}
