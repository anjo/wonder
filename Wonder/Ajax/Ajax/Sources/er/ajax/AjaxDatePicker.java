package er.ajax;

import java.text.Format;
import java.text.SimpleDateFormat;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestampFormatter;

import er.extensions.ERXResponseRewriter;

/**
 * Shameless port and adoption of Rails Date Kit.  This input understands the format symbols
 * %d, %e, %b, %m, %B, %y, and %Y. See the NSTimestampFormatter for 
 * what these symbols do. This component can also understand the corresponding symbols from 
 * java.text.SimpleDateFormat.  The translation from SimpleDateFormat symbols to NSTimestampFormatter
 * symbols may have some defects.
 * <p>Only one of format or formatter may be bound, if both are unbound the default of %m %d %Y is used.
 * If format is bound, the pattern is used to create an internal formatter for validation.  If formatter
 * is bound, its pattern is extracted and used in place of format. The format/formatter is used to control
 * the initial display in the input, the format of the value that the date picker places into the input, and 
 * validation of the input contents on form submission. The use of formatter over format is
 * preferred for reasons of efficiency and localization.</p>
 * 
 * <p><b>NOTE</b>: the AjaxDatePicker does <b>NOT</b> play nice with the AjaxModalDialogOpener.  There is some sort of 
 * initialization conflict (I think) with Prototype that leaves you with a blank page and the browser waiting
 * forever for something (and I have not been able to determine what it is) as soon as calendar.js loads and
 * initialized.  It will work if the page the AMD appears on explicitly loads the calendar.js in it's HEAD:</p>
 * <pre>
 *  public void appendToResponse(WOResponse response, WOContext context) {
 *       super.appendToResponse(response, context);
 *       ERXResponseRewriter.addScriptResourceInHead(response, context(), "Ajax", "calendar.js");
 *   }
 * </pre>
 * 
 * @binding value the value that will be shown in the input field and set by the date picker (required)
 * @binding format the format to use in the input field (only one of format or formatter may be bound)
 * @binding formatter the formatter to use with the input field (only one of format or formatter may be bound)
 *
 * @binding id HTML ID passed to the input field 
 * @binding class CSS class passed to the input field 
 * @binding style CSS style passed to the input field 
 * @binding size size attribute passed to the input field 
 * @binding maxlength maxlength attribute passed to the input field 
 * @binding name name attribute passed to the input field 
 * @binding onDateSelect JavaScript to execute when a date is selected from the calendar
 * @binding fireEvent false if the onChange event for the input should NOT be fired when a date is selected in the calendar, defaults to true
 * 
 * @binding dayNames list of day names (Sunday to Saturday) for localization, English is the default
 * @binding monthNames list of month names for localization, English is the default
 * @binding imagesDir directory to take images from, takes them from Ajax.framework by default
 *
 * @binding calendarCSS name of CSS resource with classed for calendar, defaults to "calendar.css"
 * @binding calendarCSSFramework name of framework (null for application) containing calendarCSS resource, defaults to "Ajax"
 *
 * @see java.text.SimpleDateFormat
 * @see com.webobjects.foundation.NSTimestampFormatter
 * 
 * @see <a href="http://www.methods.co.nz/rails_date_kit/rails_date_kit.html">Rails Date Kit</a>
 *
 * @author ported by Chuck Hill
 */
public class AjaxDatePicker extends AjaxComponent {
	
	private static String defaultImagesDir;
	
	private NSMutableDictionary options;
	private Format formatter;
	private String format;
	
    public AjaxDatePicker(WOContext context) {
        super(context);
        
        // I am not expecting the images to get localized, so this can be set once
        // This is hacky, but I wanted to avoid changing the JS to take the path for each image in options
        // and WO does not expose this path any other way.  Still half thinking I should have changed the JS...
        if (defaultImagesDir == null) {
			defaultImagesDir = application().resourceManager().urlForResourceNamed("calendar_prev.png", "Ajax", null, context().request()).toString();
			int lastSeperator = defaultImagesDir.lastIndexOf("%2F");
			if (lastSeperator == -1) {
				lastSeperator = defaultImagesDir.lastIndexOf('/');
			}
			defaultImagesDir = defaultImagesDir.substring(0, lastSeperator);
			
			// Need to pre-populate the cache for WOResourceManager
			application().resourceManager().urlForResourceNamed("calendar_next.png", "Ajax", null, context().request()).toString();
        }
    }
    
    /**
     * @return <code>true</code>
     */
    public boolean isStateless() {
    	return true;
    }
    
    /**
     * Sets up format / formatter values.
     */
    public void awake() {
		super.awake();
	
		if ( ! (hasBinding("formatter") || hasBinding("format"))) {
			format = "%m %d %Y";  // Default
			formatter = new NSTimestampFormatter(format);
		}
		else if (hasBinding("formatter")) {
    		formatter = (Format) valueForBinding("formatter");
    		if (formatter instanceof NSTimestampFormatter) {
    			format = translateSimpleDateFormatSymbols(((NSTimestampFormatter)formatter).pattern());
    		}
    		else if (formatter instanceof SimpleDateFormat) {
    			format = ((SimpleDateFormat)formatter).toPattern();
    		}
    		else {
    			throw new RuntimeException("Can't handle formatter of class " + formatter.getClass().getCanonicalName());
    		}
    	}
    	else {
    		format = (String) valueForBinding("format");
    		formatter = new NSTimestampFormatter(format);
    	}
		
		format = translateSimpleDateFormatSymbols(format);
    }
    
    /**
     * Clear cached values.
     */
    public void reset() {
    	options = null;
    	formatter = null;
    	format = null;
    	super.reset();
    }

    /**
     * Sets up AjaxOptions prior to rendering.
     */
    public void appendToResponse(WOResponse res, WOContext ctx) {
		
		NSMutableArray ajaxOptionsArray = new NSMutableArray();
		
		// The "constant" form of AjaxOption is used so that we can rename the bindings or convert the values
		ajaxOptionsArray.addObject(new AjaxOption("format", format(), AjaxOption.STRING));
		ajaxOptionsArray.addObject(new AjaxOption("month_names", valueForBinding("monthNames"), AjaxOption.ARRAY));
		ajaxOptionsArray.addObject(new AjaxOption("day_names", valueForBinding("dayNames"), AjaxOption.ARRAY));
		
		ajaxOptionsArray.addObject(new AjaxOption("onDateSelect", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("fireEvent", AjaxOption.BOOLEAN));
		
		if (hasBinding("imagesDir")) {
			ajaxOptionsArray.addObject(new AjaxOption("images_dir", valueForBinding("imagesDir"), AjaxOption.STRING));
		}
		else {
			ajaxOptionsArray.addObject(new AjaxOption("images_dir", defaultImagesDir, AjaxOption.STRING));
		}
		options = AjaxOption.createAjaxOptionsDictionary(ajaxOptionsArray, this);
    	super.appendToResponse(res, ctx);
    }
    
    /**
     * @return JavaScript for onFocus binding of HTML input
     */
    public String onFocusScript() {
        return showCalendarScript();
    }
    
    /**
     * @return JavaScript for onClick binding of HTML input
     */
    public String onClickScript() {
        	StringBuffer script = new StringBuffer(200);
           	script.append("event.cancelBubble=true; ");
         	script.append(showCalendarScript());
            return script.toString();
    }
    
    /**
     * @return JavaScript to load CSS and show calendar display
     */
    public String showCalendarScript() {
    	StringBuffer script = new StringBuffer(200);
    	// Load the CSS like this to avoid odd race conditions when this is used in an AjaxModalDialog: at times
    	// the CSS does not appear to be available and the calendar appears in the background
    	script.append("AOD.loadCSS('");
    	script.append(application().resourceManager().urlForResourceNamed((String)valueForBinding("calendarCSS", "calendar.css"), 
    			                                                          (String)valueForBinding("calendarCSSFramework", "Ajax"), null, context().request()).toString());
    	script.append("'); ");
    	script.append("this.select(); calendar_open(this, ");
    	AjaxOptions.appendToBuffer(options(), script, context());
    	script.append(");");
        return script.toString();
    }

    /**
     * Quick and rude translation of formatting symbols from SimpleDateFormat to the symbols
     * that this component uses.
     *
     * @param symbols the date format symbols to translate
     * @return translated date format symbols 
     */
    public String translateSimpleDateFormatSymbols(String symbols) {
    	// Wildly assume that there is no translation needed if we see a % character
    	if (symbols.indexOf('%') > -1) {
    		return symbols;
    	}
    	
    	StringBuilder sb = new StringBuilder(symbols);
    	replace(sb, "dd", "%~");
    	replace(sb, "d", "%d");
    	replace(sb, "%~", "%d");
    	replace(sb, "MMMM", "%B");
    	replace(sb, "MMM", "%b");
    	replace(sb, "MM", "%m");
    	replace(sb, "M", "%m");
    	replace(sb, "yyyy", "%Y");
    	replace(sb, "yyy", "%~");
    	replace(sb, "yy", "%~");
    	replace(sb, "y", "%y");
    	replace(sb, "%~", "%y");
    	
    	return sb.toString();
    }
    
    /**
     * Helper method for translateSimpleDateFormatSymbols.
     */
    private void replace(StringBuilder builder, String original, String replacement) {
    	int index = builder.indexOf(original);
    	if (index > -1) {
    		builder.replace(index, index + original.length(), replacement);
    	}
    }
    
    /**
     * @return format string used by date picker
     */
    public String format() {
    	return format;
    }
    
    /**
     * @return formatter controlling initial contents of input and validation
     */
    public Format formatter() {
    	return formatter;
    }
    
    /**
     * @return cached Ajax options for date picker JavaScript
     */
    public NSMutableDictionary options() {
    	return options;
    }
    
    /**
     * Includes calendar.css and calendar.js.
     */
	protected void addRequiredWebResources(WOResponse response) {
		ERXResponseRewriter.addScriptResourceInHead(response, context(), "Ajax", "prototype.js");
		ERXResponseRewriter.addScriptResourceInHead(response, context(), "Ajax", "wonder.js");
		ERXResponseRewriter.addScriptResourceInHead(response, context(), "Ajax", "calendar.js");
		ERXResponseRewriter.addScriptResourceInHead(response, context(), "Ajax", "date.js");
		ERXResponseRewriter.addStylesheetResourceInHead(response, context(), "Ajax", "calendar.css");
	}
	
	/**
	 * No action so nothing for us to handle.
	 */
	public WOActionResults handleRequest(WORequest request, WOContext context) {
		return null;
	}
    
	
	
    /**
     * Overridden so that parent will handle in the same manner as if this were a dynamic element. 
     */
    public void validationFailedWithException(Throwable t, Object value, String keyPath)
    {
    	parent().validationFailedWithException(t, value, keyPath);
    }
    
}
