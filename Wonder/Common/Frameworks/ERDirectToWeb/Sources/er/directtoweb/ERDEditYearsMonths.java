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
import java.math.*;

import er.extensions.ERXConstant;

public class ERDEditYearsMonths extends ERDCustomEditComponent {

    public ERDEditYearsMonths(WOContext context) {super(context);}
    
public boolean isStateless() { return true; }
    public boolean synchronizesVariablesWithBindings() { return false; }

    protected Integer numberOfYears;
    protected Integer numberOfMonths;

    public final static NSMutableArray _yearList=new NSMutableArray();
    static {
        for(int i = 0; i <100; i++){
            _yearList.addObject(ERXConstant.integerForInt(i));
        }
    }
    public NSArray yearList(){ return (NSArray)_yearList; }

    public final static NSMutableArray _monthList=new NSMutableArray();
    static {
        for(int i = 0; i <12; i++){
            _monthList.addObject(ERXConstant.integerForInt(i));
        }
    }
    public NSArray  monthList(){ return (NSArray)_monthList; }

    public Number totalNumberOfMonths() {
        return objectPropertyValue()!=null ?(Number)objectPropertyValue(): ERXConstant.ZeroInteger;
    }
    public Integer numberOfYears() {
        numberOfYears=(Integer)yearList().objectAtIndex((int)totalNumberOfMonths().intValue()/12);
        return numberOfYears;
    }

    public Integer numberOfMonths() {
        numberOfMonths=(Integer)monthList().objectAtIndex(totalNumberOfMonths().intValue() % 12);
        return numberOfMonths;
    }

    public void takeValuesFromRequest(WORequest q, WOContext c) throws NSValidation.ValidationException {
        super.takeValuesFromRequest(q,c);
        int yearMonths = numberOfMonths != null ? numberOfMonths.intValue() : 0;
        yearMonths += numberOfYears != null ? numberOfYears.intValue()*12 : 0;
        Integer months = ERXConstant.integerForInt(yearMonths);
        try {
            object().validateTakeValueForKeyPath(months,key());
        } catch(Throwable e) {
            validationFailedWithException (e, months, key());
        }
    }
}