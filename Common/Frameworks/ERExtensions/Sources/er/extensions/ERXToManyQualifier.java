/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package er.extensions;

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

/**
 * Optimized toMany qualifier, much, much better SQL than the Apple provided qualifier.
 * Really nice when you want to find all the eos that have say five of the
 * ten eos in their toMany relationship. This qualifier will always only
 * generate three joins no matter how many eos you are  trying to find. Example usage:
 * <pre><code>
 * NSArray employees; // given
 * // Find all of the departments that have all of those employees
 * ERXToManyQualifier q = new ERXToManyQualifier("toEmployees", employees);
 * EOFetchSpecification fs = new EOFetchSpecification("Department", q, null);
 * NSArray departments = ec.objectsWithFetchSpecification(fs);
 * </code></pre>
 * If you want to find say departments that have 5 or more of the given
 * employees (imagine you have a list of 10 or so), then you could
 * construct the qualifier like: <br>
 * <code> ERXToManyQualifier q = new ERXToManyQualifier("toEmployees", employees, 5);</code><br>
 * or to find any department that has at least one of the given employees<br>
 * <code> ERXToManyQualifier q = new ERXToManyQualifier("toEmployees", employees, 1);</code>
 */

public class ERXToManyQualifier extends EOQualifier implements Cloneable {
    /** register SQL generation support for the qualifier */
    static {
        EOQualifierSQLGeneration.Support.setSupportForClass(new ToManyQualifierSQLGenerationSupport(), ERXToManyQualifier.class);
    }

    /** logging support */
    public static final ERXLogger log = ERXLogger.getERXLogger(ERXToManyQualifier.class);

    /** holds the to many key */    
    private String _toManyKey;
    /** holds the array of elements */    
    private NSArray _elements;
    /** holds the min count to match against, defaults to 0 */
    private int _minCount = 0;

    public ERXToManyQualifier(String toManyKey, NSArray elements) {
        this(toManyKey,elements, 0);
    }

    public ERXToManyQualifier(String toManyKey, NSArray elements, int minCount) {
        _toManyKey=toManyKey;
        _elements=elements;
        _minCount = minCount;
    }
    
    public NSArray elements() {
        return _elements;
    }
    
    public String key() {
        return _toManyKey;
    }
    
    public int minCount() {
        return _minCount;
    }

    /**
     * Description of the qualfier.
     * @return description of the key and which elements it
     *		should contain.
     */
    public String toString() {
        return "<" +_toManyKey + " contains " + (_minCount > 0 ? " all " : " " + _minCount + " " ) + " of " + _elements + ">";
    }

    /**
     * Implementation of the Cloneable interface.
     * @return clone of the qualifier.
     */
    public Object clone() {
        return new ERXToManyQualifier(_toManyKey, _elements, _minCount);
    }

    /**
     * Adds SQL generation support. Note that the database needs to support
     * the IN operator.
     */
    public static class ToManyQualifierSQLGenerationSupport extends EOQualifierSQLGeneration.Support {

        /**
         * Public constructor
         */
        public ToManyQualifierSQLGenerationSupport() {
            super();
        }

        protected static void appendColumnForAttributeToStringBuffer(EOAttribute attribute, StringBuffer sb) {
            sb.append(attribute.entity().externalName());
            sb.append('.');
            sb.append(attribute.columnName());
        }

        public String sqlStringForSQLExpression(EOQualifier eoqualifier, EOSQLExpression e) {
            ERXToManyQualifier qualifier = (ERXToManyQualifier)eoqualifier;
            StringBuffer result=new StringBuffer();
            EOEntity targetEntity=e.entity();
            
            NSArray pKeys=ERXEOAccessUtilities.primaryKeysForObjects(qualifier.elements());

            String tableName=targetEntity.externalName();
            NSArray toManyKeys=NSArray.componentsSeparatedByString(qualifier.key(),".");
            EORelationship targetRelationship=null;
            for (int i=0; i<toManyKeys.count()-1;i++) {
                targetRelationship= targetEntity.relationshipNamed((String)toManyKeys.objectAtIndex(i));
                targetEntity=targetRelationship.destinationEntity();
            }
            targetRelationship=targetEntity.relationshipNamed((String)toManyKeys.lastObject());
            targetEntity=targetRelationship.destinationEntity();

            if (targetRelationship.joins()==null || targetRelationship.joins().count()==0) {
                // we have a flattened many to many
                String definitionKeyPath=targetRelationship.definition();                        
                NSArray definitionKeys=NSArray.componentsSeparatedByString(definitionKeyPath,".");
                EOEntity lastStopEntity=targetRelationship.entity();
                String lastStopPrimaryKeyName=(String)lastStopEntity.primaryKeyAttributeNames().objectAtIndex(0);
                EORelationship firstHopRelationship= lastStopEntity.relationshipNamed((String)definitionKeys.objectAtIndex(0));
                EOEntity endOfFirstHopEntity= firstHopRelationship.destinationEntity();
                EOJoin join=(EOJoin) firstHopRelationship.joins().objectAtIndex(0); // assumes 1 join
                EOAttribute sourceAttribute=join.sourceAttribute();
                EOAttribute targetAttribute=join.destinationAttribute();
                EORelationship secondHopRelationship=endOfFirstHopEntity.relationshipNamed((String)definitionKeys.objectAtIndex(1));
                join=(EOJoin) secondHopRelationship.joins().objectAtIndex(0); // assumes 1 join
                EOAttribute secondHopSourceAttribute=join.sourceAttribute();

                NSMutableArray lastStopPKeyPath=new NSMutableArray(toManyKeys);
                lastStopPKeyPath.removeLastObject();
                lastStopPKeyPath.addObject(firstHopRelationship.name());
                lastStopPKeyPath.addObject(targetAttribute.name());
                String firstHopRelationshipKeyPath=lastStopPKeyPath.componentsJoinedByString(".");
                result.append(e.sqlStringForAttributeNamed(firstHopRelationshipKeyPath));
                result.append(" IN ( SELECT ");

                result.append(lastStopEntity.externalName());
                result.append('.');
                result.append(((EOAttribute)lastStopEntity.primaryKeyAttributes().objectAtIndex(0)).columnName());

                result.append(" FROM ");

                result.append(lastStopEntity.externalName());
                result.append(',');

                lastStopPKeyPath.removeLastObject();
                String tableAliasForJoinTable=(String)e.aliasesByRelationshipPath().
                    objectForKey(lastStopPKeyPath.componentsJoinedByString("."));//"j"; //+random#
                result.append(endOfFirstHopEntity.externalName());
                result.append(' ');
                result.append(tableAliasForJoinTable);

                result.append(" WHERE ");

                appendColumnForAttributeToStringBuffer(sourceAttribute,result);
                result.append('=');
                result.append(e.sqlStringForAttributeNamed(firstHopRelationshipKeyPath));

                result.append(" AND ");
                
                result.append(tableAliasForJoinTable);
                result.append('.');
                result.append(secondHopSourceAttribute.columnName());
                
                result.append(" IN ("); 
                String pkName = (String)targetEntity.primaryKeyAttributeNames().lastObject();
                EOAttribute pk = (EOAttribute)targetEntity.primaryKeyAttributes().lastObject();
                for(int i = 0; i < pKeys.count(); i++) {
                    
                    Object key = pKeys.objectAtIndex(i);
                    String keyString = null;
                    if(key instanceof NSData) {
                        //ak: This is a fix for Postgres NSData PKs, note that you need the correct plugin for this to work
                        keyString = e.sqlStringForData((NSData)key);
                    } else {
                        keyString = key.toString();
                    }
                    result.append(keyString);
                    if(i < pKeys.count()-1) {
                        result.append(",");
                    }
                }

                result.append(") GROUP BY ");
                appendColumnForAttributeToStringBuffer(sourceAttribute,result);

                result.append(" HAVING COUNT(*)");
                if (qualifier.minCount() <= 0) {
                    result.append("=" + qualifier.elements().count());
                } else {
                    result.append(">=" + qualifier.minCount());                
                }
                result.append(" )");
            } else {
                throw new RuntimeException("not implemented!!");
            }
            return result.toString();
        }
        
        // ENHANCEME: This should support restrictive qualifiers on the root entity
        public EOQualifier schemaBasedQualifierWithRootEntity(EOQualifier eoqualifier, EOEntity eoentity) {
            return (EOQualifier)eoqualifier.clone();
        }

        public EOQualifier qualifierMigratedFromEntityRelationshipPath(EOQualifier eoqualifier, EOEntity eoentity, String relationshipPath) {
            ERXToManyQualifier qualifier=(ERXToManyQualifier)eoqualifier;
            String newPath =  EOQualifierSQLGeneration.Support._translateKeyAcrossRelationshipPath(qualifier.key(), relationshipPath, eoentity);
            return new ERXToManyQualifier(newPath, qualifier.elements(), qualifier.minCount());
        }
    }

    public EOQualifier qualifierWithBindings(NSDictionary arg0, boolean arg1) {
        throw new IllegalStateException(getClass().getName() + " doesn't support bindings");
     }

    /* (non-Javadoc)
     * @see com.webobjects.eocontrol.EOQualifier#validateKeysWithRootClassDescription(com.webobjects.eocontrol.EOClassDescription)
     */
    public void validateKeysWithRootClassDescription(EOClassDescription arg0) {
        // TODO Auto-generated method stub
        
    }

    public void addQualifierKeysToSet(NSMutableSet arg0) {
        throw new IllegalStateException(getClass().getName() + " doesn't support adding keys");
    }
}