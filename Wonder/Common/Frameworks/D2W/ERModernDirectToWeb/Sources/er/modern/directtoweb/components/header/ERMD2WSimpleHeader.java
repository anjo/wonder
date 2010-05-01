package er.modern.directtoweb.components.header;

import com.webobjects.appserver.WOContext;

/**
 * Simple h1 header that defaults to displaying the displayNameForPageConfiguration
 * 
 * @d2wKey displayNameForPageConfiguration
 * 
 * @author davidleber
 *
 */
public class ERMD2WSimpleHeader extends ERMD2WHeader {
	
	public interface Keys extends ERMD2WHeader.Keys {
		
	}
	
    public ERMD2WSimpleHeader(WOContext context) {
        super(context);
    }
    
    public String headerString() {
    	if (_headerString == null) {
			_headerString = stringValueForBinding(Keys.displayNameForPageConfiguration);;
		}
		return _headerString;
    }
    
}