package er.extensions;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

import java.text.*;
import java.util.*;

/**
 * Provides localization to timestamp formatters.<br />
 * 
 */

public class ERXTimestampFormatter extends NSTimestampFormatter {

	/** holds a reference to the repository */
	private static Hashtable _repository = new Hashtable();
	protected static final String DefaultKey = "ERXTimestampFormatter.DefaultKey";
	
	static {
		_repository.put(DefaultKey, new ERXTimestampFormatter());
	};
	

	/**
	 * @param object
	 * @return
	 */
	public static Format defaultDateFormatterForObject(Object object) {
		Format result = null;
		if(object != null && object instanceof NSTimestamp) {
			result = dateFormatterForPattern("%Y/%m/%d");
		}
		return result;
	}

	/**
	 * Returns a shared instance for the specified pattern.
	 * @return shared instance of formatter
	 */
	public static NSTimestampFormatter dateFormatterForPattern(String pattern) {
		NSTimestampFormatter formatter;
		if(ERXLocalizer.isLocalizationEnabled()) {
			ERXLocalizer localizer = ERXLocalizer.currentLocalizer();
			formatter = (NSTimestampFormatter)localizer.localizedDateFormatForKey(pattern);
		} else {
			synchronized(_repository) {
				formatter = (NSTimestampFormatter)_repository.get(pattern);
				if(formatter == null) {
					formatter = new NSTimestampFormatter(pattern);
					_repository.put(pattern, formatter);
				}
			}
		}
		return formatter;
	}
	
	/**
	 * Sets a shared instance for the specified pattern.
	 * @return shared instance of formatter
	 */
	public static void setDateFormatterForPattern(NSTimestampFormatter formatter, String pattern) {
		if(ERXLocalizer.isLocalizationEnabled()) {
			ERXLocalizer localizer = ERXLocalizer.currentLocalizer();
			localizer.setLocalizedDateFormatForKey(formatter, pattern);
		} else {
			synchronized(_repository) {
				if(formatter == null) {
					_repository.remove(pattern);
				} else {
					_repository.put(pattern, formatter);
				}
			}
		}
	}
	
	/**
	 * 
	 */
	public ERXTimestampFormatter() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ERXTimestampFormatter(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ERXTimestampFormatter(String arg0, DateFormatSymbols arg1) {
		super(arg0, arg1);
	}

}
