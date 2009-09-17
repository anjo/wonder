package com.webobjects.eocontrol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

import com.webobjects.foundation._NSUtilities;

/**
 * Rewrite of _EOIntegralKeyGlobalID with better equals() handling (700% speed improvement)
 * @author ak
 *
 */
public class _EOIntegralKeyGlobalID extends EOKeyGlobalID {

	public _EOIntegralKeyGlobalID(String entityName, Number value) {
		super(entityName, _hashCode(entityName, value));
		keyValue = value;
	}

	public _EOIntegralKeyGlobalID(String entityName, Object values[]) {
		this(entityName, (Number) values[0]);
	}

	public Object clone() {
		_EOIntegralKeyGlobalID result = new _EOIntegralKeyGlobalID(_literalEntityName(), keyValue);
		_prepClone(result);
		return result;
	}

	public Object[] keyValues() {
		return (new Object[] { keyValue });
	}

	public Object[] _keyValuesNoCopy() {
		return keyValues();
	}

	public int keyCount() {
		return 1;
	}

	public String toString() {
		StringBuffer result = new StringBuffer(_NSUtilities.shortClassName(this));
		result.append('[');
		result.append(entityName());
		result.append(" (");
		result.append(keyValue.getClass().getName());
		result.append(')');
		result.append(keyValue);
		result.append(']');
		return result.toString();
	}

	public boolean equals(Object obj) {
		if (false) {
			if (obj == this)
				return true;
			if (!(obj instanceof _EOIntegralKeyGlobalID))
				return false;
			_EOIntegralKeyGlobalID other = (_EOIntegralKeyGlobalID) obj;
			String entityName = _literalEntityName();
			String otherEntityName = other._literalEntityName();
			if (entityName == otherEntityName && keyValue == other.keyValue)
				return true;
			if (!keyValue.equals(other.keyValue))
				return false;
			if (hashCode() != other.hashCode())
				return false;
			else
				return entityName.equals(otherEntityName);
		}
		else {
			if (obj == this)
				return true;
			if (!(obj instanceof _EOIntegralKeyGlobalID))
				return false;
			_EOIntegralKeyGlobalID other = (_EOIntegralKeyGlobalID) obj;
			if (hashCode() != other.hashCode())
				return false;
			String entityName = _literalEntityName();
			String otherEntityName = other._literalEntityName();
			if (entityName == otherEntityName && keyValue == other.keyValue)
				return true;
			if (!keyValue.equals(other.keyValue))
				return false;
			else
				return entityName.equals(otherEntityName);
		}
	}

	private static int _hashCode(String entityName, Number value) {
		int hashCode = entityName.hashCode();
		if (value != null)
			hashCode ^= value.intValue();
		return hashCode == 0 ? 42 : hashCode;
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		java.io.ObjectOutputStream.PutField fields = s.putFields();
		fields.put("keyValue", keyValue);
		s.writeFields();
	}

	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		java.io.ObjectInputStream.GetField fields = null;
		fields = s.readFields();
		keyValue = (Number) fields.get("keyValue", null);
		if (keyValue == null)
			throw new IOException("key value must not be a null reference.");
		else
			return;
	}

	public static final Class _CLASS = _NSUtilities._classWithFullySpecifiedName("com.webobjects.eocontrol._EOIntegralKeyGlobalID");
	static final long serialVersionUID = -958860546L;
	private Number keyValue;
	private static final String SerializationKeyValueFieldKey = "keyValue";
	private static final ObjectStreamField serialPersistentFields[];

	static {
		serialPersistentFields = (new ObjectStreamField[] { new ObjectStreamField("keyValue", _NSUtilities._NumberClass) });
	}
}