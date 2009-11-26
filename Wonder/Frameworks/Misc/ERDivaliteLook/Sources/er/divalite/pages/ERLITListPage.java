package er.divalite.pages;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.EditPageInterface;
import com.webobjects.directtoweb.InspectPageInterface;

import er.directtoweb.pages.ERD2WListPage;

/**
 * Divalite list page
 * subTask = 'simple'
 * 
 * @author ravim
 *
 */
public class ERLITListPage extends ERD2WListPage {
	public int index;

	public ERLITListPage(WOContext aContext) {
		super(aContext);
	}
	
	//accessors
    // FIXME: turn into rule
    public String listID() {
    	String pageConfiguration = (String) d2wContext().dynamicPage();
    	return (pageConfiguration != null && pageConfiguration.contains("Embedded")) ? null : "List";
    }
    
    public String createActionName() {
    	return "Create" + d2wContext().entity().name();
    }
}
