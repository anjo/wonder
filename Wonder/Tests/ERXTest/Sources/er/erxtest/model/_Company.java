// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Company.java instead.
package er.erxtest.model;

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.*;
import java.util.*;
import org.apache.log4j.Logger;

import er.extensions.eof.*;
import er.extensions.foundation.*;

@SuppressWarnings("all")
public abstract class _Company extends er.extensions.eof.ERXGenericRecord {
  public static final String ENTITY_NAME = "Company";

  // Attribute Keys
  public static final ERXKey<String> ADDRESS1 = new ERXKey<String>("address1");
  public static final ERXKey<String> ADDRESS2 = new ERXKey<String>("address2");
  public static final ERXKey<String> CITY = new ERXKey<String>("city");
  public static final ERXKey<String> NAME = new ERXKey<String>("name");
  public static final ERXKey<String> STATE = new ERXKey<String>("state");
  public static final ERXKey<String> ZIPCODE = new ERXKey<String>("zipcode");
  // Relationship Keys
  public static final ERXKey<er.erxtest.model.Employee> EMPLOYEES = new ERXKey<er.erxtest.model.Employee>("employees");

  // Attributes
  public static final String ADDRESS1_KEY = ADDRESS1.key();
  public static final String ADDRESS2_KEY = ADDRESS2.key();
  public static final String CITY_KEY = CITY.key();
  public static final String NAME_KEY = NAME.key();
  public static final String STATE_KEY = STATE.key();
  public static final String ZIPCODE_KEY = ZIPCODE.key();
  // Relationships
  public static final String EMPLOYEES_KEY = EMPLOYEES.key();

  private static Logger LOG = Logger.getLogger(_Company.class);

  public Company localInstanceIn(EOEditingContext editingContext) {
    Company localInstance = (Company)EOUtilities.localInstanceOfObject(editingContext, this);
    if (localInstance == null) {
      throw new IllegalStateException("You attempted to localInstance " + this + ", which has not yet committed.");
    }
    return localInstance;
  }

  public String address1() {
    return (String) storedValueForKey("address1");
  }

  public void setAddress1(String value) {
    if (_Company.LOG.isDebugEnabled()) {
    	_Company.LOG.debug( "updating address1 from " + address1() + " to " + value);
    }
    takeStoredValueForKey(value, "address1");
  }

  public String address2() {
    return (String) storedValueForKey("address2");
  }

  public void setAddress2(String value) {
    if (_Company.LOG.isDebugEnabled()) {
    	_Company.LOG.debug( "updating address2 from " + address2() + " to " + value);
    }
    takeStoredValueForKey(value, "address2");
  }

  public String city() {
    return (String) storedValueForKey("city");
  }

  public void setCity(String value) {
    if (_Company.LOG.isDebugEnabled()) {
    	_Company.LOG.debug( "updating city from " + city() + " to " + value);
    }
    takeStoredValueForKey(value, "city");
  }

  public String name() {
    return (String) storedValueForKey("name");
  }

  public void setName(String value) {
    if (_Company.LOG.isDebugEnabled()) {
    	_Company.LOG.debug( "updating name from " + name() + " to " + value);
    }
    takeStoredValueForKey(value, "name");
  }

  public String state() {
    return (String) storedValueForKey("state");
  }

  public void setState(String value) {
    if (_Company.LOG.isDebugEnabled()) {
    	_Company.LOG.debug( "updating state from " + state() + " to " + value);
    }
    takeStoredValueForKey(value, "state");
  }

  public String zipcode() {
    return (String) storedValueForKey("zipcode");
  }

  public void setZipcode(String value) {
    if (_Company.LOG.isDebugEnabled()) {
    	_Company.LOG.debug( "updating zipcode from " + zipcode() + " to " + value);
    }
    takeStoredValueForKey(value, "zipcode");
  }

  public NSArray<er.erxtest.model.Employee> employees() {
    return (NSArray<er.erxtest.model.Employee>)storedValueForKey("employees");
  }

  public NSArray<er.erxtest.model.Employee> employees(EOQualifier qualifier) {
    return employees(qualifier, null, false);
  }

  public NSArray<er.erxtest.model.Employee> employees(EOQualifier qualifier, boolean fetch) {
    return employees(qualifier, null, fetch);
  }

  public NSArray<er.erxtest.model.Employee> employees(EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings, boolean fetch) {
    NSArray<er.erxtest.model.Employee> results;
    if (fetch) {
      EOQualifier fullQualifier;
      EOQualifier inverseQualifier = new EOKeyValueQualifier(er.erxtest.model.Employee.COMPANY_KEY, EOQualifier.QualifierOperatorEqual, this);
    	
      if (qualifier == null) {
        fullQualifier = inverseQualifier;
      }
      else {
        NSMutableArray qualifiers = new NSMutableArray();
        qualifiers.addObject(qualifier);
        qualifiers.addObject(inverseQualifier);
        fullQualifier = new EOAndQualifier(qualifiers);
      }

      results = er.erxtest.model.Employee.fetchEmployees(editingContext(), fullQualifier, sortOrderings);
    }
    else {
      results = employees();
      if (qualifier != null) {
        results = (NSArray<er.erxtest.model.Employee>)EOQualifier.filteredArrayWithQualifier(results, qualifier);
      }
      if (sortOrderings != null) {
        results = (NSArray<er.erxtest.model.Employee>)EOSortOrdering.sortedArrayUsingKeyOrderArray(results, sortOrderings);
      }
    }
    return results;
  }
  
  public void addToEmployees(er.erxtest.model.Employee object) {
    includeObjectIntoPropertyWithKey(object, "employees");
  }

  public void removeFromEmployees(er.erxtest.model.Employee object) {
    excludeObjectFromPropertyWithKey(object, "employees");
  }

  public void addToEmployeesRelationship(er.erxtest.model.Employee object) {
    if (_Company.LOG.isDebugEnabled()) {
      _Company.LOG.debug("adding " + object + " to employees relationship");
    }
    if (er.extensions.eof.ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships()) {
    	addToEmployees(object);
    }
    else {
    	addObjectToBothSidesOfRelationshipWithKey(object, "employees");
    }
  }

  public void removeFromEmployeesRelationship(er.erxtest.model.Employee object) {
    if (_Company.LOG.isDebugEnabled()) {
      _Company.LOG.debug("removing " + object + " from employees relationship");
    }
    if (er.extensions.eof.ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships()) {
    	removeFromEmployees(object);
    }
    else {
    	removeObjectFromBothSidesOfRelationshipWithKey(object, "employees");
    }
  }

  public er.erxtest.model.Employee createEmployeesRelationship() {
    EOClassDescription eoClassDesc = EOClassDescription.classDescriptionForEntityName("Employee");
    EOEnterpriseObject eo = eoClassDesc.createInstanceWithEditingContext(editingContext(), null);
    editingContext().insertObject(eo);
    addObjectToBothSidesOfRelationshipWithKey(eo, "employees");
    return (er.erxtest.model.Employee) eo;
  }

  public void deleteEmployeesRelationship(er.erxtest.model.Employee object) {
    removeObjectFromBothSidesOfRelationshipWithKey(object, "employees");
    editingContext().deleteObject(object);
  }

  public void deleteAllEmployeesRelationships() {
    Enumeration objects = employees().immutableClone().objectEnumerator();
    while (objects.hasMoreElements()) {
      deleteEmployeesRelationship((er.erxtest.model.Employee)objects.nextElement());
    }
  }


  public static Company createCompany(EOEditingContext editingContext, String name
) {
    Company eo = (Company) EOUtilities.createAndInsertInstance(editingContext, _Company.ENTITY_NAME);    
		eo.setName(name);
    return eo;
  }

  public static NSArray<Company> fetchAllCompanies(EOEditingContext editingContext) {
    return _Company.fetchAllCompanies(editingContext, null);
  }

  public static NSArray<Company> fetchAllCompanies(EOEditingContext editingContext, NSArray<EOSortOrdering> sortOrderings) {
    return _Company.fetchCompanies(editingContext, null, sortOrderings);
  }

  public static NSArray<Company> fetchCompanies(EOEditingContext editingContext, EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings) {
    EOFetchSpecification fetchSpec = new EOFetchSpecification(_Company.ENTITY_NAME, qualifier, sortOrderings);
    fetchSpec.setIsDeep(true);
    NSArray<Company> eoObjects = (NSArray<Company>)editingContext.objectsWithFetchSpecification(fetchSpec);
    return eoObjects;
  }

  public static Company fetchCompany(EOEditingContext editingContext, String keyName, Object value) {
    return _Company.fetchCompany(editingContext, new EOKeyValueQualifier(keyName, EOQualifier.QualifierOperatorEqual, value));
  }

  public static Company fetchCompany(EOEditingContext editingContext, EOQualifier qualifier) {
    NSArray<Company> eoObjects = _Company.fetchCompanies(editingContext, qualifier, null);
    Company eoObject;
    int count = eoObjects.count();
    if (count == 0) {
      eoObject = null;
    }
    else if (count == 1) {
      eoObject = (Company)eoObjects.objectAtIndex(0);
    }
    else {
      throw new IllegalStateException("There was more than one Company that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static Company fetchRequiredCompany(EOEditingContext editingContext, String keyName, Object value) {
    return _Company.fetchRequiredCompany(editingContext, new EOKeyValueQualifier(keyName, EOQualifier.QualifierOperatorEqual, value));
  }

  public static Company fetchRequiredCompany(EOEditingContext editingContext, EOQualifier qualifier) {
    Company eoObject = _Company.fetchCompany(editingContext, qualifier);
    if (eoObject == null) {
      throw new NoSuchElementException("There was no Company that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static Company localInstanceIn(EOEditingContext editingContext, Company eo) {
    Company localInstance = (eo == null) ? null : (Company)EOUtilities.localInstanceOfObject(editingContext, eo);
    if (localInstance == null && eo != null) {
      throw new IllegalStateException("You attempted to localInstance " + eo + ", which has not yet committed.");
    }
    return localInstance;
  }
}
