package er.extensions;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOAssociation;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResourceManager;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSBundle;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

/**
 * ERXComponentUtilities contains WOComponent/WOElement-related utility methods.
 * 
 * @author mschrag
 */
public class ERXComponentUtilities {
	/**
	 * Returns a query parameter dictionary from a set of ?key=association
	 * WOAssociation dictionary.
	 * 
	 * @param associations
	 *            the set of associations
	 * @param component
	 *            the component to evaluate their values within
	 * @return a dictionary of key=value query parameters
	 */
	public static NSMutableDictionary queryParametersInComponent(NSDictionary associations, WOComponent component) {
		NSMutableDictionary queryParameterAssociations = ERXComponentUtilities.queryParameterAssociations(associations);
		return _queryParametersInComponent(queryParameterAssociations, component);
	}

	/**
	 * Returns a query parameter dictionary from a set of ?key=association
	 * WOAssociation dictionary.
	 * 
	 * @param associations
	 *            the set of associations
	 * @param component
	 *            the component to evaluate their values within
	 * @param removeQueryParameterAssociations
	 *            should the entries be removed from the passed-in dictionary?
	 * @return a dictionary of key=value query parameters
	 */
	public static NSMutableDictionary queryParametersInComponent(NSMutableDictionary associations, WOComponent component, boolean removeQueryParametersAssociations) {
		NSMutableDictionary queryParameterAssociations = ERXComponentUtilities.queryParameterAssociations(associations, removeQueryParametersAssociations);
		return _queryParametersInComponent(queryParameterAssociations, component);
	}

	public static NSMutableDictionary _queryParametersInComponent(NSMutableDictionary associations, WOComponent component) {
		NSMutableDictionary queryParameters = new NSMutableDictionary();
		Enumeration keyEnum = associations.keyEnumerator();
		while (keyEnum.hasMoreElements()) {
			String key = (String) keyEnum.nextElement();
			WOAssociation association = (WOAssociation) associations.valueForKey(key);
			Object associationValue = association.valueInComponent(component);
			if (associationValue != null) {
				queryParameters.setObjectForKey(associationValue, key.substring(1));
			}
		}
		return queryParameters;
	}

	/**
	 * Returns the set of ?key=value associations from an associations
	 * dictionary.
	 * 
	 * @param associations
	 *            the associations to enumerate
	 */
	public static NSMutableDictionary queryParameterAssociations(NSDictionary associations) {
		return ERXComponentUtilities._queryParameterAssociations(associations, false);
	}

	/**
	 * Returns the set of ?key=value associations from an associations
	 * dictionary. If removeQueryParameterAssociations is true, the
	 * corresponding entries will be removed from the associations dictionary
	 * that was passed in.
	 * 
	 * @param associations
	 *            the associations to enumerate
	 * @param removeQueryParameterAssociations
	 *            should the entries be removed from the passed-in dictionary?
	 */
	public static NSMutableDictionary queryParameterAssociations(NSMutableDictionary associations, boolean removeQueryParameterAssociations) {
		return ERXComponentUtilities._queryParameterAssociations(associations, removeQueryParameterAssociations);
	}

	public static NSMutableDictionary _queryParameterAssociations(NSDictionary associations, boolean removeQueryParameterAssociations) {
		NSMutableDictionary mutableAssociations = null;
		if (removeQueryParameterAssociations) {
			mutableAssociations = (NSMutableDictionary) associations;
		}
		NSMutableDictionary queryParameterAssociations = new NSMutableDictionary();
		Enumeration keyEnum = associations.keyEnumerator();
		while (keyEnum.hasMoreElements()) {
			String key = (String) keyEnum.nextElement();
			if (key.startsWith("?")) {
				WOAssociation association = (WOAssociation) associations.valueForKey(key);
				if (mutableAssociations != null) {
					mutableAssociations.removeObjectForKey(key);
				}
				queryParameterAssociations.setObjectForKey(association, key);
			}
		}
		return queryParameterAssociations;
	}

	/**
	 * Returns the boolean value of a binding.
	 * 
	 * @param component the component
	 * @param bindingName the name of the boolean binding
	 * @return a boolean
	 */
	public static boolean booleanValueForBinding(WOComponent component, String bindingName) {
		Boolean value = (Boolean)component.valueForBinding(bindingName);
		return value != null && value.booleanValue();
	}
	
	/**
	 * Returns the URL of the html template for the given component name.
	 * 
	 * @param componentName the name of the component to load a template for (without the .wo)
	 * @param languages the list of languages to use for finding components
	 * @return the URL to the html template (or null if there isn't one)
	 */
	public static URL htmlTemplateUrl(String componentName, NSArray languages) {
		return ERXComponentUtilities.templateUrl(componentName, "html", languages);
	}
	
	/**
	 * Returns the URL of the template for the given component name.
	 * 
	 * @param componentName the name of the component to load a template for (without the .wo)
	 * @param extension the file extension of the template (without the dot -- i.e. "html") 
	 * @param languages the list of languages to use for finding components
	 * @return the URL to the template (or null if there isn't one)
	 */
	public static URL templateUrl(String componentName, String extension, NSArray languages) {
        String htmlPathName = componentName + ".wo/" + componentName + "." + extension;
        WOResourceManager resourcemanager = WOApplication.application().resourceManager();
        URL templateUrl = resourcemanager.pathURLForResourceNamed(htmlPathName, null, languages);
        if (templateUrl == null) {
          NSArray frameworkBundles = NSBundle.frameworkBundles();
          if (frameworkBundles != null) {
            Enumeration frameworksEnum = frameworkBundles.objectEnumerator();
            while (templateUrl == null && frameworksEnum.hasMoreElements()) {
              NSBundle frameworkBundle = (NSBundle) frameworksEnum.nextElement();
              templateUrl = resourcemanager.pathURLForResourceNamed(htmlPathName, frameworkBundle.name(), languages);
            }
          }
        }
        return templateUrl;
	}
	
	/**
	 * Returns the contents of the html template for the given component name as a string.
	 * 
	 * @param componentName the name of the component to load a template for (without the .wo)
	 * @param languages the list of languages to use for finding components
	 * @return the string contents of the html template (or null if there isn't one)
	 */
	public static String htmlTemplate(String componentName, NSArray languages) throws IOException {
		return ERXComponentUtilities.template(componentName, "html", languages);
	}
	
	/**
	 * Returns the contents of the template for the given component name as a string.
	 * 
	 * @param componentName the name of the component to load a template for (without the .wo)
	 * @param extension the file extension of the template (without the dot -- i.e. "html") 
	 * @param languages the list of languages to use for finding components
	 * @return the string contents of the template (or null if there isn't one)
	 */
	public static String template(String componentName, String extension, NSArray languages) throws IOException {
		String template;
		URL templateUrl = ERXComponentUtilities.templateUrl(componentName, extension, languages);
        if (templateUrl == null) {
          template = null;
        }
        else {
          template = ERXStringUtilities.stringFromURL(templateUrl);
        }
        return template;
	}
}
