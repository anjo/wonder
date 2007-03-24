package er.extensions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.webobjects.eoaccess.EOAdaptor;
import com.webobjects.eoaccess.EOAdaptorChannel;
import com.webobjects.eoaccess.EOAdaptorContext;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EODatabaseChannel;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EOQualifierSQLGeneration;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOSQLExpressionFactory;
import com.webobjects.eoaccess.EOSchemaGeneration;
import com.webobjects.eoaccess.EOSynchronizationFactory;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.jdbcadaptor.JDBCAdaptor;
import com.webobjects.jdbcadaptor.JDBCPlugIn;

/**
 * ERXSQLHelper provides support for additional database-vender-specific operations that JDBCPlugIn does not cover.
 * 
 * By default this will try to load the class er.extensions.ERXSQLHelper$DatabaseVendorSQLHelper. For instance,
 * er.extensions.ERXSQLHelper$FrontBaseSQLHelper. If you want to change the helper that is used for a particular
 * database vendor, then override FrontBase.SQLHelper, Oracle.SQLHelper, etc. Case is important (because the vendor name
 * is prepended to the class name), and should match what your JDBCPlugIn.databaseProductName() returns.
 * 
 * @property databaseProductName.SQLHelper the class name of the SQLHelper for the database product name
 * 
 * @author mschrag
 */
public class ERXSQLHelper {
	/** logging support */
	public static final Logger log = Logger.getLogger(ERXSQLHelper.class);

	private static Map _sqlHelperMap = new HashMap();

	private JDBCPlugIn _plugin;

	public boolean shouldExecute(String sql) {
		return true;
	}

	/**
	 * creates SQL to create tables for the specified Entities. This can be used with EOUtilities rawRowsForSQL method
	 * to create the tables.
	 * 
	 * @param entities
	 *            a NSArray containing the entities for which create table statements should be generated or null if all
	 *            entitites in the model should be used.
	 * @param modelName
	 *            the name of the EOModel
	 * @param optionsCreate
	 *            a NSDictionary containing the different options. Possible keys are
	 *            <ul>
	 *            <li>EOSchemaGeneration.DropTablesKey</li>
	 *            <li>EOSchemaGeneration.DropPrimaryKeySupportKey</li>
	 *            <li>EOSchemaGeneration.CreateTablesKey</li>
	 *            <li>EOSchemaGeneration.CreatePrimaryKeySupportKey</li>
	 *            <li>EOSchemaGeneration.PrimaryKeyConstraintsKey</li>
	 *            <li>EOSchemaGeneration.ForeignKeyConstraintsKey</li>
	 *            <li>EOSchemaGeneration.CreateDatabaseKey</li>
	 *            </ul>
	 *            <li>EOSchemaGeneration.DropDatabaseKey</li>
	 *            <br/><br>
	 *            Possible values are <code>YES</code> and <code>NO</code>
	 * 
	 * @return a <code>String</code> containing SQL statements to create tables
	 */
	public String createSchemaSQLForEntitiesInModelWithNameAndOptions(NSArray entities, String modelName, NSDictionary optionsCreate) {
		EOModel m = ERXEOAccessUtilities.modelGroup(null).modelNamed(modelName);
		return createSchemaSQLForEntitiesInModelAndOptions(entities, m, optionsCreate);
	}

	/**
	 * creates SQL to create tables for the specified Entities. This can be used with EOUtilities rawRowsForSQL method
	 * to create the tables.
	 * 
	 * @param entities
	 *            a NSArray containing the entities for which create table statements should be generated or null if all
	 *            entitites in the model should be used.
	 * @param model
	 *            the EOModel
	 * @param optionsCreate
	 *            a NSDictionary containing the different options. Possible keys are
	 *            <ul>
	 *            <li>EOSchemaGeneration.DropTablesKey</li>
	 *            <li>EOSchemaGeneration.DropPrimaryKeySupportKey</li>
	 *            <li>EOSchemaGeneration.CreateTablesKey</li>
	 *            <li>EOSchemaGeneration.CreatePrimaryKeySupportKey</li>
	 *            <li>EOSchemaGeneration.PrimaryKeyConstraintsKey</li>
	 *            <li>EOSchemaGeneration.ForeignKeyConstraintsKey</li>
	 *            <li>EOSchemaGeneration.CreateDatabaseKey</li>
	 *            </ul>
	 *            <li>EOSchemaGeneration.DropDatabaseKey</li>
	 *            <br/><br>
	 *            Possible values are <code>YES</code> and <code>NO</code>
	 * 
	 * @return a <code>String</code> containing SQL statements to create tables
	 */
	public String createSchemaSQLForEntitiesInModelAndOptions(NSArray entities, EOModel model, NSDictionary optionsCreate) {
		EODatabaseContext databaseContext = EODatabaseContext.registeredDatabaseContextForModel(model, ERXEC.newEditingContext());
		if (entities == null) {
			Enumeration e = model.entities().objectEnumerator();
			NSMutableArray ar = new NSMutableArray();
			while (e.hasMoreElements()) {
				EOEntity currentEntity = (EOEntity) e.nextElement();
				if (ERXModelGroup.isPrototypeEntity(currentEntity)) {
					// we do not want to add EOXXXPrototypes entities
					continue;
				}
				if (!ERXEOAccessUtilities.entityUsesSeparateTable(currentEntity)) {
					continue;
				}
				ar.addObject(currentEntity);
			}
			entities = ar;
		}
		return createSchemaSQLForEntitiesWithOptions(entities, databaseContext, optionsCreate);
	}

	/**
	 * Creates the schema sql for a set of entities.
	 * 
	 * @param entities
	 *            the entities to create sql for
	 * @param databaseContext
	 *            the database context to use
	 * @param optionsCreate
	 *            the options (@see createSchemaSQLForEntitiesInModelWithNameAndOptions)
	 * @return a sql script
	 */
	public String createSchemaSQLForEntitiesWithOptions(NSArray entities, EODatabaseContext databaseContext, NSDictionary optionsCreate) {
		// get the JDBCAdaptor
		EOAdaptorContext ac = databaseContext.adaptorContext();
		EOSynchronizationFactory sf = ((JDBCAdaptor) ac.adaptor()).plugIn().createSynchronizationFactory();
		return sf.schemaCreationScriptForEntities(entities, optionsCreate);
	}

	/**
	 * creates SQL to create tables for the specified Entities. This can be used with EOUtilities rawRowsForSQL method
	 * to create the tables.
	 * 
	 * @param entities
	 *            a NSArray containing the entities for which create table statements should be generated or null if all
	 *            entitites in the model should be used.
	 * @param modelName
	 *            the name of the EOModel <br/><br/>This method uses the following defaults options:
	 *            <ul>
	 *            <li>EOSchemaGeneration.DropTablesKey=YES</li>
	 *            <li>EOSchemaGeneration.DropPrimaryKeySupportKey=YES</li>
	 *            <li>EOSchemaGeneration.CreateTablesKey=YES</li>
	 *            <li>EOSchemaGeneration.CreatePrimaryKeySupportKey=YES</li>
	 *            <li>EOSchemaGeneration.PrimaryKeyConstraintsKey=YES</li>
	 *            <li>EOSchemaGeneration.ForeignKeyConstraintsKey=YES</li>
	 *            <li>EOSchemaGeneration.CreateDatabaseKey=NO</li>
	 *            <li>EOSchemaGeneration.DropDatabaseKey=NO</li>
	 *            </ul>
	 *            <br/><br>
	 *            Possible values are <code>YES</code> and <code>NO</code>
	 * 
	 * @return a <code>String</code> containing SQL statements to create tables
	 */
	public String createSchemaSQLForEntitiesInModelWithName(NSArray entities, String modelName) {
		EOModel model = ERXEOAccessUtilities.modelGroup(null).modelNamed(modelName);
		return createSchemaSQLForEntitiesInModel(entities, model);
	}

	/**
	 * creates SQL to create tables for the specified Entities. This can be used with EOUtilities rawRowsForSQL method
	 * to create the tables.
	 * 
	 * @param entities
	 *            a NSArray containing the entities for which create table statements should be generated or null if all
	 *            entitites in the model should be used.
	 * @param model
	 *            the EOModel <br/><br/>This method uses the following defaults options:
	 *            <ul>
	 *            <li>EOSchemaGeneration.DropTablesKey=YES</li>
	 *            <li>EOSchemaGeneration.DropPrimaryKeySupportKey=YES</li>
	 *            <li>EOSchemaGeneration.CreateTablesKey=YES</li>
	 *            <li>EOSchemaGeneration.CreatePrimaryKeySupportKey=YES</li>
	 *            <li>EOSchemaGeneration.PrimaryKeyConstraintsKey=YES</li>
	 *            <li>EOSchemaGeneration.ForeignKeyConstraintsKey=YES</li>
	 *            <li>EOSchemaGeneration.CreateDatabaseKey=NO</li>
	 *            <li>EOSchemaGeneration.DropDatabaseKey=NO</li>
	 *            </ul>
	 *            <br/><br>
	 *            Possible values are <code>YES</code> and <code>NO</code>
	 * 
	 * @return a <code>String</code> containing SQL statements to create tables
	 */
	public String createSchemaSQLForEntitiesInModel(NSArray entities, EOModel model) {
		NSMutableDictionary optionsCreate = new NSMutableDictionary();
		optionsCreate.setObjectForKey("YES", EOSchemaGeneration.DropTablesKey);
		optionsCreate.setObjectForKey("YES", EOSchemaGeneration.DropPrimaryKeySupportKey);
		optionsCreate.setObjectForKey("YES", EOSchemaGeneration.CreateTablesKey);
		optionsCreate.setObjectForKey("YES", EOSchemaGeneration.CreatePrimaryKeySupportKey);
		optionsCreate.setObjectForKey("YES", EOSchemaGeneration.PrimaryKeyConstraintsKey);
		optionsCreate.setObjectForKey("YES", EOSchemaGeneration.ForeignKeyConstraintsKey);
		optionsCreate.setObjectForKey("NO", EOSchemaGeneration.CreateDatabaseKey);
		optionsCreate.setObjectForKey("NO", EOSchemaGeneration.DropDatabaseKey);
		return createSchemaSQLForEntitiesInModelAndOptions(entities, model, optionsCreate);
	}

	/**
	 * creates SQL to create tables for the specified Entities. This can be used with EOUtilities rawRowsForSQL method
	 * to create the tables.
	 * 
	 * @param entities
	 *            a NSArray containing the entities for which create table statements should be generated or null if all
	 *            entitites in the model should be used.
	 * @param databaseContext
	 *            the databaseContext
	 * 
	 * @param create
	 *            if true, tables and keys are created
	 * @param drop
	 *            if true, tables and keys are dropped
	 * @return a <code>String</code> containing SQL statements to create tables
	 */
	public String createSchemaSQLForEntitiesInDatabaseContext(NSArray entities, EODatabaseContext databaseContext, boolean create, boolean drop) {
		NSMutableDictionary optionsCreate = new NSMutableDictionary();
		optionsCreate.setObjectForKey((drop) ? "YES" : "NO", EOSchemaGeneration.DropTablesKey);
		optionsCreate.setObjectForKey((drop) ? "YES" : "NO", EOSchemaGeneration.DropPrimaryKeySupportKey);
		optionsCreate.setObjectForKey((create) ? "YES" : "NO", EOSchemaGeneration.CreateTablesKey);
		optionsCreate.setObjectForKey((create) ? "YES" : "NO", EOSchemaGeneration.CreatePrimaryKeySupportKey);
		optionsCreate.setObjectForKey((create) ? "YES" : "NO", EOSchemaGeneration.PrimaryKeyConstraintsKey);
		optionsCreate.setObjectForKey((create) ? "YES" : "NO", EOSchemaGeneration.ForeignKeyConstraintsKey);
		optionsCreate.setObjectForKey("NO", EOSchemaGeneration.CreateDatabaseKey);
		optionsCreate.setObjectForKey("NO", EOSchemaGeneration.DropDatabaseKey);
		return createSchemaSQLForEntitiesWithOptions(entities, databaseContext, optionsCreate);
	}

	public String createIndexSQLForEntities(NSArray entities) {
		return createIndexSQLForEntities(entities, null);
	}

	public String createIndexSQLForEntities(NSArray entities, NSArray externalTypesToIgnore) {
		if (externalTypesToIgnore == null) {
			externalTypesToIgnore = NSArray.EmptyArray;
		}
		if (entities == null || entities.count() == 0) {
			return "";
		}
		int i = 0;
		String oldIndexName = null;
		String lineSeparator = System.getProperty("line.separator");
		StringBuffer buf = new StringBuffer();

		EOEntity ent = (EOEntity) entities.objectAtIndex(0);
		String modelName = ent.model().name();
		String commandSeparator = commandSeparatorString();

		for (Enumeration entitiesEnum = entities.objectEnumerator(); entitiesEnum.hasMoreElements();) {
			EOEntity entity = (EOEntity) entitiesEnum.nextElement();
			// only use this entity if it has its own table
			if (!ERXEOAccessUtilities.entityUsesSeparateTable(entity)) {
				continue;
			}

			NSDictionary d = entity.userInfo();
			NSMutableArray usedColumns = new NSMutableArray();
			for (Enumeration keys = d.keyEnumerator(); keys.hasMoreElements();) {
				String key = (String) keys.nextElement();
				if (key.startsWith("index")) {
					String numbers = key.substring("index".length());
					if (ERXStringUtilities.isDigitsOnly(numbers)) {
						String attributeNames = (String) d.objectForKey(key);
						if (ERXStringUtilities.stringIsNullOrEmpty(attributeNames)) {
							continue;
						}
						String indexName = "c" + System.currentTimeMillis() + new NSTimestamp().getNanos();
						String newIndexName = i == 0 ? indexName : indexName + "_" + i;
						if (oldIndexName == null) {
							oldIndexName = indexName;
						}
						else if (oldIndexName.equals(newIndexName)) {
							indexName += "_" + ++i;
						}
						else {
							i = 0;
						}
						oldIndexName = indexName;
						StringBuffer localBuf = new StringBuffer();
						StringBuffer columnBuf = new StringBuffer();
						boolean validIndex = false;
						localBuf.append("create index " + indexName + " on " + entity.externalName() + "(");
						for (Enumeration attributes = NSArray.componentsSeparatedByString(attributeNames, ",").objectEnumerator(); attributes.hasMoreElements();) {
							String attributeName = (String) attributes.nextElement();
							attributeName = attributeName.trim();
							EOAttribute attribute = entity.attributeNamed(attributeName);
							if (attribute == null) {
								attribute = ERXEOAccessUtilities.attributeWithColumnNameFromEntity(attributeName, entity);
							}
							if (attribute != null && externalTypesToIgnore.indexOfObject(attribute.externalType()) != NSArray.NotFound) {
								continue;
							}
							validIndex = true;
							String columnName = attribute == null ? attributeName : attribute.columnName();
							columnBuf.append(columnName);
							if (attributes.hasMoreElements()) {
								columnBuf.append(", ");
							}
						}
						if (validIndex) {
							String l = columnBuf.toString();
							if (l.endsWith(", ")) {
								l = l.substring(0, l.length() - 2);
							}
							if (usedColumns.indexOfObject(l) == NSArray.NotFound) {
								buf.append(localBuf).append(l);
								usedColumns.addObject(l);
								buf.append(")").append(commandSeparator).append(lineSeparator);
							}
						}
					}
				}
				else if (key.equals("additionalIndexes")) {
					// this is a space separated list of column or attribute
					// names
					String value = (String) d.objectForKey(key);
					for (Enumeration indexes = NSArray.componentsSeparatedByString(value, " ").objectEnumerator(); indexes.hasMoreElements();) {
						String indexValues = (String) indexes.nextElement();
						if (ERXStringUtilities.stringIsNullOrEmpty(indexValues)) {
							continue;
						}

						// this might be a comma separate list
						String indexName = "c" + System.currentTimeMillis() + new NSTimestamp().getNanos();
						String newIndexName = i == 0 ? indexName : indexName + "_" + i;
						if (oldIndexName == null) {
							oldIndexName = indexName;
						}
						else if (oldIndexName.equals(newIndexName)) {
							indexName += "_" + ++i;
						}
						else {
							i = 0;
						}
						oldIndexName = indexName;

						StringBuffer localBuf = new StringBuffer();
						StringBuffer columnBuf = new StringBuffer();
						boolean validIndex = false;
						localBuf.append("create index " + indexName + " on " + entity.externalName() + "(");
						for (Enumeration e = NSArray.componentsSeparatedByString(indexValues, ",").objectEnumerator(); e.hasMoreElements();) {
							String attributeName = (String) e.nextElement();
							attributeName = attributeName.trim();
							EOAttribute attribute = entity.attributeNamed(attributeName);

							if (attribute == null) {
								attribute = ERXEOAccessUtilities.attributeWithColumnNameFromEntity(attributeName, entity);
							}
							if (attribute != null && externalTypesToIgnore.indexOfObject(attribute.externalType()) != NSArray.NotFound) {
								continue;
							}
							validIndex = true;

							String columnName = attribute == null ? attributeName : attribute.columnName();
							columnBuf.append(columnName);
							if (e.hasMoreElements()) {
								columnBuf.append(", ");
							}
						}
						if (validIndex) {
							String l = columnBuf.toString();
							if (l.endsWith(", ")) {
								l = l.substring(0, l.length() - 2);
							}
							if (usedColumns.indexOfObject(l) == NSArray.NotFound) {
								buf.append(localBuf).append(l);
								usedColumns.addObject(l);
								buf.append(")").append(commandSeparator).append(lineSeparator);
							}
						}
					}
				}
			}
		}
		return buf.toString();
	}

	/**
	 * Creates the SQL which is used by the provided EOFetchSpecification, limited by the given range.
	 * 
	 * @param ec
	 *            the EOEditingContext
	 * @param spec
	 *            the EOFetchSpecification in question
	 * @param start
	 *            start of rows to fetch
	 * @param end
	 *            end of rows to fetch (-1 if not used)
	 * 
	 * @return the EOSQLExpression which the EOFetchSpecification would use
	 */
	public EOSQLExpression sqlExpressionForFetchSpecification(EOEditingContext ec, EOFetchSpecification spec, long start, long end) {
		EOEntity entity = ERXEOAccessUtilities.entityNamed(ec, spec.entityName());
		EOModel model = entity.model();
		EODatabaseContext dbc = EOUtilities.databaseContextForModelNamed(ec, model.name());
		EOAdaptor adaptor = dbc.adaptorContext().adaptor();
		EOSQLExpressionFactory sqlFactory = adaptor.expressionFactory();
		spec = (EOFetchSpecification) spec.clone();
		NSArray attributes = entity.attributesToFetch();
		if (spec.fetchesRawRows()) {
			NSMutableArray arr = new NSMutableArray();
			for (Enumeration e = spec.rawRowKeyPaths().objectEnumerator(); e.hasMoreElements();) {
				String keyPath = (String) e.nextElement();
				arr.addObject(entity.anyAttributeNamed(keyPath));
			}
			attributes = arr.immutableClone();
		}

		EOQualifier qualifier = spec.qualifier();
		if (qualifier != null) {
			qualifier = EOQualifierSQLGeneration.Support._schemaBasedQualifierWithRootEntity(qualifier, entity);
		}
		if (qualifier != spec.qualifier()) {
			spec.setQualifier(qualifier);
		}
		if (spec.fetchLimit() > 0) {
			spec.setFetchLimit(0);
			spec.setPromptsAfterFetchLimit(false);
		}
		spec = ERXEOAccessUtilities.localizeFetchSpecification(ec, spec);
		String url = (String) model.connectionDictionary().objectForKey("URL");
		String lowerCaseURL = (url != null ? url.toLowerCase() : "");
		EOSQLExpression sqlExpr = sqlFactory.selectStatementForAttributes(attributes, false, spec, entity);
		String sql = sqlExpr.statement();
		if (end >= 0) {
			sql = limitExpressionForSQL(sqlExpr, spec, sql, start, end);
			sqlExpr.setStatement(sql);
		}
		return sqlExpr;
	}

	protected String limitExpressionForSQL(EOSQLExpression expression, EOFetchSpecification fetchSpecification, String sql, long start, long end) {
		throw new UnsupportedOperationException("There is no database-specific implementation for generating limit expressions.");
	}

	/**
	 * Returns the SQL expression for a regular expression query.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public String sqlForRegularExpressionQuery(String key, String value) {
		throw new UnsupportedOperationException("There is no database-specific implementation for generating regex expressions.");
	}

	/**
	 * Returns the number of rows the supplied EOFetchSpecification would return.
	 * 
	 * @param ec
	 *            the EOEditingContext
	 * @param spec
	 *            the EOFetchSpecification in question
	 * @return the number of rows
	 */
	public int rowCountForFetchSpecification(EOEditingContext ec, EOFetchSpecification spec) {
		int rowCount = -1;
		EOEntity entity = ERXEOAccessUtilities.entityNamed(ec, spec.entityName());
		EOModel model = entity.model();
		if (spec.fetchLimit() > 0 || spec.sortOrderings() != null) {
			spec = new EOFetchSpecification(spec.entityName(), spec.qualifier(), null);
		}

		EOSQLExpression sql = sqlExpressionForFetchSpecification(ec, spec, 0, -1);
		String statement = sql.statement();
		int index = statement.toLowerCase().indexOf(" from ");
		statement = "select count(*) " + statement.substring(index, statement.length());
		sql.setStatement(statement);
		NSArray result = ERXEOAccessUtilities.rawRowsForSQLExpression(ec, model.name(), sql);

		if (result.count() > 0) {
			NSDictionary dict = (NSDictionary) result.objectAtIndex(0);
			NSArray values = dict.allValues();
			if (values.count() > 0) {
				Object value = values.objectAtIndex(0);
				if (value instanceof Number) {
					return ((Number) value).intValue();
				}
				try {
					int c = Integer.parseInt(value.toString());
					rowCount = c;
				}
				catch (NumberFormatException e) {
					throw new IllegalStateException("sql " + sql + " returned a wrong result, could not convert " + value + " into an int!");
				}
			}
			else {
				throw new IllegalStateException("sql " + sql + " returned no result!");
			}
		}
		else {
			throw new IllegalStateException("sql " + sql + " returned no result!");
		}
		return rowCount;
	}

	/**
	 * Convenience method to get the next unique ID from a sequence.
	 * 
	 * @param ec
	 *            editing context
	 * @param modelName
	 *            name of the model which connects to the database that has the sequence in it
	 * @param sequenceName
	 *            name of the sequence
	 * @return next value in the sequence
	 */
	// ENHANCEME: Need a non-oracle specific way of doing this. Should poke
	// around at
	// the adaptor level and see if we can't find something better.
	public Number getNextValFromSequenceNamed(EOEditingContext ec, String modelName, String sequenceName) {
		String sqlString = "select " + sequenceName + ".nextVal from dual";
		NSArray array = EOUtilities.rawRowsForSQL(ec, modelName, sqlString, null);
		if (array.count() == 0) {
			throw new RuntimeException("Unable to generate value from sequence named: " + sequenceName + " in model: " + modelName);
		}
		NSDictionary dictionary = (NSDictionary) array.objectAtIndex(0);
		NSArray valuesArray = dictionary.allValues();
		return (Number) valuesArray.objectAtIndex(0);
	}

	/**
	 * Creates a where clause string " someKey IN ( someValue1,...)". Can migrate keyPaths.
	 */
	public String sqlWhereClauseStringForKey(EOSQLExpression e, String key, NSArray valueArray) {
		if (valueArray.count() == 0) {
			return "0=1";
		}
		StringBuffer sb = new StringBuffer();
		NSArray attributePath = ERXEOAccessUtilities.attributePathForKeyPath(e.entity(), key);
		EOAttribute attribute = (EOAttribute) attributePath.lastObject();
		if (attributePath.count() > 1) {
			sb.append(e.sqlStringForAttributePath(attributePath));
		}
		else {
			sb.append(e.sqlStringForAttribute(attribute));
		}
		sb.append(" IN ");
		sb.append("(");
		for (int i = 0; i < valueArray.count(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			Object value = valueArray.objectAtIndex(i);
			// AK : crude hack for queries with number constants.
			// Apparently EOAttribute.adaptorValueByConvertingAttributeValue() doesn't actually return a suitable value
			if (value instanceof ERXConstant.NumberConstant) {
				value = new Long(((Number) value).longValue());
			}
			else {
				value = formatValueForAttribute(e, value, attribute, key);
			}
			sb.append(value);
		}
		sb.append(")");
		return sb.toString();
	}

	protected String formatValueForAttribute(EOSQLExpression expression, Object value, EOAttribute attribute, String key) {
		return expression.sqlStringForValue(value, key);
	}

	/**
	 * Splits semicolon-separate sql statements into an array of strings
	 * 
	 * @param sql
	 *            a multi-line sql statement
	 * @return an array of sql statements
	 */
	public NSArray splitSQLStatements(String sql) {
		NSMutableArray statements = new NSMutableArray();
		if (sql != null) {
			StringBuffer statementBuffer = new StringBuffer();
			int length = sql.length();
			boolean inQuotes = false;
			for (int i = 0; i < length; i++) {
				char ch = sql.charAt(i);
				if (ch == '\r' || ch == '\n') {
					// ignore
				}
				// MS: Should this use commandSeparatorString?
				else if (!inQuotes && ch == ';') {
					String statement = statementBuffer.toString().trim();
					if (statement.length() > 0) {
						statements.addObject(statement);
					}
					statementBuffer.setLength(0);
				}
				else {
					if (ch == '\'') {
						inQuotes = !inQuotes;
					}
					statementBuffer.append(ch);
				}
			}
			String statement = statementBuffer.toString().trim();
			if (statement.length() > 0) {
				statements.addObject(statement);
			}
		}
		return statements;
	}

	/**
	 * Splits the SQL statements from the given input stream
	 * 
	 * @param is
	 *            the input stream to read from
	 * @return an array of SQL statements
	 * @throws IOException
	 *             if there is a problem reading the stream
	 */
	public NSArray splitSQLStatementsFromInputStream(InputStream is) throws IOException {
		return splitSQLStatements(ERXStringUtilities.stringFromInputStream(is));
	}

	/**
	 * Splits the SQL statements from the given file.
	 * 
	 * @param f
	 *            the file to read from
	 * @return an array of SQL statements
	 * @throws IOException
	 *             if there is a problem reading the stream
	 */
	public NSArray splitSQLStatementsFromFile(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		try {
			BufferedInputStream bis = new BufferedInputStream(fis);
			return splitSQLStatementsFromInputStream(bis);
		}
		finally {
			fis.close();
		}
	}

	protected String commandSeparatorString() {
		String lineSeparator = System.getProperty("line.separator");
		return ";" + lineSeparator;
	}

	public static ERXSQLHelper newSQLHelper(EOSQLExpression expression) {
		// This is REALLY hacky.
		String className = expression.getClass().getName();
		int dotIndex = className.lastIndexOf('$');
		if (dotIndex == -1) {
			dotIndex = className.lastIndexOf('.');
		}
		int expressionIndex = className.lastIndexOf("Expression");
		if (expressionIndex == -1) {
			throw new RuntimeException("Failed to create sql helper for expression " + expression + ".");
		}
		String databaseProductName = className.substring(dotIndex + 1, expressionIndex);
		return ERXSQLHelper.newSQLHelper(databaseProductName);
	}

	public static ERXSQLHelper newSQLHelper(EOEditingContext ec, String modelName) {
		ec.lock();
		try {
			EODatabaseContext databaseContext = EOUtilities.databaseContextForModelNamed(ec, modelName);
			return ERXSQLHelper.newSQLHelper(databaseContext);
		}
		finally {
			ec.unlock();
		}
	}

	public static ERXSQLHelper newSQLHelper(EODatabaseContext databaseContext) {
		JDBCAdaptor adaptor = (JDBCAdaptor) databaseContext.database().adaptor();
		return ERXSQLHelper.newSQLHelper(adaptor);
	}

	public static ERXSQLHelper newSQLHelper(EODatabaseChannel databaseChannel) {
		JDBCAdaptor adaptor = (JDBCAdaptor) databaseChannel.adaptorChannel().adaptorContext().adaptor();
		return ERXSQLHelper.newSQLHelper(adaptor);
	}

	public static ERXSQLHelper newSQLHelper(EOAdaptorChannel adaptorChannel) {
		JDBCAdaptor adaptor = (JDBCAdaptor) adaptorChannel.adaptorContext().adaptor();
		return ERXSQLHelper.newSQLHelper(adaptor);
	}

	public static ERXSQLHelper newSQLHelper(JDBCAdaptor adaptor) {
		JDBCPlugIn plugin = adaptor.plugIn();
		return ERXSQLHelper.newSQLHelper(plugin);
	}

	public static ERXSQLHelper newSQLHelper(JDBCPlugIn plugin) {
		String databaseProductName = plugin.databaseProductName();
		return ERXSQLHelper.newSQLHelper(databaseProductName);
	}

	public static ERXSQLHelper newSQLHelper(String databaseProductName) {
		synchronized (_sqlHelperMap) {
			ERXSQLHelper sqlHelper = (ERXSQLHelper) _sqlHelperMap.get(databaseProductName);
			if (sqlHelper == null) {
				try {
					String sqlHelperClassName = ERXProperties.stringForKey(databaseProductName + ".SQLHelper");
					if (sqlHelperClassName == null) {
						if (databaseProductName.equalsIgnoreCase("frontbase")) {
							sqlHelper = new FrontBaseSQLHelper();
						}
						else if (databaseProductName.equalsIgnoreCase("mysql")) {
							sqlHelper = new MySQLSQLHelper();
						}
						else if (databaseProductName.equalsIgnoreCase("oracle")) {
							sqlHelper = new OracleSQLHelper();
						}
						else if (databaseProductName.equalsIgnoreCase("postgresql")) {
							sqlHelper = new PostgresqlSQLHelper();
						}
						else if (databaseProductName.equalsIgnoreCase("openbase")) {
							sqlHelper = new OpenBaseSQLHelper();
						}
						else {
							try {
								sqlHelper = (ERXSQLHelper) Class.forName(ERXSQLHelper.class.getName() + "$" + databaseProductName + "SQLHelper").newInstance();
							}
							catch (ClassNotFoundException e) {
								sqlHelper = new ERXSQLHelper();
							}
						}
					}
					else {
						sqlHelper = (ERXSQLHelper) Class.forName(sqlHelperClassName).newInstance();
					}
					_sqlHelperMap.put(databaseProductName, sqlHelper);
				}
				catch (Exception e) {
					throw new NSForwardException(e, "Failed to create sql helper for '" + databaseProductName + "'.");
				}
			}
			return sqlHelper;
		}
	}

	public static class EROracleSQLHelper extends ERXSQLHelper.OracleSQLHelper {
	}

	public static class OracleSQLHelper extends ERXSQLHelper {
		/**
		 * oracle 9 has a maximum length of 30 characters for table names, column names and constraint names Foreign key
		 * constraint names are defined like this from the plugin:<br/><br/>
		 * 
		 * TABLENAME_FOEREIGNKEYNAME_FK <br/><br/>
		 * 
		 * The whole statement looks like this:<br/><br/>
		 * 
		 * ALTER TABLE [TABLENAME] ADD CONSTRAINT [CONSTRAINTNAME] FOREIGN KEY ([FK]) REFERENCES [DESTINATION_TABLE]
		 * ([PK]) DEFERRABLE INITIALLY DEFERRED
		 * 
		 * THIS means that the tablename and the columnname together cannot be longer than 26 characters.<br/><br/>
		 * 
		 * This method checks each foreign key constraint name and if it is longer than 30 characters its replaced with
		 * a unique name.
		 * 
		 * @see createSchemaSQLForEntitiesInModelWithNameAndOptions
		 */
		public String createSchemaSQLForEntitiesInModelWithNameAndOptions(NSArray entities, String modelName, NSDictionary optionsCreate) {
			String oldConstraintName = null;
			int i = 0;
			String s = super.createSchemaSQLForEntitiesInModelWithNameAndOptions(entities, modelName, optionsCreate);
			NSArray a = NSArray.componentsSeparatedByString(s, "/");
			StringBuffer buf = new StringBuffer(s.length());
			Pattern pattern = Pattern.compile(".*ALTER TABLE .* ADD CONSTRAINT (.*) FOREIGN KEY .* REFERENCES .* \\(.*\\) DEFERRABLE INITIALLY DEFERRED.*");
			Pattern pattern2 = Pattern.compile("(.*ALTER TABLE .* ADD CONSTRAINT ).*( FOREIGN KEY .* REFERENCES .* \\(.*\\) DEFERRABLE INITIALLY DEFERRED.*)");
			String lineSeparator = System.getProperty("line.separator");

			for (Enumeration e = a.objectEnumerator(); e.hasMoreElements();) {
				String statementLine = (String) e.nextElement();
				NSArray b = NSArray.componentsSeparatedByString(statementLine, lineSeparator);
				for (Enumeration e1 = b.objectEnumerator(); e1.hasMoreElements();) {
					String statement = (String) e1.nextElement();
					if (!pattern.matcher(statement).matches()) {
						buf.append(statement);
						buf.append(lineSeparator);
						continue;
					}

					String constraintName = pattern.matcher(statement).replaceAll("$1");
					if (constraintName.length() <= 30) {
						buf.append(statement);
						buf.append(lineSeparator);
						continue;
					}

					constraintName = "fk" + System.currentTimeMillis() + new NSTimestamp().getNanos();
					String newConstraintName = i == 0 ? constraintName : constraintName + "_" + i;

					if (oldConstraintName == null) {
						oldConstraintName = constraintName;
					}
					else if (oldConstraintName.equals(newConstraintName)) {
						constraintName += "_" + ++i;
					}
					else {
						i = 0;
					}
					oldConstraintName = constraintName;

					String newConstraint = pattern2.matcher(statement).replaceAll("$1" + constraintName + "$2");
					buf.append(newConstraint);
					buf.append(lineSeparator);
				}
				if (e.hasMoreElements()) {
					buf.append("/");
				}
			}
			System.out.println("finished!");
			return buf.toString();
		}

		protected String limitExpressionForSQL(EOSQLExpression expression, EOFetchSpecification fetchSpecification, String sql, long start, long end) {
			String limitSQL;
			/*
			 * Oracle can make you puke... These are grabbed from tips all over the net and I can't test them as it
			 * doesn't even install on OSX. Pick your poison.
			 */
			int debug = ERXProperties.intForKeyWithDefault("OracleBatchMode", 3);
			if (debug == 1) {
				// this only works for the first page
				limitSQL = "select * from (" + sql + ") where rownum between " + (start + 1) + " and " + (end + 1);
			}
			else if (debug == 2) {
				// this doesn't work at all when have have *no* order by
				limitSQL = "select * from (" + "select " + expression.listString() + ", row_number() over (" + expression.orderByString() + ") as eo_rownum from (" + sql + ")) where eo_rownum between " + (start + 1) + " and " + (end + 1);
			}
			else if (debug == 3) {
				// this works, but breaks with horizontal inheritance
				limitSQL = "select * from (" + "select " + expression.listString().replaceAll("[Tt]\\d\\.", "") + ", rownum eo_rownum from (" + sql + ")) where eo_rownum between " + (start + 1) + " and " + (end + 1);
			}
			else {
				// this might work, too, but only if we have an ORDER BY
				limitSQL = "select * from (" + "select " + (fetchSpecification.usesDistinct() ? " distinct " : "") + expression.listString() + ", row_number() over (" + expression.orderByString() + ") eo_rownum" + " from " + expression.joinClauseString() + " where " + expression.whereClauseString() + ") where eo_rownum between " + (start + 1) + " and " + (end + 1);
			}
			return limitSQL;
		}

		protected String commandSeparatorString() {
			String lineSeparator = System.getProperty("line.separator");
			String commandSeparator = lineSeparator + "/" + lineSeparator;
			return commandSeparator;
		}

		public String createIndexSQLForEntities(NSArray entities, NSArray externalTypesToIgnore) {
			NSMutableArray oracleExternalTypesToIgnore = new NSMutableArray();
			if (externalTypesToIgnore != null) {
				oracleExternalTypesToIgnore.addObjectsFromArray(externalTypesToIgnore);
			}
			oracleExternalTypesToIgnore.addObject("BLOB");
			oracleExternalTypesToIgnore.addObject("CLOB");
			return super.createIndexSQLForEntities(entities, oracleExternalTypesToIgnore);

		}

		public String sqlForRegularExpressionQuery(String key, String value) {
			return "REGEXP_LIKE(" + key + ", " + value + ")";
		}
	}

	public static class OpenBaseSQLHelper extends ERXSQLHelper {
		protected String limitExpressionForSQL(EOSQLExpression expression, EOFetchSpecification fetchSpecification, String sql, long start, long end) {
			// Openbase support for limiting result set
			return sql + " return results " + start + " to " + end;
		}
	}

	public static class FrontBaseSQLHelper extends ERXSQLHelper {
		public boolean shouldExecute(String sql) {
			boolean shouldExecute = true;
			if (sql.startsWith("SET TRANSACTION ISOLATION LEVEL")) {
				shouldExecute = false;
			}
			else if (sql.startsWith("COMMIT")) {
				// shouldExecute = false;
			}
			return shouldExecute;
		}

		protected String limitExpressionForSQL(EOSQLExpression expression, EOFetchSpecification fetchSpecification, String sql, long start, long end) {
			// add TOP(start, (end - start)) after the SELECT word
			int index = sql.indexOf("select");
			if (index == -1) {
				index = sql.indexOf("SELECT");
			}
			index += 6;
			String limitSQL = sql.substring(0, index) + " TOP(" + start + ", " + (end - start) + ")" + sql.substring(index + 1, sql.length());
			return limitSQL;
		}
	}

	public static class MySQLSQLHelper extends ERXSQLHelper {
		protected String limitExpressionForSQL(EOSQLExpression expression, EOFetchSpecification fetchSpecification, String sql, long start, long end) {
			return sql + " LIMIT " + start + ", " + (end - start);
		}

		public String sqlForRegularExpressionQuery(String key, String value) {
			return key + " REGEXP " + value + "";
		}
	}

	public static class PostgresqlSQLHelper extends ERXSQLHelper {
		protected String formatValueForAttribute(EOSQLExpression expression, Object value, EOAttribute attribute, String key) {
			// The Postgres Expression has a problem using bind variables so we have to get the formatted
			// SQL string for a value instead. All Apple provided plugins must use the bind variables
			// however. Frontbase can go either way
			// MS: is expression always instanceof PostgresExpression for postgres?
			// boolean isPostgres = e.getClass().getName().equals("com.webobjects.jdbcadaptor.PostgresqlExpression");
			return expression.formatValueForAttribute(value, attribute);
		}

		protected String limitExpressionForSQL(EOSQLExpression expression, EOFetchSpecification fetchSpecification, String sql, long start, long end) {
			return sql + " LIMIT " + (end - start) + " OFFSET " + start;
		}

		public String sqlForRegularExpressionQuery(String key, String value) {
			return key + " ~* " + value + "";
		}
	}
}
