package er.directtoweb.components.relationships;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;

import er.directtoweb.components.ERDCustomQueryComponent;
import er.extensions.eof.qualifiers.ERXPrimaryKeyListQualifier;

/**
 * Allows you to query for objects that have a one or more of a set of related objects.
 * Example: given Child->School, you could select a few schools and find all the children that are in those schools.
 *
 * @binding displayGroup displayGroup to set the queryMatch in (queryOperator = isContainedInArray)
 * @binding key relationship key ("school")
 * @binding destinationEntityName name of the destination entity ("School")
 * @binding restrictedChoiceKey keypath returning an array of preselections (session.user.district.schools)
 * @binding restrictingFetchSpecification name of a fetchspec (elementarySchoools)
 * @binding keyWhenRelationship display key for destination ("name"->school.name)
 * @binding sortOrderAttributeName sort key for destination ("name"->school.name)
 * @binding toManyUIStyle "browser" or "checkbox"
 * @binding numCols number of columns when "checkbox" is the UIStyle
 * @binding size number of rows for "checkbox" or "browser" UIStyle
 * 
 * @author ak on Wed Apr 07 2004
 * @project ERDirectToWeb
 */

public class ERDQueryIsContainedInArray extends ERDCustomQueryComponent {

    /** logging support */
    private static final Logger log = Logger.getLogger(ERDQueryIsContainedInArray.class);
	
    /**
     * Public constructor
     * @param context the context
     */
    public ERDQueryIsContainedInArray(WOContext context) {
        super(context);
    }

    /** component does not synchronize it's variables */
    public boolean synchronizesVariablesWithBindings() { return false; }
    
    public void awake() {
        super.awake();
        displayGroup().queryOperator().setObjectForKey(ERXPrimaryKeyListQualifier.IsContainedInArraySelectorName, key());
    }
    
    public Object restrictedChoiceList() {
        String restrictedChoiceKey=(String)valueForBinding("restrictedChoiceKey");
        if( restrictedChoiceKey!=null &&  restrictedChoiceKey.length()>0 )
            return valueForKeyPath(restrictedChoiceKey);
        String fetchSpecName=(String)valueForBinding("restrictingFetchSpecification");
        if(fetchSpecName != null &&  fetchSpecName.length()>0) {
            EOEditingContext ec = session().defaultEditingContext();
            return EOUtilities.objectsWithFetchSpecificationAndBindings(ec, (String)valueForBinding("destinationEntityName"),fetchSpecName,null);
        }
        return null;
    }
}
