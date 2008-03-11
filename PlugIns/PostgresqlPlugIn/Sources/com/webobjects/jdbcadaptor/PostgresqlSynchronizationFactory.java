package com.webobjects.jdbcadaptor;

import java.util.Iterator;

import com.webobjects.eoaccess.EOAdaptor;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOSchemaGeneration;
import com.webobjects.eoaccess.EOSchemaSynchronization;
import com.webobjects.eoaccess.EOSynchronizationFactory;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation._NSStringUtilities;

/**
 * A synchronization factory usable outside EOModeler
 * 
 * @author giorgio_v
 */
public class PostgresqlSynchronizationFactory extends EOSynchronizationFactory implements EOSchemaGeneration, EOSchemaSynchronization {
    public static final String USING_KEY = "USING";
    
    private Boolean _enableIdentifierQuoting;
  
    public PostgresqlSynchronizationFactory(EOAdaptor adaptor) {
        super(adaptor);
    }

    private boolean enableIdentifierQuoting() {
        if(_enableIdentifierQuoting == null) {
            _enableIdentifierQuoting = Boolean.getBoolean(PostgresqlExpression.class.getName() + ".enableIdentifierQuoting") ? Boolean.TRUE : Boolean.FALSE;
        }
        return _enableIdentifierQuoting.booleanValue();
    }

    protected String formatTableName(String name) {
        if (!enableIdentifierQuoting()) {
            return name;
        }
        return "\"" + name + "\"";
    }

    protected String formatColumnName(String name) {
        if (!enableIdentifierQuoting()) {
            return name;
        }
        return "\"" + name + "\"";
    }
    
    public NSArray _foreignKeyConstraintStatementsForEntityGroup(NSArray group) {
        if (group == null)
            return NSArray.EmptyArray;
        NSMutableArray result = new NSMutableArray();
        NSMutableSet generatedStatements = new NSMutableSet();
        int i = 0;
        for (int groupCount = group.count(); i < groupCount; i++) {
            EOEntity currentEntity = (EOEntity) group.objectAtIndex(i);
            if (currentEntity.externalName() != null) {
                NSArray relationships = currentEntity.relationships();
                int relCount = relationships.count();
                for (int j = 0; j < relCount; j++) {
                    EORelationship currentRelationship = ((EORelationship) relationships.objectAtIndex(j));
                    if (_shouldGenerateForeignKeyConstraints(currentRelationship)) {
                        NSArray statements = foreignKeyConstraintStatementsForRelationship(currentRelationship);
                        if (!generatedStatements.containsObject(statements.valueForKey("statement"))) {
                            result.addObjectsFromArray(statements);
                            generatedStatements.addObject(statements.valueForKey("statement"));
                        }
                    }
                }
            }
        }
        return result;
    }

    protected boolean _shouldGenerateForeignKeyConstraints(EORelationship rel) {
        EOEntity destinationEntity = rel.destinationEntity();
        return !rel.isFlattened() && destinationEntity.externalName() != null && rel.entity().model() == destinationEntity.model();
    }

    /**
     * <code>PostgresqlExpression</code> factory method.
     * 
     * @param entity
     *            the entity to which <code>PostgresqlExpression</code> is to
     *            be rooted
     * @param statement
     *            the SQL statement
     * @return a <code>PostgresqlExpression</code> rooted to
     *         <code>entity</code>
     */
    private static PostgresqlExpression createExpression(EOEntity entity, String statement) {
        PostgresqlExpression result = new PostgresqlExpression(entity);
        result.setStatement(statement);
        return result;
    }

    /**
     * Generates the PostgreSQL-specific SQL statements to drop the primary key
     * support.
     * 
     * @param entityGroup
     *            an array of <code>EOEntity</code> objects
     * @return the array of SQL statements
     */
    public NSArray dropPrimaryKeySupportStatementsForEntityGroup(NSArray entityGroup) {
        NSMutableSet sequenceNames = new NSMutableSet();
        NSMutableArray results = new NSMutableArray();
        int count = entityGroup.count();
        for (int i = 0; i < count; i++) {
            EOEntity entity = (EOEntity) entityGroup.objectAtIndex(i);
            String sequenceName = PostgresqlPlugIn.sequenceNameForEntity(entity);
            if (!sequenceNames.containsObject(sequenceName)) {
                sequenceNames.addObject(sequenceName);
                String sql = "DROP SEQUENCE " + sequenceName + " CASCADE";
                results.addObject(createExpression(entity, sql));
            }
        }
        return results;
    }

    /**
     * Generates the PostgreSQL-specific SQL statements to drop tables.
     * 
     * @param entityGroup
     *            an array of <code>EOEntity</code> objects
     * @return the array of SQL statements
     */
    public NSArray dropTableStatementsForEntityGroup(NSArray entityGroup) {
        NSMutableArray results = new NSMutableArray();
        int count = entityGroup.count();
        for (int i = 0; i < count; i++) {
            EOEntity entity = (EOEntity) entityGroup.objectAtIndex(i);
            // timc 2006-11-06 create result here so we can check for
            // enableIdentifierQuoting while building the statement
            PostgresqlExpression result = new PostgresqlExpression(entity);
            String tableName = result.sqlStringForSchemaObjectName(entity.externalName());
            if(entityUsesSeparateTable(entity)) {
                result.setStatement("DROP TABLE " + tableName + " CASCADE");
                results.addObject(result);
            }
        }
        return results;
    }

    /**
     * Generates the PostgreSQL-specific SQL statements to enforce the foreign
     * key constraints for <code>relationship</code>.
     * 
     * @param relationship
     *            the relationship, as represented by EOF
     * @return the array of SQL statements
     */
    public NSArray foreignKeyConstraintStatementsForRelationship(EORelationship relationship) {
        NSArray superResults;
        NSMutableArray results;
        int count;
        int i;
        EOSQLExpression expression;

        results = new NSMutableArray();
        superResults = super.foreignKeyConstraintStatementsForRelationship(relationship);

        count = superResults.count();
        for (i = 0; i < count; i++) {
            expression = (EOSQLExpression) superResults.objectAtIndex(i);
            String s = expression.statement();
            s = replaceStringByStringInString(") INITIALLY DEFERRED", ") DEFERRABLE INITIALLY DEFERRED", s);
            expression.setStatement(s);
            results.addObject(expression);
            // timc 2006-11-06 check for enableIdentifierQuoting
            String tableName = expression.sqlStringForSchemaObjectName(expression.entity().externalName());
            NSArray columnNames = ((NSArray) relationship.sourceAttributes().valueForKey("columnName"));
            StringBuffer sbColumnNames = new StringBuffer();
            for (int j = 0; j < columnNames.count(); j++) {
                sbColumnNames.append((j == 0 ? "" : ", ") + expression.sqlStringForSchemaObjectName((String) columnNames.objectAtIndex(j)));
            }
            String indexName = relationship.entity().externalName() + "_" + columnNames.componentsJoinedByString("_") + "_idx";
            results.addObject(createExpression(expression.entity(), "CREATE INDEX " + indexName + " ON " + tableName + "( " + sbColumnNames.toString() + " )"));
        }
        return results;
    }

    /**
     * Generates the PostgreSQL-specific SQL statements to enforce primary key
     * constraints.
     * 
     * @param entityGroup
     *            an array of <code>EOEntity</code> objects
     * @return the array of SQL statements
     */
    public NSArray primaryKeyConstraintStatementsForEntityGroup(NSArray entityGroup) {
        EOEntity entity;
        int count;
        int i;
        NSMutableArray results;
        NSArray priKeyAttributes;
        EOAttribute priKeyAttribute;
        int priKeyAttributeCount;
        int j;

        results = new NSMutableArray();
        count = entityGroup.count();
        for (i = 0; i < count; i++) {
            entity = (EOEntity) entityGroup.objectAtIndex(i);
            if (!entityUsesSeparateTable(entity))
                continue;
            // timc 2006-11-06 create result here so we can check for
            // enableIdentifierQuoting while building the statement
            PostgresqlExpression result = new PostgresqlExpression(entity);
            String constraintName = result.sqlStringForSchemaObjectName(entity.externalName() + "_pk");
            String tableName = result.sqlStringForSchemaObjectName(entity.externalName());

            StringBuffer statement = new StringBuffer("ALTER TABLE ");
            statement.append(tableName);
            statement.append(" ADD CONSTRAINT ");
            statement.append(constraintName);
            statement.append(" PRIMARY KEY (");
            priKeyAttributes = entity.primaryKeyAttributes();
            priKeyAttributeCount = priKeyAttributes.count();
            for (j = 0; j < priKeyAttributeCount; j++) {
                priKeyAttribute = (EOAttribute) priKeyAttributes.objectAtIndex(j);
                String attributeName = result.sqlStringForAttribute(priKeyAttribute);
                statement.append(attributeName);
                if (j < priKeyAttributeCount - 1) {
                    statement.append(", ");
                } else {
                    statement.append(")");
                }
            }
            result.setStatement(statement.toString());
            results.addObject(result);
        }
        return results;
    }
    
    /**
     * Returns true if Entity Modeler is running the operation on this model.
     * 
     * @param model the model to check
     * @return true if Entity Modeler is running
     */
    protected boolean isInEntityModeler(EOModel model) {
      boolean inEntityModeler = false;
      if (model != null) {
        NSDictionary userInfo = model.userInfo();
        NSDictionary entityModelerDict = (NSDictionary) userInfo.objectForKey("_EntityModeler");
        if (entityModelerDict != null) {
          Boolean inEntityModelerBoolean = (Boolean)entityModelerDict.objectForKey("inEntityModeler");
          if (inEntityModelerBoolean != null && inEntityModelerBoolean.booleanValue()) {
            inEntityModeler = inEntityModelerBoolean.booleanValue();
          }
        }
      }
      return inEntityModeler;
    }

    /**
     * Generates the PostgreSQL-specific SQL statements to create the primary
     * key support.
     * 
     * @param entityGroup
     *            an array of <code>EOEntity</code> objects
     * @return the array of SQL statements
     */
    public NSArray primaryKeySupportStatementsForEntityGroup(NSArray entityGroup) {
        EOEntity entity;
        int count;
        int i;
        NSMutableArray results;
        NSArray priKeyAttributes;
        EOAttribute priKeyAttribute;
        String sequenceName;

        NSMutableSet sequenceNames = new NSMutableSet();
        results = new NSMutableArray();
        count = entityGroup.count();
        for (i = 0; i < count; i++) {
            entity = (EOEntity) entityGroup.objectAtIndex(i);
            priKeyAttributes = entity.primaryKeyAttributes();
            if (priKeyAttributes.count() == 1) {
                priKeyAttribute = (EOAttribute) priKeyAttributes.objectAtIndex(0);
                String sql;

                sequenceName = PostgresqlPlugIn.sequenceNameForEntity(entity);
                if (!sequenceNames.containsObject(sequenceName)) {
                    sequenceNames.addObject(sequenceName);
                    // timc 2006-11-06 create result here so we can check for
                    // enableIdentifierQuoting while building the statement
                    PostgresqlExpression result = new PostgresqlExpression(entity);
                    String attributeName = result.sqlStringForAttribute(priKeyAttribute);
                    String tableName = result.sqlStringForSchemaObjectName(entity.externalName());

                    sql = "CREATE SEQUENCE " + sequenceName;
                    results.addObject(createExpression(entity, sql));

                    sql = "CREATE TEMP TABLE EOF_TMP_TABLE AS SELECT SETVAL('" + sequenceName + "', (SELECT MAX(" + attributeName + ") FROM " + tableName + "))";
                    results.addObject(createExpression(entity, sql));
                    
                    // In Entity Modeler, we want to skip over the drop statement
                    if (!isInEntityModeler(entity.model())) {
                      sql = "DROP TABLE EOF_TMP_TABLE";
                      results.addObject(createExpression(entity, sql));
                    }

                    sql = "ALTER TABLE " + tableName + " ALTER COLUMN " + attributeName + " SET DEFAULT nextval( '" + sequenceName + "' )";
                    results.addObject(createExpression(entity, sql));
                }
            }
        }
        return results;
    }

    public static boolean entityUsesSeparateTable(EOEntity entity) {
        if (entity.parentEntity() == null)
            return true;
        EOEntity parent = entity.parentEntity();
        while (parent != null) {
            if (!entity.externalName().equals(parent.externalName()))
                return true;
            entity = parent;
            parent = entity.parentEntity();
        }
        return false;
    }
    
    /**
     * Quote table name if necessary
     */
    public NSArray createTableStatementsForEntityGroup(NSArray entityGroup) {
		NSMutableSet columnNames = new NSMutableSet();
		StringBuffer aStatement = new StringBuffer(128);
		if (entityGroup != null && entityGroup.count() > 0) {
			EOSQLExpression sqlExpr = _expressionForEntity((EOEntity) entityGroup.objectAtIndex(0));
			for (Iterator entityIterator = entityGroup.iterator(); entityIterator.hasNext();) {
				EOEntity entity = (EOEntity) entityIterator.next();
				Iterator attributeIterator = entity.attributes().iterator();
				while (attributeIterator.hasNext()) {
					EOAttribute attribute = (EOAttribute) attributeIterator.next();
					String columnName = attribute.columnName();
					if (!attribute.isDerived() && !attribute.isFlattened() && columnName != null && columnName.length() > 0 && !columnNames.contains(columnName)) {
						sqlExpr.appendItemToListString(_columnCreationClauseForAttribute(attribute), aStatement);
						columnNames.addObject(columnName);
					}
				}
			}
			return new NSArray(_expressionForString((new StringBuilder()).append("CREATE TABLE ").append(this.formatTableName(((EOEntity) entityGroup.objectAtIndex(0)).externalName())).append(" (").append(aStatement.toString()).append(")").toString()));
		} else {
			return new NSArray();
		}
	}
    
    /**
	 * Replaces a given string by another string in a string.
	 * 
	 * @param old
	 *            string to be replaced
	 * @param newString
	 *            to be inserted
	 * @param buffer
	 *            string to have the replacement done on it
	 * @return string after having all of the replacement done.
	 */
    public static String replaceStringByStringInString(String old, String newString, String buffer) {
        int begin, end;
        int oldLength = old.length();
        int length = buffer.length();
        StringBuffer convertedString = new StringBuffer(length + 100);

        begin = 0;
        while (begin < length) {
            end = buffer.indexOf(old, begin);
            if (end == -1) {
                convertedString.append(buffer.substring(begin));
                break;
            }
            if (end == 0)
                convertedString.append(newString);
            else {
                convertedString.append(buffer.substring(begin, end));
                convertedString.append(newString);
            }
            begin = end + oldLength;
        }
        return convertedString.toString();
    }
    
    // I blame statementstToConvertColumnType for not taking a damn EOAttribute for
    // having to steal this from EOSQLExpression
    public String columnTypeStringForAttribute(EOAttribute attribute) {
      if (attribute.precision() != 0) {
        String precision = String.valueOf(attribute.precision());
        String scale = String.valueOf(attribute.scale());
        return _NSStringUtilities.concat(attribute.externalType(), "(", precision, ",", scale, ")");
      }
      if (attribute.width() != 0) {
        String width = String.valueOf(attribute.width());
        return _NSStringUtilities.concat(attribute.externalType(), "(", width, ")");
      }
      else {
        return attribute.externalType();
      }
    }

    @Override
    public NSArray statementsToConvertColumnType(String columnName, String tableName, ColumnTypes oldType, ColumnTypes newType, NSDictionary options) {
      EOAttribute attr = new EOAttribute();
      attr.setName(columnName);
      attr.setColumnName(columnName);
      attr.setExternalType(newType.name());
      attr.setScale(newType.scale());
      attr.setPrecision(newType.precision());
      attr.setWidth(newType.width());

      String usingClause = "";
      String columnTypeString = columnTypeStringForAttribute(attr);
      if (options != null) {
        String usingExpression = (String) options.objectForKey(PostgresqlSynchronizationFactory.USING_KEY);
        if (usingExpression != null) {
          usingClause = " USING " + usingExpression;
        }
      }
      NSArray statements = new NSArray(_expressionForString("alter column " + formatTableName(tableName) + "." + formatColumnName(columnName) + " type " + columnTypeString + usingClause));
      return statements;
    }

    @Override
    public NSArray statementsToInsertColumnForAttribute(EOAttribute attribute, NSDictionary options) {
      String clause = _columnCreationClauseForAttribute(attribute);
      return new NSArray(_expressionForString("alter table " + attribute.entity().externalName() + " add " + clause));
    }
}
