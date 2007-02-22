package er.directtoweb;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WQueryToManyRelationship;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;

import er.extensions.ERXEC;
import er.extensions.ERXPrimaryKeyListQualifier;
import er.extensions.ERXValueUtilities;

/**
 * Enhanced relationship query component to to-many relationships.
 * @d2wKey multiple when true, the user can choose multiple items and they get selected as <code>OR</code>
 * @d2wKey restrictedChoiceKey keypath off the component that returns the list of objects to display
 * @d2wKey restrictingFetchSpecification name of the fetchSpec to use for the list of objects.
 */

public class ERD2WQueryToManyRelationship extends D2WQueryToManyRelationship {

    /** logging support */
    private static final Logger log = Logger.getLogger(ERD2WQueryToManyRelationship.class);
	
    /**
     * Public constructor
     * @param context the context
     */
    public ERD2WQueryToManyRelationship(WOContext context) {
        super(context);
    }

    public boolean hasMultipleSelection() {
        return ERXValueUtilities.booleanValue(d2wContext().valueForKey("multiple"));
    }
    
    public String componentName() {
        return !hasMultipleSelection() ? "ERXToOneRelationship" :  "ERXToManyRelationship";
    }
    
    public WOComponent self() {
        return this;
    }

    public void setValue(Object newValue) {
        if(hasMultipleSelection()) {
            if (newValue instanceof NSArray) {
                NSArray array = (NSArray) newValue;
                if(array.count() == 0) {
                    newValue = null;
                }
            }
            displayGroup().queryOperator().takeValueForKey(ERXPrimaryKeyListQualifier.IsContainedInArraySelectorName, propertyKey());
        }
        super.setValue(newValue);
    }

    public Object restrictedChoiceList() {
        String restrictedChoiceKey=(String)d2wContext().valueForKey("restrictedChoiceKey");
        if( restrictedChoiceKey!=null &&  restrictedChoiceKey.length() > 0 )
            return valueForKeyPath(restrictedChoiceKey);
        String fetchSpecName=(String)d2wContext().valueForKey("restrictingFetchSpecification");
        if(fetchSpecName != null) {
            EOEditingContext ec = ERXEC.newEditingContext();
            EOEntity entity = d2wContext().entity();
            EORelationship relationship = entity.relationshipNamed((String)d2wContext().valueForKey("propertyKey"));
            return EOUtilities.objectsWithFetchSpecificationAndBindings(ec, relationship.destinationEntity().name(),fetchSpecName,null);
        }
        return null;
    }
}
