/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * The validation factory controls creating validation
 * exceptions, both from model thrown exceptions and 
 * custom validation exceptions. The factory is responsible
 * for resolving validation remplates for validation
 * exceptions and generating validation messages.
 */
public class ERXValidationFactory {

    /** logging support */
    public final static ERXLogger log = ERXLogger.getERXLogger(ERXValidationFactory.class);
    
    /** holds a reference to the default validation factory */
    private static ERXValidationFactory _defaultFactory;
    
    /** holds a reference to the default validation delegate */
    // FIXME: This should be a weak reference
    private static Object _defaultValidationDelegate = null;
    
    /** holds the default mappings that map model thrown validation strings to exception types */
    private static NSDictionary _mappings;

    /** holds the method name 'messageForException' */
    // FIXME: This is better done with an NSSelector and using the method: implementedByObject
    private static final String EDI_MFE_METHOD_NAME = "messageForException";
    
    /** holds the method name 'templateForException' */
    private static final String EDI_TFE_METHOD_NAME = "templateForException";
    
    /** holds the class argument array for delegate validation exception messages */    
    private static final Class[] EDI_FE_ARGS = new Class[] { ERXValidationException.class };

    /** Regular ERXValidationException constructor parameters */
    private static Class[] _regularConstructor = new Class[] { String.class, Object.class, String.class, Object.class };

    /** holds the marker for an un defined validation template */
    private final static String UNDEFINED_VALIDATION_TEMPLATE = "Undefined Validation Template";
    
    /**
     * Sets the default factory to be used for converting
     * model thrown exceptions.
     * @param aFactory new factory
     */
    public static void setDefaultFactory(ERXValidationFactory aFactory) { _defaultFactory = aFactory; }
    
    /**
     * Returns the default factory. If one has not
     * been set then a factory is created of type
     * ERXValidationFactory.
     * @return the default validation factory
     */
    public static ERXValidationFactory defaultFactory() {
        if (_defaultFactory == null)
            setDefaultFactory(new ERXValidationFactory());
        return _defaultFactory;
    }
    /**
     * Returns the default validation delegate that will
     * be set on all validation exceptions created. At the
     * moment delegates should implement the ExceptionDelegateInterface.
     * This will change to an informal implementation soon.
     * @return the default validation exception delegate.
     */
    public static Object defaultDelegate() { return _defaultValidationDelegate; }
    
    /**
     * Sets the default validation delegate that
     * will be set on all validation exceptions that
     * are created by the factory. At the moment the
     * delegate set needs to implement the interface
     * ExceptionDelegateInterface.
     * @param obj default validation delegate
     */
    public static void setDefaultDelegate(Object obj) { _defaultValidationDelegate = obj; }

    /**
     * The validation factory interface. This interface
     * is currently not being used.
     */
    public interface FactoryInterface {
        public Class validationExceptionClass();
        public void setValidationExceptionClass(Class class1);
        public ERXValidationException createException(EOEnterpriseObject eo, String property, Object value, String type);
        public ERXValidationException createCustomException(EOEnterpriseObject eo, String method);
    }

    /**
     * Exception delegates can be used to provide hooks to customize
     * how messages are generated for validation exceptions and how
     * tempates are looked up. A validation exception can have a 
     * delegate set or a default delegate can be set on the factory
     * itself.
     */
    public interface ExceptionDelegateInterface {
        public String messageForException(ERXValidationException erv);
        public String templateForException(ERXValidationException erv);
        public NSKeyValueCoding contextForException(ERXValidationException erv);
    }

    /**
     * In the static initializer the mapping dictionary is
     * created.
     */
    static {
        Object keys[] = {				// MESSAGE LIST:
            "to be null", 				// "The 'xxxxx' property is not allowed to be NULL"
            "Invalid Number", 				// "Invalid Number"
            "must have a", 				// "The owner property of Bug must have a People assigned "
            "must have at least one",			// "The exercises property of ERPCompanyRole must have at least one ERPExercise"
            "relationship, there is a related object",	// "Removal of ERPAccount object denied because its children relationship is not empty"
            "relationship, there are related objects",	// "Removal of ERPAccount object denied because its children relationship is not empty"
            "exceeds maximum length of"
        };
        Object objects[] = {
            ERXValidationException.NullPropertyException,
            ERXValidationException.InvalidNumberException,
            ERXValidationException.MandatoryToOneRelationshipException,
            ERXValidationException.MandatoryToManyRelationshipException,
            ERXValidationException.ObjectRemovalException,
            ERXValidationException.ObjectsRemovalException,
            ERXValidationException.ExceedsMaximiumLengthException
        };
        _mappings = new NSDictionary( objects, keys );
    }

    /** holds the validation exception class */
    private Class _validationExceptionClass;

    /** holds the template cache for a given set of keys */
    private Hashtable _cache=new Hashtable(1000);

    /** holds the default template delimiter, "@" */
    private String _delimiter = "@";
    
    /** caches the constructor used to build validation exceptions */
    protected Constructor regularConstructor;
    
    /**
     * Sets the validation class to be used when
     * creating validation exceptions.
     * @param class1 validation exception class
     */
    public void setValidationExceptionClass(Class class1) { _validationExceptionClass = class1; }
    
    /**
     * Returns the validation exception class to use
     * when creating exceptions. If none is specified
     * {@link ERXValidationException} is used.
     * @return class object of validation exceptions to
     *		be used.
     */
    public Class validationExceptionClass() {
        if (_validationExceptionClass == null)
            _validationExceptionClass = ERXValidationException.class;
        return _validationExceptionClass;
    }

    /**
     * Simple method used to lookup and cache the 
     * constructor to build validation exceptions.
     * @return constructor used to build validation exceptions
     */
    protected Constructor regularValidationExceptionConstructor() {
        if (regularConstructor == null) {
            try {
                regularConstructor = validationExceptionClass().getConstructor(_regularConstructor);
            } catch (Exception e) {
                log.error("Exception looking up regular constructor. Exception: " + e.getMessage());
            }
        }
        return regularConstructor;
    }
    
    /**
     * Entry point for creating validation exceptions. This
     * method is used by all of the other methods to create
     * validation exceptions for an enterprise object, a property
     * key, a value and a type. The type should correspond to 
     * one of the validation exception types defined in
     * {@link ERXValidationException ERXValidationException}.
     * @param eo enterprise object that is failing validation
     * @param property attribute that failed validation
     * @param value that failed validating
     * @param type of the validation exception
     * @return validation exception for the given information
     */
    public ERXValidationException createException(EOEnterpriseObject eo, String property, Object value, String type) {
        ERXValidationException erve = null;
        try {
            log.debug("Creating exception for type: " + type + " validationExceptionClass: " + validationExceptionClass().getName());
            erve = (ERXValidationException)regularValidationExceptionConstructor().newInstance(new Object[] {type, eo, property, value});
        } catch (InvocationTargetException ite) {
            log.error("Caught InvocationTargetException creating regular validation exception: " + ite.getTargetException());            
        } catch (Exception e) {
            log.error("Caught exception creating regular validation exception: " + e);
        }
        return erve;
    }

    /**
     * Creates a custom validation exception for a given
     * enterprise object and method. This method is just
     * a cover method for calling the four arguement method
     * specifying <code>null</code> for proptery and value.
     * @param eo enterprise object failing validation
     * @param name of the method to use to look up the validation
     *		exception template, for instance "FirstNameCanNotMatchLastNameValidationException"
     * @return a custom validation exception for the given criteria
     */
    public ERXValidationException createCustomException(EOEnterpriseObject eo, String method) {
        return createCustomException(eo, null, null, method);
    }

    /**
     * Creates a custom validation exception. This is the prefered 
     * way of creating custom validation exceptions.
     * @param eo enterprise object failing validation
     * @param property attribute that failed validation
     * @param value that failed validation
     * @param method unique identified usually corresponding to a 
     *		method name to pick up the validation template
     * @return custom validation exception
     */
    public ERXValidationException createCustomException(EOEnterpriseObject eo, String property, Object value, String method) {
        ERXValidationException erv = createException(eo, property, value, ERXValidationException.CustomMethodException);
        if (erv != null)
            erv.setMethod(method);
        return erv;
    }    
    
    /**
     * Converts a model thrown validation exception into
     * an {@link ERXValidationException ERXValidationException}.
     * This is a cover method for the two argument version
     * passing in null as the value.
     * @param eov validation exception to be converted
     * @return converted validation exception
     */
    public ERXValidationException convertException(NSValidation.ValidationException eov) { 
        return convertException(eov, null); 
    }

    /**
     * Converts a given model thrown validation exception into
     * an {@link ERXValidationException ERXValidationException}.
     * This method is used by {@link ERXEntityClassDescription ERXEntityClassDescription}
     * to covert model thrown validation exceptions. This isn't 
     * a very elegant solution, but until we can register our
     * our validation exception class this is what we have to do.
     * @param eov validation exception to be converted
     * @param value that failed validation
     * @return converted validation exception
     */
    public ERXValidationException convertException(NSValidation.ValidationException eov, Object value) {
        ERXValidationException erve = null;
        if (log.isDebugEnabled())
            log.debug("Converting exception: " + eov + " value: " + (value != null ? value : "<NULL>"));
        if (!(eov instanceof ERXValidationException)) {
            String message = eov.getMessage();
            EOEnterpriseObject eo = (EOEnterpriseObject)eov.object();
            //NSDictionary userInfo = eov.userInfo() != null ? (NSDictionary)eov.userInfo() : NSDictionary.EmptyDictionary;
            for (Enumeration e = _mappings.allKeys().objectEnumerator(); e.hasMoreElements();) {
                //EOEnterpriseObject eo = (EOEnterpriseObject)userInfo.objectForKey(NSValidation.ValidationException.ValidatedObjectUserInfoKey);
                String key = (String)e.nextElement();
                String type = (String)_mappings.objectForKey(key);
                if (message.lastIndexOf(key) >= 0) {
                    String property = eov.key();
                    if(property == null && message.indexOf("Removal") == 0) {
                        //FIXME: (ak) pattern matching?
                        property = (String)(NSArray.componentsSeparatedByString(message, "'").objectAtIndex(3));
                    }
                    erve = createException(eo, property, value, type);
                    break;
                }
            }
            NSArray additionalExceptions = eov.additionalExceptions();
            if (erve == null) {
                log.error("Unable to convert validation exception: " + eov);
            } else if (additionalExceptions != null && additionalExceptions.count() > 0) {
                NSMutableArray erveAddtionalExceptions = new NSMutableArray();
                for (Enumeration e = additionalExceptions.objectEnumerator(); e.hasMoreElements();) {
                    ERXValidationException erven = convertException((NSValidation.ValidationException)e.nextElement());
                    if (erven != null)
                        erveAddtionalExceptions.addObject(erven);
                }
                if (erveAddtionalExceptions.count() > 0)
                    erve.setAdditionalExceptions(erveAddtionalExceptions);
            }
        } else {
            log.warn("Attempting to convert validation exception: " + eov + " that is already of type ERXValidationException");
            erve = (ERXValidationException)eov;
        }
        return erve;
    }

    /**
     * Entry point for generating an exception message
     * for a given message. The method <code>getMessage</code>
     * off of {@link ERXValidationException ERXValidationException}
     * calls this method passing in itself as the parameter.
     * @param erv validation exception
     * @return a localized validation message for the given exception
     */
    // FIXME: Right now the delegate methods are implemented as a formal interface.  Not ideal.  Should be implemented as
    //	an informal interface.  Can still return null to not have an effect.
    public String messageForException(ERXValidationException erv) {
        String message = null;
        if (erv.delegate() != null && erv.delegate() instanceof ExceptionDelegateInterface) {
            message = ((ExceptionDelegateInterface)erv.delegate()).messageForException(erv);
        }
        if (message == null) {
            message = ERXSimpleTemplateParser.sharedInstance().parseTemplateWithObject(templateForException(erv),
                                                                                       templateDelimiter(),
                                                                                       erv);
        }
        return message;
    }

    /**
     * Entry point for finding a template for a given validation
     * exception. Override this method to provide your own
     * template resolution scheme.
     * @param erv validation exception
     * @return validation template for the given exception
     */
    public String templateForException(ERXValidationException erv) {
        String template = null;
        if (erv.delegate() != null && erv.delegate() instanceof ExceptionDelegateInterface) {
            template = ((ExceptionDelegateInterface)erv.delegate()).templateForException(erv);
        }
        if (template == null) {
            String entityName = erv.eoObject().entityName();
            String property = erv.isCustomMethodException() ? erv.method() : erv.propertyKey();
            String type = erv.type();
            String targetLanguage = erv.targetLanguage();
            if (targetLanguage == null) {
                targetLanguage = ERXLocalizer.currentLocalizer() != null ? ERXLocalizer.currentLocalizer().language() : ERXLocalizer.defaultLanguage();
            }
            
            if (log.isDebugEnabled ())
                log.debug("templateForException with entityName: " + entityName + "; property: " + property + "; type: " + type + "; targetLanguage: " + targetLanguage);
            ERXMultiKey k = new ERXMultiKey (new Object[] {entityName, property,
                type,targetLanguage});
            template = (String)_cache.get(k);
            // Not in the cache.  Simple resolving.
            if (template == null) {
                template = templateForEntityPropertyType(entityName, property, type, targetLanguage);
                _cache.put(k, template);
            }
        }
        return template;
    }

    /**
     * Called when the Localizer is reset. This will
     * reset the template cache.
     * @param n notification posted when the localizer
     *		is reset.
     */
    public void resetTemplateCache(NSNotification n) {
        _cache = new Hashtable(1000);
        if (log.isDebugEnabled()) log.debug("Resetting template cache");
    }

    /**
     * The context for a given validation exception can be used
     * to resolve keys in validation template. If a context is
     * not provided for a validation exception then this method
     * will be called if a context is needed for a validation
     * exception. Override this method if you want to provide
     * your own default contexts to validation exception template
     * parsing.
     * @param erv a given validation exception
     * @return context to be used for this validation exception
     */
    // CHECKME: Doesn't need to be the NSKeyValueCoding interface now with WO 5
    public NSKeyValueCoding contextForException(ERXValidationException erv) {
        NSKeyValueCoding context = null;
        if (erv.delegate() != null && erv.delegate() instanceof ExceptionDelegateInterface) {
            context = ((ExceptionDelegateInterface)erv.delegate()).contextForException(erv);
        }
        return context;
    }
    
    /**
     * Returns the template delimiter, the
     * default delimiter is "@".
     * @return template delimiter
     */
    public String templateDelimiter() { return _delimiter; }
    
    /**
     * sets the template delimiter to be used
     * when parsing templates for creating validation
     * exception messages.
     * @param delimiter to be set.
     */
    // FIXME: Should be setTemplateDelimiter, what was I thinking
    public void setDelimiter(String delimiter) { _delimiter = delimiter; }

    /**
     * Method used to configure the validation factory
     * for operation. This method is called on the default
     * factory from an observer when the application is
     * finished launching.
     */
    public void configureFactory() {
        // CHECKME: This might be better configured in a static init block of ERXValidationFactory.        
        ERXValidation.setPushChangesDefault(ERXUtilities.booleanValueWithDefault(System.getProperties().getProperty("er.extensions.ERXValidationShouldPushChangesToObject"), ERXValidation.DO_NOT_PUSH_INCORRECT_VALUE_ON_EO));

        if (WOApplication.application()!=null && WOApplication.application().isCachingEnabled()) {
            NSNotificationCenter center = NSNotificationCenter.defaultCenter();
            center.addObserver(this,
                               new NSSelector("resetTemplateCache",  ERXConstant.NotificationClassArray),
                               ERXLocalizer.LocalizationDidResetNotification,
                               null);
        }
    }

    /**
     * Finds a template for a given entity, property key, exception type and target
     * language. This method provides the defaulting behaviour needed to handle model
     * thrown validation exceptions.
     * @param entityName name of the entity
     * @param property key name
     * @param type validation exception type
     * @param targetLanguage target language name
     * @return a templaet for the given set of parameters
     */
    protected String templateForEntityPropertyType(String entityName, String property, String type, String targetLanguage) {
        // 1st try the whole string.
        String template = templateForKeyPath(entityName + "." + property + "." + type, targetLanguage);
        // 2nd try everything minus the type.
        if (template == null)
            template = templateForKeyPath(entityName + "." + property, targetLanguage);
        // 3rd try property plus type
        if (template == null)
            template = templateForKeyPath(property + "." + type, targetLanguage);
        // 4th try just property
        if (template == null)
            template = templateForKeyPath(property, targetLanguage);
        // 5th try just type
        if (template == null)
            template = templateForKeyPath(type, targetLanguage);
        if (template == null) {
            log.error("Unable to find template for entity: " + entityName + " property: " + property + " type: "
                      + type + " targetLanguage: " + targetLanguage);
            template = UNDEFINED_VALIDATION_TEMPLATE;
        }
        return template;
    }

    /**
     * Get the template for a given key in a given language.
     * Uses {@link ERXLocalizer} to handle the actual lookup.
     * @returns template for key or null if none is found
     */
    public String templateForKeyPath(String key, String language) {
        return ERXLocalizer.localizerForLanguage(language).localizedStringForKey(key);
    }
}
