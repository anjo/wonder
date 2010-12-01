package er.attachment;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WORequestHandler;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.NSLog;

import er.attachment.model.ERAttachment;
import er.attachment.processors.ERAttachmentProcessor;
import er.extensions.components.ERXDynamicURL;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXEOGlobalIDUtilities;

/**
 * ERAttachmentRequestHandler is the request handler that is used for loading 
 * any proxied attachment.  To control security, you can set the delegate of this 
 * request handler in your application constructor.  By default, all proxied 
 * attachments are visible.
 * 
 * @author mschrag
 */
public class ERAttachmentRequestHandler extends WORequestHandler {
  public static final String REQUEST_HANDLER_KEY = "attachments";
  public static final Logger log = Logger.getLogger(ERAttachmentRequestHandler.class);

  /**
   * The delegate definition for this request handler.
   */
  public static interface Delegate {
    /**
     * Called prior to displaying a proxied attachment to a user and can be used to implement
     * security on top of attachments.
     * 
     * @param attachment the attachment that was requested
     * @param request the current request
     * @param context the current context
     * @return true if the current user is allowed to view this attachment
     */
    public boolean attachmentVisible(ERAttachment attachment, WORequest request, WOContext context);
  }

  private ERAttachmentRequestHandler.Delegate _delegate;

  /**
   * Sets the delegate for this request handler.
   * 
   * @param delegate the delegate for this request handler
   */
  public void setDelegate(ERAttachmentRequestHandler.Delegate delegate) {
    _delegate = delegate;
  }

  @Override
  public WOResponse handleRequest(WORequest request) {
    int bufferSize = 16384;

    WOApplication application = WOApplication.application();
    application.awake();
    try {
      WOContext context = application.createContextForRequest(request);
      WOResponse response = application.createResponseInContext(context);

      String wosid = (String) request.formValueForKey("wosid");
      if (wosid == null) {
        wosid = request.cookieValueForKey("wosid");
      }
      context._setRequestSessionID(wosid);
      WOSession session = null;
      if (context._requestSessionID() != null) {
        session = WOApplication.application().restoreSessionWithID(wosid, context);
      }
      try {
        ERXDynamicURL url = new ERXDynamicURL(request._uriDecomposed());
        String requestHandlerPath = url.requestHandlerPath();
        Matcher idMatcher = Pattern.compile("^id/(\\d+)/").matcher(requestHandlerPath);
        String idStr;
        String webPath;
        if (idMatcher.find()) {
          idStr = idMatcher.group(1);
          webPath = idMatcher.replaceFirst("/");
        }
        else {
          // MS: This is kind of goofy because we lookup by path, your web path needs to 
          // have a leading slash on it.
          webPath = "/" + requestHandlerPath;
          idStr = null;
        }

        try {
          InputStream attachmentInputStream;
          String mimeType;
          String fileName;
          long length;
          String queryString = url.queryString();
          boolean proxyAsAttachment = (queryString != null && queryString.contains("attachment=true"));

          EOEditingContext editingContext = ERXEC.newEditingContext();
          editingContext.lock();

          try {
            ERAttachment attachment;
            if (idStr != null) {
              EOGlobalID gid = EOKeyGlobalID.globalIDWithEntityName(ERAttachment.ENTITY_NAME, new Object[] { Integer.parseInt(idStr) });
              attachment = (ERAttachment) ERXEOGlobalIDUtilities.fetchObjectWithGlobalID(editingContext, gid);
              String actualWebPath = attachment.webPath();
              if (!actualWebPath.equals(webPath)) {
                throw new SecurityException("You are not allowed to view the requested attachment."); 
              }
            }
            else {
              attachment = ERAttachment.fetchRequiredAttachmentWithWebPath(editingContext, webPath);
            }
            if (_delegate != null && !_delegate.attachmentVisible(attachment, request, context)) {
              throw new SecurityException("You are not allowed to view the requested attachment.");
            }
            mimeType = attachment.mimeType();
            length = attachment.size().longValue();
            fileName = attachment.originalFileName();
            ERAttachmentProcessor<ERAttachment> attachmentProcessor = ERAttachmentProcessor.processorForType(attachment);
            if (!proxyAsAttachment) { 
              proxyAsAttachment = attachmentProcessor.proxyAsAttachment(attachment);
            }
            InputStream rawAttachmentInputStream = attachmentProcessor.attachmentInputStream(attachment);
            attachmentInputStream = new BufferedInputStream(rawAttachmentInputStream, bufferSize);
          }
          finally {
            editingContext.unlock();
          }
          response.setHeader(mimeType, "Content-Type");
          response.setHeader(String.valueOf(length), "Content-Length");

          if (proxyAsAttachment) {
            response.setHeader("attachment; filename=\"" + fileName+"\"", "Content-Disposition");
          }

          response.setStatus(200);
          response.setContentStream(attachmentInputStream, bufferSize, (int) length);
        }
        catch (SecurityException e) {
          NSLog.out.appendln(e);
          response.setContent(e.getMessage());
          response.setStatus(403);
        }
        catch (NoSuchElementException e) {
          NSLog.out.appendln(e);
          response.setContent(e.getMessage());
          response.setStatus(404);
        }
        catch (FileNotFoundException e) {
          NSLog.out.appendln(e);
          response.setContent(e.getMessage());
          response.setStatus(404);
        }
        catch (IOException e) {
          NSLog.out.appendln(e);
          response.setContent(e.getMessage());
          response.setStatus(500);
        }

        return response;
      }
      finally {
        if (context._requestSessionID() != null) {
          WOApplication.application().saveSessionForContext(context);
        }
      }
    }
    finally {
      application.sleep();
    }
  }
}