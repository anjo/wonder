//
// ERXNonPluralFormLocalizer.java
// Project ERExtensions
//
// Created by tatsuya on Wed May 01 2002
//
package er.extensions;

import com.webobjects.foundation.*;

/**
 *  ERXNonPluralFormLocalizer is a subclass of {@link ERXLocalizer}.<br/>
 *  <br/>
 *  Overrides <code>plurifiedString</code> from its super class 
 *  and cancels all plural form translations includind the one provided by 
 *  <code>plurifiedStringWithTemplateForKey</code>.
 *  <br/>
 *  Good for languages that don't have plural forms (such as Japanese).
 */
public class ERXNonPluralFormLocalizer extends ERXLocalizer {
    static final ERXLogger cat = ERXLogger.getLogger(ERXNonPluralFormLocalizer.class);

    public ERXNonPluralFormLocalizer(String aLanguage) { 
        super(aLanguage); 
    }
    
    public String plurifiedString(String name, int count) { return name; }

    public String toString() { return "<ERXNonPluralFormLocalizer "+language+">"; }
}
