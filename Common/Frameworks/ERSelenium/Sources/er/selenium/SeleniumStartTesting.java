/*
 * Copyright (c) 2007 Design Maximum - http://www.designmaximum.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 */

package er.selenium;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODirectAction;
import com.webobjects.appserver.WORedirect;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSSelector;

/**
 * DirectAction that starts testing. 
 * Has wa/SeleniumStartTesting, wa/SeleniumStartTesting/run and wa/SeleniumStartTesting/edit.
 */
public class SeleniumStartTesting extends WODirectAction {
	private static final Logger log = Logger.getLogger(SeleniumStartTesting.class);
	
	public static final String RESULTS_FILE_KEY = "resultsFile";
	
	public SeleniumStartTesting(WORequest request) {
		super(request);
	}
	
	public String buildUrl(String suite, boolean auto) {
		WOContext context = context();
        // context._generateCompleteURLs();
		String baseUrl = "http://" + WOApplication.application().hostAddress().getHostAddress();
		
		StringBuilder queryStr = new StringBuilder();
//		queryStr.append("baseUrl=" + baseUrl);
		queryStr.append("test=" + context.directActionURLForActionNamed("SeleniumTestSuite" + (suite != null ? "/" + suite :  ""), null));
		queryStr.append("&resultsUrl=" + context.directActionURLForActionNamed( "SeleniumTestResults", null));
		//TODO: add filename check here
		String resultsFile = (String)context().request().formValueForKey(RESULTS_FILE_KEY);
		if (resultsFile != null)
			queryStr.append("/" + resultsFile);
		
		if (auto)
			queryStr.append("&auto=true");
		String url = context.urlWithRequestHandlerKey("_sl_", "selenium-core/TestRunner.html", queryStr.toString());
        // doesn't work, pity
        // url = url.replaceFirst(".*?selenium-core/TestRunner.html", "chrome://selenium-ide/content/selenium/TestRunner.html");
        return url;
	}
	
	// @Override
	public WOActionResults defaultAction() {
		return runAction();
	}
    
    private WOActionResults redirect(String url) {
        WORedirect redirect = new WORedirect(context());
        redirect.setUrl(url);
        return redirect;
    }
    
    private WOActionResults html(String url) {
        WOResponse response = new WOResponse();
        response.appendContentString("<html><body><a href='" + url + "'>go</a><body></html>");
        return response;
    }
    
    private WOActionResults result(String suite, boolean edit) {
        String url = buildUrl(suite, edit);
        if(context().request().formValueForKey("ide") != null) {
            return html(url);
        }
        return redirect(url);
    }
 
    public WOActionResults editAction() {
        return result(null, true);
    }
    
    public WOActionResults runAction() {
        return result(null, false);
    }

    public WOActionResults performActionNamed(String anActionName) {
        if(!ERSelenium.testsEnabled()) {
            return new WOResponse();
        }
        if("default".equals(anActionName)) {
            anActionName = null;
        } else if(new NSSelector(anActionName + "Action").implementedByObject(this)) {
            return super.performActionNamed(anActionName);
        }
        return result(anActionName, false);
    }
}