package com.webobjects.jdbcadaptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOSynchronizationFactory;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSBundle;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSLog;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSPropertyListSerialization;

/**
 * WO runtime plugin with support for Postgresql.
 *
 * @author ak
 * @author giorgio_v
 */
public class PostgresqlPlugIn extends JDBCPlugIn {
  private static final String QUERY_STRING_USE_BUNDLED_JDBC_INFO = "useBundledJdbcInfo";
  
  static {
      setPlugInNameForSubprotocol(PostgresqlPlugIn.class.getName(), "postgresql");
  }
    
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
   * <P>WebObjects 5.4's version of JDBCAdaptor will use this
   * in order to assemble the name of the prototype to use when
   * it loads models.</P>
   * @return the name of the plugin.
   */
  public String name() {
    return "Postgresql";
  }

  /**
   * <P>This method returns true if the connection URL for the
   * database has a special flag on it which indicates to the
   * system that the jdbcInfo which has been bundled into the
   * plugin is acceptable to use in place of actually going to
   * the database and getting it.
   */
  protected boolean shouldUseBundledJdbcInfo() {
    boolean shouldUseBundledJdbcInfo = false;
    String url = connectionURL();
    if (url != null) {
      shouldUseBundledJdbcInfo = url.toLowerCase().matches(".*(\\?|\\?.*&)" + PostgresqlPlugIn.QUERY_STRING_USE_BUNDLED_JDBC_INFO.toLowerCase() + "=(true|yes)(\\&|$)");
    }
    return shouldUseBundledJdbcInfo;
  }

  /**
   * <P>This is usually extracted from the the database using
   * JDBC, but this is really inconvenient for users who are
   * trying to generate SQL at some.  A specific version of the
   * data has been written into the property list of the
   * framework and this can be used as a hard-coded equivalent.
   * </P> 
   */
  public NSDictionary jdbcInfo() {
    // you can swap this code out to write the property list out in order
    // to get a fresh copy of the JDBCInfo.plist.
//    try {
//      String jdbcInfoS = NSPropertyListSerialization.stringFromPropertyList(super.jdbcInfo());
//      FileOutputStream fos = new FileOutputStream("/tmp/JDBCInfo.plist");
//      fos.write(jdbcInfoS.getBytes());
//      fos.close();
//    }
//    catch(Exception e) {
//      throw new IllegalStateException("problem writing JDBCInfo.plist",e);
//    }

    NSDictionary jdbcInfo;
    // have a look at the JDBC connection URL to see if the flag has been set to
    // specify that the hard-coded jdbcInfo information should be used.
    if(shouldUseBundledJdbcInfo()) {
      if(NSLog.debugLoggingAllowedForLevel(NSLog.DebugLevelDetailed)) {
        NSLog.debug.appendln("Loading jdbcInfo from JDBCInfo.plist as opposed to using the JDBCPlugIn default implementation.");
      }
      
      InputStream jdbcInfoStream = NSBundle.bundleForClass(getClass()).inputStreamForResourcePath("JDBCInfo.plist");
      if (jdbcInfoStream == null) {
        throw new IllegalStateException("Unable to find 'JDBCInfo.plist' in this plugin jar.");
      }

      try {
        jdbcInfo = (NSDictionary) NSPropertyListSerialization.propertyListFromData(new NSData(jdbcInfoStream, 2048), "US-ASCII");
      }
      catch (IOException e) {
        throw new RuntimeException("Failed to load 'JDBCInfo.plist' from this plugin jar.", e);
      }
    }
    else {
      jdbcInfo = super.jdbcInfo();
    }
    return jdbcInfo;
  }

  /**
   * Returns a "pure java" synchronization factory.
   * Useful for testing purposes.
   */
  public EOSynchronizationFactory createSynchronizationFactory() {
    try {
      return new PostgresqlSynchronizationFactory(adaptor());
    }
    catch (Exception e) {
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
   * @param channel open JDBCChannel
   * @return NSArray of NSDictionary where each dictionary corresponds to a unique  primary key value
   */
  public NSArray newPrimaryKeys(int count, EOEntity entity, JDBCChannel channel) {
    if (isPrimaryKeyGenerationNotSupported(entity)) {
      return null;
    }
    
    EOAttribute attribute = (EOAttribute) entity.primaryKeyAttributes().lastObject();
    String attrName = attribute.name();
    boolean isIntType = "i".equals(attribute.valueType());

    NSMutableArray results = new NSMutableArray(count);
    String sequenceName = _sequenceNameForEntity(entity);
    PostgresqlExpression expression = new PostgresqlExpression(entity);
    
    // MS: The original implementation of this did something like select setval('seq', nextval('seq') + count)
    // which apparently is not an atomic operation, which causes terrible problems under load with multiple
    // instances.  The new implementation does batch requests for keys.
    int keysPerBatch = 20;
    boolean succeeded = false;
    for (int tries = 0; !succeeded && tries < 2; tries++) {
      while (results.count() < count) {
        try {
          StringBuffer sql = new StringBuffer();
          sql.append("SELECT ");
          for (int keyBatchNum = Math.min(keysPerBatch, count - results.count()) - 1; keyBatchNum >= 0; keyBatchNum --) {
            sql.append("NEXTVAL('" + sequenceName + "') AS KEY" + keyBatchNum);
            if (keyBatchNum > 0) {
              sql.append(", ");
            }
          }
          expression.setStatement(sql.toString());
          channel.evaluateExpression(expression);
          try {
            NSDictionary row;
            while ((row = channel.fetchRow()) != null) {
              Enumeration pksEnum = row.allValues().objectEnumerator();
              while (pksEnum.hasMoreElements()) {
                Number pkObj = (Number)pksEnum.nextElement();
                Number pk;
                if (isIntType) {
                  pk = new Integer(pkObj.intValue());
                }
                else {
                  pk = new Long(pkObj.longValue());
                }
                results.addObject(new NSDictionary(pk, attrName));
              }            
            }
          }
          finally {
            channel.cancelFetch();
          }
          succeeded = true;
        }
        catch (JDBCAdaptorException ex) {
          //timc 2006-11-06 Check if sequence name contains schema name
          int dotIndex = sequenceName.indexOf(".");
          if (dotIndex == -1) {
            expression.setStatement("select count(*) from pg_class where relname = '" + sequenceName.toLowerCase() + "' and relkind = 'S'");
          }
          else {
            String schemaName = sequenceName.substring(0, dotIndex);
            String sequenceNameOnly = sequenceName.toLowerCase().substring(dotIndex + 1);
            expression.setStatement("select count(c.*) from pg_catalog.pg_class c, pg_catalog.pg_namespace n where c.relnamespace=n.oid AND c.relkind = 'S' AND c.relname='" + sequenceNameOnly + "' AND n.nspname='" + schemaName + "'");
          }
          channel.evaluateExpression(expression);
          NSDictionary row;
          try {
            row = channel.fetchRow();
          }
          finally {
            channel.cancelFetch();
          }
          // timc 2006-11-06 row.objectForKey("COUNT") returns BigDecimal not Long
          //if( new Long( 0 ).equals( row.objectForKey( "COUNT" ) ) ) {
          Number numCount = (Number) row.objectForKey("COUNT");
          if (numCount != null && numCount.longValue() == 0L) {
            EOSynchronizationFactory f = createSynchronizationFactory();
            NSArray statements = f.primaryKeySupportStatementsForEntityGroup(new NSArray(entity));
            int stmCount = statements.count();
            for (int i = 0; i < stmCount; i++) {
              channel.evaluateExpression((EOSQLExpression) statements.objectAtIndex(i));
            }
          }
          else if (numCount == null) {
            throw new IllegalStateException("Couldn't call sequence " + sequenceName + " and couldn't get sequence information from pg_class: " + ex);
          }
          else {
            throw new IllegalStateException("Caught exception, but sequence did already exist: " + ex);
          }
        }
      }
    }
    
    if (results.count() != count) {
      throw new IllegalStateException("Unable to generate primary keys from the sequence for " + entity + ".");
    }
    
    return results;
  }

  /**
   * Utility method that returns the name of the sequence associated
   * with <code>entity</code>
   *
   * @param entity    the entity
   * @return  the name of the sequence
   */
  protected static String _sequenceNameForEntity(EOEntity entity) {
    /* timc 2006-11-06 
     * This used to say ... + "_SEQ";
     * _SEQ would get converted to _seq because postgresql converts all unquoted identifiers to lower case.
    * In the future we may use enableIdentifierQuoting for sequence names so we need to set the correct case here in the first place
     */
    return entity.primaryKeyRootName() + "_seq";
  }

  /**
   * Checks whether primary key generation can be supported for <code>entity</code>
   *
   * @param entity    the entity to be checked
   * @return  yes/no
   */
  private boolean isPrimaryKeyGenerationNotSupported(EOEntity entity) {
    return entity.primaryKeyAttributes().count() > 1 || ((EOAttribute) entity.primaryKeyAttributes().lastObject()).adaptorValueType() != EOAttribute.AdaptorNumberType;
  }

}
