package er.extensions.components;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODirectAction;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.WOSession;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

import er.extensions.ERXExtensions;
import er.extensions.appserver.ERXApplication;
import er.extensions.appserver.ERXResourceManager;
import er.extensions.appserver.ERXResponseRewriter;
import er.extensions.appserver.ajax.ERXAjaxApplication;
import er.extensions.foundation.ERXExpiringCache;
import er.extensions.foundation.ERXProperties;
import er.extensions.foundation.ERXStringUtilities;

/**
 * Copied from ERExtensions to enable the "title"-attribute for stylesheets.
 * 
 * 
 * 
 * 
 * 
 * 
 * Adds a style sheet to a page. You can either supply a complete URL, a file
 * and framework name or put something in the component content. The content of
 * the component is cached under a "key" binding and then delivered via a direct
 * action, so it doesn't need to get re-rendered too often.
 * 
 * @binding filename name of the style sheet
 * @binding framework name of the framework for the style sheet
 * @binding href url to the style sheet
 * @binding key key to cache the style sheet under when using the component
 *          content. Default is the sessionID. That means, you should *really*
 *          explicitly set a key, when you use more than one ERXStyleSheet using
 *          the component content method within one session
 * @binding inline when true, the generated link tag will be appended inline,
 *          when false it'll be placed in the head of the page, when unset it
 *          will be placed inline for ajax requests and in the head for regular
 *          requests
 * @property er.extensions.ERXStyleSheet.xhtml (defaults true) if false, link
 *           tags are not closed, which is compatible with older HTML
 */
// FIXME: cache should be able to cache on calues of bindings, not a single key
public class ERXStyleSheet extends ERXStatelessComponent {

	/** logging support */
	public static final Logger log = Logger.getLogger( ERXStyleSheet.class );

	/**
	 * Public constructor
	 * 
	 * @param aContext
	 *            a context
	 */
	public ERXStyleSheet( WOContext aContext ) {
		super( aContext );
	}

	@SuppressWarnings( "unchecked" )
	private static ERXExpiringCache<String, WOResponse> cache( WOSession session ) {
		ERXExpiringCache<String, WOResponse> cache = (ERXExpiringCache<String, WOResponse>)session.objectForKey( "ERXStylesheet.cache" );
		if( cache == null ) {
			cache = new ERXExpiringCache<String, WOResponse>( 60 );
			cache.startBackgroundExpiration();
			session.setObjectForKey( cache, "ERXStylesheet.cache" );
		}
		return cache;
	}

	public static class Sheet extends WODirectAction {
		public Sheet( WORequest worequest ) {
			super( worequest );
		}

		@Override
		public WOActionResults performActionNamed( String name ) {
			WOResponse response = ERXStyleSheet.cache( session() ).objectForKey( name );
			String md5 = ERXStringUtilities.md5Hex( response.contentString(), null );
			String queryMd5 = response.headerForKey( "checksum" );
			if( ERXExtensions.safeEquals( md5, queryMd5 ) ) {
				//TODO check for last-whatever time and return not modified if not changed
			}
			return response;
		}
	}

	/**
	 * returns the complete url to the style sheet.
	 * 
	 * @return style sheet url
	 */
	public String styleSheetUrl() {
		String url = (String)valueForBinding( "styleSheetUrl" );
		url = (url == null ? (String)valueForBinding( "href" ) : url);
		if( url == null ) {
			String name = styleSheetName();
			if( name != null ) {
				url = application().resourceManager().urlForResourceNamed( styleSheetName(), styleSheetFrameworkName(), languages(), context().request() );
				if( ERXResourceManager._shouldGenerateCompleteResourceURL( context() ) ) {
					url = ERXResourceManager._completeURLForResource( url, null, context() );
				}
			}
		}
		return url;
	}

	/**
	 * Returns the style sheet framework name either resolved via the binding
	 * <b>framework</b>.
	 * 
	 * @return style sheet framework name
	 */
	public String styleSheetFrameworkName() {
		String result = (String)valueForBinding( "styleSheetFrameworkName" );
		result = (result == null ? (String)valueForBinding( "framework" ) : result);
		return result;
	}

	/**
	 * Returns the style sheet name either resolved via the binding <b>filename</b>.
	 * 
	 * @return style sheet name
	 */
	public String styleSheetName() {
		String result = (String)valueForBinding( "styleSheetName" );
		result = (result == null ? (String)valueForBinding( "filename" ) : result);
		return result;
	}

	/**
	 * Returns key under which the stylesheet should be placed in the cache. If
	 * no key is given, the session id is used.
	 * 
	 * @return style sheet framework name
	 */
	public String styleSheetKey() {
		String result = (String)valueForBinding( "key" );
		if( result == null ) {
			result = context().session().sessionID();
		}
		return result;
	}

	/**
	 * Specifies the relationship between the current document and the linked document.
	 */
	public String rel() {
		return stringValueForBinding( "rel" );
	}

	/**
	 * Specifies extra information about an element.
	 */
	public String title() {
		return stringValueForBinding( "title" );
	}

	/**
	 * Specifies on what device the linked document will be displayed.
	 */
	public String mediaType() {
		return stringValueForBinding( "media" );
	}

	/**
	 * Returns the languages for the request.
	 */
	@SuppressWarnings( "unchecked" )
	private NSArray<String> languages() {
		if( hasSession() ) {
			return session().languages();
		}
		WORequest request = context().request();
		if( request != null ) {
			return request.browserLanguages();
		}
		return null;
	}

	/**
	 * Appends the &ltlink&gt; tag, either by using the style sheet name and
	 * framework or by using the component content and then generating a link to
	 * it.
	 */
	@Override
	public void appendToResponse( WOResponse originalResponse, WOContext wocontext ) {
		String styleSheetFrameworkName = styleSheetFrameworkName();
		String styleSheetName = styleSheetName();
		boolean isResourceStyleSheet = styleSheetName != null;
		if( isResourceStyleSheet && ERXResponseRewriter.isResourceAddedToHead( wocontext, styleSheetFrameworkName, styleSheetName ) ) {
			// Skip, because this has already been added ... 
		}
		else {
			// default to inline for ajax requests
			boolean inline = booleanValueForBinding( "inline", ERXAjaxApplication.isAjaxRequest( wocontext.request() ) );
			WOResponse response = inline ? originalResponse : new WOResponse();

			String href = styleSheetUrl();
			if( href == null ) {
				String key = styleSheetKey();
				ERXExpiringCache<String, WOResponse> cache = cache( session() );
				String md5;
				WOResponse cachedResponse = cache.objectForKey( key );
				if( cache.isStale( key ) || ERXApplication.isDevelopmentModeSafe() ) {
					cachedResponse = new WOResponse();
					super.appendToResponse( cachedResponse, wocontext );
					// appendToResponse above will change the response of
					// "wocontext" to "newresponse". When this happens during an
					// Ajax request, it will lead to backtracking errors on
					// subsequent requests, so restore the original response "r"
					wocontext._setResponse( originalResponse );
					cachedResponse.setHeader( "text/css", "content-type" );
					cache.setObjectForKey( cachedResponse, key );
					md5 = ERXStringUtilities.md5Hex( cachedResponse.contentString(), null );
					cachedResponse.setHeader( md5, "checksum" );
				}
				md5 = cachedResponse.headerForKey( "checksum" );
				NSDictionary query = new NSDictionary<String, String>( md5, "checksum" );
				href = wocontext.directActionURLForActionNamed( Sheet.class.getName() + "/" + key, query );
			}

			response._appendContentAsciiString( "<link " );

			String rel = rel();

			if( rel == null )
				rel = "stylesheet";

			response._appendTagAttributeAndValue( "rel", rel, false );
			response._appendTagAttributeAndValue( "type", "text/css", false );
			response._appendTagAttributeAndValue( "href", href, false );

			String media = mediaType();
			if( media != null ) {
				response._appendTagAttributeAndValue( "media", media, false );
			}

			String title = title();
			if( title != null ) {
				response._appendTagAttributeAndValue( "title", title, false );
			}

			response._appendContentAsciiString( ">" );
			if( ERXStyleSheet.shouldCloseLinkTags() ) {
				response._appendContentAsciiString( "</link>" );
			}
			response.appendContentString("\n");
			boolean inserted = true;
			if( !inline ) {
				String stylesheetLink = response.contentString();
				inserted = ERXResponseRewriter.insertInResponseBeforeHead( originalResponse, wocontext, stylesheetLink, ERXResponseRewriter.TagMissingBehavior.Inline );
			}
			if( inserted ) {
				if( isResourceStyleSheet ) {
					ERXResponseRewriter.resourceAddedToHead( wocontext, styleSheetFrameworkName, styleSheetName );
				}
				else if( href != null ) {
					ERXResponseRewriter.resourceAddedToHead( wocontext, null, href );
				}
			}
		}
	}

	/**
	 * Returns whether or not XHTML link tags should be used. If false, then
	 * link tags will not be closed, which is more compatible with certain
	 * browser parsers. Set the 'er.extensions.ERXStyleSheet.xhtml' to control
	 * this property.
	 * 
	 * @return true of link tags should be closed, false otherwise
	 */
	public static boolean shouldCloseLinkTags() {
		return ERXProperties.booleanForKeyWithDefault( "er.extensions.ERXStyleSheet.xhtml", true );
	}
}