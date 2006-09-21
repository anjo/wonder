package com.webobjects.jdbcadaptor;

import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOSynchronizationFactory;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSMutableArray;

/**
 * WO runtime plugin with support for Postgresql.
 *
 * @author ak
 * @author giorgio_v
 */
public class PostgresqlPlugIn extends JDBCPlugIn {
    
    /**
     * Designated constructor.
     */    
    public PostgresqlPlugIn(JDBCAdaptor adaptor) {
        super(adaptor);
    }
    
    /**
     * Name of the driver.
     */    
    public String defaultDriverName() {
        return "org.postgresql.Driver";
    }

    /**
     * Name of the database.
     */
    public String databaseProductName() {
        return "Postgresql";
    }
    
    /**
     * Returns a "pure java" synchronization factory.
     * Useful for testing purposes.
     */
    public EOSynchronizationFactory createSynchronizationFactory() {
        try {
             return new PostgresqlSynchronizationFactory(adaptor());
        } catch ( Exception e ) {
            throw new NSForwardException(e, "Couldn't create synchronization factory");
        }
    }

    /**                                                                                                                                                         
     * Expression class to create. We have custom code, so we need our own class.                                                                               
     */
    public Class defaultExpressionClass() {
        return PostgresqlExpression.class;
    }
    
    /** 
     * Overrides the parent implementation to provide a more efficient mechanism for generating primary keys,
     * while generating the primary key support on the fly.
     *
     * @param count the batch size
     * @param entity the entity requesting primary keys
     * @param n open JDBCChannel
     * @return An NSArray of NSDictionary where each dictionary corresponds to a unique  primary key value
     */
    public NSArray newPrimaryKeys (int count, EOEntity entity, JDBCChannel channel) {
        if( isPrimaryKeyGenerationNotSupported( entity ) )
            return null;
        NSMutableArray results = new NSMutableArray(count);
        String sequenceName = sequenceNameForEntity( entity );
        PostgresqlExpression expression = new PostgresqlExpression(entity);
        for(int tries = 0; tries < 2; tries ++) {
        	try {
        		expression.setStatement("SELECT SETVAL( '"+ sequenceName +"', NEXTVAL('"+ sequenceName +"') + "+ (count-1) +" )"); //, false
        		channel.evaluateExpression( expression );
        		NSDictionary row = channel.fetchRow();
        		channel.cancelFetch();
        		long maxValue = ((Number) row.objectForKey("SETVAL")).longValue();
        		EOAttribute attribute =  (EOAttribute) entity.primaryKeyAttributes().lastObject();
        		String attrName = attribute.name();
        		boolean isIntType = "i".equals(attribute.valueType());
        		for( int i = 0; i < count; i++ ) {
        			long newPK = maxValue - count + 1 + i;
        			Number value;
        			if(isIntType) { 
        				value = new Integer((int) newPK);
        			} else {
        				value = new Long(newPK);
        			}
        			results.addObject(new NSDictionary( value, attrName));
        		}
        		return results;
        	} catch(JDBCAdaptorException ex) {
        		expression.setStatement( "select count(*) from pg_class where relname = '"+ sequenceName.toLowerCase() +"' and relkind = 'S'" );
        		channel.evaluateExpression( expression );
        		NSDictionary row = channel.fetchRow();
        		channel.cancelFetch();
        		if( new Long( 0 ).equals( row.objectForKey( "COUNT" ) ) ) {
        			EOSynchronizationFactory f = createSynchronizationFactory();
        			NSArray statements = f.primaryKeySupportStatementsForEntityGroup( new NSArray( entity ) );
        			int stmCount = statements.count();
        			for( int i = 0; i < stmCount; i++ ) {
        				channel.evaluateExpression( (EOSQLExpression) statements.objectAtIndex(i) );
        			}            
        		} else {
        			throw new IllegalStateException("Caught exception, but sequence did already exist: " + ex);
        		}
        	}
        }
		throw new IllegalStateException("Could not create or read sequence " + sequenceName);
    }
    
    /**
     * Utility method that returns the name of the sequence associated
     * with <code>entity</code>
     *
     * @param entity    the entity
     * @return  the name of the sequence
     */
    protected static String sequenceNameForEntity(EOEntity entity) {
        return entity.primaryKeyRootName() + "_SEQ";
    }
    
    /**
     * Checks whether primary key generation can be supported for <code>entity</code>
     *
     * @param entity    the entity to be checked
     * @return  yes/no
     */
    private boolean isPrimaryKeyGenerationNotSupported( EOEntity entity ) {
        return entity.primaryKeyAttributes().count() > 1 || 
        ( (EOAttribute) entity.primaryKeyAttributes().lastObject() )
        .adaptorValueType() != EOAttribute.AdaptorNumberType;
    }
    
}
