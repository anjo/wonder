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

package er.selenium.io;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;

import er.selenium.SeleniumTest;

public class SeleniumPresentationExporter implements SeleniumTestExporter {
	private static final Logger log = Logger.getLogger(SeleniumPresentationExporter.class);
	private static final String PRESENTATION_COMPONENT = "SeleniumTestPresentation";
	private static final String PRESENTATION_TEST_BINDING = "test";
	
	public String name() {
		return "presentation";
	}
	
	public String process(SeleniumTest test) {
		WORequest request = WOApplication.application().createRequest("GET", WOApplication.application().baseURL(), "HTTP/1.1", new NSDictionary(), new NSData(), new NSDictionary());
		WOContext context = WOApplication.application().createContextForRequest(request);
		WOComponent component = WOApplication.application().pageWithName(PRESENTATION_COMPONENT, context);
		WOResponse response = WOApplication.application().createResponseInContext(context);
		assert(request != null && context != null && component != null && response != null);
		
		component.takeValueForKey(test, PRESENTATION_TEST_BINDING);
		component.appendToResponse(response, context);
		return response.contentString();
	}
}