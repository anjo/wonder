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

import com.webobjects.appserver.WOApplication;

import er.extensions.ERXFrameworkPrincipal;
import er.extensions.foundation.ERXProperties;
import er.selenium.io.SeleniumComponentExporter;
import er.selenium.io.SeleniumImporterExporterFactory;
import er.selenium.io.SeleniumPresentationExporterPage;
import er.selenium.io.SeleniumSeleneseExporter;
import er.selenium.io.SeleniumSeleneseImporter;
import er.selenium.io.SeleniumXHTMLExporterPage;
import er.selenium.io.SeleniumXHTMLImporter;

/**
 * Framework startup which registers the request handler and patches the Setup
 * class into the runtime.
 */
public class ERSelenium extends ERXFrameworkPrincipal {

    public static final String SUITE_SEPERATOR = "|";
    
    public static final String SELENIUM_TESTS_DISABLED_MESSAGE = "Selenium tests are disabled.";

    public static final String ACTION_COMMAND_FAILED_MESSAGE = "Action command failed.";

    public static final String ACTION_COMMAND_SUCCEEDED_MESSAGE = "Action command succeeded.";

    private static SeleniumTestFilesFinder testFilesFinder;
    
    static {
        setUpFrameworkPrincipalClass(ERSelenium.class);
    }
    
    public static void registerImportersExporters() {
        SeleniumImporterExporterFactory.instance().registerImporter(".html", new SeleniumXHTMLImporter());
        SeleniumImporterExporterFactory.instance().registerExporter(new SeleniumComponentExporter("xhtml", SeleniumXHTMLExporterPage.class.getName()));
        SeleniumImporterExporterFactory.instance().registerImporter(".sel", new SeleniumSeleneseImporter());
        SeleniumImporterExporterFactory.instance().registerExporter(new SeleniumSeleneseExporter());
        SeleniumImporterExporterFactory.instance().registerExporter(new SeleniumComponentExporter("presentation", SeleniumPresentationExporterPage.class.getName()));    	
    }

    ERSelenium sharedInstance;

    public ERSelenium sharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = (ERSelenium) ERXFrameworkPrincipal.sharedInstance(ERSelenium.class);
        }
        return sharedInstance;
    }

    public static boolean testsEnabled() {
        return ERXProperties.booleanForKeyWithDefault("SeleniumTestsEnabled", false);
    }

    // @Override
    public void finishInitialization() {
        // TODO: check for multithreading/synchronization issued with factory
        // instance() method
    	registerImportersExporters();
    	setTestFilesFinder(new DefaultSeleniumTestFilesFinder());

        WOApplication.application().registerRequestHandler(new SeleniumTestRunnerProxy(), "_sl_");
    }

	public static SeleniumTestFilesFinder testFilesFinder() {
		return testFilesFinder;
	}
	
	public static void setTestFilesFinder(SeleniumTestFilesFinder finder) {
		testFilesFinder = finder;
	}
}