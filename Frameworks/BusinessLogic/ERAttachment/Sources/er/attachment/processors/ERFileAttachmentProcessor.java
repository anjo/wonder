package er.attachment.processors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSTimestamp;

import er.attachment.model.ERFileAttachment;
import er.extensions.foundation.ERXFileUtilities;
import er.extensions.foundation.ERXProperties;

/**
 * ERFileAttachmentProcessor implements storing attachments as files on the system that are either served
 * via a proxy request handler or directly by the webserver.  For more information about configuring
 * an ERFileAttachmentProcessor, see the top level documentation.
 * 
 * @author mschrag
 */
public class ERFileAttachmentProcessor extends ERAttachmentProcessor<ERFileAttachment> {
  @Override
  public ERFileAttachment _process(EOEditingContext editingContext, File uploadedFile, String recommendedFileName, String mimeType, String configurationName, String ownerID, boolean pendingDelete) throws IOException {
    boolean proxy = true;
    String proxyStr = ERXProperties.stringForKey("er.attachment." + configurationName + ".file.proxy");
    if (proxyStr == null) {
      proxyStr = ERXProperties.stringForKey("er.attachment.file.proxy");
    }
    if (proxyStr != null) {
      proxy = Boolean.parseBoolean(proxyStr);
    }

    boolean overwrite = false;
    String overwriteStr = ERXProperties.stringForKey("er.attachment." + configurationName + ".file.overwrite");
    if (overwriteStr == null) {
      overwriteStr = ERXProperties.stringForKey("er.attachment.file.overwrite");
    }
    if (overwriteStr != null) {
      overwrite = Boolean.parseBoolean(overwriteStr);
    }

    String filesystemPath = ERXProperties.stringForKey("er.attachment." + configurationName + ".file.filesystemPath");
    if (filesystemPath == null) {
      filesystemPath = ERXProperties.stringForKey("er.attachment.file.filesystemPath");
    }
    if (filesystemPath == null) {
      throw new IllegalArgumentException("There is no 'er.attachment." + configurationName + ".file.filesystemPath' or 'er.attachment.file.filesystemPath' property set.");
    }
    if (!filesystemPath.contains("${")) {
      filesystemPath = filesystemPath + "/attachments/${hash}/${pk}${ext}";
    }

    String webPath = ERXProperties.stringForKey("er.attachment." + configurationName + ".file.webPath");
    if (webPath == null) {
      webPath = ERXProperties.stringForKey("er.attachment.file.webPath");
    }
    if (webPath == null) {
      if (!proxy) {
        throw new IllegalArgumentException("There is no 'er.attachment." + configurationName + ".file.webPath' or 'er.attachment.file.webPath' property set.");
      }
      webPath = "/${pk}${ext}";
    }
    else if (!webPath.startsWith("/")) {
      webPath = "/" + webPath;
    }

    ERFileAttachment attachment = ERFileAttachment.createERFileAttachment(editingContext, Boolean.TRUE, new NSTimestamp(), mimeType, recommendedFileName, Boolean.valueOf(proxy), Integer.valueOf((int) uploadedFile.length()), webPath);
    if (delegate() != null) {
      delegate().attachmentCreated(this, attachment);
    }
    try {
      webPath = ERAttachmentProcessor._parsePathTemplate(attachment, webPath, recommendedFileName);
      filesystemPath = ERAttachmentProcessor._parsePathTemplate(attachment, filesystemPath, recommendedFileName);

      File desiredFilesystemPath = new File(filesystemPath);
      File actualFilesystemPath = ERXFileUtilities.reserveUniqueFile(desiredFilesystemPath, overwrite);

      ERXFileUtilities.copyFileToFile(uploadedFile, actualFilesystemPath, pendingDelete, true);

      String desiredFileName = desiredFilesystemPath.getName();
      String actualFileName = actualFilesystemPath.getName();
      // in case the name was not unique and changed, we need to update webPath ...
      webPath = webPath.replaceAll(desiredFileName + "$", actualFileName);

      attachment.setWebPath(webPath);
      attachment.setFilesystemPath(actualFilesystemPath.getAbsolutePath());

      if (delegate() != null) {
        delegate().attachmentAvailable(this, attachment);
      }
    }
    catch (IOException e) {
      attachment.delete();
      throw e;
    }
    catch (RuntimeException e) {
      attachment.delete();
      throw e;
    }

    return attachment;
  }

  @Override
  public InputStream attachmentInputStream(ERFileAttachment attachment) throws IOException {
    return new FileInputStream(new File(attachment.filesystemPath()));
  }
  
  @Override
  public String attachmentUrl(ERFileAttachment attachment, WORequest request, WOContext context) {
    String attachmentUrl;
    if (!attachment.proxied().booleanValue()) {
      attachmentUrl = attachment.webPath();
    }
    else {
      attachmentUrl = proxiedUrl(attachment, context);
    }
    return attachmentUrl;
  }
  
  @Override
  public void deleteAttachment(ERFileAttachment attachment) throws IOException {
    String filesystemPath = attachment.filesystemPath();
    File attachmentFile = new File(filesystemPath);
    if (attachmentFile.exists() && !attachmentFile.delete()) {
      throw new IOException("Failed to delete the attachment '" + attachmentFile + "'.");
    }
  }
}