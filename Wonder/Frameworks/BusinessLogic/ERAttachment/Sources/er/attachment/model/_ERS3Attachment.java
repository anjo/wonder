// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to ERS3Attachment.java instead.
package er.attachment.model;

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.*;
import java.util.*;
import org.apache.log4j.Logger;

import er.extensions.eof.*;
import er.extensions.foundation.*;

@SuppressWarnings("all")
public abstract class _ERS3Attachment extends er.attachment.model.ERAttachment {
	public static final String ENTITY_NAME = "ERS3Attachment";

	// Attributes
	public static final String AVAILABLE_KEY = "available";
	public static final ERXKey<Boolean> AVAILABLE = new ERXKey<Boolean>(AVAILABLE_KEY);
	public static final String CONFIGURATION_NAME_KEY = "configurationName";
	public static final ERXKey<String> CONFIGURATION_NAME = new ERXKey<String>(CONFIGURATION_NAME_KEY);
	public static final String CREATION_DATE_KEY = "creationDate";
	public static final ERXKey<NSTimestamp> CREATION_DATE = new ERXKey<NSTimestamp>(CREATION_DATE_KEY);
	public static final String HEIGHT_KEY = "height";
	public static final ERXKey<Integer> HEIGHT = new ERXKey<Integer>(HEIGHT_KEY);
	public static final String MIME_TYPE_KEY = "mimeType";
	public static final ERXKey<String> MIME_TYPE = new ERXKey<String>(MIME_TYPE_KEY);
	public static final String ORIGINAL_FILE_NAME_KEY = "originalFileName";
	public static final ERXKey<String> ORIGINAL_FILE_NAME = new ERXKey<String>(ORIGINAL_FILE_NAME_KEY);
	public static final String OWNER_ID_KEY = "ownerID";
	public static final ERXKey<String> OWNER_ID = new ERXKey<String>(OWNER_ID_KEY);
	public static final String PROXIED_KEY = "proxied";
	public static final ERXKey<Boolean> PROXIED = new ERXKey<Boolean>(PROXIED_KEY);
	public static final String S3_PATH_KEY = "s3Path";
	public static final ERXKey<String> S3_PATH = new ERXKey<String>(S3_PATH_KEY);
	public static final String SIZE_KEY = "size";
	public static final ERXKey<Integer> SIZE = new ERXKey<Integer>(SIZE_KEY);
	public static final String STORAGE_TYPE_KEY = "storageType";
	public static final ERXKey<String> STORAGE_TYPE = new ERXKey<String>(STORAGE_TYPE_KEY);
	public static final String THUMBNAIL_KEY = "thumbnail";
	public static final ERXKey<String> THUMBNAIL = new ERXKey<String>(THUMBNAIL_KEY);
	public static final String WEB_PATH_KEY = "webPath";
	public static final ERXKey<String> WEB_PATH = new ERXKey<String>(WEB_PATH_KEY);
	public static final String WIDTH_KEY = "width";
	public static final ERXKey<Integer> WIDTH = new ERXKey<Integer>(WIDTH_KEY);

	// Relationships
	public static final String CHILDREN_ATTACHMENTS_KEY = "childrenAttachments";
	public static final ERXKey<er.attachment.model.ERAttachment> CHILDREN_ATTACHMENTS = new ERXKey<er.attachment.model.ERAttachment>(CHILDREN_ATTACHMENTS_KEY);
	public static final String PARENT_ATTACHMENT_KEY = "parentAttachment";
	public static final ERXKey<er.attachment.model.ERAttachment> PARENT_ATTACHMENT = new ERXKey<er.attachment.model.ERAttachment>(PARENT_ATTACHMENT_KEY);

  private static Logger LOG = Logger.getLogger(_ERS3Attachment.class);

  public ERS3Attachment localInstanceIn(EOEditingContext editingContext) {
    ERS3Attachment localInstance = (ERS3Attachment)EOUtilities.localInstanceOfObject(editingContext, this);
    if (localInstance == null) {
      throw new IllegalStateException("You attempted to localInstance " + this + ", which has not yet committed.");
    }
    return localInstance;
  }

  public String s3Path() {
    return (String) storedValueForKey("s3Path");
  }

  public void setS3Path(String value) {
    if (_ERS3Attachment.LOG.isDebugEnabled()) {
    	_ERS3Attachment.LOG.debug( "updating s3Path from " + s3Path() + " to " + value);
    }
    takeStoredValueForKey(value, "s3Path");
  }


  public static ERS3Attachment createERS3Attachment(EOEditingContext editingContext, Boolean available
, NSTimestamp creationDate
, String mimeType
, String originalFileName
, Boolean proxied
, Integer size
, String webPath
) {
    ERS3Attachment eo = (ERS3Attachment) EOUtilities.createAndInsertInstance(editingContext, _ERS3Attachment.ENTITY_NAME);    
		eo.setAvailable(available);
		eo.setCreationDate(creationDate);
		eo.setMimeType(mimeType);
		eo.setOriginalFileName(originalFileName);
		eo.setProxied(proxied);
		eo.setSize(size);
		eo.setWebPath(webPath);
    return eo;
  }

  public static NSArray<ERS3Attachment> fetchAllERS3Attachments(EOEditingContext editingContext) {
    return _ERS3Attachment.fetchAllERS3Attachments(editingContext, null);
  }

  public static NSArray<ERS3Attachment> fetchAllERS3Attachments(EOEditingContext editingContext, NSArray<EOSortOrdering> sortOrderings) {
    return _ERS3Attachment.fetchERS3Attachments(editingContext, null, sortOrderings);
  }

  public static NSArray<ERS3Attachment> fetchERS3Attachments(EOEditingContext editingContext, EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings) {
    EOFetchSpecification fetchSpec = new EOFetchSpecification(_ERS3Attachment.ENTITY_NAME, qualifier, sortOrderings);
    fetchSpec.setIsDeep(true);
    NSArray<ERS3Attachment> eoObjects = (NSArray<ERS3Attachment>)editingContext.objectsWithFetchSpecification(fetchSpec);
    return eoObjects;
  }

  public static ERS3Attachment fetchERS3Attachment(EOEditingContext editingContext, String keyName, Object value) {
    return _ERS3Attachment.fetchERS3Attachment(editingContext, new EOKeyValueQualifier(keyName, EOQualifier.QualifierOperatorEqual, value));
  }

  public static ERS3Attachment fetchERS3Attachment(EOEditingContext editingContext, EOQualifier qualifier) {
    NSArray<ERS3Attachment> eoObjects = _ERS3Attachment.fetchERS3Attachments(editingContext, qualifier, null);
    ERS3Attachment eoObject;
    int count = eoObjects.count();
    if (count == 0) {
      eoObject = null;
    }
    else if (count == 1) {
      eoObject = (ERS3Attachment)eoObjects.objectAtIndex(0);
    }
    else {
      throw new IllegalStateException("There was more than one ERS3Attachment that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static ERS3Attachment fetchRequiredERS3Attachment(EOEditingContext editingContext, String keyName, Object value) {
    return _ERS3Attachment.fetchRequiredERS3Attachment(editingContext, new EOKeyValueQualifier(keyName, EOQualifier.QualifierOperatorEqual, value));
  }

  public static ERS3Attachment fetchRequiredERS3Attachment(EOEditingContext editingContext, EOQualifier qualifier) {
    ERS3Attachment eoObject = _ERS3Attachment.fetchERS3Attachment(editingContext, qualifier);
    if (eoObject == null) {
      throw new NoSuchElementException("There was no ERS3Attachment that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static ERS3Attachment localInstanceIn(EOEditingContext editingContext, ERS3Attachment eo) {
    ERS3Attachment localInstance = (eo == null) ? null : (ERS3Attachment)EOUtilities.localInstanceOfObject(editingContext, eo);
    if (localInstance == null && eo != null) {
      throw new IllegalStateException("You attempted to localInstance " + eo + ", which has not yet committed.");
    }
    return localInstance;
  }
}