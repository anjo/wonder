/*
 * WOProjectBuilderAction.java
 * � Copyright 2001 Apple Computer, Inc. All rights reserved.
 * This a modified version.
 * Original license: http://www.opensource.apple.com/apsl/
 */

package com.webobjects.woextensions;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects._ideservices.*;
import com.webobjects.appserver._private.*;

import java.net.*;
import java.io.*;
import java.util.Properties;

/* This DirectAction is used for driving ProjectBuilder : it creates a socket and hope that
 ProjectBuilder is listening on it, then it sends the details about the exception so PB can
 display the correct file at the correct line number.
 The result of the DirectAction is a basic HTML page that use a JavaScript to go back. The JavaScript
 source code is directly hard coded in the page.
*/
public class WOProjectBuilderAction extends WODirectAction {

    public WOProjectBuilderAction(WORequest aRequest) {
        super(aRequest);
    }

    protected WOResponse javascriptBack() {
        // Return an HTML page that contains a JavaScript code to do a 'back'
        WOResponse response = WOApplication.application().createResponseInContext(null);
        response.appendContentString("<HTML><BODY><SCRIPT>history.go(-1);</SCRIPT><P>Please use the <B>back</B> button of your browser to go back to the Exception page.</P></BODY></HTML>");
        return response;
    }
    
    public WOActionResults openInProjectBuilderAction() {

        // Read now the information about the request : which method, which line #, which file, which message
        WORequest request = request();
        String methodName,filename, errorMessage, fullClassName;
        Number line;
        
        methodName = (String)request.stringFormValueForKey("methodName");
        line = request.numericFormValueForKey("line",new NSNumberFormatter("#0"));
        filename = (String)request.stringFormValueForKey("filename");
        errorMessage = (String)request.stringFormValueForKey("errorMessage");
        fullClassName = (String)request.stringFormValueForKey("fullClassName");
        WOResourceManager resources = WOApplication.application().resourceManager();

        // pay no attention to this use of protected API
        WODeployedBundle appBundle = resources._appProjectBundle();
        if (appBundle instanceof WOProjectBundle) {
            WOProjectBundle project = (WOProjectBundle) appBundle;
            _WOProject woproject = project._woProject();
            String filePath = woproject._pathToSourceFileForClass(fullClassName, filename);
            if (filePath == null) {
                
                // inform user file not found?
            } else {
                
                _IDEProject ideproject = woproject.ideProject();
                int lineInt = (line == null) ? 0 : line.intValue();

                ideproject.openFile(filePath, lineInt, errorMessage);
            }
        }

        return javascriptBack();
    }
}
