package er.excel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import er.extensions.*;

/**
 * Class for Excel Component EGWrapper.
 *
 * @binding sample sample binding explanation
 *
 * @created ak on Thu Mar 04 2004
 * @project ExcelGenerator
 */

public class EGWrapper extends ERXNonSynchronizingComponent {

    /** logging support */
    private static final ERXLogger log = ERXLogger.getLogger(EGWrapper.class,"components,excel");
	private String _fileName;
	private NSDictionary _styles;
    private NSDictionary _fonts;
    
    /**
     * Public constructor
     * @param context the context
     */
    public EGWrapper(WOContext context) {
        super(context);
    }
    
    public boolean isEnabled() {
    	return ERXValueUtilities.booleanValueForBindingOnComponentWithDefault("enabled", this, false);
    }
    
    public String fileName() {
    	if(_fileName == null) {
    		_fileName = (String)valueForBinding("fileName");
    	}
    	return _fileName;
    }
    public void setFileName(String value) {
    	_fileName = value;
    }
    
    public NSDictionary styles() {
    	if (_styles == null) {
    		_styles = (NSDictionary) valueForBinding("styles");
    	}
    	return _styles;
    }
    public void setStyles(NSDictionary value) {
    	_styles = value;
    }
    
    
    public NSDictionary fonts() {
    	if (_fonts == null) {
			_fonts = (NSDictionary) valueForBinding("fonts");
		}
		return _fonts;
    }
    public void setFonts(NSDictionary value) {
    	_fonts = value;
    }
    
    
    public void appendToResponse(WOResponse response, WOContext context) {
		if(isEnabled()) {
			WOResponse newResponse = new WOResponse();
			
			super.appendToResponse(newResponse, context);
			
			String contentString = newResponse.contentString();
			InputStream stream = new ByteArrayInputStream(contentString.getBytes());
			EGSimpleTableParser parser = new EGSimpleTableParser(stream, fonts(), styles());
			NSData data = parser.data();
			if((hasBinding("data") && canSetValueForBinding("data")) ||
				(hasBinding("stream") && canSetValueForBinding("stream"))
			) {
				if(hasBinding("data")) {
					setValueForBinding(data, "data");
				}
				if(hasBinding("stream")) {
					setValueForBinding(data.stream(), "stream");
				}
				response.appendContentString(contentString);
			} else {
				response.appendContentData(data);
				String fileName = fileName();
				if(fileName == null) {
					fileName = "results.xls";
				}
				response.setHeader("inline; filename=" + fileName, "content-disposition");
				response.setHeader("application/msexcel", "content-type");
			}
		} 
		else {
			super.appendToResponse(response, context);
		}
    }
}

