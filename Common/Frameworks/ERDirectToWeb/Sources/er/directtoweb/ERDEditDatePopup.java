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

///////////////////////////////////////////////////////////////////////////////////////////////////////
// Important D2W Keys:
//	editTime - 0 || 1 denotes whether to show the time aspect of the component.
//	yearRangeTop - specifies the upper limit for year to be displayed.
//	yearRangeBottom - specifies the lower limit for year to be displayed
///////////////////////////////////////////////////////////////////////////////////////////////////////
public class ERDEditDatePopup extends ERDEditDatePopupCommon {

    public ERDEditDatePopup(WOContext context) { super(context); }
    
    public void takeValuesFromRequest (WORequest request, WOContext context) {
        super.takeValuesFromRequest (request,context);
        NSTimestamp date = null;
        try {
            if (time==null || time.length()!=5)
                date = (NSTimestamp)ALL_FORMAT.parseObject(day+" "+month+" "+year);
            else
                date = (NSTimestamp)ALL_FORMAT_AND_TIME.parseObject(day+" "+month+" "+year+" "+time);
            object().validateTakeValueForKeyPath(date, key());
        } catch (NSValidation.ValidationException v) {
            parent().validationFailedWithException(v,date,key());
        } catch(Exception e) {
            parent().validationFailedWithException(e,date,key());           
        }
    }
}
