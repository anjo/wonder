//
// DirectAction.java
// Project ERMovies
//
// Created by max on Thu Feb 27 2003
//
package er.examples.movies;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import er.directtoweb.*;

public class DirectAction extends ERD2WDirectAction {

    /**
     * (ak) WARNING: normally, you would not just subclass ERD2WDirectAction
     * without taking proper precautions that limit acces to the app.
     * For example, you could check the name of the action performed
     * and require that your user has logged in.
     */
    
    public DirectAction(WORequest aRequest) {
        super(aRequest);
    }

    public WOActionResults defaultAction() {
        return pageWithName("Main");
    }

}
