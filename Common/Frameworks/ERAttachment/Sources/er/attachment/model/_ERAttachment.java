// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to ERAttachment.java instead.
package er.attachment.model;

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.*;
import java.util.*;
import org.apache.log4j.Logger;

import er.extensions.eof.ERXGenericRecord;
import er.extensions.eof.ERXKey;

@SuppressWarnings("all")
public abstract class _ERAttachment extends er.extensions.eof.ERXGenericRecord {
	public static final String ENTITY_NAME = "ERAttachment";

	// Attributes
	public static final String AVAILABLE_KEY = "available";
	public static final ERXKey AVAILABLE = new ERXKey(AVAILABLE_KEY);
	public static final String CONFIGURATION_NAME_KEY = "configurationName";
	public static final ERXKey CONFIGURATION_NAME = new ERXKey(CONFIGURATION_NAME_KEY);
	public static final String CREATION_DATE_KEY = "creationDate";
	public static final ERXKey CREATION_DATE = new ERXKey(CREATION_DATE_KEY);
	public static final String HEIGHT_KEY = "height";
	public static final ERXKey HEIGHT = new ERXKey(HEIGHT_KEY);
	public static final String MIME_TYPE_KEY = "mimeType";
	public static final ERXKey MIME_TYPE = new ERXKey(MIME_TYPE_KEY);
	public static final String ORIGINAL_FILE_NAME_KEY = "originalFileName";
	public static final ERXKey ORIGINAL_FILE_NAME = new ERXKey(ORIGINAL_FILE_NAME_KEY);
	public static final String OWNER_ID_KEY = "ownerID";
	public static final ERXKey OWNER_ID = new ERXKey(OWNER_ID_KEY);
	public static final String PROXIED_KEY = "proxied";
	public static final ERXKey PROXIED = new ERXKey(PROXIED_KEY);
	public static final String SIZE_KEY = "size";
	public static final ERXKey SIZE = new ERXKey(SIZE_KEY);
	public static final String STORAGE_TYPE_KEY = "storageType";
	public static final ERXKey STORAGE_TYPE = new ERXKey(STORAGE_TYPE_KEY);
	public static final String THUMBNAIL_KEY = "thumbnail";
	public static final ERXKey THUMBNAIL = new ERXKey(THUMBNAIL_KEY);
	public static final String WEB_PATH_KEY = "webPath";
	public static final ERXKey WEB_PATH = new ERXKey(WEB_PATH_KEY);
	public static final String WIDTH_KEY = "width";
	public static final ERXKey WIDTH = new ERXKey(WIDTH_KEY);

	// Relationships
	public static final String CHILDREN_ATTACHMENTS_KEY = "childrenAttachments";
	public static final ERXKey CHILDREN_ATTACHMENTS = new ERXKey(CHILDREN_ATTACHMENTS_KEY);
	public static final String PARENT_ATTACHMENT_KEY = "parentAttachment";
	public static final ERXKey PARENT_ATTACHMENT = new ERXKey(PARENT_ATTACHMENT_KEY);

  private static Logger LOG = Logger.getLogger(_ERAttachment.class);

  public ERAttachment localInstanceIn(EOEditingContext editingContext) {
    ERAttachment localInstance = (ERAttachment)EOUtilities.localInstanceOfObject(editingContext, this);
    if (localInstance == null) {
      throw new IllegalStateException("You attempted to localInstance " + this + ", which has not yet committed.");
    }
    return localInstance;
  }

  public java.lang.Boolean available() {
    return (java.lang.Boolean) storedValueForKey("available");
  }

  public void setAvailable(java.lang.Boolean value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating available from " + available() + " to " + value);
    }
    takeStoredValueForKey(value, "available");
  }

  public String configurationName() {
    return (String) storedValueForKey("configurationName");
  }

  public void setConfigurationName(String value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating configurationName from " + configurationName() + " to " + value);
    }
    takeStoredValueForKey(value, "configurationName");
  }

  public NSTimestamp creationDate() {
    return (NSTimestamp) storedValueForKey("creationDate");
  }

  public void setCreationDate(NSTimestamp value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating creationDate from " + creationDate() + " to " + value);
    }
    takeStoredValueForKey(value, "creationDate");
  }

  public Integer height() {
    return (Integer) storedValueForKey("height");
  }

  public void setHeight(Integer value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating height from " + height() + " to " + value);
    }
    takeStoredValueForKey(value, "height");
  }

  public String mimeType() {
    return (String) storedValueForKey("mimeType");
  }

  public void setMimeType(String value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating mimeType from " + mimeType() + " to " + value);
    }
    takeStoredValueForKey(value, "mimeType");
  }

  public String originalFileName() {
    return (String) storedValueForKey("originalFileName");
  }

  public void setOriginalFileName(String value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating originalFileName from " + originalFileName() + " to " + value);
    }
    takeStoredValueForKey(value, "originalFileName");
  }

  public String ownerID() {
    return (String) storedValueForKey("ownerID");
  }

  public void setOwnerID(String value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating ownerID from " + ownerID() + " to " + value);
    }
    takeStoredValueForKey(value, "ownerID");
  }

  public java.lang.Boolean proxied() {
    return (java.lang.Boolean) storedValueForKey("proxied");
  }

  public void setProxied(java.lang.Boolean value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating proxied from " + proxied() + " to " + value);
    }
    takeStoredValueForKey(value, "proxied");
  }

  public Integer size() {
    return (Integer) storedValueForKey("size");
  }

  public void setSize(Integer value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating size from " + size() + " to " + value);
    }
    takeStoredValueForKey(value, "size");
  }

  public String storageType() {
    return (String) storedValueForKey("storageType");
  }

  public void setStorageType(String value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating storageType from " + storageType() + " to " + value);
    }
    takeStoredValueForKey(value, "storageType");
  }

  public String thumbnail() {
    return (String) storedValueForKey("thumbnail");
  }

  public void setThumbnail(String value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating thumbnail from " + thumbnail() + " to " + value);
    }
    takeStoredValueForKey(value, "thumbnail");
  }

  public String webPath() {
    return (String) storedValueForKey("webPath");
  }

  public void setWebPath(String value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating webPath from " + webPath() + " to " + value);
    }
    takeStoredValueForKey(value, "webPath");
  }

  public Integer width() {
    return (Integer) storedValueForKey("width");
  }

  public void setWidth(Integer value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
    	_ERAttachment.LOG.debug( "updating width from " + width() + " to " + value);
    }
    takeStoredValueForKey(value, "width");
  }

  public er.attachment.model.ERAttachment parentAttachment() {
    return (er.attachment.model.ERAttachment)storedValueForKey("parentAttachment");
  }

  public void setParentAttachmentRelationship(er.attachment.model.ERAttachment value) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
      _ERAttachment.LOG.debug("updating parentAttachment from " + parentAttachment() + " to " + value);
    }
    if (value == null) {
    	er.attachment.model.ERAttachment oldValue = parentAttachment();
    	if (oldValue != null) {
    		removeObjectFromBothSidesOfRelationshipWithKey(oldValue, "parentAttachment");
      }
    } else {
    	addObjectToBothSidesOfRelationshipWithKey(value, "parentAttachment");
    }
  }
  
  public NSArray<er.attachment.model.ERAttachment> childrenAttachments() {
    return (NSArray<er.attachment.model.ERAttachment>)storedValueForKey("childrenAttachments");
  }

  public NSArray<er.attachment.model.ERAttachment> childrenAttachments(EOQualifier qualifier) {
    return childrenAttachments(qualifier, null, false);
  }

  public NSArray<er.attachment.model.ERAttachment> childrenAttachments(EOQualifier qualifier, boolean fetch) {
    return childrenAttachments(qualifier, null, fetch);
  }

  public NSArray<er.attachment.model.ERAttachment> childrenAttachments(EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings, boolean fetch) {
    NSArray<er.attachment.model.ERAttachment> results;
    if (fetch) {
      EOQualifier fullQualifier;
      EOQualifier inverseQualifier = new EOKeyValueQualifier(er.attachment.model.ERAttachment.PARENT_ATTACHMENT_KEY, EOQualifier.QualifierOperatorEqual, this);
    	
      if (qualifier == null) {
        fullQualifier = inverseQualifier;
      }
      else {
        NSMutableArray qualifiers = new NSMutableArray();
        qualifiers.addObject(qualifier);
        qualifiers.addObject(inverseQualifier);
        fullQualifier = new EOAndQualifier(qualifiers);
      }

      results = er.attachment.model.ERAttachment.fetchERAttachments(editingContext(), fullQualifier, sortOrderings);
    }
    else {
      results = childrenAttachments();
      if (qualifier != null) {
        results = (NSArray<er.attachment.model.ERAttachment>)EOQualifier.filteredArrayWithQualifier(results, qualifier);
      }
      if (sortOrderings != null) {
        results = (NSArray<er.attachment.model.ERAttachment>)EOSortOrdering.sortedArrayUsingKeyOrderArray(results, sortOrderings);
      }
    }
    return results;
  }
  
  public void addToChildrenAttachmentsRelationship(er.attachment.model.ERAttachment object) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
      _ERAttachment.LOG.debug("adding " + object + " to childrenAttachments relationship");
    }
    addObjectToBothSidesOfRelationshipWithKey(object, "childrenAttachments");
  }

  public void removeFromChildrenAttachmentsRelationship(er.attachment.model.ERAttachment object) {
    if (_ERAttachment.LOG.isDebugEnabled()) {
      _ERAttachment.LOG.debug("removing " + object + " from childrenAttachments relationship");
    }
    removeObjectFromBothSidesOfRelationshipWithKey(object, "childrenAttachments");
  }

  public er.attachment.model.ERAttachment createChildrenAttachmentsRelationship() {
    EOClassDescription eoClassDesc = EOClassDescription.classDescriptionForEntityName("ERAttachment");
    EOEnterpriseObject eo = eoClassDesc.createInstanceWithEditingContext(editingContext(), null);
    editingContext().insertObject(eo);
    addObjectToBothSidesOfRelationshipWithKey(eo, "childrenAttachments");
    return (er.attachment.model.ERAttachment) eo;
  }

  public void deleteChildrenAttachmentsRelationship(er.attachment.model.ERAttachment object) {
    removeObjectFromBothSidesOfRelationshipWithKey(object, "childrenAttachments");
    editingContext().deleteObject(object);
  }

  public void deleteAllChildrenAttachmentsRelationships() {
    Enumeration objects = childrenAttachments().immutableClone().objectEnumerator();
    while (objects.hasMoreElements()) {
      deleteChildrenAttachmentsRelationship((er.attachment.model.ERAttachment)objects.nextElement());
    }
  }


  public static ERAttachment createERAttachment(EOEditingContext editingContext, java.lang.Boolean available
, NSTimestamp creationDate
, String mimeType
, String originalFileName
, java.lang.Boolean proxied
, Integer size
, String webPath
) {
    ERAttachment eo = (ERAttachment) EOUtilities.createAndInsertInstance(editingContext, _ERAttachment.ENTITY_NAME);    
		eo.setAvailable(available);
		eo.setCreationDate(creationDate);
		eo.setMimeType(mimeType);
		eo.setOriginalFileName(originalFileName);
		eo.setProxied(proxied);
		eo.setSize(size);
		eo.setWebPath(webPath);
    return eo;
  }

  public static NSArray<ERAttachment> fetchAllERAttachments(EOEditingContext editingContext) {
    return _ERAttachment.fetchAllERAttachments(editingContext, null);
  }

  public static NSArray<ERAttachment> fetchAllERAttachments(EOEditingContext editingContext, NSArray<EOSortOrdering> sortOrderings) {
    return _ERAttachment.fetchERAttachments(editingContext, null, sortOrderings);
  }

  public static NSArray<ERAttachment> fetchERAttachments(EOEditingContext editingContext, EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings) {
    EOFetchSpecification fetchSpec = new EOFetchSpecification(_ERAttachment.ENTITY_NAME, qualifier, sortOrderings);
    fetchSpec.setIsDeep(true);
    NSArray<ERAttachment> eoObjects = (NSArray<ERAttachment>)editingContext.objectsWithFetchSpecification(fetchSpec);
    return eoObjects;
  }

  public static ERAttachment fetchERAttachment(EOEditingContext editingContext, String keyName, Object value) {
    return _ERAttachment.fetchERAttachment(editingContext, new EOKeyValueQualifier(keyName, EOQualifier.QualifierOperatorEqual, value));
  }

  public static ERAttachment fetchERAttachment(EOEditingContext editingContext, EOQualifier qualifier) {
    NSArray<ERAttachment> eoObjects = _ERAttachment.fetchERAttachments(editingContext, qualifier, null);
    ERAttachment eoObject;
    int count = eoObjects.count();
    if (count == 0) {
      eoObject = null;
    }
    else if (count == 1) {
      eoObject = (ERAttachment)eoObjects.objectAtIndex(0);
    }
    else {
      throw new IllegalStateException("There was more than one ERAttachment that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static ERAttachment fetchRequiredERAttachment(EOEditingContext editingContext, String keyName, Object value) {
    return _ERAttachment.fetchRequiredERAttachment(editingContext, new EOKeyValueQualifier(keyName, EOQualifier.QualifierOperatorEqual, value));
  }

  public static ERAttachment fetchRequiredERAttachment(EOEditingContext editingContext, EOQualifier qualifier) {
    ERAttachment eoObject = _ERAttachment.fetchERAttachment(editingContext, qualifier);
    if (eoObject == null) {
      throw new NoSuchElementException("There was no ERAttachment that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static ERAttachment localInstanceIn(EOEditingContext editingContext, ERAttachment eo) {
    ERAttachment localInstance = (eo == null) ? null : (ERAttachment)EOUtilities.localInstanceOfObject(editingContext, eo);
    if (localInstance == null && eo != null) {
      throw new IllegalStateException("You attempted to localInstance " + eo + ", which has not yet committed.");
    }
    return localInstance;
  }
}
