package er.extensions;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

/**
 * Use this to return a string from direct actions.
 *
 * @binding value value
 * @binding escapeHTML escape HTML
 *
 * @created ak on Sat Sep 27 2003
 * @project ERExtensions
 */

public class ERXStringHolder extends ERXStatelessComponent {

    /** logging support */
    private static final ERXLogger log = ERXLogger.getLogger(ERXStringHolder.class,"components");

    protected String _value;
    protected Boolean _escapeHTML = Boolean.FALSE;

    /**
     * Public constructor
     * @param context the context
     */
    public ERXStringHolder(WOContext context) {
        super(context);
    }

    public String value() { return _value; }
    public boolean escapeHTML() { return _escapeHTML.booleanValue(); }
    public void setValue(Object value) {
        _value = (value == null ? "" : value.toString());
    }
    public void setEscapeHTML(boolean value) {
        _escapeHTML = value ? Boolean.TRUE : Boolean.FALSE;
    }
    public void reset() {
        _value = null;
        _escapeHTML = Boolean.FALSE;
    }
}
