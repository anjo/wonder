//
// Application.java
// Project DRGroupingTestJava
//
// Created by dneumann on Tue Oct 02 2001
//
package er.grouping.examples;

import er.extensions.*;
import er.grouping.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

public class Application extends ERXApplication {
    
    public static void main(String argv[]) {
        WOApplication.main(argv, Application.class);
    }

    public Application() {
        super();
        System.out.println("Welcome to " + this.name() + "!");
        
    }
    
}
