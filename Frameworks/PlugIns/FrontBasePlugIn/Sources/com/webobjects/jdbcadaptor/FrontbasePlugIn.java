package com.webobjects.jdbcadaptor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.FieldPosition;
import java.util.Enumeration;
import java.util.Properties;

import com.webobjects.eoaccess.EOAdaptor;
import com.webobjects.eoaccess.EOAdaptorContext;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOGeneralAdaptorException;
import com.webobjects.eoaccess.EOJoin;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOSchemaSynchronization;
import com.webobjects.eoaccess.EOSynchronizationFactory;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOQualifierVariable;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSPropertyListSerialization;
import com.webobjects.foundation.NSSelector;
import com.webobjects.foundation.NSTimeZone;
import com.webobjects.foundation.NSTimestampFormatter;

/**
 * This is the wo5 java runtime plugin for FrontBase.
 * 
 * @author Cail Borrell
 */

public class FrontbasePlugIn extends JDBCPlugIn {
	private static final String QUERY_STRING_USE_BUNDLED_JDBC_INFO = "useBundledJdbcInfo";

	static final boolean USE_NAMED_CONSTRAINTS = true;

	static final String _frontbaseIncludeSynonyms = System.getProperty("jdbcadaptor.frontbase.includeSynonyms", null);
	static final String _frontbaseWildcardPatternForAttributes = System.getProperty("jdbcadaptor.frontbase.wildcardPatternForAttributes", null);
	static final String _frontbaseWildcardPatternForTables = System.getProperty("jdbcadaptor.frontbase.wildcardPatternForTables", "%");
	static final String _frontbaseWildcardPatternForSchema = System.getProperty("jdbcadaptor.frontbase.wildcardPatternForSchema", null);
	static final String _frontbaseSqlStatementForGettingProcedureNames = System.getProperty("jdbcadaptor.frontbase.sqlStatementForGettingProcedureNames", null);
	static final String _frontbaseStoredProcedureCatalogPattern = System.getProperty("jdbcadaptor.frontbase.storedProcedureCatalogPattern", null);
	static final String _frontbaseStoredProcedureSchemaPattern = System.getProperty("jdbcadaptor.frontbase.storedProcedureSchemaPattern", null);
	static final String _frontbaseSqlStatementForGettingTableNames = System.getProperty("jdbcadaptor.frontbase.sqlStatementForGettingTableNames", null);

	public FrontbasePlugIn(JDBCAdaptor jdbcadaptor) {
		super(jdbcadaptor);
	}

	public static String getPlugInVersion() {
		return "2.6.4";
	}

	public boolean canDescribeStoredProcedure(String s) {
		return true;
	}

	public EOSynchronizationFactory createSynchronizationFactory() {
		return new FrontbasePlugIn.FrontbaseSynchronizationFactory(_adaptor);
	}

	public String defaultDriverName() {
		return "com.frontbase.jdbc.FBJDriver";
	}

	public String databaseProductName() {
		return "FrontBase";
	}

	/**
	 * <P>
	 * WebObjects 5.4's version of JDBCAdaptor will use this in order to assemble the name of the prototype to use when
	 * it loads models.
	 * </P>
	 * 
	 * @return the name of the plugin.
	 */
	public String name() {
		return "FrontBase";
	}

	/**
	 * <P>
	 * This method returns true if the connection URL for the database has a special flag on it which indicates to the
	 * system that the jdbcInfo which has been bundled into the plugin is acceptable to use in place of actually going
	 * to the database and getting it.
	 * 
	 * @return
	 */
	protected boolean shouldUseBundledJdbcInfo() {
		boolean shouldUseBundledJdbcInfo = false;
		String url = connectionURL();
		if (url != null) {
			shouldUseBundledJdbcInfo = url.toLowerCase().matches(".*/" + FrontbasePlugIn.QUERY_STRING_USE_BUNDLED_JDBC_INFO.toLowerCase() + "=(true|yes)(\\/|$)");
		}
		return shouldUseBundledJdbcInfo;
	}

	public Class defaultExpressionClass() {
		return FrontbaseExpression.class;
	}

	public String wildcardPatternForSchema() {
		if (_frontbaseWildcardPatternForSchema != null)
			return _frontbaseWildcardPatternForSchema;
		else {
			String schema = (String) adaptor().connectionDictionary().objectForKey("schema");
			return (schema != null) ? schema.toUpperCase() : "CURRENT_SCHEMA";
		}
	}

	public String schemaNameForEntity(EOEntity eoentity) {
		String s = super.schemaNameForEntity(eoentity);

		if (s == null) {
			s = (String) adaptor().connectionDictionary().objectForKey("schema");
			return (s != null) ? s.toUpperCase() : "CURRENT_SCHEMA";
		}
		else
			return s;
	}

	public String storedProcedureSchemaPattern() {
		if (_frontbaseStoredProcedureSchemaPattern != null)
			return _frontbaseStoredProcedureSchemaPattern;
		else
			return "CURRENT_SCHEMA";
	}

	public Properties connectionPropertiesForConnectionDictionary(NSDictionary connectionDictionary) {
		Properties properties = super.connectionPropertiesForConnectionDictionary(connectionDictionary);

		// Check for dbPasswd in connection Dictionary
		Object temp = connectionDictionary.objectForKey("dbpasswd");
		if (temp != null) {
			properties.put("dbpasswd", temp);
		}
		// Check for session in connection Dictionary
		temp = connectionDictionary.objectForKey("session");
		if (temp != null) {
			properties.put("session", temp);
		}
		// Check for session in connection Dictionary
		temp = connectionDictionary.objectForKey("system");
		if (temp != null) {
			properties.put("system", temp);
		}
		// Check for session in connection Dictionary
		temp = connectionDictionary.objectForKey("isolation");
		if (temp != null) {
			properties.put("isolation", temp);
		}
		// Check for session in connection Dictionary
		temp = connectionDictionary.objectForKey("locking");
		if (temp != null) {
			properties.put("locking", temp);
		}
		// Check for session in connection Dictionary
		temp = connectionDictionary.objectForKey("readOnly");
		if (temp != null) {
			properties.put("readOnly", temp);
		}
		return properties;
	}

	/**
	 * <P>
	 * This is usually extracted from the the database using JDBC, but this is really inconvenient for users who are
	 * trying to generate SQL at some. A specific version of the data has been written into the property list of the
	 * framework and this can be used as a hard-coded equivalent.
	 * </P>
	 */
	public NSDictionary jdbcInfo() {
		// you can swap this code out to write the property list out in order // to get a fresh copy of the
		// JDBCInfo.plist
//		try {
//			String jdbcInfoS = NSPropertyListSerialization.stringFromPropertyList(super.jdbcInfo());
//			FileOutputStream fos = new FileOutputStream("/tmp/JDBCInfo.plist");
//			fos.write(jdbcInfoS.getBytes());
//			fos.close();
//		}
//		catch (Exception e) {
//			throw new IllegalStateException("problem writing JDBCInfo.plist", e);
//		}

		boolean shouldUseBundledJdbcInfo = shouldUseBundledJdbcInfo();
		NSDictionary jdbcInfo;
		// have a look at the JDBC connection URL to see if the flag has been set to
		// specify that the hard-coded jdbcInfo information should be used.
		if (shouldUseBundledJdbcInfo) {
			if (NSLog.debugLoggingAllowedForLevel(NSLog.DebugLevelDetailed)) {
				NSLog.debug.appendln("Loading jdbcInfo from JDBCInfo.plist as opposed to using the JDBCPlugIn default implementation.");
			}

			// MS: Note that the name is not just /JDBCInfo.plist like it used to be.  Because we're loading
			// resources from the classpath instead of the Resources folder (so that it loads properly in EM),
			// if you have two plugins loaded, their resource names will overlap.
			InputStream jdbcInfoStream = getClass().getResourceAsStream("/FrontBaseJDBCInfo.plist");
			if (jdbcInfoStream == null) {
				throw new IllegalStateException("Unable to find 'FrontBaseJDBCInfo.plist' in this plugin jar.");
			}

			try {
				jdbcInfo = (NSDictionary) NSPropertyListSerialization.propertyListFromData(new NSData(jdbcInfoStream, 2048), "US-ASCII");
			}
			catch (IOException e) {
				throw new RuntimeException("Failed to load 'FrontBaseJDBCInfo.plist' from this plugin jar.", e);
			}
		}
		else {
			jdbcInfo = super.jdbcInfo();
		}

		NSMutableDictionary mutableJdbcInfo = new NSMutableDictionary(jdbcInfo);
		NSMutableDictionary typeInfoDict = new NSMutableDictionary((NSDictionary) mutableJdbcInfo.objectForKey("typeInfo"));
		NSDictionary typeDict = (NSDictionary) typeInfoDict.objectForKey("CHARACTER");
		typeInfoDict.setObjectForKey(typeDict, "CHAR");
		typeDict = (NSDictionary) typeInfoDict.objectForKey("CHARACTER VARYING");
		typeInfoDict.setObjectForKey(typeDict, "VARCHAR");
		typeInfoDict.setObjectForKey(typeDict, "CHAR VARYING");
		typeDict = (NSDictionary) typeInfoDict.objectForKey("BIT");
		typeInfoDict.setObjectForKey(typeDict, "BYTE");
		typeDict = (NSDictionary) typeInfoDict.objectForKey("BIT VARYING");
		typeInfoDict.setObjectForKey(typeDict, "BYTE VARYING");

		mutableJdbcInfo.setObjectForKey(typeInfoDict, "typeInfo");
		if (!shouldUseBundledJdbcInfo) {
			JDBCContext jdbccontext = adaptor()._cachedAdaptorContext();
			try {
				jdbccontext.connection().commit();
			}
			catch (SQLException sqlexception) {
				if (NSLog.debugLoggingAllowedForLevelAndGroups(3, 0x000010000L))
					NSLog.debug.appendln(sqlexception);
			}
		}
		return mutableJdbcInfo;
	}

	EOQualifier primaryKeyQualifier(EOQualifier eoqualifier, EOEntity eoentity) {
		if (eoqualifier instanceof EOAndQualifier) {
			NSArray qualifiers = ((EOAndQualifier) eoqualifier).qualifiers();
			NSArray attributeNames = eoentity.primaryKeyAttributeNames();
			NSMutableArray nsmutablearray = new NSMutableArray();

			for (int i = 0; i < qualifiers.count(); i++) {
				EOQualifier eoqualifier1 = (EOQualifier) qualifiers.objectAtIndex(i);

				if (eoqualifier1 instanceof EOKeyValueQualifier) {
					EOKeyValueQualifier eokeyvaluequalifier = (EOKeyValueQualifier) eoqualifier1;

					if (attributeNames.containsObject(eokeyvaluequalifier.key()))
						nsmutablearray.addObject(eokeyvaluequalifier);
				}
			}

			if (nsmutablearray.count() == 1)
				return (EOQualifier) nsmutablearray.objectAtIndex(0);
			else
				return new EOAndQualifier(qualifiers);
		}
		else {
			return eoqualifier;
		}
	}

	public void updateLOBs(JDBCChannel channel, JDBCExpression expression, NSDictionary dictionary, EOEntity entity) {
		FrontbaseExpression frontbaseexpression = (FrontbaseExpression) expression;
		if (!frontbaseexpression.hasLOBsToUpdate())
			return;

		NSArray array = frontbaseexpression.lobList();

		try {
			Connection con = ((JDBCContext) channel.adaptorContext()).connection();

			NSMutableDictionary d = new NSMutableDictionary();

			for (int i = 0; i < array.count(); i += 2) {
				d.setObjectForKey(getLobHandle(con, array.objectAtIndex(i), array.objectAtIndex(i + 1)), ((EOAttribute) array.objectAtIndex(i)).name());
			}

			EOQualifier qualifier = frontbaseexpression.qualifier();
			if (qualifier == null)
				qualifier = entity.qualifierForPrimaryKey(dictionary);
			else
				qualifier = primaryKeyQualifier(qualifier, entity);

			frontbaseexpression.resetlobList();
			channel.updateValuesInRowsDescribedByQualifier(d, qualifier, entity);
		}
		catch (SQLException e) {
			System.err.print(e.getMessage());
		}
	}

	// When using BLOB as an external type NSData is expected as the inernal type.
	// When using CLOB as an external type String is expected as the inernal type.
	String getLobHandle(Connection con, Object attribute, Object value) throws SQLException {
		// MS: This is weird, but to allow for people to build FrontBasePlugIn without actually
		// having the FrontBase JDBC driver installed, I've switched these two calls to be reflection.
		try {
			switch (internalTypeForExternal(((EOAttribute) attribute).externalType())) {
			case FB_BLOB:
				Method writeBLOBBytes = con.getClass().getMethod("writeBLOB", new Class[] { byte[].class });
				return (String) writeBLOBBytes.invoke(con, new Object[] { ((NSData) value).bytes() });
			case FB_CLOB:
				Method writeCLOBString = con.getClass().getMethod("writeCLOB", new Class[] { String.class });
				return (String) writeCLOBString.invoke(con, new Object[] { (String) value });
			default:
				return "NULL";
			}
		}
		catch (Throwable e) {
			if (e instanceof SQLException) {
				throw (SQLException) e;
			}
			throw new RuntimeException("Failed to get LOB handle.", e);
		}
	}

	public Object fetchBLOB(ResultSet resultset, int i, EOAttribute attribute, boolean flag) throws SQLException {
		Blob blob = resultset.getBlob(i);
		if (blob == null)
			return null;
		if (!flag)
			return blob;
		else {
			try {
				return attribute.newValueForBytes(blob.getBytes(1, (int) blob.length()), 0);
			}
			catch (Exception ioexception) {
				throw new JDBCAdaptorException(ioexception.getMessage(), null);
			}
		}
	}

	public Object fetchCLOB(ResultSet resultset, int i, EOAttribute attribute, boolean flag) throws SQLException {
		Clob clob = resultset.getClob(i);
		if (clob == null)
			return null;
		if (!flag)
			return clob;
		else
			return clob.getSubString(1L, (int) clob.length());
	}

	public NSArray newPrimaryKeys(int numberOfKeys, EOEntity eoentity, JDBCChannel jdbcchannel) {
		NSMutableArray pkDicts = new NSMutableArray();

		boolean pksGenerated = true;
		int numberOfKeysLeft = numberOfKeys;
		int keyBatchSize = 10;
		while (pksGenerated && numberOfKeysLeft > 0) {
			int thisKeyBatchSize = Math.min(keyBatchSize, numberOfKeysLeft);
			pksGenerated = _newPrimaryKeys(thisKeyBatchSize, eoentity, jdbcchannel, pkDicts);
			numberOfKeysLeft -= thisKeyBatchSize;
		}

		if (!pksGenerated) {
			pkDicts = null;
		}

		return pkDicts;
	}

	private boolean _newPrimaryKeys(int keyBatchSize, EOEntity eoentity, JDBCChannel jdbcchannel, NSMutableArray pkDicts) {
		if (keyBatchSize == 0) {
			return true;
		}

		NSArray primaryKeyAttributes = eoentity.primaryKeyAttributes();
		if (primaryKeyAttributes == null) {
			return false;
		}

		EOAttribute firstPrimaryKeyAttribute = (EOAttribute) primaryKeyAttributes.lastObject();
		boolean isNSData = firstPrimaryKeyAttribute.className().endsWith("NSData");

		NSMutableArray attributesToFetch = new NSMutableArray();
		StringBuffer sql = new StringBuffer();
		sql.append("VALUES (");
		for (int keyNum = 0; keyNum < keyBatchSize; keyNum++) {
			if (isNSData) {
				if (firstPrimaryKeyAttribute.externalType().startsWith("BIT")) {
					sql.append("NEW_UID(" + (firstPrimaryKeyAttribute.width() >> 3) + ")");
				}
				else {
					sql.append("NEW_UID(" + firstPrimaryKeyAttribute.width() + ")");
				}
			}
			else {
				sql.append("SELECT UNIQUE FROM " + quoteTableName(eoentity.primaryKeyRootName()));
			}
			if (keyNum < keyBatchSize - 1) {
				sql.append(", ");
			}

			EOAttribute generatedPrimaryKeyAttribute = new EOAttribute();
			generatedPrimaryKeyAttribute.setName("Unique" + keyNum);
			generatedPrimaryKeyAttribute.setColumnName(firstPrimaryKeyAttribute.columnName());
			generatedPrimaryKeyAttribute.setExternalType(firstPrimaryKeyAttribute.externalType());
			generatedPrimaryKeyAttribute.setClassName(firstPrimaryKeyAttribute.className());
			generatedPrimaryKeyAttribute.setValueType(firstPrimaryKeyAttribute.valueType());
			generatedPrimaryKeyAttribute.setPrecision(firstPrimaryKeyAttribute.precision());
			generatedPrimaryKeyAttribute.setScale(firstPrimaryKeyAttribute.scale());
			generatedPrimaryKeyAttribute.setWidth(firstPrimaryKeyAttribute.width());
			generatedPrimaryKeyAttribute.setAllowsNull(firstPrimaryKeyAttribute.allowsNull());
			adaptor().plugIn().assignTypeForAttribute(generatedPrimaryKeyAttribute);
			attributesToFetch.addObject(generatedPrimaryKeyAttribute);
		}

		sql.append(")");

		boolean pksGenerated = false;
		EOSQLExpression eosqlexpression = expressionFactory().expressionForString(sql.toString());
		EOAdaptorContext adaptorContext = jdbcchannel.adaptorContext();
		adaptorContext.transactionDidBegin();
		jdbcchannel.evaluateExpression(eosqlexpression);
		if (jdbcchannel._errorEvaluateExpression()) {
			adaptorContext.transactionDidRollback();
			jdbcchannel._setErrorEvaluateExpression(false);
		}
		else {
			jdbcchannel.setAttributesToFetch(attributesToFetch);
			NSMutableDictionary row = jdbcchannel.fetchRow();
			jdbcchannel.cancelFetch();
			adaptorContext.transactionDidCommit();
			if (row != null && row.count() > 0) {
				NSArray pkValues = row.allValues();
				if (pkValues.count() == keyBatchSize) {
					Enumeration keysEnum = pkValues.objectEnumerator();
					while (keysEnum.hasMoreElements()) {
						Object obj = keysEnum.nextElement();
						NSMutableDictionary pkDict = new NSMutableDictionary();
						Enumeration pkAttributeEnum = primaryKeyAttributes.objectEnumerator();
						while (pkAttributeEnum.hasMoreElements()) {
							EOAttribute pkAttribute = (EOAttribute) pkAttributeEnum.nextElement();
							pkDict.setObjectForKey(obj, pkAttribute.name());
						}
						pkDicts.addObject(pkDict);
					}
					pksGenerated = true;
				}
			}
		}

		return pksGenerated;
	}

	protected static final int FB_Boolean = 1;
	protected static final int FB_Integer = 2;
	protected static final int FB_SmallInteger = 3;
	protected static final int FB_Float = 4;
	protected static final int FB_Real = 5;
	protected static final int FB_Double = 6;
	protected static final int FB_Numeric = 7;
	protected static final int FB_Decimal = 8;
	protected static final int FB_Character = 9;
	protected static final int FB_VCharacter = 10;
	protected static final int FB_Bit = 11;
	protected static final int FB_VBit = 12;
	protected static final int FB_Date = 13;
	protected static final int FB_Time = 14;
	protected static final int FB_TimeTZ = 15;
	protected static final int FB_Timestamp = 16;
	protected static final int FB_TimestampTZ = 17;
	protected static final int FB_YearMonth = 18;
	protected static final int FB_DayTime = 19;
	protected static final int FB_CLOB = 20;
	protected static final int FB_BLOB = 21;
	protected static final int FB_TinyInteger = 22;
	protected static final int FB_LongInteger = 23;

	protected static String notNullConstraintName(EOAttribute attribute) {
		return notNullConstraintName(attribute.entity().externalName(), attribute.columnName());
	}

	protected static String notNullConstraintName(String tableName, String columnName) {
		StringBuffer constraintBuffer = new StringBuffer();
		constraintBuffer.append("NOT_NULL_");
		constraintBuffer.append(tableName);
		constraintBuffer.append("__");
		constraintBuffer.append(columnName);
		return constraintBuffer.toString();
	}

	protected static String quoteTableName(String s) {
		if (s == null)
			return null;
		int i = s.lastIndexOf(46);
		if (i == -1)
			return "\"" + s + "\"";
		else
			return "\"" + s.substring(0, i) + "\".\"" + s.substring(i + 1, s.length()) + "\"";
	}

	protected static int internalTypeForExternal(String externalType) {
		if (externalType.equals("BOOLEAN"))
			return FB_Boolean;
		else if (externalType.equals("INTEGER") || externalType.equals("INT"))
			return FB_Integer;
		else if (externalType.equals("SMALLINT"))
			return FB_SmallInteger;
		else if (externalType.equals("LONGINT"))
			return FB_LongInteger;
		else if (externalType.equals("TINYINT"))
			return FB_TinyInteger;
		else if (externalType.equals("FLOAT"))
			return FB_Float;
		else if (externalType.equals("REAL"))
			return FB_Real;
		else if (externalType.equals("DOUBLE PRECISION"))
			return FB_Double;
		else if (externalType.equals("NUMERIC"))
			return FB_Numeric;
		else if (externalType.equals("DECIMAL"))
			return FB_Decimal;
		else if (externalType.equals("CHAR") || externalType.equals("CHARACTER"))
			return FB_Character;
		else if (externalType.equals("VARCHAR") || externalType.equals("CHARACTER VARYING") || externalType.equals("CHAR VARYING"))
			return FB_VCharacter;
		else if (externalType.equals("BIT") || externalType.equals("BYTE"))
			return FB_Bit;
		else if (externalType.equals("BIT VARYING") || externalType.equals("BYTE VARYING"))
			return FB_VBit;
		else if (externalType.equals("DATE"))
			return FB_Date;
		else if (externalType.equals("TIME"))
			return FB_Time;
		else if (externalType.equals("TIME WITH TIME ZONE"))
			return FB_TimeTZ;
		else if (externalType.equals("TIMESTAMP"))
			return FB_Timestamp;
		else if (externalType.equals("TIMESTAMP WITH TIME ZONE"))
			return FB_TimestampTZ;
		else if (externalType.equals("BLOB"))
			return FB_BLOB;
		else if (externalType.equals("CLOB"))
			return FB_CLOB;
		else
			return -1;
	}

	public static class FrontbaseSynchronizationFactory extends EOSynchronizationFactory {

		public FrontbaseSynchronizationFactory(EOAdaptor eoadaptor) {
			super(eoadaptor);
		}

		public boolean supportsSchemaSynchronization() {
			return true;
		}

		public static boolean boolValueForKeyDefault(NSDictionary nsdictionary, String s, boolean flag) {
			String s1 = (String) nsdictionary.objectForKey(s);
			if (s1 == null)
				return flag;
			else
				return s1.equals("YES");
		}
	    
	    public String schemaCreationScriptForEntities(NSArray allEntities, NSDictionary options) {
	/* 741*/        StringBuffer result = new StringBuffer();
	/* 744*/        if(options == null)
	/* 745*/            options = NSDictionary.EmptyDictionary;
	/* 747*/        NSArray statements = schemaCreationStatementsForEntities(allEntities, options);
	/* 748*/        int i = 0;
	/* 748*/        for(int count = statements.count(); i < count; i++)
	/* 749*/            appendExpressionToScript((EOSQLExpression)statements.objectAtIndex(i), result);

	/* 751*/        return new String(result);
	    }

		public NSArray schemaCreationStatementsForEntities(NSArray entities, NSDictionary options) {
			NSMutableArray result = new NSMutableArray();

			if (entities == null || entities.count() == 0)
				return result;

			result.addObject(_expressionForString("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE, LOCKING PESSIMISTIC"));

			NSDictionary nsdictionary1 = ((EOEntity) entities.lastObject()).model().connectionDictionary();
			if (boolValueForKeyDefault(options, "dropDatabase", false)) {
				result.addObjectsFromArray(dropDatabaseStatementsForConnectionDictionary(nsdictionary1, null));
			}
			if (boolValueForKeyDefault(options, "createDatabase", false)) {
				result.addObjectsFromArray(createDatabaseStatementsForConnectionDictionary(nsdictionary1, null));
			}
			if (boolValueForKeyDefault(options, "dropPrimaryKeySupport", true)) {
				NSArray nsarray1 = primaryKeyEntityGroupsForEntities(entities);
				result.addObjectsFromArray(dropPrimaryKeySupportStatementsForEntityGroups(nsarray1));
			}
			if (boolValueForKeyDefault(options, "dropTables", true)) {
				NSArray nsarray2 = tableEntityGroupsForEntities(entities);
				result.addObjectsFromArray(dropTableStatementsForEntityGroups(nsarray2));
			}
			if (boolValueForKeyDefault(options, "createTables", true)) {
				NSArray nsarray3 = tableEntityGroupsForEntities(entities);
				result.addObjectsFromArray(createTableStatementsForEntityGroups(nsarray3));
				result.addObjectsFromArray(createIndexStatementsForEntityGroups(nsarray3));
			}
			if (boolValueForKeyDefault(options, "createPrimaryKeySupport", true)) {
				NSArray nsarray4 = primaryKeyEntityGroupsForEntities(entities);
				result.addObjectsFromArray(primaryKeySupportStatementsForEntityGroups(nsarray4));
			}
			if (boolValueForKeyDefault(options, "primaryKeyConstraints", true)) {
				NSArray nsarray5 = tableEntityGroupsForEntities(entities);
				result.addObjectsFromArray(primaryKeyConstraintStatementsForEntityGroups(nsarray5));
			}
			if (boolValueForKeyDefault(options, "foreignKeyConstraints", false)) {
				NSMutableSet nsmutableset = new NSMutableSet();
				NSArray nsarray6 = tableEntityGroupsForEntities(entities);
				for (int i = 0; i < nsarray6.count(); i++)
					result.addObjectsFromArray(_foreignKeyConstraintStatementsForEntityGroup((NSArray) nsarray6.objectAtIndex(i)));
			}
			result.addObject(_expressionForString("COMMIT"));
			return result;
		}

		public NSArray dropPrimaryKeySupportStatementsForEntityGroups(NSArray nsarray) {
			return new NSArray(_expressionForString("-- The 'Drop Primary Key Support' option is unavailable."));
		}

		public NSArray dropDatabaseStatementsForConnectionDictionary(NSDictionary nsdictionary, NSDictionary nsdictionary1) {

			return new NSArray(_expressionForString("-- The 'Drop Database' option is unavailable."));
		}

		public NSArray createDatabaseStatementsForConnectionDictionary(NSDictionary nsdictionary, NSDictionary nsdictionary1) {
			return new NSArray(_expressionForString("-- The 'Create Database' option is unavailable."));
		}

		public NSArray dropTableStatementsForEntityGroup(NSArray nsarray) {
			EOEntity entity = (EOEntity) nsarray.objectAtIndex(0);
			String dropType = " CASCADE";

			if (entity.userInfo() != null) {
				NSDictionary dictionary = entity.userInfo();
				if (dictionary.valueForKey("Restrict") != null && ((String) dictionary.valueForKey("Restrict")).equals("true"))
					dropType = " RESTRICT";
			}

			EOSQLExpression expression = _expressionForString("DROP TABLE " + quoteTableName(entity.externalName()) + dropType);

			return new NSArray(expression);
		}

		public NSArray primaryKeySupportStatementsForEntityGroup(NSArray entityGroup) {
			if (entityGroup == null)
				return NSArray.EmptyArray;

			NSMutableArray result = new NSMutableArray();
			EOEntity eoentity = null;

			for (int i = entityGroup.count() - 1; i >= 0; i--) {
				eoentity = (EOEntity) entityGroup.objectAtIndex(i);
				String externalName = eoentity.externalName();

				if (externalName != null && externalName.length() > 0) {
					result.addObject(_expressionForString("SET UNIQUE = 1000000 FOR " + quoteTableName(externalName)));
				}
			}
			return result;
		}

		public NSArray foreignKeyConstraintStatementsForRelationship(EORelationship relationship) {
			if (!relationship.isToMany() && isPrimaryKeyAttributes(relationship.destinationEntity(), relationship.destinationAttributes())) {
				StringBuffer sql = new StringBuffer();
				String tableName = relationship.entity().externalName();

				sql.append("ALTER TABLE ");
				sql.append(quoteTableName(tableName.toUpperCase()));
				sql.append(" ADD");

				StringBuffer constraint = new StringBuffer(" CONSTRAINT FOREIGN_KEY_");
				constraint.append(tableName);

				StringBuffer fkSql = new StringBuffer(" FOREIGN KEY (");
				NSArray attributes = relationship.sourceAttributes();

				for (int i = 0; i < attributes.count(); i++) {
					constraint.append("_");
					if (i != 0)
						fkSql.append(", ");

					fkSql.append("\"");
					String columnName = ((EOAttribute) attributes.objectAtIndex(i)).columnName();
					fkSql.append(columnName.toUpperCase());
					constraint.append(columnName);
					fkSql.append("\"");
				}

				fkSql.append(") REFERENCES ");
				constraint.append("_");

				String referencedExternalName = relationship.destinationEntity().externalName();
				fkSql.append(quoteTableName(referencedExternalName.toUpperCase()));
				constraint.append(referencedExternalName);

				fkSql.append(" (");

				attributes = relationship.destinationAttributes();

				for (int i = 0; i < attributes.count(); i++) {
					constraint.append("_");
					if (i != 0)
						fkSql.append(", ");

					fkSql.append("\"");
					String referencedColumnName = ((EOAttribute) attributes.objectAtIndex(i)).columnName();
					fkSql.append(referencedColumnName.toUpperCase());
					constraint.append(referencedColumnName);
					fkSql.append("\"");
				}

				fkSql.append(") DEFERRABLE INITIALLY DEFERRED");

				if (USE_NAMED_CONSTRAINTS)
					sql.append(constraint);
				sql.append(fkSql);

				return new NSArray(_expressionForString(sql.toString()));
			}
			return NSArray.EmptyArray;
		}

		public NSArray createTableStatementsForEntityGroups(NSArray nsarray) {
			NSMutableArray nsmutablearray = new NSMutableArray();

			for (int i = 0; i < nsarray.count(); i++) {
				nsmutablearray.addObjectsFromArray(createTableStatementsForEntityGroup((NSArray) nsarray.objectAtIndex(i)));
			}

			return nsmutablearray;
		}

		public NSArray createTableStatementsForEntityGroup(NSArray nsarray) {
			EOSQLExpression eosqlexpression = null;
			EOEntity eoentity = null;
			NSMutableArray nsmutablearray = new NSMutableArray();
			int j = nsarray != null ? nsarray.count() : 0;

			if (j == 0)
				return NSArray.EmptyArray;

			StringBuffer columns = new StringBuffer();
			eosqlexpression = _expressionForEntity((EOEntity) nsarray.objectAtIndex(0));

			for (int i = 0; i < j; i++) {
				eoentity = (EOEntity) nsarray.objectAtIndex(i);
				NSArray nsarray1 = eoentity.attributes();
				int l = nsarray1 != null ? nsarray1.count() : 0;

				for (int k = 0; k < l; k++) {
					EOAttribute eoattribute = (EOAttribute) nsarray1.objectAtIndex(k);
					String column = eoattribute.columnName();

					if (!eoattribute.isDerived() && !eoattribute.isFlattened() && column != null && column.length() > 0 && nsmutablearray.indexOfObject(column) == NSArray.NotFound) {

						if (columns.length() > 0) {
							columns.append(',');
							columns.append('\n');
							columns.append('\t');
						}

						columns.append(addCreateClauseForAttribute(eoattribute));
						nsmutablearray.addObject(column);
					}
				}
			}

			StringBuffer sql = new StringBuffer();
			sql.append("CREATE TABLE ");
			sql.append(quoteTableName(eoentity.externalName()));
			sql.append(" (\n\t");
			sql.append(columns.toString());
			sql.append("\n)");

			eosqlexpression.setStatement(sql.toString());

			return new NSArray(eosqlexpression);
		}

		public NSArray createIndexStatementsForEntityGroups(NSArray nsarray) {
			NSMutableArray nsmutablearray = new NSMutableArray();

			for (int i = 0; i < nsarray.count(); i++) {
				nsmutablearray.addObjectsFromArray(createIndexStatementsForEntityGroup((NSArray) nsarray.objectAtIndex(i)));
			}

			return nsmutablearray;
		}

		public NSArray createIndexStatementsForEntityGroup(NSArray nsarray) {
			NSMutableArray result = new NSMutableArray();
			EOSQLExpression eosqlexpression = null;
			EOEntity eoentity = null;
			NSMutableArray nsmutablearray = new NSMutableArray();
			int j = nsarray != null ? nsarray.count() : 0;

			if (j == 0)
				return NSArray.EmptyArray;

			eosqlexpression = _expressionForEntity((EOEntity) nsarray.objectAtIndex(0));

			for (int i = 0; i < j; i++) {
				eoentity = (EOEntity) nsarray.objectAtIndex(i);
				NSDictionary dictionary = eoentity.userInfo();

				if (dictionary != null && dictionary.valueForKey("Index") != null) {
					dictionary = (NSDictionary) dictionary.valueForKey("Index");
					java.util.Enumeration e = dictionary.keyEnumerator();

					while (e.hasMoreElements()) {
						eosqlexpression.setStatement((String) dictionary.objectForKey(e.nextElement()));
						result.addObject(eosqlexpression);
					}
				}
			}
			return result;
		}

		public StringBuffer addCreateClauseForAttribute(EOAttribute eoattribute) {
			EOSQLExpression expression = _expressionForEntity(eoattribute.entity());
			expression.addCreateClauseForAttribute(eoattribute);
			return new StringBuffer(expression.listString());
		}

		public String columnTypeStringForAttribute(EOAttribute eoattribute) {
			EOSQLExpression expression = _expressionForEntity(eoattribute.entity());
			return expression.columnTypeStringForAttribute(eoattribute);
		}

		public NSArray statementsToConvertColumnType(String columnName, String tableName, ColumnTypes oldType, ColumnTypes newType, NSDictionary nsdictionary) {
			String columnTypeString = statementToCreateDataTypeClause(newType);
			NSArray statements = new NSArray(_expressionForString("alter column " + quoteTableName(tableName) + "." + quoteTableName(columnName) + " to " + columnTypeString));
			return statements;
		}

		public NSArray statementsToModifyColumnNullRule(String columnName, String tableName, boolean allowsNull, NSDictionary nsdictionary) {
			NSArray statements;
			if (allowsNull) {
				if (USE_NAMED_CONSTRAINTS) {
					statements = new NSArray(_expressionForString("alter table " + quoteTableName(tableName) + " drop constraint " + quoteTableName(FrontbasePlugIn.notNullConstraintName(tableName, columnName)) + " cascade"));
				}
				else {
					statements = null;
				}
			}
			else {
				if (USE_NAMED_CONSTRAINTS) {
					statements = new NSArray(_expressionForString("alter table " + quoteTableName(tableName) + " add constraint " + quoteTableName(FrontbasePlugIn.notNullConstraintName(tableName, columnName)) + " check (" + quoteTableName(columnName) + " is not null)"));
				}
				else {
					statements = new NSArray(_expressionForString("alter table " + quoteTableName(tableName) + " add check (" + quoteTableName(columnName) + " is not null)"));
				}
			}
			return statements;
		}

		public NSArray statementsToDeleteColumnNamed(String columnName, String tableName, NSDictionary options) {
			return new NSArray(_expressionForString("alter table " + quoteTableName(tableName) + " drop column \"" + columnName.toUpperCase() + "\" cascade"));
		}

		public NSArray statementsToInsertColumnForAttribute(EOAttribute attribute, NSDictionary options) {
			String clause = _columnCreationClauseForAttribute(attribute);
			return new NSArray(_expressionForString("alter table " + attribute.entity().externalName() + " add " + clause));
		}

		private String statementToCreateDataTypeClause(EOSchemaSynchronization.ColumnTypes columntypes) {
			switch (internalTypeForExternal(columntypes.name())) {
			case FB_Decimal:
			case FB_Numeric:
				int j = columntypes.precision();
				if (j == 0)
					return columntypes.name();
				int k = columntypes.scale();
				if (k == 0)
					return columntypes.name() + "(" + j + ")";
				else
					return columntypes.name() + "(" + j + "," + k + ")";

			case FB_Float:
			case FB_Bit:
			case FB_VBit:
			case FB_Character:
			case FB_VCharacter:
				int l = columntypes.width();
				if (l == 0)
					l = columntypes.precision();
				if (l == 0)
					return columntypes.name();
				else
					return columntypes.name() + "(" + l + ")";
			case FB_Timestamp:
				int m = columntypes.precision();
				if (m == 0)
					return columntypes.name();
				else
					return columntypes.name() + "(" + m + ")";
			}
			return columntypes.name();
		}

		public NSArray statementsToRenameColumnNamed(String columnName, String tableName, String newName, NSDictionary nsdictionary) {
			return new NSArray(_expressionForString("alter column name " + quoteTableName(tableName) + "." + quoteTableName(columnName) + " to " + quoteTableName(newName)));
		}

		public NSArray statementsToRenameTableNamed(String tableName, String newName, NSDictionary options) {
			return new NSArray(_expressionForString("alter table name " + quoteTableName(tableName) + " to " + quoteTableName(newName)));
		}

		boolean isPrimaryKeyAttributes(EOEntity entity, NSArray attributes) {
			NSArray keys = entity.primaryKeyAttributeNames();
			boolean result = attributes.count() == keys.count();

			if (result) {
				for (int i = 0; i < keys.count(); i++) {
					if (!(result = keys.indexOfObject(((EOAttribute) attributes.objectAtIndex(i)).name()) != NSArray.NotFound))
						break;
				}
			}
			return result;
		}

		public NSArray primaryKeyConstraintStatementsForEntityGroups(NSArray entityGroups) {
			NSMutableArray result = new NSMutableArray();

			for (int i = 0; i < entityGroups.count(); i++) {
				result.addObjectsFromArray(primaryKeyConstraintStatementsForEntityGroup((NSArray) entityGroups.objectAtIndex(i)));
			}

			return result;
		}

		public NSArray primaryKeyConstraintStatementsForEntityGroup(NSArray entityGroup) {
			if (entityGroup.count() != 0) {
				EOEntity entity = (EOEntity) entityGroup.objectAtIndex(0);
				String tableName = entity.externalName();
				NSArray keys = entity.primaryKeyAttributeNames();
				StringBuffer sql = new StringBuffer();

				if (tableName != null && keys.count() > 0) {
					sql.append("ALTER TABLE ");
					sql.append(quoteTableName(tableName.toUpperCase()));
					sql.append(" ADD");

					StringBuffer constraint = new StringBuffer(" CONSTRAINT PRIMARY_KEY_");
					constraint.append(tableName);

					StringBuffer pkSql = new StringBuffer(" PRIMARY KEY (");

					for (int j = 0; j < keys.count(); j++) {
						constraint.append("_");
						if (j != 0)
							pkSql.append(",");

						pkSql.append("\"");
						String columnName = entity.attributeNamed((String) keys.objectAtIndex(j)).columnName();
						pkSql.append(columnName.toUpperCase());
						pkSql.append("\"");
						constraint.append(columnName);
					}
					pkSql.append(") NOT DEFERRABLE INITIALLY IMMEDIATE");

					if (USE_NAMED_CONSTRAINTS)
						sql.append(constraint);
					sql.append(pkSql);

					return new NSArray(_expressionForString(sql.toString()));
				}
			}
			return NSArray.EmptyArray;
		}
	}

	public static class FrontbaseExpression extends JDBCExpression {
		EOQualifier _qualifier;
		NSMutableArray _lobList;

	    /**
	     * Holds array of join clauses.
	     */
	    private NSMutableArray _alreadyJoined = new NSMutableArray();

		public FrontbaseExpression(EOEntity eoentity) {
			super(eoentity);
			_rtrimFunctionName = null;
			_externalQuoteChar = "\"";
		}

		public void addCreateClauseForAttribute(EOAttribute attribute) {
			StringBuffer sql = new StringBuffer();

			sql.append("\"");
			sql.append(attribute.columnName());
			sql.append("\" ");
			sql.append(columnTypeStringForAttribute(attribute));

			NSDictionary dictionary = attribute.userInfo();
			int internalType = internalTypeForExternal(attribute.externalType());
			boolean isLOB = internalType == FB_BLOB || internalType == FB_CLOB;
			if (dictionary == null) {
				_appendNotNullConstraintIfNecessary(attribute, sql);
			}
			else {
				// Default values.
				if (dictionary.valueForKey("Default") != null) {
					sql.append(" DEFAULT ");
					sql.append(dictionary.valueForKey("Default"));
				}

				if (dictionary.valueForKey("er.extensions.eoattribute.default") != null) {
					sql.append(" DEFAULT ");
					sql.append(formatValueForAttribute(dictionary.valueForKey("er.extensions.eoattribute.default"), attribute));
				}

				// Column constraints.
				_appendNotNullConstraintIfNecessary(attribute, sql);

				if (dictionary.valueForKey("Unique") != null && dictionary.valueForKey("Unique").equals("true")) {
					sql.append(" UNIQUE");
				}
				if (dictionary.valueForKey("Check") != null) {
					sql.append(" CHECK ");
					sql.append(dictionary.valueForKey("Check"));
				}

				// Column collation.
				if (dictionary.valueForKey("Collate") != null) {
					sql.append(" COLLATE ");
					sql.append(dictionary.valueForKey("Collate"));
				}
			}
			appendItemToListString(sql.toString(), _listString());
		}

		private void _appendNotNullConstraintIfNecessary(EOAttribute attribute, StringBuffer sql) {
			if (!attribute.allowsNull()) {
				if (USE_NAMED_CONSTRAINTS) {
					sql.append(" CONSTRAINT ");
					sql.append(notNullConstraintName(attribute));
				}
				sql.append(" NOT NULL");

				int internalType = internalTypeForExternal(attribute.externalType());
				boolean isLOB = internalType == FB_BLOB || internalType == FB_CLOB;
				if (isLOB)
					sql.append(" DEFERRABLE INITIALLY DEFERRED");
			}
		}

		public String columnTypeStringForAttribute(EOAttribute attribute) {
			String externalTypeName = attribute.externalType();
			NSDictionary typeInfo = (NSDictionary) jdbcInfo().objectForKey(JDBCAdaptor.TypeInfoKey);
			if (typeInfo == null) {
				typeInfo = JDBCAdaptor.typeInfoForModel(((EOEntity) attribute.parent()).model());
			}
			NSDictionary externalTypeInfo = (NSDictionary) typeInfo.objectForKey(externalTypeName);
			if (externalTypeInfo == null && externalTypeName != null) {
				externalTypeInfo = (NSDictionary) typeInfo.objectForKey(externalTypeName.toUpperCase());
			}

			if (externalTypeInfo == null) {
				throw new JDBCAdaptorException("Unable to find type information for external type '" + externalTypeName + "' in attribute '" + attribute.name() + "' of entity '" + ((EOEntity) attribute.parent()).name() + "'.  Check spelling and capitalization.", null);
			}
			int createParams;
			try {
				Object createParamsObj = externalTypeInfo.objectForKey("createParams");
				if (createParamsObj instanceof Integer) {
					createParams = ((Integer) createParamsObj).intValue();
				}
				else {
					createParams = Integer.parseInt((String) createParamsObj);
				}
			}
			catch (NumberFormatException numberformatexception) {
				createParams = 0;
			}
			switch (createParams) {
			case 2:
				int precision = attribute.precision();
				if (precision == 0) {
					return attribute.externalType();
				}
				int scale = attribute.scale();
				if (scale == 0) {
					return attribute.externalType() + "(" + precision + ")";
				}
				else {
					return attribute.externalType() + "(" + precision + "," + scale + ")";
				}
			case 1:
				int length = attribute.width();
				if (length == 0) {
					length = attribute.precision();
				}
				if (length == 0) {
					return attribute.externalType();
				}
				else {
					return attribute.externalType() + "(" + length + ")";
				}
			}
			return attribute.externalType();
		}

		public Class _synchronizationFactoryClass() {
			return FrontbaseSynchronizationFactory.class;
		}

		EOQualifier qualifier() {
			return _qualifier;
		}

		public String sqlStringForSelector(NSSelector qualifierOperator, Object value) {
			if (qualifierOperator.equals(EOQualifier.QualifierOperatorContains))
				if (value == NSKeyValueCoding.NullValue)
					return "is";
				else
					return "like";
			else
				return super.sqlStringForSelector(qualifierOperator, value);
		}

		public String externalNameQuoteCharacter() {
			return "\"";
		}

		public String sqlStringForAttribute(EOAttribute attribute) {
			String value = super.sqlStringForAttribute(attribute);

			if (!useAliases())
				value = "\"" + value + "\"";

			return value;
		}

	    /**
	     * Overridden to not call the super implementation.
	     *
	     * @param leftName  the table name on the left side of the clause
	     * @param rightName the table name on the right side of the clause
	     * @param semantic  the join semantic
	     */
	    public void addJoinClause(String leftName,
	                              String rightName,
	                              int semantic) {
	        assembleJoinClause(leftName, rightName, semantic);
	    }

	    /**
	     * Overridden to construct a valid SQL92 JOIN clause as opposed to
	     * the Oracle-like SQL the superclass produces.
	     *
	     * @param leftName  the table name on the left side of the clause
	     * @param rightName the table name on the right side of the clause
	     * @param semantic  the join semantic
	     * @return  the join clause
	     */
	    public String assembleJoinClause(String leftName,
	                                     String rightName,
	                                     int semantic) {
	    	// Can't handle this
	        if (!useAliases()) {
	            return super.assembleJoinClause(leftName, rightName, semantic);
	        }

	        String leftAlias = leftName.substring(0, leftName.indexOf("."));
	        String rightAlias = rightName.substring(0, rightName.indexOf("."));

	        NSArray k;
	        EOEntity rightEntity;
	        EOEntity leftEntity;
	        String relationshipKey = null;
	        EORelationship r;


	        if (leftAlias.equals("t0")) {
	            leftEntity = entity();
	        } else {
	            k = aliasesByRelationshipPath().allKeysForObject(leftAlias);
	            relationshipKey = k.count()>0? (String)k.lastObject() : "";
	            leftEntity = entityForKeyPath(relationshipKey);
	        }

	        if (rightAlias.equals("t0")) {
	            rightEntity = entity();
	        } else {
	            k = aliasesByRelationshipPath().allKeysForObject(rightAlias);
	            relationshipKey = k.count()>0? (String)k.lastObject() : "";
	            rightEntity = entityForKeyPath(relationshipKey);
	        }

	        int dotIndex = relationshipKey.indexOf( "." );
	        relationshipKey = dotIndex == -1
	            ? relationshipKey
	            : relationshipKey.substring( relationshipKey.lastIndexOf( "." ) + 1 );
	        r = rightEntity.anyRelationshipNamed( relationshipKey );

	        // fix from Michael M�ller for the case Foo.fooBars.bar has a Bar.foo relationship (instead of Bar.foos)
	        if( r == null || r.destinationEntity() != leftEntity ) {
	            r = leftEntity.anyRelationshipNamed( relationshipKey );
	        }

	        String rightTable = rightEntity.valueForSQLExpression(this);
	        String leftTable = leftEntity.valueForSQLExpression(this);
	        JoinClause jc = new JoinClause();

	        jc.table1 = leftTable + " " + leftAlias;
	        jc.table2 = rightTable + " " + rightAlias;

	        switch (semantic) {
	            case EORelationship.LeftOuterJoin:
	                jc.op = " LEFT OUTER JOIN ";
	                break;
	            case EORelationship.RightOuterJoin:
	                jc.op = " RIGHT OUTER JOIN ";
	                break;
	            case EORelationship.FullOuterJoin:
	                jc.op = " FULL OUTER JOIN ";
	                break;
	            case EORelationship.InnerJoin:
	                jc.op = " INNER JOIN ";
	                break;
	        }

	        NSArray joins = r.joins();
	        int joinsCount = joins.count();
	        NSMutableArray joinStrings = new NSMutableArray( joinsCount );
	        for( int i = 0; i < joinsCount; i++ ) {
	            EOJoin currentJoin = (EOJoin)joins.objectAtIndex(i);
            	String left = leftAlias +"."+ sqlStringForSchemaObjectName(currentJoin.sourceAttribute().columnName());
            	String right =  rightAlias +"."+ sqlStringForSchemaObjectName(currentJoin.destinationAttribute().columnName());
	            joinStrings.addObject( left + " = " + right);
	        }
	        jc.joinCondition = " ON " + joinStrings.componentsJoinedByString( " AND " );
	        if( !_alreadyJoined.containsObject( jc ) ) {
	            _alreadyJoined.insertObjectAtIndex(jc, 0);
	            return jc.toString();
	        }
	        return null;
	    }

	    /**
	     * Overridden to handle correct placements of join conditions and
	     * to handle DISTINCT fetches with compareCaseInsensitiveA(De)scending sort orders.
	     *
	     * @param attributes    the attributes to select
	     * @param lock  flag for locking rows in the database
	     * @param qualifier the qualifier to restrict the selection
	     * @param fetchOrder    specifies the fetch order
	     * @param columnList    the SQL columns to be fetched
	     * @param tableList the the SQL tables to be fetched
	     * @param whereClause   the SQL where clause
	     * @param joinClause    the SQL join clause
	     * @param orderByClause the SQL sort order clause
	     * @param lockClause    the SQL lock clause
	     * @return  the select statement
	     */
	    public String assembleSelectStatementWithAttributes(NSArray attributes,
	                                                        boolean lock,
	                                                        EOQualifier qualifier,
	                                                        NSArray fetchOrder,
	                                                        String selectString,
	                                                        String columnList,
	                                                        String tableList,
	                                                        String whereClause,
	                                                        String joinClause,
	                                                        String orderByClause,
	                                                        String lockClause) {

	        // Adds the labels in an order by clause to the list of columns in the select
            // clause if not already existing.
            if (orderByClause != null && orderByClause.length() > 0) {
                int i = 0;
                while (i != -1) {
                    if (orderByClause.indexOf(' ', i) == i + 1)
                        i += 2;

                    int j = orderByClause.indexOf(' ', i);
                    int k = orderByClause.indexOf(',', i);
                    if (j > k && k != -1)
                        j = k;
                    else if (j == -1 && k == -1)
                        j = orderByClause.length();

                    String orderColumn = orderByClause.substring(i, j);
                    if (columnList.indexOf(orderColumn) == -1)
                    	columnList = columnList.concat(", " + orderColumn);

                    i = orderByClause.indexOf(',', i);
                }
            }

	        StringBuffer sb = new StringBuffer();
	        sb.append(selectString);
	        sb.append(columnList);
	        // AK: using DISTINCT with ORDER BY UPPER(foo) is an error if it is not also present in the columns list...
	        // This implementation sucks, but should be good enough for the normal case
	        if(selectString.indexOf(" DISTINCT") != -1) {
	            String [] columns = orderByClause.split(",");
	            for(int i = 0; i < columns.length; i++) {
	                String column = columns[i].replaceFirst("\\s+(ASC|DESC)\\s*", "");
	                if(columnList.indexOf(column) == -1) {
	                    sb.append(", ");
	                    sb.append(column);
	                }
	            }
	        }

	        sb.append(" FROM ");
	        String fieldString;
	        if (_alreadyJoined.count() > 0) {
	            fieldString = joinClauseString();
	        } else {
	            fieldString = tableList;
	        }
	        sb.append(fieldString);

	        if ((whereClause != null && whereClause.length() > 0) ||
	            (joinClause != null && joinClause.length() > 0)) {
	            sb.append(" WHERE ");
	            if (joinClause != null && joinClause.length() > 0) {
	                sb.append(joinClause);
	                if (whereClause != null && whereClause.length() > 0)
	                    sb.append(" AND ");
	            }

	            if (whereClause != null && whereClause.length() > 0) {
	                sb.append(whereClause);
	            }
	        }

	        if (orderByClause != null && orderByClause.length() > 0) {
	            sb.append(" ORDER BY ");
	            sb.append(orderByClause);
	        }
	        if (lockClause != null && lockClause.length() > 0) {
	            sb.append(" ");
	            sb.append(lockClause);
	        }
	        return sb.toString();
	    }


	    /**
	     * Overrides the parent implementation to compose the final string
	     * expression for the join clauses.
	     */
	    public String joinClauseString() {
	        NSMutableDictionary seenIt = new NSMutableDictionary();
	        StringBuffer sb = new StringBuffer();
	        JoinClause jc;
	        EOSortOrdering.sortArrayUsingKeyOrderArray
	            ( _alreadyJoined, new NSArray( EOSortOrdering.sortOrderingWithKey( "sortKey", EOSortOrdering.CompareCaseInsensitiveAscending ) ) );
	        if (_alreadyJoined.count() > 0) {
	            jc = (JoinClause)_alreadyJoined.objectAtIndex(0);

	            sb.append(jc);
	            seenIt.setObjectForKey(Boolean.TRUE, jc.table1);
	            seenIt.setObjectForKey(Boolean.TRUE, jc.table2);
	        }

	        for (int i = 1; i < _alreadyJoined.count(); i++) {
	            jc = (JoinClause)_alreadyJoined.objectAtIndex(i);

	            sb.append(jc.op);
	            if (seenIt.objectForKey(jc.table1) == null) {
	                sb.append(jc.table1);
	                seenIt.setObjectForKey(Boolean.TRUE, jc.table1);
	            }
	            else if (seenIt.objectForKey(jc.table2) == null) {
	                sb.append(jc.table2);
	                seenIt.setObjectForKey(Boolean.TRUE, jc.table2);
	            }
	            sb.append(jc.joinCondition);
	        }
	        return sb.toString();
	    }

		public void addOrderByAttributeOrdering(EOSortOrdering eosortordering) {
			NSSelector sortOrdering = eosortordering.selector();
			String attribute = eosortordering.key();
			String column = sqlStringForAttributeNamed(attribute);

			if (column == null)
				// Super throws exception.
				super.addOrderByAttributeOrdering(eosortordering);

			StringBuffer sql = new StringBuffer(column);

			if (sortOrdering == EOSortOrdering.CompareCaseInsensitiveAscending)
				if (entity()._attributeForPath(attribute).adaptorValueType() == 1)
					sql.append(" COLLATE INFORMATION_SCHEMA.CASE_INSENSITIVE ASC");
				else
					sql.append(" ASC");
			else if (sortOrdering == EOSortOrdering.CompareCaseInsensitiveDescending)
				if (entity()._attributeForPath(attribute).adaptorValueType() == 1)
					sql.append(" COLLATE INFORMATION_SCHEMA.CASE_INSENSITIVE DESC");
				else
					sql.append(" DESC");
			else if (sortOrdering == EOSortOrdering.CompareAscending)
				sql.append(" ASC");
			else if (sortOrdering == EOSortOrdering.CompareDescending)
				sql.append(" DESC");

			appendItemToListString(sql.toString(), _orderByString());
		}

		public String assembleDeleteStatementWithQualifier(EOQualifier eoqualifier, String table, String qualifier) {
			if (table != null && table.indexOf('"') == -1)
				return super.assembleDeleteStatementWithQualifier(eoqualifier, quoteTableName(table), qualifier);
			else
				return super.assembleDeleteStatementWithQualifier(eoqualifier, table, qualifier);
		}

		public String assembleInsertStatementWithRow(NSDictionary nsdictionary, String table, String columns, String values) {
			if (table != null && table.indexOf('"') == -1)
				return super.assembleInsertStatementWithRow(nsdictionary, quoteTableName(table), columns, values);
			else
				return super.assembleInsertStatementWithRow(nsdictionary, table, columns, values);
		}

		public String assembleUpdateStatementWithRow(NSDictionary dictionary, EOQualifier qualifier, String table, String values, String sqlQualifier) {
			_qualifier = qualifier;
			if (table != null && table.indexOf('"') == -1)
				return super.assembleUpdateStatementWithRow(dictionary, qualifier, quoteTableName(table), values, sqlQualifier);
			else
				return super.assembleUpdateStatementWithRow(dictionary, qualifier, table, values, sqlQualifier);
		}

		public String lockClause() {
			return "";
		}

		public boolean useBindVariables() {
			return false;
		}

		public boolean shouldUseBindVariableForAttribute(EOAttribute eoattribute) {
			return false;
		}

		public boolean mustUseBindVariableForAttribute(EOAttribute eoattribute) {
			return false;
		}

		public String sqlStringForCaseInsensitiveLike(String value, String column) {
			StringBuffer sql = new StringBuffer();

			sql.append(column);
			sql.append(" LIKE ");
			sql.append(value);
			sql.append(" COLLATE INFORMATION_SCHEMA.CASE_INSENSITIVE");

			return sql.toString();
		}

		boolean hasLOBsToUpdate() {
			return _lobList != null && _lobList.count() > 0;
		}

		void resetlobList() {
			_lobList = null;
		}

		NSArray lobList() {
			return _lobList != null ? _lobList : NSArray.EmptyArray;
		}

		public String sqlStringForKeyValueQualifier(EOKeyValueQualifier eokeyvaluequalifier) {
			String attrubute = eokeyvaluequalifier.key();
			String column = sqlStringForAttributeNamed(attrubute);

			if (column == null)
				throw new IllegalStateException("sqlStringForKeyValueQualifier: attempt to generate SQL for " + eokeyvaluequalifier.getClass().getName() + " " + eokeyvaluequalifier + " failed because attribute identified by key '" + attrubute + "' was not reachable from from entity '" + _entity.name() + "'");
			Object qualifier = eokeyvaluequalifier.value();
			if (qualifier instanceof EOQualifierVariable)
				throw new IllegalStateException("sqlStringForKeyValueQualifier: attempt to generate SQL for " + eokeyvaluequalifier.getClass().getName() + " " + eokeyvaluequalifier + " failed because the qualifier variable '$" + ((EOQualifierVariable) qualifier).key() + "' is unbound.");
			column = formatSQLString(column, _entity._attributeForPath(attrubute).readFormat());
			NSSelector nsselector = eokeyvaluequalifier.selector();

			boolean flag = nsselector.equals(EOQualifier.QualifierOperatorLike) || nsselector.equals(EOQualifier.QualifierOperatorCaseInsensitiveLike) || nsselector.equals(EOQualifier.QualifierOperatorContains);

			if (flag) {
				qualifier = sqlPatternFromShellPattern(qualifier.toString());
			}

			StringBuffer sql = new StringBuffer();
			char sqlEscapeChar = sqlEscapeChar();
			String value;

			if (nsselector.equals(EOQualifier.QualifierOperatorCaseInsensitiveLike)) {
				value = sqlStringForValue(qualifier, attrubute);

				sql.append(sqlStringForCaseInsensitiveLike(value, column));
			}
			else {
				value = sqlStringForValue(qualifier, attrubute);

				sql.append(column);
				sql.append(" ");
				sql.append(sqlStringForSelector(nsselector, qualifier));
				sql.append(" ");
				sql.append(value);
			}

			if (value.indexOf(sqlEscapeChar) != -1 && flag) {
				sql.append(" ESCAPE '");
				sql.append(sqlEscapeChar);
				sql.append("'");
			}
			return sql.toString();
		}

		public String formatValueForAttribute(Object obj, EOAttribute eoattribute) {
			if (obj != null && obj != NSKeyValueCoding.NullValue) {
				if (eoattribute.valueFactoryMethod() != null && eoattribute.valueFactoryMethod().implementedByObject(obj) && eoattribute.adaptorValueConversionMethod().implementedByObject(obj)) {
					obj = eoattribute.adaptorValueByConvertingAttributeValue(obj);
				}

				if (eoattribute.externalType() == null)
				{
				    throw new EOGeneralAdaptorException("Attribute " + eoattribute.name() +
				    		" on entity " + eoattribute.entity().name() +
				    		" with prototype named " + eoattribute.prototypeName() +
				    		" has no external type defined");
				}

				switch (internalTypeForExternal(eoattribute.externalType())) {
				case FB_Character:
				case FB_VCharacter: {
					String value = obj.toString();
					if (value.indexOf("'") == -1)
						return "'" + value + "'";
					else {
						return "'" + addEscapeChars(value) + "'";
					}
				}
				case FB_DayTime: {
					return obj.toString();
				}
				case FB_BLOB: {
					if (obj instanceof String)
						if (((String) obj).length() == 27 && ((String) obj).startsWith("@"))
							return (String) obj;
					if (_lobList == null)
						_lobList = new NSMutableArray();
					_lobList.addObject(eoattribute);
					_lobList.addObject(obj);
					return "NULL";
				}
				case FB_CLOB: {
					if (obj instanceof String)
						if (((String) obj).length() == 27 && ((String) obj).startsWith("@"))
							return (String) obj;
					if (_lobList == null)
						_lobList = new NSMutableArray();
					_lobList.addObject(eoattribute);
					_lobList.addObject(obj);
					return "NULL";
				}
				case FB_VBit:
				case FB_Bit: {
					if (obj instanceof NSData) {
						return formatBit((NSData) obj);
					}
					else {
						return "ERROR: Can not convert value from " + obj + " to Bit or Byte data type";
					}
				}
				case FB_Time: {
					NSTimestampFormatter f = new NSTimestampFormatter("%H:%M:%S.%F");

					StringBuffer time = new StringBuffer("TIME '");
					f.format(obj, time, new FieldPosition(0));
					time.append("'");
					return time.toString();
				}

				case FB_TimeTZ: {
					NSTimestampFormatter f = new NSTimestampFormatter("%H:%M:%S.%F");

					StringBuffer time = new StringBuffer("TIME '");
					f.format(obj, time, new FieldPosition(0));
					time.append(getTimeZone(NSTimeZone.defaultTimeZone()));
					time.append("'");
					return time.toString();
				}

				case FB_Timestamp: {
					NSTimestampFormatter f = new NSTimestampFormatter("%Y-%m-%d %H:%M:%S.%F");

					StringBuffer time = new StringBuffer("TIMESTAMP '");
					f.format(obj, time, new FieldPosition(0));
					time.append("'");
					return time.toString();
				}
				case FB_TimestampTZ: {
					NSTimestampFormatter f = new NSTimestampFormatter("%Y-%m-%d %H:%M:%S.%F");

					StringBuffer time = new StringBuffer("TIMESTAMP '");
					f.format(obj, time, new FieldPosition(0));
					time.append(getTimeZone(java.util.TimeZone.getDefault()));
					time.append("'");
					return time.toString();
				}
				case FB_Date: {
					NSTimestampFormatter f = new NSTimestampFormatter("%Y-%m-%d");

					StringBuffer time = new StringBuffer("DATE '");
					f.format(obj, time, new FieldPosition(0));
					time.append("'");
					return time.toString();
				}
				case FB_Boolean: {
					if (obj instanceof Boolean || obj instanceof String)
						return obj.toString();
					if (obj instanceof NSData)
						return (((NSData) obj).bytes()[0] == 0) ? "FALSE" : "TRUE";
					if (((Number) obj).intValue() == 0)
						return "FALSE";
					else
						return "TRUE";
				}
				case FB_TinyInteger:
				case FB_Numeric:
				case FB_Integer:
				case FB_Decimal: {
					if (obj instanceof BigDecimal) {
						return ((BigDecimal) obj).setScale(eoattribute.scale(), BigDecimal.ROUND_HALF_UP).toString();
					}
					else if (obj instanceof Number) {
						String valueType = eoattribute.valueType();
						if (valueType == null || "i".equals(valueType)) {
							return String.valueOf(((Number) obj).intValue());
						}
						else if ("l".equals(valueType)) {
							return String.valueOf(((Number) obj).longValue());
						}
						else if ("f".equals(valueType)) {
							return String.valueOf(((Number) obj).floatValue());
						}
						else if ("d".equals(valueType)) {
							return String.valueOf(((Number) obj).doubleValue());
						}
						else if ("s".equals(valueType)) {
							return String.valueOf(((Number) obj).shortValue());
						}
						else if ("c".equals(valueType)) {
							return String.valueOf(((Number) obj).intValue());
						}
					}
					else if (obj instanceof Boolean) {
						String valueType = eoattribute.valueType();
						return String.valueOf(((Boolean) obj).booleanValue() ? 1 : 0);
					}
					else if (obj instanceof String) {
						return obj.toString();
					}
				}
				default:
					return obj.toString();
				}
			}
			return super.formatValueForAttribute(obj, eoattribute);
		}

		String addEscapeChars(String value) {
			StringBuffer sb = new StringBuffer(value);
			int index = 0;

			for (int i = 0; i < value.length(); i++, index++) {
				index = value.indexOf("'", index);
				if (index == -1)
					break;
				sb.insert(index + i, "'");
			}

			return sb.toString();
		}

		String formatBit(NSData data) {
			char[] heximals = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
			byte[] bytes = data.bytes();
			int b;

			StringBuffer result = new StringBuffer((2 * data.length()) + 3);
			result.append("X'");

			for (int i = 0; i < data.length(); i++) {
				b = bytes[i] & 0xFF;
				result.append(heximals[b / 16]);
				result.append(heximals[b % 16]);
			}

			result.append("'");
			return result.toString();
		}

		private String getTimeZone(java.util.TimeZone tz) {
			String sign = "+";
			int tzOffset = tz.getRawOffset();

			if (tz.useDaylightTime() && tz.inDaylightTime(new java.util.Date(System.currentTimeMillis()))) {
				tzOffset += 3600000L;
			}

			if (tzOffset < 0) {
				tzOffset = -tzOffset;
				sign = "-";
			}

			int hour = tzOffset / 3600000;
			int minute = (tzOffset % 3600000) / 60000;

			String hourString = String.valueOf(hour);
			String minuteString = String.valueOf(minute);

			if (hourString.length() < 2)
				hourString = "0" + hourString;
			if (minuteString.length() < 2)
				minuteString = "0" + minuteString;

			return sign + hourString + ":" + minuteString;
		}

	    /**
	     * Utility that traverses a key path to find the last destination entity
	     *
	     * @param keyPath   the key path
	     * @return  the entity at the end of the keypath
	     */
	    private EOEntity entityForKeyPath(String keyPath) {
	        NSArray keys = NSArray.componentsSeparatedByString(keyPath, ".");
	        EOEntity ent = entity();

	        for (int i = 0; i < keys.count(); i++) {
	            String k = (String)keys.objectAtIndex(i);
	            EORelationship rel = ent.anyRelationshipNamed(k);
	            if (rel == null) {
	                // it may be an attribute
	                if (ent.anyAttributeNamed(k) != null) {
	                    break;
	                }
	                throw new IllegalArgumentException("relationship " + keyPath + " generated null");
	            }
	            ent = rel.destinationEntity();
	        }
	        return ent;
	    }

	    /**
	     * Helper class that stores a join definition and
	     * helps <code>FrontbaseExpression</code> to assemble
	     * the correct join clause.
	     */
	    public class JoinClause {
	        String table1;
	        String op;
	        String table2;
	        String joinCondition;

	        public String toString() {
	            return table1 + op + table2 + joinCondition;
	        }

	        public boolean equals( Object obj ) {
	            if( obj == null || !(obj instanceof JoinClause) ) {
	                return false;
	            }
	            return toString().equals( obj.toString() );
	        }

	        /**
	         * Returns the table alias for the first table (e.g. returns T2 if table 1 is "Students" T2).  This makes this class "sortable"
	         * which is needed to correctly assemble a join clause.
	         *
	         * @return the table alias (e.g. returns T2 if table1 is "Students" T2)
	         */
	        public String sortKey() {
	            return table1.substring( table1.indexOf( " " ) + 1 );
	        }
	    }

	}

}
