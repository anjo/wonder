/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions.foundation;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOApplication;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSBundle;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSProperties;
import com.webobjects.foundation.NSPropertyListSerialization;

import er.extensions.appserver.ERXApplication;
import er.extensions.crypting.ERXCrypto;

/**
 * Collection of simple utility methods used to get and set properties
 * in the system properties. The only reason this class is needed is
 * because all of the methods in NSProperties have been deprecated.
 * This is a wee bit annoying. The usual method is to have a method
 * like <code>getBoolean</code> off of Boolean which would resolve
 * the System property as a Boolean object.
 * 
 * Properties can be set in all the following places:
 * <ul>
 * <li>Properties in a bundle Resources directory</li>
 * <li>Properties.dev in a bundle Resources directory</li>
 * <li>Properties.username in a bundle Resources directory </li>
 * <li>~/Library/WebObjects.properties file</li>
 * <li>in the eclipse launcher or on the command-line</li>
 * </ul>
 * 
 * TODO - If this would fallback to calling the System getProperty, we
 * could ask that Project Wonder frameworks only use this class.
 * 
 * @property er.extensions.ERXProperties.RetainDefaultsEnabled
 */
public class ERXProperties extends Properties implements NSKeyValueCoding {

    /** default string */
    public static final String DefaultString = "Default";
    
    private static Boolean RetainDefaultsEnabled;
    private static String UndefinedMarker = "-undefined-";
    /** logging support */
    public final static Logger log = Logger.getLogger(ERXProperties.class);
    private static final Map AppSpecificPropertyNames = new HashMap(128);

    /** WebObjects version number as string */
    private static String _webObjectsVersion;
    
    /** WebObjects version number as double */ 
    private static double _webObjectsVersionDouble;

    /** Internal cache of type converted values to avoid reconverting attributes that are asked for frequently */
    private static Map _cache = Collections.synchronizedMap(new HashMap());

    private static boolean retainDefaultsEnabled() {
        if (RetainDefaultsEnabled == null) {
            final String propertyValue = ERXSystem.getProperty("er.extensions.ERXProperties.RetainDefaultsEnabled", "false");
            final boolean isEnabled = "true".equals(propertyValue);
            RetainDefaultsEnabled = Boolean.valueOf(isEnabled);
        }
        return RetainDefaultsEnabled.booleanValue();
    }

    /**
     * Puts handy properties such as <code>com.webobjects.version</code> 
     * into the system properties. This method is called when 
     * the framework is initialized  
     * (when WOApplication.ApplicationWillFinishLaunchingNotification 
     * is posted.)
     */
    public static void populateSystemProperties() {
        System.setProperty("com.webobjects.version", webObjectsVersion());
    }

    /** 
     * Returns the version string of the application.  
     * It checks <code>CFBundleShortVersionString</code> property 
     * in the <code>info.plist</code> resource and returns 
     * a trimmed version of the value. 
     * 
     * @return version number as string; can be a null-string when 
     *          the applicaiton doesn't have the value of
     *          <code>CFBundleShortVersionString</code>
     *                  in its <code>info.plist</code> resource. 
     * @see #versionStringForFrameworkNamed
     * @see #webObjectsVersion
     */ 
    public static String versionStringForApplication() {
        NSBundle bundle = NSBundle.mainBundle();
        String versionString = (String) bundle.infoDictionary()
                                            .objectForKey("CFBundleShortVersionString");
        return versionString == null  ?  ""  :  versionString.trim(); // remove the line ending char
    }

    /** 
     * Returns the version string of the given framework.
     * It checks <code>CFBundleShortVersionString</code> property 
     * in the <code>info.plist</code> resource and returns 
     * a trimmed version of the value. 
     * 
     * @param  frameworkName name
     * @return version number as string; can be null-string when 
     *          the framework is not found or the framework
     *          doesn't have the value of
     *                  <code>CFBundleShortVersionString</code> in its
     *                  <code>info.plist</code> resource.
     * @see #versionStringForApplication
     * @see #webObjectsVersion
     * @see ERXStringUtilities#removeExtraDotsFromVersionString
     */ 
    public static String versionStringForFrameworkNamed(String frameworkName) {
        NSBundle bundle = NSBundle.bundleForName(frameworkName);
        if (bundle == null)  return "";
        
        String versionString = (String) bundle.infoDictionary()
                                            .objectForKey("CFBundleShortVersionString");
        return versionString == null  ?  ""  :  versionString.trim(); // trim() removes the line ending char
    }

    /**
     * Returns the version string of the given framework.
     * It checks <code>SourceVersion</code> property
     * in the <code>version.plist</code> resource and returns
     * a trimmed version of the value.
     *
     * @return version number as string; can be null-string when
     *          the framework is not found or the framework
     *          doesn't have the value of
     *                  <code>SourceVersion</code> in its
     *                  <code>info.plist</code> resource.
     * @see #versionStringForApplication
     * @see #webObjectsVersion
     */
    public static String sourceVersionString() {
        NSBundle bundle = NSBundle.bundleForName("JavaWebObjects");
        if (bundle == null)  return "";
        String dictString = new String(bundle.bytesForResourcePath("version.plist"));
        NSDictionary versionDictionary = NSPropertyListSerialization.dictionaryForString(dictString);

        String versionString = (String) versionDictionary.objectForKey("SourceVersion");
        return versionString == null  ?  ""  :  versionString.trim(); // trim() removes the line ending char
    }

    /** 
     * Returns WebObjects version as string. If it's one of those 
     * version 5.1s (5.1, 5.1.1, 5.1.2...), this method will only 
     * return 5.1. If it's 5.2s, this mothod will return more precise 
     * version numbers such as 5.2.1. Note that version 5.0 series 
     * is not supported and may return incorrect version numbers 
     * (it will return 5.1). 
     * 
     * @return WebObjects version number as string
     * @see #webObjectsVersionAsDouble
     * @see ERXStringUtilities#removeExtraDotsFromVersionString
     */ 
    public static String webObjectsVersion() {
        if (_webObjectsVersion == null) {
            _webObjectsVersion = versionStringForFrameworkNamed("JavaWebObjects");
            
            // if _webObjectsVersion is a null-string, we assume it's WebObjects 5.1.x
            if (_webObjectsVersion.equals("")) 
                _webObjectsVersion = "5.1";
        }
        return _webObjectsVersion;
    }

    /** 
     * Returns WebObjects version as double. If it's one of those 
     * version 5.1s (5.1, 5.1.1, 5.1.2...), this method will only 
     * return 5.1. If it's 5.2s, this mothod will return more precise 
     * version numbers such as 5.2.1. Note that version 5.0 series 
     * is not supported and may return incorrect version numbers 
     * (it will return 5.1). 
     * 
     * @return WebObjects version number as double
     * @see #webObjectsVersion
     */
    public static double webObjectsVersionAsDouble() {
        if (_webObjectsVersionDouble == 0.0d) {
            String woVersionString = ERXStringUtilities.removeExtraDotsFromVersionString(webObjectsVersion());
            try {
                _webObjectsVersionDouble = Double.parseDouble(woVersionString);
            } catch (NumberFormatException ex) {
                log.error("An exception occurred while parsing webObjectVersion " + woVersionString 
                    + " as a double value: " + ex.getClass().getName() + " " + ex.getMessage());
            }
        }
        return _webObjectsVersionDouble;
    }

    /**
     * Quick convience method used to determine if the current
     * webobjects version is 5.2 or higher.
     * @return if the version of webobjects is 5.2 or better
     */
    public static boolean webObjectsVersionIs52OrHigher() {
        if(ERXProperties.booleanForKey("er.extensions.ERXProperties.checkOldVersions")) {
            return webObjectsVersionAsDouble() >= 5.2d;
        }
        return true;
    }

    /**
     * Quick convience method used to determine if the current
     * webobjects version is 5.22 or higher.
     * @return if the version of webobjects is 5.22 or better
     */
    public static boolean webObjectsVersionIs522OrHigher() {
        if(ERXProperties.booleanForKey("er.extensions.ERXProperties.checkOldVersions")) {
            String webObjectsVersion = webObjectsVersion();
            if("5.2".equals(webObjectsVersion)) {
                String sourceVersion = sourceVersionString();
                if("9260000".equals(sourceVersion)) {
                    return true;
                }
            }
            return webObjectsVersionAsDouble() >= 5.22d;
        }
        return true;
    }

    
    /**
     * Cover method for returning an NSArray for a
     * given system property.
     * @param s system property
     * @return array de-serialized from the string in
     *      the system properties
     */
    public static NSArray arrayForKey(String s) {
        return arrayForKeyWithDefault(s, null);
    }

    /**
     * Converts the standard propertyName into one with a .&lt;AppName> on the end, if the property is defined with
     * that suffix.  If not, then this caches the standard propertyName.  A cache is maintained to avoid concatenating
     * strings frequently, but may be overkill since most usage of this system doesn't involve frequent access.
     * @param propertyName
     */
    private static String getApplicationSpecificPropertyName(final String propertyName) {
        synchronized(AppSpecificPropertyNames) {
            // only keep 128 of these around
            if (AppSpecificPropertyNames.size() > 128) {
                AppSpecificPropertyNames.clear();
            }
            String appSpecificPropertyName = (String)AppSpecificPropertyNames.get(propertyName);
            if (appSpecificPropertyName == null) {
                final WOApplication application = WOApplication.application();
                if (application != null) {
                    final String appName = application.name();
                    appSpecificPropertyName = propertyName + "." + appName;
                }
                else {
                    appSpecificPropertyName = propertyName;
                }
                final String propertyValue = ERXSystem.getProperty(appSpecificPropertyName);
                if (propertyValue == null) {
                    appSpecificPropertyName = propertyName;
                }
                AppSpecificPropertyNames.put(propertyName, appSpecificPropertyName);
            }
            return appSpecificPropertyName;
        }
    }

    /**
     * Cover method for returning an NSArray for a
     * given system property and set a default value if not given.
     * @param s system property
     * @param defaultValue default value
     * @return array de-serialized from the string in
     *      the system properties or default value
     */
    public static NSArray arrayForKeyWithDefault(final String s, final NSArray defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);
		NSArray value;
		Object cachedValue = _cache.get(propertyName);
		if (cachedValue == UndefinedMarker) {
			value = defaultValue;
		} else if (cachedValue instanceof NSArray) {
			value = (NSArray) cachedValue;
		} else {
			value = ERXValueUtilities.arrayValueWithDefault(ERXSystem.getProperty(propertyName), null);
			_cache.put(s, value == null ? (Object)UndefinedMarker : value);
			if (value == null) {
				value = defaultValue;
			}
	        if (retainDefaultsEnabled() && value == null && defaultValue != null) {
	            setArrayForKey(defaultValue, propertyName);
	        }
		}
		return value;
    }
    
    /**
     * Cover method for returning a boolean for a
     * given system property. This method uses the
     * method <code>booleanValue</code> from
     * {@link ERXUtilities}.
     * @param s system property
     * @return boolean value of the string in the
     *      system properties.
     */    
    public static boolean booleanForKey(String s) {
        return booleanForKeyWithDefault(s, false);
    }

    /**
     * Cover method for returning a boolean for a
     * given system property or a default value. This method uses the
     * method <code>booleanValue</code> from
     * {@link ERXUtilities}.
     * @param s system property
     * @param defaultValue default value
     * @return boolean value of the string in the
     *      system properties.
     */
    public static boolean booleanForKeyWithDefault(final String s, final boolean defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);
        boolean value;
		Object cachedValue = _cache.get(propertyName);
		if (cachedValue == UndefinedMarker) {
			value = defaultValue;
		} else if (cachedValue instanceof Boolean) {
			value = ((Boolean) cachedValue).booleanValue();
		} else {
			Boolean objValue = ERXValueUtilities.BooleanValueWithDefault(ERXSystem.getProperty(propertyName), null);
			_cache.put(propertyName, objValue == null ? (Object)UndefinedMarker : objValue);
			if (objValue == null) {
				value = defaultValue;
			} else {
				value = objValue.booleanValue();
			}
	        if (retainDefaultsEnabled() && objValue == null) {
	            System.setProperty(propertyName, Boolean.toString(defaultValue));
	        }
		}
		return value;
    }
    
    /**
     * Cover method for returning an NSDictionary for a
     * given system property.
     * @param s system property
     * @return dictionary de-serialized from the string in
     *      the system properties
     */    
    public static NSDictionary dictionaryForKey(String s) {
        return dictionaryForKeyWithDefault(s, null);
    }

    /**
     * Cover method for returning an NSDictionary for a
     * given system property or the default value.
     * @param s system property
     * @param defaultValue default value
     * @return dictionary de-serialized from the string in
     *      the system properties
     */
    public static NSDictionary dictionaryForKeyWithDefault(final String s, final NSDictionary defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);
		NSDictionary value;
		Object cachedValue = _cache.get(propertyName);
		if (cachedValue == UndefinedMarker) {
			value = defaultValue;
		} else if (cachedValue instanceof NSDictionary) {
			value = (NSDictionary) cachedValue;
		} else {
			value = ERXValueUtilities.dictionaryValueWithDefault(ERXSystem.getProperty(propertyName), null);
			_cache.put(s, value == null ? (Object)UndefinedMarker : value);
			if (value == null) {
				value = defaultValue;
			}
	        if (retainDefaultsEnabled() && value == null && defaultValue != null) {
	            setDictionaryForKey(defaultValue, propertyName);
	        }
		}
		return value;
    }

    /**
     * Cover method for returning an int for a
     * given system property.
     * @param s system property
     * @return int value of the system property or 0
     */
    public static int intForKey(String s) {
        return intForKeyWithDefault(s, 0);
    }

    /**
     * Cover method for returning a long for a
     * given system property.
     * @param s system property
     * @return long value of the system property or 0
     */
    public static long longForKey(String s) {
        return longForKeyWithDefault(s, 0);
    }

    /**
     * Cover method for returning a float for a
     * given system property.
     * @param s system property
     * @return float value of the system property or 0
     */
    public static float floatForKey(String s) {
        return floatForKeyWithDefault(s, 0);
    }

    /**
     * Cover method for returning a double for a
     * given system property.
     * @param s system property
     * @return double value of the system property or 0
     */
    public static double doubleForKey(String s) {
        return doubleForKeyWithDefault(s, 0);
    }

    /**
     * Cover method for returning a BigDecimal for a
     * given system property. This method uses the
     * method <code>bigDecimalValueWithDefault</code> from
     * {@link ERXValueUtilities}.
     * @param s system property
     * @return bigDecimal value of the string in the
     *      system properties.  Scale is controlled by the string, ie "4.400" will have a scale of 3.
     */
    public static BigDecimal bigDecimalForKey(String s) {
        return bigDecimalForKeyWithDefault(s,null);
    }

    /**
     * Cover method for returning a BigDecimal for a
     * given system property or a default value. This method uses the
     * method <code>bigDecimalValueWithDefault</code> from
     * {@link ERXValueUtilities}.
     * @param s system property
     * @param defaultValue default value
     * @return BigDecimal value of the string in the
     *      system properties. Scale is controlled by the string, ie "4.400" will have a scale of 3.
     */
    public static BigDecimal bigDecimalForKeyWithDefault(String s, BigDecimal defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);

        Object value = _cache.get(propertyName);
        if (value == UndefinedMarker) {
            return defaultValue;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal)value;
        }
        
        String propertyValue = ERXSystem.getProperty(propertyName);
        final BigDecimal bigDecimal = ERXValueUtilities.bigDecimalValueWithDefault(propertyValue, defaultValue);
        if (retainDefaultsEnabled() && propertyValue == null && bigDecimal != null) {
            propertyValue = bigDecimal.toString();
            System.setProperty(propertyName, propertyValue);
        }
        _cache.put(propertyName, propertyValue == null ? (Object)UndefinedMarker : bigDecimal);
        return bigDecimal;
    }

    /**
     * Cover method for returning an int for a
     * given system property with a default value.
     * @param s system property
     * @param defaultValue default value
     * @return int value of the system property or the default value
     */    
    public static int intForKeyWithDefault(final String s, final int defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);
		int value;
		Object cachedValue = _cache.get(propertyName);
		if (cachedValue == UndefinedMarker) {
			value = defaultValue;
		} else if (cachedValue instanceof Integer) {
			value = ((Integer) cachedValue).intValue();
		} else {
			Integer objValue = ERXValueUtilities.IntegerValueWithDefault(ERXSystem.getProperty(propertyName), null);
			_cache.put(s, objValue == null ? (Object)UndefinedMarker : objValue);
			if (objValue == null) {
				value = defaultValue;
			} else {
				value = objValue.intValue();
			}
	        if (retainDefaultsEnabled() && objValue == null) {
	            System.setProperty(propertyName, Integer.toString(defaultValue));
	        }
		}
		return value;
    }

    /**
     * Cover method for returning a long for a
     * given system property with a default value.
     * @param s system property
     * @param defaultValue default value
     * @return long value of the system property or the default value
     */    
    public static long longForKeyWithDefault(final String s, final long defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);
		long value;
		Object cachedValue = _cache.get(propertyName);
		if (cachedValue == UndefinedMarker) {
			value = defaultValue;
		} else if (cachedValue instanceof Long) {
			value = ((Long) cachedValue).longValue();
		} else {
			Long objValue = ERXValueUtilities.LongValueWithDefault(ERXSystem.getProperty(propertyName), null);
			_cache.put(s, objValue == null ? (Object)UndefinedMarker : objValue);
			if (objValue == null) {
				value = defaultValue;
			} else {
				value = objValue.longValue();
			}
	        if (retainDefaultsEnabled() && objValue == null) {
	            System.setProperty(propertyName, Long.toString(defaultValue));
	        }
		}
		return value;
    }

    /**
     * Cover method for returning a float for a
     * given system property with a default value.
     * @param s system property
     * @param defaultValue default value
     * @return float value of the system property or the default value
     */    
    public static float floatForKeyWithDefault(final String s, final float defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);

		float value;
		Object cachedValue = _cache.get(propertyName);
		if (cachedValue == UndefinedMarker) {
			value = defaultValue;
		} else if (cachedValue instanceof Float) {
			value = ((Float) cachedValue).floatValue();
		} else {
			Float objValue = ERXValueUtilities.FloatValueWithDefault(ERXSystem.getProperty(propertyName), null);
			_cache.put(s, objValue == null ? (Object)UndefinedMarker : objValue);
			if (objValue == null) {
				value = defaultValue;
			} else {
				value = objValue.floatValue();
			}
	        if (retainDefaultsEnabled() && objValue == null) {
	            System.setProperty(propertyName, Float.toString(defaultValue));
	        }
		}
		return value;
    }

    /**
     * Cover method for returning a double for a
     * given system property with a default value.
     * @param s system property
     * @param defaultValue default value
     * @return double value of the system property or the default value
     */    
    public static double doubleForKeyWithDefault(final String s, final double defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);

		double value;
		Object cachedValue = _cache.get(propertyName);
		if (cachedValue == UndefinedMarker) {
			value = defaultValue;
		} else if (cachedValue instanceof Double) {
			value = ((Double) cachedValue).doubleValue();
		} else {
			Double objValue = ERXValueUtilities.DoubleValueWithDefault(ERXSystem.getProperty(propertyName), null);
			_cache.put(s, objValue == null ? (Object)UndefinedMarker : objValue);
			if (objValue == null) {
				value = defaultValue;
			} else {
				value = objValue.doubleValue();
			}
	        if (retainDefaultsEnabled() && objValue == null) {
	            System.setProperty(propertyName, Double.toString(defaultValue));
	        }
		}
		return value;
    }
    
    /**
     * Returning an string for a given system 
     * property. This is a cover method of 
     * {@link java.lang.System#getProperty}
     * @param s system property
     * @return string value of the system propery or null
     */
    public static String stringForKey(String s) {
        return stringForKeyWithDefault(s, null);
    }

    /**
     * Returning an string for a given system
     * property. This is a cover method of
     * {@link java.lang.System#getProperty}
     * @param s system property
     * @return string value of the system propery or null
     */
    public static String stringForKeyWithDefault(final String s, final String defaultValue) {
        final String propertyName = getApplicationSpecificPropertyName(s);
        final String propertyValue = ERXSystem.getProperty(propertyName);
        final String stringValue = propertyValue == null ? defaultValue : propertyValue;
        if (retainDefaultsEnabled() && propertyValue == null) {
            System.setProperty(propertyName, stringValue == null ? UndefinedMarker : stringValue);
        }
        return stringValue == UndefinedMarker ? null : stringValue;
    }
    
    /**
     * Returns the decrypted value for the given property name using
     * the default crypter if the property propertyName.encrypted=true.  For
     * instance, if you are requesting my.password, if my.password.encrypted=true
     * the value of my.password will be passed to the default crypter's decrypt
     * method.
     * 
     * @param propertyName the property name to retrieve and optionally decrypt
     * @return the decrypted property value
     */
    public static String decryptedStringForKey(String propertyName) {
    	return ERXProperties.decryptedStringForKeyWithDefault(propertyName, null);
    }
    
    /**
     * If the <code>propertyName.encrypted</code> property is set to true, returns
     * the plain text value of the given property name, after decrypting it with the
     * {@link ERXCrypto.defaultCrypter}. For instance, if you are requesting
     * my.password and <code>my.password.encrypted</code> is set to true,
     * the value of <code>my.password</code> will be sent to the default crypter's
     * decrypt() method.
     * 
     * @param propertyName the property name to retrieve and optionally decrypt
     * @param defaultValue the default value to return if there is no password
     * @return the decrypted property value
     */
    public static String decryptedStringForKeyWithDefault(String propertyName, String defaultValue) {
		boolean propertyNameEncrypted = ERXProperties.booleanForKeyWithDefault(propertyName + ".encrypted", false);
		String decryptedPassword;
		if (propertyNameEncrypted) {
			String encryptedPassword = ERXProperties.stringForKey(propertyName);
			decryptedPassword = ERXCrypto.defaultCrypter().decrypt(encryptedPassword);
		}
		else {
			decryptedPassword = ERXProperties.stringForKey(propertyName);
		}
		if (decryptedPassword == null) {
			decryptedPassword = defaultValue;
		}
		return decryptedPassword;
    }

    /**
     * Returns the decrypted value for the given property name using the
     * {@link ERXCrypto.defaultCrypter}. This is slightly different than
     * decryptedStringWithKeyWithDefault in that it does not require  the encrypted
     * property to be set.
     *  
     * @param propertyName the name of the property to decrypt
     * @param defaultValue the default encrypted value
     * @return the decrypted value
     */
    public static String decryptedStringForKeyWithEncryptedDefault(String propertyName, String defaultValue) {
    	String encryptedPassword = ERXProperties.stringForKeyWithDefault(propertyName, defaultValue);
    	return ERXCrypto.defaultCrypter().decrypt(encryptedPassword);
    }

    /**
     * Returns an array of strings separated with the given separator string.
     * 
     * @param key the key to lookup
     * @param separator the separator (",")
     * @return the array of strings or NSArray.EmptyArray if not found
     */
    @SuppressWarnings("unchecked")
    public static NSArray<String> componentsSeparatedByString(String key, String separator) {
    	return ERXProperties.componentsSeparatedByStringWithDefault(key, separator, (NSArray<String>)NSArray.EmptyArray);
    }

    /**
     * Returns an array of strings separated with the given separator string.
     * 
     * @param key the key to lookup
     * @param separator the separator (",")
     * @param defaultValue the default array to return if there is no value
     * @return the array of strings
     */
    @SuppressWarnings("unchecked")
	public static NSArray<String> componentsSeparatedByStringWithDefault(String key, String separator, NSArray<String> defaultValue) {
    	NSArray<String> array;
    	String str = stringForKeyWithDefault(key, null);
    	if (str == null) {
    		array = defaultValue;
    	}
    	else {
    		array = (NSArray<String>)NSArray.componentsSeparatedByString(str, separator);
    	}
    	return array;
    }
    
    /**
     * Sets an array in the System properties for
     * a particular key.
     * @param array to be set in the System properties
     * @param key to be used to get the value
     */
    public static void setArrayForKey(NSArray array, String key) {
        setStringForKey(NSPropertyListSerialization.stringFromPropertyList(array), key);
    }

    /**
     * Sets a dictionary in the System properties for
     * a particular key.
     * @param dictionary to be set in the System properties
     * @param key to be used to get the value
     */    
    public static void setDictionaryForKey(NSDictionary dictionary, String key) {
        setStringForKey(NSPropertyListSerialization.stringFromPropertyList(dictionary), key);
    }

    /**
     * Sets a string in the System properties for
     * another string.
     * @param string to be set in the System properties
     * @param key to be used to get the value
     */
    // DELETEME: Really not needed anymore -- MS: Why?  We need the cache clearing.
    public static void setStringForKey(String string, String key) {
        System.setProperty(key, string);
        _cache.remove(key);
    }
    
    public static void removeKey(String key) {
    	System.getProperties().remove(key);
    	_cache.remove(key);
    }
    
    /** 
     * Copies all properties from source to dest. 
     * 
     * @param source  properties copied from 
     * @param dest  properties copied to
     */
    public static void transferPropertiesFromSourceToDest(Properties source, Properties dest) {
        if (source != null) {
            dest.putAll(source);
            if (dest == System.getProperties()) {
                systemPropertiesChanged();
            }
        }
    }
    

    
    /**
     * Reads a Java properties file at the given path 
     * and returns a {@link java.util.Properties Properties} object 
     * as the result. If the file does not exist, returns 
     * an empty properties object. 
     * 
     * @param path  file path to the properties file
     * @return properties object with the values from the file
     *      specified.
     */
    // FIXME: This shouldn't eat the exception
    public static Properties propertiesFromPath(String path) {
    	ERXProperties._Properties prop = new ERXProperties._Properties();

        if (path == null  ||  path.length() == 0) {
            log.warn("Attempting to read property file for null file path");
            return prop;
        }

        File file = new File(path);
        if (! file.exists()  ||  ! file.isFile()  ||  ! file.canRead()) {
            log.warn("File " + path + " doesn't exist or can't be read.");
            return prop;
        }

        try {
        	prop.load(file);
            log.debug("Loaded configuration file at path: "+ path);
        } catch (IOException e) {
            log.error("Unable to initialize properties from file \"" + path + "\"", e);
        }
        return prop;
    }

    /**
     * Gets the properties for a given file.
     * 
     * @param file the properties file
     * @return properties from the given file
     * @throws java.io.IOException if the file is not found or cannot be read
     */
    public static Properties propertiesFromFile(File file) throws java.io.IOException {
        if (file == null)
            throw new IllegalStateException("Attempting to get properties for a null file!");
        ERXProperties._Properties prop = new ERXProperties._Properties();
        prop.load(file);
        return prop;
    }
    
    /**
     * Sets and returns properties object with the values from 
     * the given command line arguments string array. 
     * 
     * @param argv  string array typically provided by 
     *               the command line arguments
     * @return properties object with the values from 
     *          the argv
     */
    public static Properties propertiesFromArgv(String[] argv) {
    	ERXProperties._Properties properties = new ERXProperties._Properties();
        NSDictionary argvDict = NSProperties.valuesFromArgv(argv);
        Enumeration e = argvDict.allKeys().objectEnumerator();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            properties.put(key, argvDict.objectForKey(key));
        }
        return properties;
    }

    /** 
     * Returns an array of paths to the <code>Properties</code> and 
     * <code>WebObjects.properties</code> files contained in the 
     * application/framework bundles and home directory. 
     * <p>
     * If ProjectBuilder (for Mac OS X) has the project opened, 
     * it will attempt to get the path to the one in the project 
     * directory instead of the one in the bundle. 
     * <p>
     * This opened project detection feature is pretty fragile and 
     * will change between versions of the dev-tools.
     * 
     * @return paths to Properties files
     */
    public static NSArray pathsForUserAndBundleProperties() {
        return pathsForUserAndBundleProperties(false);
    }

    private static void addIfPresent(String info, String path, NSMutableArray<String> propertiesPaths, NSMutableArray<String> projectsInfo) {
    	if(path != null && path.length() > 0) {
    		path = getActualPath(path);
    		if(propertiesPaths.containsObject(path)) {
    			log.error("Path was already included: " + path + "");
    		}
    		projectsInfo.addObject("  " + info +" -> " + path);
    		propertiesPaths.addObject(path);
    	}
    }
    
    public static NSArray pathsForUserAndBundleProperties(boolean reportLoggingEnabled) {
        NSMutableArray<String> propertiesPaths = new NSMutableArray();
        NSMutableArray<String> projectsInfo = new NSMutableArray();

        /*  Properties for frameworks */
        NSArray frameworkNames = (NSArray) NSBundle.frameworkBundles().valueForKey("name");
        Enumeration e = frameworkNames.reverseObjectEnumerator();
        while (e.hasMoreElements()) {
        	String frameworkName = (String) e.nextElement();

        	String propertyPath = ERXFileUtilities.pathForResourceNamed("Properties", frameworkName, null);
        	addIfPresent(frameworkName + ".framework", propertyPath, propertiesPaths, projectsInfo);

        	/** Properties.dev -- per-Framework-dev properties 
        	 * This adds support for Properties.dev in your Frameworks new load order will be
        	 */
        	String devPropertiesPath = ERXApplication.isDevelopmentModeSafe() ? ERXProperties.variantPropertiesInBundle("dev", frameworkName) : null;
        	addIfPresent(frameworkName + ".framework.dev", devPropertiesPath, propertiesPaths, projectsInfo);
        	
        	/** Properties.<userName> -- per-Framework-per-User properties */
        	String userPropertiesPath = ERXProperties.variantPropertiesInBundle(ERXSystem.getProperty("user.name"), frameworkName);
        	addIfPresent(frameworkName + ".framework.user", userPropertiesPath, propertiesPaths, projectsInfo);
        }

		NSBundle mainBundle = NSBundle.mainBundle();
		
		if( mainBundle != null ) {
	        String mainBundleName = mainBundle.name();
	
	        String appPath = ERXFileUtilities.pathForResourceNamed("Properties", "app", null);
	    	addIfPresent(mainBundleName + ".app", appPath, propertiesPaths, projectsInfo);
		}

		/*  WebObjects.properties in the user home directory */
		String userHome = ERXSystem.getProperty("user.home");
		if (userHome != null && userHome.length() > 0) {
			File file = new File(userHome, "WebObjects.properties");
			if (file.exists() && file.isFile() && file.canRead()) {
				try {
					String userHomePath = file.getCanonicalPath();
			    	addIfPresent("{$user.home}/WebObjects.properties", userHomePath, propertiesPaths, projectsInfo);
				}
				catch (java.io.IOException ex) {
					ERXProperties.log.error("Failed to load the configuration file '" + file.getAbsolutePath() + "'.", ex);
				}
			}
        }

		/*  Optional properties files */
		if (optionalConfigurationFiles() != null && optionalConfigurationFiles().count() > 0) {
			for (Enumeration configEnumerator = optionalConfigurationFiles().objectEnumerator(); configEnumerator.hasMoreElements();) {
				String configFile = (String) configEnumerator.nextElement();
				File file = new File(configFile);
				if (file.exists() && file.isFile() && file.canRead()) {
					try {
						String optionalPath = file.getCanonicalPath();
				    	addIfPresent("Optional Configuration", optionalPath, propertiesPaths, projectsInfo);
					}
					catch (java.io.IOException ex) {
						ERXProperties.log.error("Failed to load configuration file '" + file.getAbsolutePath() + "'.", ex);
					}
				}
				else {
					ERXProperties.log.error("The optional configuration file '" + file.getAbsolutePath() + "' either does not exist or could not be read.");
				}
			}
		}

        /** /etc/WebObjects/AppName/Properties -- per-Application-per-Machine properties */
        String applicationMachinePropertiesPath = ERXProperties.applicationMachinePropertiesPath("Properties");
    	addIfPresent("Application-Machine Properties", applicationMachinePropertiesPath, propertiesPaths, projectsInfo);

        /** Properties.dev -- per-Application-dev properties */
        String applicationDeveloperPropertiesPath = ERXProperties.applicationDeveloperProperties();
    	addIfPresent("Application-Developer Properties", applicationDeveloperPropertiesPath, propertiesPaths, projectsInfo);

        /** Properties.<userName> -- per-Application-per-User properties */
        String applicationUserPropertiesPath = ERXProperties.applicationUserProperties();
    	addIfPresent("Application-User Properties", applicationUserPropertiesPath, propertiesPaths, projectsInfo);
        
        /*  Report the result */
		if (reportLoggingEnabled && projectsInfo.count() > 0 && log.isInfoEnabled()) {
			StringBuffer message = new StringBuffer();
			message.append("\n\n").append("ERXProperties has found the following Properties files: \n");
			message.append(projectsInfo.componentsJoinedByString("\n"));
			message.append("\n");
			message.append("ERXProperties currently has the following properties:\n");
			message.append(ERXProperties.logString(ERXSystem.getProperties()));
			// ERXLogger.configureLoggingWithSystemProperties();
			log.info(message.toString());
		}

    	return propertiesPaths.immutableClone();
    }

    /**
     * Apply the current configuration to the supplied properties.
     * @param source
     * @param commandLine
     */
    public static Properties applyConfiguration(Properties source, Properties commandLine) {

    	Properties dest = source != null ? (Properties) source.clone() : new Properties();
    	NSArray additionalConfigurationFiles = ERXProperties.pathsForUserAndBundleProperties(false);

    	if (additionalConfigurationFiles.count() > 0) {
    		for (Enumeration configEnumerator = additionalConfigurationFiles.objectEnumerator(); configEnumerator.hasMoreElements();) {
    			String configFile = (String)configEnumerator.nextElement();
    			File file = new File(configFile);
    			if (file.exists() && file.isFile() && file.canRead()) {
    				try {
    					Properties props = ERXProperties.propertiesFromFile(file);
    					if(log.isDebugEnabled()) {
    						log.debug("Loaded: " + file + "\n" + ERXProperties.logString(props));
    					}
    					ERXProperties.transferPropertiesFromSourceToDest(props, dest);
    				} catch (java.io.IOException ex) {
    					log.error("Unable to load optional configuration file: " + configFile, ex);
    				}
    			}
    			else {
    				ERXConfigurationManager.log.error("The optional configuration file '" + file.getAbsolutePath() + "' either does not exist or cannot be read.");
    			}
    		}
    	}

    	if(commandLine != null) {
    		ERXProperties.transferPropertiesFromSourceToDest(commandLine, dest);
    	}
		return dest;
    	
    }

    /**
     * Returns all of the properties in the system mapped to their evaluated values, sorted by key.
     * 
     * @param protectValues if true, keys with the word "password" in them will have their values removed 
     * @return all of the properties in the system mapped to their evaluated values, sorted by key
     */
    public static Map<String, String> allPropertiesMap(boolean protectValues) {
    	return propertiesMap(ERXSystem.getProperties(), protectValues);
    }

    /**
     * Returns all of the properties in the system mapped to their evaluated values, sorted by key.
     * 
     * @param protectValues if true, keys with the word "password" in them will have their values removed 
     * @return all of the properties in the system mapped to their evaluated values, sorted by key
     */
    public static Map<String, String> propertiesMap(Properties properties, boolean protectValues) {
    	Map<String, String> props = new TreeMap<String, String>();
    	for (Enumeration e = properties.keys(); e.hasMoreElements();) {
    		String key = (String) e.nextElement();
    		if (protectValues && key.toLowerCase().contains("password")) {
    			props.put(key, "<deleted for log>");
    		}
    		else {
    			props.put(key, String.valueOf(properties.getProperty(key)));
    		}
    	}
    	return props;
    }
    
    /**
     * Returns a string suitable for logging.
     * @param properties
     */
    public static String logString(Properties properties) {
    	StringBuffer message = new StringBuffer();
        for (Map.Entry<String, String> entry : propertiesMap(properties, true).entrySet()) {
        	message.append("  " + entry.getKey() + "=" + entry.getValue() + "\n");
        }
        return message.toString();
    }
    
    public static class Property {
    	public String key, value;
    	public Property(String key, String value) {
    		this.key = key;
    		this.value = value;
    	}
    	public String toString() {
    		return key + " = " + value;
    	}
    }

    public static NSArray<Property> allProperties() {
    	NSMutableArray props = new NSMutableArray();
    	for (Enumeration e = ERXSystem.getProperties().keys(); e.hasMoreElements();) {
    		String key = (String) e.nextElement();
    		String object = "" + ERXSystem.getProperty(key);
    		props.addObject(new Property(key, object));
    	}
    	return (NSArray) props.valueForKey("@sortAsc.key");
     }

    /** 
     * Returns the full path to the Properties file under the 
     * given project path. At the current implementation, 
     * it looks for /Properties and /Resources/Properties. 
     * If the Properties file doesn't exist, returns null.  
     * 
     * @param projectPath  string to the project root directory
     * @return  the path to the Properties file if it exists
     */
    public static String pathForPropertiesUnderProjectPath(String projectPath) {
        String path = null; 
        final NSArray supportedPropertiesPaths = new NSArray(new Object[] 
                                        {"/Properties", "/Resources/Properties"});
        Enumeration e = supportedPropertiesPaths.objectEnumerator();
        while (e.hasMoreElements()) {
            File file = new File(projectPath + (String) e.nextElement());
            if (file.exists()  &&  file.isFile()  &&  file.canRead()) {
                try {
                    path = file.getCanonicalPath();
                } catch (IOException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage());
                }
                break;
            }
        }
        return path;
    }
    
    /**
     * Returns the application-specific user properties.
     */
    public static String applicationDeveloperProperties() {
    	String applicationDeveloperPropertiesPath = null;
    	if (ERXApplication.isDevelopmentModeSafe()) {
	        String devName = ERXSystem.getProperty("er.extensions.ERXProperties.devPropertiesName", "dev");
	        applicationDeveloperPropertiesPath = variantPropertiesInBundle(devName, "app");
    	}
        return applicationDeveloperPropertiesPath;
    }
    
    /**
     * Returns the application-specific variant properties for the given bundle.
     */
    public static String variantPropertiesInBundle(String userName, String bundleName) {
    	String applicationUserPropertiesPath = null;
        if (userName != null  &&  userName.length() > 0) { 
        	String resourceApplicationUserPropertiesPath = ERXFileUtilities.pathForResourceNamed("Properties." + userName, bundleName, null);
            if (resourceApplicationUserPropertiesPath != null) {
            	applicationUserPropertiesPath = ERXProperties.getActualPath(resourceApplicationUserPropertiesPath);
            }
        }
        return applicationUserPropertiesPath;
    }

    /**
     * Returns the application-specific user properties.
     */
    public static String applicationUserProperties() {
    	return variantPropertiesInBundle(ERXSystem.getProperty("user.name"), "app");
    }
    
    /**
     * Returns the path to the application-specific system-wide file "fileName".  By default this path is /etc/WebObjects, 
     * and the application name will be appended.  For instance, if you are asking for the MyApp Properties file for the
     * system, it would go in /etc/WebObjects/MyApp/Properties.
     * 
     * @return the path, or null if the path does not exist
     */
    public static String applicationMachinePropertiesPath(String fileName) {
    	String applicationMachinePropertiesPath = null;
    	String machinePropertiesPath = ERXSystem.getProperty("er.extensions.ERXProperties.machinePropertiesPath", "/etc/WebObjects");
    	WOApplication application = WOApplication.application();
    	String applicationName;
    	if (application != null) {
    		applicationName = application.name();
    	}
    	else {
    		applicationName = ERXSystem.getProperty("WOApplicationName");
    		if (applicationName == null) {
    			NSBundle mainBundle = NSBundle.mainBundle();
    			if (mainBundle != null) {
    				applicationName = mainBundle.name();
    			}
    			if (applicationName == null) {
    				applicationName = "Unknown";
    			}
    		}
    	}
    	File applicationPropertiesFile = new File(machinePropertiesPath + File.separator + fileName);
    	if (!applicationPropertiesFile.exists()) {
    		applicationPropertiesFile = new File(machinePropertiesPath + File.separator + applicationName + File.separator + fileName);
    	}
    	if (applicationPropertiesFile.exists()) {
    		try {
    			applicationMachinePropertiesPath = applicationPropertiesFile.getCanonicalPath();
    		}
    		catch (IOException e) {
    			ERXProperties.log.error("Failed to load machine Properties file '" + fileName + "'.", e);
    		}
    	}
    	return applicationMachinePropertiesPath;
    }

    /**
     * Gets an array of optionally defined configuration files.  For each file, if it does not
     * exist as an absolute path, ERXProperties will attempt to resolve it as an application resource
     * and use that instead.
     *  
     * @return array of configuration file names
     */
    public static NSArray optionalConfigurationFiles() {
    	NSArray immutableOptionalConfigurationFiles = arrayForKey("er.extensions.ERXProperties.OptionalConfigurationFiles");
    	NSMutableArray optionalConfigurationFiles = null;
    	if (immutableOptionalConfigurationFiles != null) {
    		optionalConfigurationFiles = immutableOptionalConfigurationFiles.mutableClone();
	    	for (int i = 0; i < optionalConfigurationFiles.count(); i ++) {
	    		String optionalConfigurationFile = (String)optionalConfigurationFiles.objectAtIndex(i);
	    		if (!new File(optionalConfigurationFile).exists()) {
		        	String resourcePropertiesPath = ERXFileUtilities.pathForResourceNamed(optionalConfigurationFile, "app", null);
		        	if (resourcePropertiesPath != null) {
		            	optionalConfigurationFiles.replaceObjectAtIndex(ERXProperties.getActualPath(resourcePropertiesPath), i);
		        	}
	    		}
	    	}
    	}
    	return optionalConfigurationFiles;
    }
    
    /**
     * Returns actual full path to the given file system path  
     * that could contain symbolic links. For example: 
     * /Resources will be converted to /Versions/A/Resources
     * when /Resources is a symbolic link.
     * 
     * @param path  path string to a resource that could 
     *               contain symbolic links
     * @return actual path to the resource
     */
    public static String getActualPath(String path) {
        String actualPath = null;
        File file = new File(path);
        try {
            actualPath = file.getCanonicalPath();
        } catch (Exception ex) {
            log.warn("The file at " + path + " does not seem to exist: " 
                + ex.getClass().getName() + ": " + ex.getMessage());
        }
        return actualPath;
    }
    
    public static void systemPropertiesChanged() {
        synchronized (AppSpecificPropertyNames) {
            AppSpecificPropertyNames.clear();
        }
        _cache.clear();
        // MS: Leave for future WO support ...
        NSNotificationCenter.defaultCenter().postNotification("PropertiesDidChange", null, null);
    }

    //	===========================================================================
    //	Instance Variable(s)
    //	---------------------------------------------------------------------------

    /** caches the application name that is appended to the key for lookup */
    protected String applicationNameForAppending;

    //	===========================================================================
    //	Instance Method(s)
    //	---------------------------------------------------------------------------

    /**
     * Caches the application name for appending to the key.
     * Note that for a period when the application is starting up
     * application() will be null and name() will be null.
     * @return application name used for appending, for example ".ERMailer"
     * Note: this is redundant with the scheme checked in on March 21, 2005 by clloyd (ben holt did checkin).
     * This scheme requires the user to swizzle the existing properties file with a new one of this type.
     */
    protected String applicationNameForAppending() {
        if (applicationNameForAppending == null) {
            applicationNameForAppending = WOApplication.application() != null ? WOApplication.application().name() : null;
            if (applicationNameForAppending != null) {
                applicationNameForAppending = "." + applicationNameForAppending;
            }
        }
        return applicationNameForAppending;
    }

    /**
     * Overriding the default getProperty method to first check:
     * key.&lt;ApplicationName> before checking for key. If nothing
     * is found then key.Default is checked.
     * @param key to check
     * @return property value
     */
    public String getProperty(String key) {
        String property = null;
        String application = applicationNameForAppending();
        if (application != null) {
            property = super.getProperty(key + application);
        }
        if (property == null) {
            property = super.getProperty(key);
            if (property == null) {
                property = super.getProperty(key + DefaultString);
            }
            // We go ahead and set the value to increase the lookup the next time the
            // property is accessed.
            if (property != null && application != null) {
                setProperty(key + application, property);
            }
        }
        return property;
    }

    /**
     * Returns the properties as a String in Property file format. Useful when you use them 
     * as custom value types, you would set this as the conversion method name.
     * @throws IOException
     */
    public Object toExternalForm() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        store(os, null);
        return new String(os.toByteArray());
    }
    
    /**
     * Load the properties from a String in Property file format. Useful when you use them 
     * as custom value types, you would set this as the factory method name.
     * @param string
     */
    public static ERXProperties fromExternalForm(String string) {
        ERXProperties result = new ERXProperties();
        try {
			result.load(new ByteArrayInputStream(string.getBytes()));
		}
		catch (IOException e) {
			// AK: shouldn't ever happen...
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
        return result;
    }

    /**
     * KVC implementation.
     * @param anObject
     * @param aKey
     */
    public void takeValueForKey(Object anObject, String aKey) {
         setProperty(aKey, (anObject != null ? anObject.toString() : null));
    }

    /**
     * KVC implementation.
     *
     * @param aKey
     */
    public Object valueForKey(String aKey) {
         return getProperty(aKey);
    }

	/**
	 * Stores the mapping between operator keys and operators
	 */
	private static final NSMutableDictionary<String, ERXProperties.Operator> operators = new NSMutableDictionary<String, ERXProperties.Operator>();

	/**
	 * Registers a property operator for a particular key.
	 * 
	 * @param operator
	 *            the operator to register
	 * @param key
	 *            the key name of the operator
	 */
	public static void setOperatorForKey(ERXProperties.Operator operator, String key) {
		ERXProperties.operators.setObjectForKey(operator, key);
	}

	/**
	 * <p>
	 * Property operators work like array operators. In your properties, you can
	 * define keys like:
	 * </p>
	 * 
	 * <code>
	 * er.extensions.akey.@someOperatorKey.aparameter=somevalue
	 * </code>
	 * 
	 * <p>
	 * Which will be processed by the someOperatorKey operator. Because
	 * properties get handled very early in the startup process, you should
	 * register operators somewhere like a static block in your Application
	 * class. For instance, if you wanted to register the forInstance operator,
	 * you might put the following your Application class:
	 * </p>
	 * 
	 * <code>
	 * static {
	 *   ERXProperties.setOperatorForKey(new ERXProperties.InRangeOperator(100), ERXProperties.InRangeOperator.ForInstanceKey);
	 * }
	 * </code>
	 * 
	 * <p>
	 * It's important to note that property operators evaluate at load time, not
	 * access time, so the compute function should not depend on any runtime
	 * state to execute. Additionally, access to other properties inside the
	 * compute method should be very carefully considered because it's possible
	 * that the operators are evaluated before all of the properties in the
	 * system are loaded.
	 * </p>
	 * 
	 * @author mschrag
	 */
	public static interface Operator {
		/**
		 * Performs some computation on the key, value, and parameters and
		 * returns a dictionary of new properties. If this method returns null,
		 * the original key and value will be used. If any other dictionary is
		 * returned, the properties in the dictionary will be copied into the
		 * destination properties.
		 * 
		 * @param key
		 *            the key ("er.extensions.akey" in
		 *            "er.extensions.akey.@someOperatorKey.aparameter=somevalue")
		 * @param value
		 *            ("somevalue" in
		 *            "er.extensions.akey.@someOperatorKey.aparameter=somevalue")
		 * @param parameters
		 *            ("aparameter" in
		 *            "er.extensions.akey.@someOperatorKey.aparameter=somevalue")
		 * @return a dictionary of properties (or null to use the original key
		 *         and value)
		 */
		public NSDictionary<String, String> compute(String key, String value, String parameters);
	}

	/**
	 * <p>
	 * InRangeOperator provides support for defining properties that only
	 * get set if a value falls within a specific range of values.
	 * </p>
	 * 
	 * <p>
	 * An example of this is instance-number-based properties, where you want to 
	 * only set a specific value if the instance number of the application falls
	 * within a certain value. In this example, because instance number is 
	 * something that is associated with a request rather than the application 
	 * itself, it is up to the class registering this operator to specify which 
	 * instance number this application is (via, for instance, a custom system property).
	 * </p>
	 * 
	 * <p>
	 * InRangeOperator supports specifying keys like:
	 * </p>
	 * 
	 * <code>er.extensions.akey.@forInstance.50=avalue</code>
	 * <p>
	 * which would set the value of "er.extensions.akey" to "avalue" if this
	 * instance is 50.
	 * </p>
	 * 
	 * <code>er.extensions.akey.@forInstance.60,70=avalue</code>
	 * <p>
	 * which would set the value of "er.extensions.akey" to "avalue" if this
	 * instance is 60 or 70.
	 * </p>
	 * 
	 * <code>er.extensions.akey.@forInstance.100-300=avalue</code>
	 * <p>
	 * which would set the value of "er.extensions.akey" to "avalue" if this
	 * instance is between 100 and 300 (inclusive).
	 * </p>
	 * 
	 * <code>er.extensions.akey.@forInstance.20-30,500=avalue</code>
	 * <p>
	 * which would set the value of "er.extensions.akey" to "avalue" if this
	 * instance is between 20 and 30 (inclusive), or if the instance is 50.
	 * </p>
	 * 
	 * <p>
	 * If there are multiple inRange operators that match for the same key,
	 * the last property (when sorted alphabetically by key name) will win. As a
	 * result, it's important to not define overlapping ranges, or you
	 * may get unexpected results.
	 * </p>
	 * 
	 * @author mschrag
	 */
	public static class InRangeOperator implements ERXProperties.Operator {
		/**
		 * The default key name of the ForInstance variant of the InRange operator.
		 */
		public static final String ForInstanceKey = "forInstance";

		private int _instanceNumber;

		/**
		 * Constructs a new InRangeOperator.
		 * 
		 * @param value
		 *            the instance number of this application
		 */
		public InRangeOperator(int value) {
			_instanceNumber = value;
		}

		public NSDictionary<String, String> compute(String key, String value, String parameters) {
			NSDictionary computedProperties = null;
			if (parameters != null && parameters.length() > 0) {
				if (ERXStringUtilities.isValueInRange(_instanceNumber, parameters)) {
					computedProperties = new NSDictionary(value, key);
				}
				else {
					computedProperties = NSDictionary.EmptyDictionary;
				}
			}
			return computedProperties;
		}
	}

	/**
	 * <p>
	 * Encrypted operator supports decrypting values using the default crypter. To register this
	 * operator, add the following static block to your Application class:
	 * </p>
	 * 
	 * <code>
	 * static {
	 *   ERXProperties.setOperatorForKey(new ERXProperties.EncryptedOperator(), ERXProperties.EncryptedOperator.Key);
	 * }
	 * </code>
	 * 
	 * Call er.extensions.ERXProperties.EncryptedOperator.register() in an Application static
	 * block to register this operator.
	 * </p> 
	 * 
	 * @author mschrag
	 */
	public static class EncryptedOperator implements ERXProperties.Operator {
		public static final String Key = "encrypted";

		public static void register() {
			ERXProperties.setOperatorForKey(new ERXProperties.EncryptedOperator(), ERXProperties.EncryptedOperator.Key);
		}

		public NSDictionary<String, String> compute(String key, String value, String parameters) {
			String decryptedValue = ERXCrypto.defaultCrypter().decrypt(value);
			return new NSDictionary<String, String>(decryptedValue, key);
		}
	}

	/**
	 * For each property in originalProperties, process the keys and avlues with
	 * the registered property operators and stores the converted value into
	 * destinationProperties.
	 * 
	 * @param originalProperties
	 *            the properties to convert
	 * @param destinationProperties
	 *            the properties to copy into
	 */
	public static void evaluatePropertyOperators(Properties originalProperties, Properties destinationProperties) {
		NSArray<String> operatorKeys = ERXProperties.operators.allKeys();
		for (Object keyObj : new TreeSet<Object>(originalProperties.keySet())) {
			String key = (String) keyObj;
			if (key != null && key.length() > 0) {
				String value = originalProperties.getProperty(key);
				if (operatorKeys.count() > 0 && key.indexOf(".@") != -1) {
					ERXProperties.Operator operator = null;
					NSDictionary<String, String> computedProperties = null;
					for (String operatorKey : operatorKeys) {
						String operatorKeyWithAt = ".@" + operatorKey;
						if (key.endsWith(operatorKeyWithAt)) {
							operator = ERXProperties.operators.objectForKey(operatorKey);
							computedProperties = operator.compute(key.substring(0, key.length() - operatorKeyWithAt.length()), value, null);
							break;
						}
						else {
							int keyIndex = key.indexOf(operatorKeyWithAt + ".");
							if (keyIndex != -1) {
								operator = ERXProperties.operators.objectForKey(operatorKey);
								computedProperties = operator.compute(key.substring(0, keyIndex), value, key.substring(keyIndex + operatorKeyWithAt.length() + 1));
								break;
							}
						}
					}

					if (computedProperties == null) {
						destinationProperties.put(key, value);
					}
					else {
						originalProperties.remove(key);
						
						// If the key exists in the System properties' defaults with a different value, we must reinsert
						// the property so it doesn't get overwritten with the default value when we evaluate again.
						// This happens because ERXConfigurationManager processes the properties after a configuration
						// change in multiple passes and each calls this method.
						if (System.getProperty(key) != null && !System.getProperty(key).equals(value)) {
							originalProperties.put(key, value);
						}
						
						for (String computedKey : computedProperties.allKeys()) {
							destinationProperties.put(computedKey, computedProperties.objectForKey(computedKey));
						}
					}
				}
				else {
					destinationProperties.put(key, value);
				}
			}
		}
	}

	/**
	 * _Properties is a subclass of Properties that provides support for including other
	 * Properties files on the fly.  If you create a property named .includeProps, the value
	 * will be interpreted as a file to load.  If the path is absolute, it will just load it
	 * directly.  If it's relative, the path will be loaded relative to the current user's
	 * home directory.  Multiple .includeProps can be included in a Properties file and they
	 * will be loaded in the order they appear within the file.
	 *  
	 * @author mschrag
	 */
	public static class _Properties extends Properties {
		public static final String IncludePropsKey = ".includeProps";
		
		private Stack<File> _files = new Stack<File>();
		
		@Override
		public synchronized Object put(Object key, Object value) {
			if (_Properties.IncludePropsKey.equals(key)) {
				String propsFileName = (String)value;
                File propsFile = new File(propsFileName);
                if (!propsFile.isAbsolute()) {
                    // if we don't have any context for a relative (non-absolute) props file,
                    // we presume that it's relative to the user's home directory
    				File cwd = null;
    				if (_files.size() > 0) {
    					cwd = _files.peek();
    				}
    				else {
    					cwd = new File(System.getProperty("user.home"));
                	}
                    propsFile = new File(cwd, propsFileName);
                }

                // Detect mutually recursing props files by tracking what we've already loaded:
                String existingIncludeProps = this.getProperty(_Properties.IncludePropsKey);
                if (existingIncludeProps == null) {
                	existingIncludeProps = "";
                }
                if (existingIncludeProps.indexOf(propsFile.getPath()) > -1) {
                    log.error("_Properties.load(): recursive includeProps detected! " + propsFile + " in " + existingIncludeProps);
                    log.error("_Properties.load() cannot proceed - QUITTING!");
                    System.exit(1);
                }
                if (existingIncludeProps.length() > 0) {
                	existingIncludeProps += ", ";
                }
                existingIncludeProps += propsFile;
                super.put(_Properties.IncludePropsKey, existingIncludeProps);

                try {
                    log.info("_Properties.load(): Including props file: " + propsFile);
					this.load(propsFile);
				} catch (IOException e) {
					throw new RuntimeException("Failed to load the property file '" + value + "'.", e);
				}
				return null;
			}
			else {
				return super.put(key, value);
			}
		}

		public synchronized void load(File propsFile) throws IOException {
			_files.push(propsFile.getParentFile());
			try {
	            BufferedInputStream is = new BufferedInputStream(new FileInputStream(propsFile));
	            try {
	            	load(is);
	            }
	            finally {
	            	is.close();
	            }
			}
			finally {
				_files.pop();
			}
		}
	}

	public static void setCommandLineArguments(String[] argv) {
		
	}
}
