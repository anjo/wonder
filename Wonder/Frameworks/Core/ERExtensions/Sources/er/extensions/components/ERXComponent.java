package er.extensions.components;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.appserver.ERXResponseRewriter;
import er.extensions.eof.ERXEC;
import er.extensions.foundation.ERXUtilities;
import er.extensions.foundation.ERXValueUtilities;
import er.extensions.localization.ERXLocalizer;

/**
 * ERXComponent provides a common base class for WOComponents along with a bunch
 * of miscellaneous handy features.
 * 
 * @author mschrag
 */
public abstract class ERXComponent extends WOComponent {
	protected NSMutableDictionary _dynamicBindings = null;
	private EOEditingContext _editingContext;
	
	/**
	 * Constructs a new ERXComponent.
	 * 
	 * @param context
	 *            the WOContext
	 */
	public ERXComponent(WOContext context) {
		super(context);
	}
	
	/**
	 * Returns whether or not this component should use a shared editing context at the page level when you call the
	 * editingContext() method. This method defaults to true.
	 * 
	 * @return whether or not this component should use a shared editing context at the page level
	 */
	protected boolean usePageEditingContext() {
		return true;
	}
	
	/**
	 * Explicitly sets the editing context for this component.
	 * 
	 * @param editingContext the new editing context to use for this component
	 */
	public void setEditingContext(EOEditingContext editingContext) {
		_editingContext = editingContext;
	}
	
	/**
	 * Returns an editing context for use in this component. If usePageEditingContext() returns true (the default), 
	 * this component will attempt to retrieve the page editing context by calling "editingContext" on the page
	 * component. If that returns null (or usePageEditingContext() returns false), this component will create a
	 * new editing context and store it in an ivar. Subsequent calls to this method will return the same 
	 * editing context instance. This functionality is not available for stateless components. 
	 * 
	 * @return an editing context for use in this page
	 */
	public EOEditingContext editingContext() {
		EOEditingContext editingContext = _editingContext;
		if (editingContext == null) {
			if (usePageEditingContext()) {
				WOComponent page = context().page();
				if (page != this) {
					editingContext = (EOEditingContext)page.valueForKey("editingContext");
				}
			}
			if (editingContext == null) {
				if (isStateless()) {
					throw new IllegalStateException("You can not request an editing context for a stateless component.");
				}
				_editingContext = ERXEC.newEditingContext();
			}
		}
		return _editingContext;
	}

	/**
	 * This variant of pageWithName provides a Java5 genericized version of the
	 * original pageWithName. You would call it with:
	 * 
	 * MyNextPage nextPage = pageWithName(MyNextPage.class);
	 * 
	 * @param <T>
	 *            the type of component to create
	 * @param componentClass
	 *            the Class of the component to load
	 * @return an instance of the requested component class
	 */
	@SuppressWarnings("unchecked")
	public <T extends WOComponent> T pageWithName(Class<T> componentClass) {
		return (T) super.pageWithName(componentClass.getName());
	}

	@Override
	public void _awakeInContext(WOContext aArg0) {
		super._awakeInContext(aArg0);
		if (isStateless()) {
			_dynamicBindings = null;
		}
	}

	/**
	 * _checkAccess is called prior to all three phases of the R-R loop to
	 * ensure that the user has permission to access this component. You should
	 * override checkAccess to implement addition security checks.
	 * 
	 * @throws SecurityException
	 *             if the user does not have permission
	 */
	protected void _checkAccess() throws SecurityException {
		if (!isPageAccessAllowed() && _isPage()) {
			throw new SecurityException("You are not allowed to directly access the component '" + name() + "'.");
		}
		if (_isPage()) {
			checkAccess();
		}
	}

	/**
	 * Calls _checkAccess prior to super.takeValuesFromRequest.
	 */
	@Override
	public void takeValuesFromRequest(WORequest request, WOContext context) {
		_checkAccess();
		super.takeValuesFromRequest(request, context);
	}

	/**
	 * Calls _checkAccess prior to super.invokeAction.
	 */
	@Override
	public WOActionResults invokeAction(WORequest request, WOContext context) {
		_checkAccess();
		return super.invokeAction(request, context);
	}

	/**
	 * Calls _checkAccess prior to super.appendToResponse and adds support for
	 * ClickToOpen (TM).
	 */
	@Override
	public void appendToResponse(WOResponse response, WOContext context) {
		_checkAccess();
		preAppendToResponse(response, context);

		boolean clickToOpenEnabled = clickToOpenEnabled(response, context);
		ERXClickToOpenSupport.preProcessResponse(response, context, clickToOpenEnabled);
		try {
			super.appendToResponse(response, context);
		}
		finally {
			ERXClickToOpenSupport.postProcessResponse(getClass(), response, context, clickToOpenEnabled);
		}

		postAppendToResponse(response, context);

		_includeCSSResources(response, context);
		_includeJavascriptResources(response, context);
	}

	/**
	 * Returns whether or not click-to-open should be enabled for this
	 * component. By default this returns ERXClickToOpenSupport.isEnabled().
	 * 
	 * @param response
	 *            the response
	 * @param context
	 *            the context
	 * @return whether or not click-to-open is enabled for this component
	 */
	public boolean clickToOpenEnabled(WOResponse response, WOContext context) {
		return ERXClickToOpenSupport.isEnabled();
	}

	/**
	 * Resolves a given binding as a int value. Useful for image sizes and the
	 * like.
	 * 
	 * @param binding
	 *            binding to be resolved as a int value.
	 * @param defaultValue
	 *            default int value to be used if the binding is not bound.
	 * @return result of evaluating binding as a int.
	 */
	protected int intValueForBinding(String binding, int defaultValue) {
		return ERXValueUtilities.intValueWithDefault(valueForBinding(binding), defaultValue);
	}

	/**
	 * Resolves a given binding as a float value.
	 * 
	 * @param binding
	 *            binding to be resolved as a float value.
	 * @param defaultValue
	 *            default float value to be used if the binding is not bound.
	 * @return result of evaluating binding as a float.
	 */
	protected float floatValueForBinding(String binding, float defaultValue) {
		return ERXValueUtilities.floatValueWithDefault(valueForBinding(binding), defaultValue);
	}

	/**
	 * Resolves a given binding as a boolean value. Defaults to false.
	 * 
	 * @param binding
	 *            binding to be resolved as a boolean value.
	 * @return result of evaluating binding as a boolean.
	 */
	protected boolean booleanValueForBinding(String binding) {
		return booleanValueForBinding(binding, false);
	}

	/**
	 * Resolves a given binding as a boolean value.
	 * 
	 * @param binding
	 *            binding to be resolved as a boolean value.
	 * @param defaultValue
	 *            default boolean value to be used if the binding is not bound.
	 * @return result of evaluating binding as a boolean.
	 */
	protected boolean booleanValueForBinding(String binding, boolean defaultValue) {
		return ERXComponentUtilities.booleanValueForBinding(this, binding, defaultValue);
	}

	/**
	 * Resolves a given binding as a boolean value with the option of specifing
	 * a boolean operator as the default value.
	 * 
	 * @param binding
	 *            name of the component binding.
	 * @param defaultValue
	 *            boolean operator to be evaluated if the binding is not
	 *            present.
	 * @return result of evaluating binding as a boolean.
	 */
	protected boolean booleanValueForBinding(String binding, ERXUtilities.BooleanOperation defaultValue) {
		if (hasBinding(binding)) {
			return booleanValueForBinding(binding, false);
		}
		return defaultValue.value();
	}

	/**
	 * Resolves a given binding as an object in the normal fashion of calling
	 * <code>valueForBinding</code>. This has the one advantage of being able to
	 * resolve the resulting object as a {link ERXUtilities$Operation} if it is
	 * an Operation and then returning the result as the evaluation of that
	 * operation.
	 * 
	 * @param binding
	 *            name of the component binding.
	 * @return the object for the given binding and in the case that it is an
	 *         instance of an Operation the value of that operation.
	 */
	protected Object objectValueForBinding(String binding) {
		return objectValueForBinding(binding, null);
	}

	/**
	 * Resolves a given binding as an object in the normal fashion of calling
	 * <code>valueForBinding</code>. This has the one advantage of being able to
	 * resolve the resulting object as a {link ERXUtilities$Operation} if it is
	 * an Operation and then returning the result as the evaluation of that
	 * operation.
	 * 
	 * @param binding
	 *            name of the component binding.
	 * @param defaultValue
	 *            value to be used if <code>valueForBinding</code> returns null.
	 * @return the object for the given binding and in the case that it is an
	 *         instance of an Operation the value of that operation.
	 */
	protected Object objectValueForBinding(String binding, Object defaultValue) {
		Object result = null;
		if (hasBinding(binding)) {
			Object o = valueForBinding(binding);
			result = (o == null) ? defaultValue : o;
		}
		else {
			result = defaultValue;
		}
		if (result instanceof ERXUtilities.Operation) {
			result = ((ERXUtilities.Operation) result).value();
		}
		return result;
	}

	/**
	 * Retrieves a given binding and if it is not null then returns
	 * <code>toString</code> called on the bound object.
	 * 
	 * @param binding
	 *            to be resolved
	 * @return resolved binding in string format
	 */
	protected String stringValueForBinding(String binding) {
		return stringValueForBinding(binding, null);
	}

	/**
	 * Retrieves a given binding and if it is not null then returns
	 * <code>toString</code> called on the bound object.
	 * 
	 * @param binding
	 *            to be resolved
	 * @param defaultValue
	 *            value to be used if <code>valueForBinding</code> returns null.
	 * @return resolved binding in string format
	 */
	protected String stringValueForBinding(String binding, String defaultValue) {
		Object v = objectValueForBinding(binding, defaultValue);
		return v != null ? v.toString() : null;
	}

	/**
	 * Convenience method to get the localizer.
	 * 
	 * @return the current localizer 
	 */
	public ERXLocalizer localizer() {
		return ERXLocalizer.currentLocalizer();
	}

	/**
	 * Lazily initialized dictionary which can be used for the 'item' binding in
	 * a repetition for example: 'item = dynamicBindings.myVariable'. Useful in
	 * rapid turnaround modes where adding a iVar would cause hot code swapping
	 * to stop working.
	 * 
	 * @return a dictionay for use with dynamic bindings 
	 */
	public NSMutableDictionary dynamicBindings() {
		if (_dynamicBindings == null) {
			_dynamicBindings = new NSMutableDictionary();
		}
		return _dynamicBindings;
	}

	@Override
	public void reset() {
		super.reset();
		if (_dynamicBindings != null) {
			_dynamicBindings.removeAllObjects();
		}
	}

	/**
	 * Returns the name of this component without the package name.
	 * 
	 * @return the name of this component without the package name
	 */
	public String componentName() {
		String componentName = name();
		if (componentName != null) {
			int lastDotIndex = componentName.lastIndexOf('.');
			if (lastDotIndex != -1) {
				componentName = componentName.substring(lastDotIndex + 1);
			}
		}
		return componentName;
	}

	/**
	 * <p>
	 * Injects per-component CSS dependencies into the head tag based on the
	 * definitions in useDefaultComponentCSS(), defaultCSSPath(),
	 * primaryCSSFile(), and additionalCSSFiles().
	 * </p>
	 * <p>
	 * If you return true for useDefaultComponentCSS (and do not override
	 * primaryCSSFile), this component will inject a reference to
	 * defaultCSSPath() + /YourComponentName.css. For instance, if your
	 * component is named HeaderFooter, useDefaultComponentCSS will
	 * automatically add a reference to defaultCSSPath() + /HeaderFooter.css for
	 * you. This allows you to very easily specify per-component CSS files
	 * without upper-level components knowing about them. Currently
	 * _includeCSSResources does not try to do anything fancy in terms of
	 * recombinding CSS files.
	 * </p>
	 * <p>
	 * Override defaultCSSPath to provide the base path relative to
	 * WebServerResources that contains your CSS files. If all of your CSS is in
	 * WebServerResources/css, you would return "css" from defaultCSSPath().
	 * </p>
	 * <p>
	 * If you do not want to use the component's name as the name of the CSS
	 * file, you can optionally override primaryCSSFile() to return the name of
	 * a specific CSS file, as well as additionalCSSFiles() to return an NSArray
	 * of CSS files. All of these file names will be prepended with the
	 * defaultCSSPath if it is set.
	 * </p>
	 * 
	 * @param response
	 *            the response to write into
	 * @param context
	 *            the current context
	 */
	protected void _includeCSSResources(WOResponse response, WOContext context) {
		String primaryCSSFile = primaryCSSFile();
		if (primaryCSSFile == null && useDefaultComponentCSS()) {
			String componentName = componentName();
			primaryCSSFile = componentName + ".css";
		}
		if (primaryCSSFile != null) {
			String defaultCSSPath = defaultCSSPath();
			if (defaultCSSPath != null && defaultCSSPath.length() > 0 && !defaultCSSPath.endsWith("/")) {
				defaultCSSPath += "/";
			}
			String frameworkName = _frameworkName();
			ERXResponseRewriter.addStylesheetResourceInHead(response, context, frameworkName, defaultCSSPath + primaryCSSFile);
		}

		NSArray<String> additionalCSSFiles = additionalCSSFiles();
		if (additionalCSSFiles != null) {
			String defaultCSSPath = defaultCSSPath();
			if (defaultCSSPath != null && defaultCSSPath.length() > 0 && !defaultCSSPath.endsWith("/")) {
				defaultCSSPath += "/";
			}
			String frameworkName = _frameworkName();
			for (String additionalCSSFile : additionalCSSFiles) {
				ERXResponseRewriter.addStylesheetResourceInHead(response, context, frameworkName, defaultCSSPath + additionalCSSFile);
			}
		}
	}

	/**
	 * <p>
	 * Injects per-component javascript dependencies into the head tag based on
	 * the definitions in useDefaultComponentJavascript(),
	 * defaultJavascriptPath(), primaryJavascriptFile(), and
	 * additionalJavascriptFiles().
	 * </p>
	 * <p>
	 * If you return true for useDefaultComponentJavascript (and do not override
	 * primaryJavascriptFile), this component will inject a reference to
	 * defaultJavascriptPath() + /YourComponentName.js. For instance, if your
	 * component is named HeaderFooter, useDefaultComponentJavascript will
	 * automatically add a reference to defaultJavascriptPath() +
	 * /HeaderFooter.js for you. This allows you to very easily specify
	 * per-component Javascript files without upper-level components knowing
	 * about them. Currently _includeJavascriptResources does not try to do
	 * anything fancy in terms of recombinding Javascript files.
	 * </p>
	 * <p>
	 * Override defaultJavascriptPath to provide the base path relative to
	 * WebServerResources that contains your Javascript files. If all of your
	 * Javascript is in WebServerResources/scripts, you would return "scripts"
	 * from defaultJavascriptPath().
	 * </p>
	 * <p>
	 * If you do not want to use the component's name as the name of the
	 * Javascript file, you can optionally override primaryJavascriptFile() to
	 * return the name of a specific Javascript file, as well as
	 * additionalJavascriptFiles() to return an NSArray of Javascript files. All
	 * of these file names will be prepended with the defaultJavascriptPath if
	 * it is set.
	 * </p>
	 * 
	 * @param response
	 *            the response to write into
	 * @param context
	 *            the current context
	 */
	protected void _includeJavascriptResources(WOResponse response, WOContext context) {
		String primaryJavascriptFile = primaryJavascriptFile();
		if (primaryJavascriptFile == null && useDefaultComponentJavascript()) {
			String componentName = componentName();
			primaryJavascriptFile = componentName + ".js";
		}
		if (primaryJavascriptFile != null) {
			String defaultJavascriptPath = defaultJavascriptPath();
			if (defaultJavascriptPath != null && defaultJavascriptPath.length() > 0 && !defaultJavascriptPath.endsWith("/")) {
				defaultJavascriptPath += "/";
			}
			String frameworkName = _frameworkName();
			ERXResponseRewriter.addScriptResourceInHead(response, context, frameworkName, defaultJavascriptPath + primaryJavascriptFile);
		}

		NSArray<String> additionalJavascriptFiles = additionalJavascriptFiles();
		if (additionalJavascriptFiles != null) {
			String defaultJavascriptPath = defaultJavascriptPath();
			if (defaultJavascriptPath != null && defaultJavascriptPath.length() > 0 && !defaultJavascriptPath.endsWith("/")) {
				defaultJavascriptPath += "/";
			}
			String frameworkName = _frameworkName();
			for (String additionalJavascriptFile : additionalJavascriptFiles) {
				ERXResponseRewriter.addScriptResourceInHead(response, context, frameworkName, defaultJavascriptPath + additionalJavascriptFile);
			}
		}
	}

	/**
	 * Returns the name of this component's framework or "app" if
	 * frameworkName() returns null.
	 * 
	 * @return the name of this component's framework
	 */
	protected String _frameworkName() {
		String frameworkName = super.frameworkName();
		if (frameworkName == null) {
			frameworkName = "app";
		}
		return frameworkName;
	}

	/**
	 * Returns true if this component provides a default CSS file that has the
	 * same name as the component itself.
	 * 
	 * @return true if this component provides a default-named CSS
	 */
	protected boolean useDefaultComponentCSS() {
		return false;
	}

	/**
	 * Returns the default path prefix for CSS, which will be prepended to all
	 * required CSS files for this component. The default is "".
	 * 
	 * @return the default CSS path.
	 */
	protected String defaultCSSPath() {
		return "";
	}

	/**
	 * Returns the primary CSS file for this component, or null if there isn't
	 * one. This path will be prepended with defaultCSSPath().
	 * 
	 * @return the primary CSS file for this component
	 */
	protected String primaryCSSFile() {
		return null;
	}

	/**
	 * Returns an array of additional CSS files for this component, or null (or
	 * empty array) if there aren't any. Each path will be prepended with
	 * defaultCSSPath().
	 * 
	 * @return an array of additional CSS files for this component.
	 */
	protected NSArray<String> additionalCSSFiles() {
		return null;
	}

	/**
	 * Returns true if this component provides a default Javascript file that
	 * has the same name as the component itself.
	 * 
	 * @return true if this component provides a default-named Javascript
	 */
	protected boolean useDefaultComponentJavascript() {
		return false;
	}

	/**
	 * Returns the default path prefix for Javascript, which will be prepended
	 * to all required Javascript files for this component. The default is "".
	 * 
	 * @return the default Javascript path.
	 */
	protected String defaultJavascriptPath() {
		return "";
	}

	/**
	 * Returns the primary Javascript file for this component, or null if there
	 * isn't one. This path will be prepended with defaultJavascriptPath().
	 * 
	 * @return the primary Javascript file for this component
	 */
	protected String primaryJavascriptFile() {
		return null;
	}

	/**
	 * Returns an array of additional Javascript files for this component, or
	 * null (or empty array) if there aren't any. Each path will be prepended
	 * with defaultJavascriptPath().
	 * 
	 * @return an array of additional Javascript files for this component.
	 */
	protected NSArray<String> additionalJavascriptFiles() {
		return null;
	}

	/**
	 * Override and return true for any components to which you would like to
	 * allow page level access.
	 * 
	 * @return true by default
	 */
	protected boolean isPageAccessAllowed() {
		return true;
	}

	/**
	 * Override to provide custom security checks. It is not necessary to call
	 * super on this method.
	 * 
	 * @throws SecurityException
	 *             if the security check fails
	 */
	protected void checkAccess() throws SecurityException {
	}

	/**
	 * Override to hook into appendToResponse after security checks but before
	 * the super.appendToResponse. It is not necessary to call super on this
	 * method.
	 * 
	 * @param response
	 *            the current response
	 * @param context
	 *            the current context
	 */
	protected void preAppendToResponse(WOResponse response, WOContext context) {
	}

	/**
	 * Override to hook into appendToResponse after super.appendToResponse. It
	 * is not necessary to call super on this method.
	 * 
	 * @param response
	 *            the current response
	 * @param context
	 *            the current context
	 */
	protected void postAppendToResponse(WOResponse response, WOContext context) {
	}
}
