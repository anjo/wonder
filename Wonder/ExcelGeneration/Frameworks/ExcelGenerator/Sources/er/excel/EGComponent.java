package er.excel;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import er.extensions.components.ERXStatelessComponent;
import er.extensions.logging.ERXLogger;

/**
 * Class for Excel Component EGComponent.
 *
 * @binding sample sample binding explanation
 *
 * @created ak on Wed Mar 03 2004
 * @project ExcelGenerator
 */

public class EGComponent extends ERXStatelessComponent {

    /** logging support */
    protected final ERXLogger log = ERXLogger.getLogger(getClass(),"components,excel");
	
    /**
     * Public constructor
     * @param context the context
     */
    public EGComponent(WOContext context) {
        super(context);
    }
    
    public void reset() {
    	super.reset();
    	
    }
    
    public EGComponent parentExcelComponent() {
    	WOComponent parent = parent();
    	while(parent != null && !(parent instanceof EGComponent)) {
    		parent = parent.parent();
    	}
    	return (EGComponent)parent;
    }
}
