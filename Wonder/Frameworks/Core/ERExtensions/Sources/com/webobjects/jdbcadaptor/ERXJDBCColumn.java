package com.webobjects.jdbcadaptor;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSKeyValueCoding;

import er.extensions.eof.ERXConstant;
import er.extensions.foundation.ERXPatcher;
import er.extensions.jdbc.ERXJDBCAdaptor.Channel;

/**
 * Adds numerical constant support to EOF. See ERXConstant for more info. 
 * @author ak
 *
 */
public class ERXJDBCColumn extends JDBCColumn {
	
	private static final Logger log = Logger.getLogger(ERXJDBCColumn.class);

	private String _constantClassName;
	private static final String NO_NAME = "no name";

	public ERXJDBCColumn(EOAttribute attribute, JDBCChannel channel, int column, ResultSet rs) {
		super(attribute, channel, column, rs);
	}

	public ERXJDBCColumn(Channel aChannel) {
		super(aChannel);
	}

	public void takeInputValue(Object arg0, int arg1, boolean arg2) {
		try {
			super.takeInputValue(arg0, arg1, arg2);
		} catch(NSForwardException ex) {
			if (ex.originalException() instanceof NoSuchMethodException) {
				Class clazz = ERXPatcher.classForName(_attribute.className());
				if(ERXConstant.Constant.class.isAssignableFrom(clazz)) {
					Object value = ERXConstant.constantForClassNamed(arg0, _attribute.className());
					super.takeInputValue(value, arg1, arg2);
					return;
				}
			}
			throw ex;
		}
	}

	Object _fetchValue(boolean flag) {
		if (_rs == null || _column < 1)
			throw new JDBCAdaptorException(" *** JDBCColumn : trying to fetchValue on a null ResultSet [" + _rs
					+ "] or unknow col [" + _column + "]!!", null);
		if (_adaptorValueType == 0) {
			if(_constantClassName == null) {
				if(_attribute.userInfo() != null) {
					_constantClassName = (String) _attribute.userInfo().objectForKey("ERXConstantClassName");
				}
				if(_constantClassName == null) {
					_constantClassName = NO_NAME;
				}
			}
			if(_constantClassName != NO_NAME) {
				try {
					int i =_rs.getInt(_column);
					if(_rs.wasNull()) {
						return NSKeyValueCoding.NullValue;
					}
					Object result = ERXConstant.NumberConstant.constantForClassNamed(i, _constantClassName);
 					return result;
				} catch (SQLException e) {
					throw new JDBCAdaptorException("Can't read constant: " + _constantClassName, e);
				}
			}
		}
		try {
			return super._fetchValue(flag);
		} catch(NSForwardException ex) {
			log.error("There's an error with this attribute: " + _attribute);
			throw ex;
		}
	}
}