/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr 
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */

/* ERGraphUtilities.java created by angela on Thu 01-Nov-2001 */
package er.extensions;

import com.webobjects.directtoweb.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import org.apache.log4j.Category;
import java.util.*;
import java.awt.*; //for pie charts

public class ERXGraphUtilities {

    public final static Category cat = Category.getInstance(ERXGraphUtilities.class);

    public static Integer fiftyOne = ERXConstant.integerForInt(51);
    public static Integer oneHundredTwo = ERXConstant.integerForInt(102);
    public static Integer oneHundredFiftyThree = ERXConstant.integerForInt(153);
    public static Integer oneHundredSixtySix = ERXConstant.integerForInt(166);
    public static Integer twoHundredFour = ERXConstant.integerForInt(204);
    public static Integer twoHundredFiftyFour = ERXConstant.integerForInt(254);
    public static Integer twoHundredFiftyFive = ERXConstant.integerForInt(255);

    public static NSArray orange = new NSArray(new Object[]{twoHundredFiftyFive,oneHundredTwo, ERXConstant.ZeroInteger});
    public static NSArray yellow = new NSArray(new Object[]{twoHundredFiftyFive,oneHundredFiftyThree,ERXConstant.ZeroInteger });
    public static NSArray blue = new NSArray(new Object[]{fiftyOne,oneHundredTwo,oneHundredFiftyThree});
    public static NSArray green = new NSArray(new Object[]{oneHundredTwo, oneHundredTwo, fiftyOne});
    public static NSArray grey = new NSArray(new Object[]{twoHundredFour,twoHundredFour,twoHundredFour});

    public static Color awtOrange = new Color(255, 102, 0);
    public static Color awtYellow = new Color(255, 153, 0);
    public static Color awtBlue = new Color(51, 102, 153);
    public static Color awtGreen = new Color(102, 102, 51);
    public static Color awtGrey = new Color(204, 204, 204);

    public static int computeSumForKey(NSArray values, String key) {
        int sum = 0;
        for(Enumeration e = values.objectEnumerator(); e.hasMoreElements();)
            sum += Integer.parseInt(((NSDictionary)e.nextElement()).objectForKey(key).toString());
        return sum;
    }

    //X-AXIS
        private static NSArray _lastNMonthsAsStringsArray;
        public static NSArray lastNMonthsAsStringsArray(int numberDesiredMonths) {
            if (_lastNMonthsAsStringsArray == null) {
                NSMutableArray result = new NSMutableArray();
                NSTimestamp today = new NSTimestamp();
                for (int i=1;i<=numberDesiredMonths;i++)
                    result.addObject(today.timestampByAddingGregorianUnits(0, (i * -1), 0, 0, 0, 0).toString());
                _lastNMonthsAsStringsArray = result;
                cat.debug("*********** result for lastNMonthsAsStringsArray = " + result);
            }
            return _lastNMonthsAsStringsArray;
        }

        private static NSArray _lastNMonthsArray;
        public static NSArray lastNMonthsArray(int numberDesiredMonths) {
            if (_lastNMonthsArray == null) {
                NSMutableArray result = new NSMutableArray();
                NSTimestamp today = new NSTimestamp();
                for (int i=1;i<=numberDesiredMonths;i++)
                    result.addObject(today.timestampByAddingGregorianUnits(0, (i * -1), 0, 0, 0, 0));
              //  cat.debug("*********** today = " + today);
               // cat.debug("*********** numberDesiredMonths = " + numberDesiredMonths);
                _lastNMonthsArray = result;
                cat.debug("*********** result for lastNMonthsArray = " + result);
            }
            return _lastNMonthsArray;
        }

}
