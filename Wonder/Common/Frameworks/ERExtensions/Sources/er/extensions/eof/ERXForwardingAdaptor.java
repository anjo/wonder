//
//  ERXForwardingAdaptor.java
//
//  Created by Thomas Burkholder on Thu May 10, 2005.
//
package er.extensions.eof;

import com.webobjects.eoaccess.EOAdaptor;
import com.webobjects.eoaccess.EOAdaptorContext;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EOSQLExpressionFactory;
import com.webobjects.eoaccess.EOSchemaGeneration;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSTimestamp;

public abstract class ERXForwardingAdaptor extends EOAdaptor {

    public EOSQLExpressionFactory expressionFactory() {
        return _forwardedAdaptor.expressionFactory();
    }

    public EOSchemaGeneration synchronizationFactory() {
        return _forwardedAdaptor.synchronizationFactory();
    }

    public NSArray prototypeAttributes() {
        return _forwardedAdaptor.prototypeAttributes();
    }

    protected abstract String forwardedAdaptorName();

    public ERXForwardingAdaptor(String name) {
        super(name);
        Object delegate = delegate();
        _forwardedAdaptor = adaptorWithName(forwardedAdaptorName());
        if (delegate != null) {
            _forwardedAdaptor.setDelegate(delegate);
        }
    }

    private EOAdaptor _forwardedAdaptor;

    public EOAdaptor forwardedAdaptor() {
        return _forwardedAdaptor;
    }

    public EOAdaptorContext createAdaptorContext() {
        return _forwardedAdaptor.createAdaptorContext();
    }

    public void handleDroppedConnection() {
        _forwardedAdaptor.handleDroppedConnection();
    }

    public Class expressionClass() {
        return _forwardedAdaptor.expressionClass();
    }

    public Class defaultExpressionClass() {
        return _forwardedAdaptor.defaultExpressionClass();
    }

    public boolean isValidQualifierType(String typeName, EOModel model) {
        return _forwardedAdaptor.isValidQualifierType(typeName,model);
    }

    public void assertConnectionDictionaryIsValid() {
        _forwardedAdaptor.assertConnectionDictionaryIsValid();
    }

    public boolean hasOpenChannels() {
        return _forwardedAdaptor.hasOpenChannels();
    }

    public NSDictionary connectionDictionary() {
        return _forwardedAdaptor.connectionDictionary();
    }

    public void setConnectionDictionary(NSDictionary dictionary) {
        _forwardedAdaptor.setConnectionDictionary(dictionary);
    }

    public boolean canServiceModel(EOModel model) {
        return _forwardedAdaptor.canServiceModel(model);
    }

    public Object fetchedValueForValue(Object value, EOAttribute att) {
        return _forwardedAdaptor.fetchedValueForValue(value,att);
    }

    public String fetchedValueForStringValue(String value, EOAttribute att) {
        return _forwardedAdaptor.fetchedValueForStringValue(value,att);
    }

    public Number fetchedValueForNumberValue(Number value, EOAttribute att) {
        return _forwardedAdaptor.fetchedValueForNumberValue(value,att);
    }

    public NSTimestamp fetchedValueForDateValue(NSTimestamp value, EOAttribute att) {
        return _forwardedAdaptor.fetchedValueForDateValue(value,att);
    }
    
    public NSData fetchedValueForDataValue(NSData value, EOAttribute att) {
        return _forwardedAdaptor.fetchedValueForDataValue(value,att);
    }        

    public boolean isDroppedConnectionException(Exception exception) {
        return _forwardedAdaptor.isDroppedConnectionException(exception);
    }

    public Object delegate() {
        return (_forwardedAdaptor == null) ? super.delegate() : _forwardedAdaptor.delegate();
    }

    public void setDelegate(Object delegate) {
    	if (_forwardedAdaptor != null) {
    		_forwardedAdaptor.setDelegate(delegate);
    	}
    	else {
    		super.setDelegate(delegate);
    	}
    }

    public String internalTypeForExternalType(String extType, EOModel model) {
        return _forwardedAdaptor.internalTypeForExternalType(extType, model);
    }

    public NSArray externalTypesWithModel(EOModel model) {
        return _forwardedAdaptor.externalTypesWithModel(model);
    }

    public void assignExternalTypeForAttribute(EOAttribute attribute) {
        _forwardedAdaptor.assignExternalTypeForAttribute(attribute);
    }

    public void assignExternalInfoForAttribute(EOAttribute attribute) {
        _forwardedAdaptor.assignExternalInfoForAttribute(attribute);
    }

    public void assignExternalInfoForEntity(EOEntity entity) { 
        _forwardedAdaptor.assignExternalInfoForEntity(entity);
    }

    public void assignExternalInfoForEntireModel(EOModel model) {
        _forwardedAdaptor.assignExternalInfoForEntireModel(model);
    }

    public void dropDatabaseWithAdministrativeConnectionDictionary(NSDictionary administrativeConnectionDictionary) {
        _forwardedAdaptor.dropDatabaseWithAdministrativeConnectionDictionary(administrativeConnectionDictionary);
    }

    public void createDatabaseWithAdministrativeConnectionDictionary(NSDictionary administrativeConnectionDictionary) {
        _forwardedAdaptor.createDatabaseWithAdministrativeConnectionDictionary(administrativeConnectionDictionary);
    }

    public NSDictionary administrativeConnectionDictionaryForAdaptor(EOAdaptor adaptor) {
        return _forwardedAdaptor.administrativeConnectionDictionaryForAdaptor(adaptor);
    }

}
