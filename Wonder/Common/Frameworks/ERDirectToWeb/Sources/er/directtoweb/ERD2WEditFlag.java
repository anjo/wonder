//
// FRD2WEditYesNo.java: Class file for WO Component 'FRD2WEditYesNo'
// Project FRAdmin
//
// Created by bposokho on Fri Aug 02 2002
//
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.directtoweb.*;

public class ERD2WEditFlag extends D2WEditString {

    public ERD2WEditFlag(WOContext context) {
        super(context);
    }

    public String yesNoBoolean() {
        return (String)object().valueForKey(propertyKey());
    }
    public void setYesNoBoolean(String newYesNoBoolean) {
        object().validateTakeValueForKeyPath(newYesNoBoolean, propertyKey());
    }
    public String radioBoxGroupName(){
        return ("YesNoGroup_"+d2wContext().propertyKey());
    }

    public void validationFailedWithException(Throwable theException,Object theValue, String theKeyPath) {
        parent().validationFailedWithException(theException, theValue, theKeyPath);
    }

    public void takeValuesFromRequest(WORequest r, WOContext c) {
        super.takeValuesFromRequest(r,c);
        try {
            object().validateTakeValueForKeyPath(objectPropertyValue(), propertyKey());
        } catch (NSValidation.ValidationException e) {
            validationFailedWithException(e, objectPropertyValue(), propertyKey());
        }
    }
}
