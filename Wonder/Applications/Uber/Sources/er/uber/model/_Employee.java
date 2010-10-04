// DO NOT EDIT.  Make changes to Employee.java instead.
package er.uber.model;

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.*;
import java.util.*;
import org.apache.log4j.Logger;

import er.extensions.eof.*;
import er.extensions.foundation.*;

@SuppressWarnings("all")
public abstract class _Employee extends  ERXGenericRecord {
  public static final String ENTITY_NAME = "Employee";

  // Attribute Keys
  public static final ERXKey<Boolean> ADMIN = new ERXKey<Boolean>("admin");
  public static final ERXKey<Integer> EXEMPTIONS = new ERXKey<Integer>("exemptions");
  public static final ERXKey<String> FIRST_NAME = new ERXKey<String>("firstName");
  public static final ERXKey<NSTimestamp> HIRE_DATE = new ERXKey<NSTimestamp>("hireDate");
  public static final ERXKey<Boolean> INSURED = new ERXKey<Boolean>("insured");
  public static final ERXKey<String> LAST_NAME = new ERXKey<String>("lastName");
  public static final ERXKey<java.math.BigDecimal> SALARY = new ERXKey<java.math.BigDecimal>("salary");
  public static final ERXKey<er.uber.model.EmployeeStatus> STATUS = new ERXKey<er.uber.model.EmployeeStatus>("status");
  // Relationship Keys
  public static final ERXKey<er.uber.model.Company> COMPANY = new ERXKey<er.uber.model.Company>("company");
  public static final ERXKey<er.attachment.model.ERAttachment> PHOTO = new ERXKey<er.attachment.model.ERAttachment>("photo");

  // Attributes
  public static final String ADMIN_KEY = ADMIN.key();
  public static final String EXEMPTIONS_KEY = EXEMPTIONS.key();
  public static final String FIRST_NAME_KEY = FIRST_NAME.key();
  public static final String HIRE_DATE_KEY = HIRE_DATE.key();
  public static final String INSURED_KEY = INSURED.key();
  public static final String LAST_NAME_KEY = LAST_NAME.key();
  public static final String SALARY_KEY = SALARY.key();
  public static final String STATUS_KEY = STATUS.key();
  // Relationships
  public static final String COMPANY_KEY = COMPANY.key();
  public static final String PHOTO_KEY = PHOTO.key();

  private static Logger LOG = Logger.getLogger(_Employee.class);

  public Employee localInstanceIn(EOEditingContext editingContext) {
    Employee localInstance = (Employee)EOUtilities.localInstanceOfObject(editingContext, this);
    if (localInstance == null) {
      throw new IllegalStateException("You attempted to localInstance " + this + ", which has not yet committed.");
    }
    return localInstance;
  }

  public Boolean admin() {
    return (Boolean) storedValueForKey(_Employee.ADMIN_KEY);
  }

  public void setAdmin(Boolean value) {
    if (_Employee.LOG.isDebugEnabled()) {
    	_Employee.LOG.debug( "updating admin from " + admin() + " to " + value);
    }
    takeStoredValueForKey(value, _Employee.ADMIN_KEY);
  }

  public Integer exemptions() {
    return (Integer) storedValueForKey(_Employee.EXEMPTIONS_KEY);
  }

  public void setExemptions(Integer value) {
    if (_Employee.LOG.isDebugEnabled()) {
    	_Employee.LOG.debug( "updating exemptions from " + exemptions() + " to " + value);
    }
    takeStoredValueForKey(value, _Employee.EXEMPTIONS_KEY);
  }

  public String firstName() {
    return (String) storedValueForKey(_Employee.FIRST_NAME_KEY);
  }

  public void setFirstName(String value) {
    if (_Employee.LOG.isDebugEnabled()) {
    	_Employee.LOG.debug( "updating firstName from " + firstName() + " to " + value);
    }
    takeStoredValueForKey(value, _Employee.FIRST_NAME_KEY);
  }

  public NSTimestamp hireDate() {
    return (NSTimestamp) storedValueForKey(_Employee.HIRE_DATE_KEY);
  }

  public void setHireDate(NSTimestamp value) {
    if (_Employee.LOG.isDebugEnabled()) {
    	_Employee.LOG.debug( "updating hireDate from " + hireDate() + " to " + value);
    }
    takeStoredValueForKey(value, _Employee.HIRE_DATE_KEY);
  }

  public Boolean insured() {
    return (Boolean) storedValueForKey(_Employee.INSURED_KEY);
  }

  public void setInsured(Boolean value) {
    if (_Employee.LOG.isDebugEnabled()) {
    	_Employee.LOG.debug( "updating insured from " + insured() + " to " + value);
    }
    takeStoredValueForKey(value, _Employee.INSURED_KEY);
  }

  public String lastName() {
    return (String) storedValueForKey(_Employee.LAST_NAME_KEY);
  }

  public void setLastName(String value) {
    if (_Employee.LOG.isDebugEnabled()) {
    	_Employee.LOG.debug( "updating lastName from " + lastName() + " to " + value);
    }
    takeStoredValueForKey(value, _Employee.LAST_NAME_KEY);
  }

  public java.math.BigDecimal salary() {
    return (java.math.BigDecimal) storedValueForKey(_Employee.SALARY_KEY);
  }

  public void setSalary(java.math.BigDecimal value) {
    if (_Employee.LOG.isDebugEnabled()) {
    	_Employee.LOG.debug( "updating salary from " + salary() + " to " + value);
    }
    takeStoredValueForKey(value, _Employee.SALARY_KEY);
  }

  public er.uber.model.EmployeeStatus status() {
    return (er.uber.model.EmployeeStatus) storedValueForKey(_Employee.STATUS_KEY);
  }

  public void setStatus(er.uber.model.EmployeeStatus value) {
    if (_Employee.LOG.isDebugEnabled()) {
    	_Employee.LOG.debug( "updating status from " + status() + " to " + value);
    }
    takeStoredValueForKey(value, _Employee.STATUS_KEY);
  }

  public er.uber.model.Company company() {
    return (er.uber.model.Company)storedValueForKey(_Employee.COMPANY_KEY);
  }
  
  public void setCompany(er.uber.model.Company value) {
    takeStoredValueForKey(value, _Employee.COMPANY_KEY);
  }

  public void setCompanyRelationship(er.uber.model.Company value) {
    if (_Employee.LOG.isDebugEnabled()) {
      _Employee.LOG.debug("updating company from " + company() + " to " + value);
    }
    if (er.extensions.eof.ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships()) {
    	setCompany(value);
    }
    else if (value == null) {
    	er.uber.model.Company oldValue = company();
    	if (oldValue != null) {
    		removeObjectFromBothSidesOfRelationshipWithKey(oldValue, _Employee.COMPANY_KEY);
      }
    } else {
    	addObjectToBothSidesOfRelationshipWithKey(value, _Employee.COMPANY_KEY);
    }
  }
  
  public er.attachment.model.ERAttachment photo() {
    return (er.attachment.model.ERAttachment)storedValueForKey(_Employee.PHOTO_KEY);
  }
  
  public void setPhoto(er.attachment.model.ERAttachment value) {
    takeStoredValueForKey(value, _Employee.PHOTO_KEY);
  }

  public void setPhotoRelationship(er.attachment.model.ERAttachment value) {
    if (_Employee.LOG.isDebugEnabled()) {
      _Employee.LOG.debug("updating photo from " + photo() + " to " + value);
    }
    if (er.extensions.eof.ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships()) {
    	setPhoto(value);
    }
    else if (value == null) {
    	er.attachment.model.ERAttachment oldValue = photo();
    	if (oldValue != null) {
    		removeObjectFromBothSidesOfRelationshipWithKey(oldValue, _Employee.PHOTO_KEY);
      }
    } else {
    	addObjectToBothSidesOfRelationshipWithKey(value, _Employee.PHOTO_KEY);
    }
  }
  

  public static Employee createEmployee(EOEditingContext editingContext, Boolean admin
, String firstName
, NSTimestamp hireDate
, Boolean insured
, String lastName
, er.uber.model.EmployeeStatus status
, er.uber.model.Company company) {
    Employee eo = (Employee) EOUtilities.createAndInsertInstance(editingContext, _Employee.ENTITY_NAME);    
		eo.setAdmin(admin);
		eo.setFirstName(firstName);
		eo.setHireDate(hireDate);
		eo.setInsured(insured);
		eo.setLastName(lastName);
		eo.setStatus(status);
    eo.setCompanyRelationship(company);
    return eo;
  }

  public static NSArray<Employee> fetchAllEmployees(EOEditingContext editingContext) {
    return _Employee.fetchAllEmployees(editingContext, null);
  }

  public static NSArray<Employee> fetchAllEmployees(EOEditingContext editingContext, NSArray<EOSortOrdering> sortOrderings) {
    return _Employee.fetchEmployees(editingContext, null, sortOrderings);
  }

  public static NSArray<Employee> fetchEmployees(EOEditingContext editingContext, EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings) {
    ERXFetchSpecification<Employee> fetchSpec = new ERXFetchSpecification<Employee>(_Employee.ENTITY_NAME, qualifier, sortOrderings);
    fetchSpec.setIsDeep(true);
    NSArray<Employee> eoObjects = fetchSpec.fetchObjects(editingContext);
    return eoObjects;
  }

  public static Employee fetchEmployee(EOEditingContext editingContext, String keyName, Object value) {
    return _Employee.fetchEmployee(editingContext, new EOKeyValueQualifier(keyName, EOQualifier.QualifierOperatorEqual, value));
  }

  public static Employee fetchEmployee(EOEditingContext editingContext, EOQualifier qualifier) {
    NSArray<Employee> eoObjects = _Employee.fetchEmployees(editingContext, qualifier, null);
    Employee eoObject;
    int count = eoObjects.count();
    if (count == 0) {
      eoObject = null;
    }
    else if (count == 1) {
      eoObject = eoObjects.objectAtIndex(0);
    }
    else {
      throw new IllegalStateException("There was more than one Employee that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static Employee fetchRequiredEmployee(EOEditingContext editingContext, String keyName, Object value) {
    return _Employee.fetchRequiredEmployee(editingContext, new EOKeyValueQualifier(keyName, EOQualifier.QualifierOperatorEqual, value));
  }

  public static Employee fetchRequiredEmployee(EOEditingContext editingContext, EOQualifier qualifier) {
    Employee eoObject = _Employee.fetchEmployee(editingContext, qualifier);
    if (eoObject == null) {
      throw new NoSuchElementException("There was no Employee that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static Employee localInstanceIn(EOEditingContext editingContext, Employee eo) {
    Employee localInstance = (eo == null) ? null : ERXEOControlUtilities.localInstanceOfObject(editingContext, eo);
    if (localInstance == null && eo != null) {
      throw new IllegalStateException("You attempted to localInstance " + eo + ", which has not yet committed.");
    }
    return localInstance;
  }
}
