/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.directtoweb;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

public class ERDEditDatePopupOrNull extends ERDEditDatePopupCommon {

    public ERDEditDatePopupOrNull(WOContext context) { super(context); }

    protected static final String empty = "null";
    protected static final String date = "date";
    protected String _radioValue;

    public String empty() { return empty; }
    public String date() { return date; }

    public boolean isStateless() { return true; }
    public boolean synchronizesVariablesWithBindings() { return false; }

    public String radioValue(){
        if(_radioValue == null) {
            NSTimestamp dateValue = (NSTimestamp)objectPropertyValue();
            _radioValue = dateValue==null ? empty : date;
        }
        return _radioValue;
    }

    public void reset(){
        super.reset();
        _radioValue = null;
    }

    public void setRadioValue(String newString) { _radioValue = newString; }

    public String radioBoxGroupName() { return ("DateOrNullGroup_"+key()); }

    public void takeValuesFromRequest (WORequest request, WOContext context) {
        super.takeValuesFromRequest (request,context);
        if (radioValue().equals(date)){
            NSTimestamp date = null;
            try {
                if (time==null || time.length()!=5) {
                    date = (NSTimestamp)ALL_FORMAT.parseObject(day+" "+month+" "+year);
                } else {
                    date = (NSTimestamp)ALL_FORMAT_AND_TIME.parseObject(day+" "+month+" "+year+" "+time);
                }
                object().validateTakeValueForKeyPath(date, key());
            } catch (NSValidation.ValidationException v) {
                parent().validationFailedWithException(v,date,key());
            } catch(Exception e) {
                parent().validationFailedWithException(e,date,key());
            }
        } else {
            try {
                object().validateTakeValueForKeyPath(null, key());
            } catch (NSValidation.ValidationException v) {
                parent().validationFailedWithException(v,null,key());
            } catch(Exception e) {
                parent().validationFailedWithException(e,null,key());
            }
        }
    }
}
