package er.directtoweb;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.directtoweb.*;
import er.extensions.*;

/**
 * Page that can query a set of entities.
 * It is like the D2WQueryAll page except that you can partition your entities into sections.
 *
 * @created ak on Mon Sep 01 2003
 * @project ERDirectToWeb
 */

public class ERD2WQueryEntitiesPage extends ERD2WPage implements QueryAllPageInterface {

    /** logging support */
    private static final ERXLogger log = ERXLogger.getLogger(ERD2WQueryEntitiesPage.class,"components");

    protected EODatabaseDataSource queryDataSource;
    protected  WODisplayGroup displayGroup;
    
    /**
     * Public constructor
     * @param context the context
     */
    public ERD2WQueryEntitiesPage(WOContext context) {
        super(context);
    }

    public WODisplayGroup displayGroup() {
        if(displayGroup == null) {
            displayGroup = new WODisplayGroup();
        }
        return displayGroup;
    }

    public EODataSource queryDataSource() {
        return queryDataSource;
    }

    public String queryConfigurationName() { return (String)d2wContext().valueForKey("queryConfigurationName"); }
    public String listConfigurationName() { return (String)d2wContext().valueForKey("listConfigurationName"); }
    
    public WOComponent queryAction() {
        WOComponent result = null;
        if(entity() != null) {
            // construct datasource
            queryDataSource = new EODatabaseDataSource(session().defaultEditingContext(), entity().name());
            queryDataSource.setAuxiliaryQualifier(displayGroup().qualifierFromQueryValues());

            ListPageInterface lpi;
            if(listConfigurationName() != null) {
                lpi = (ListPageInterface)D2W.factory().pageForConfigurationNamed(listConfigurationName(), session());
            } else {
                lpi = D2W.factory().listPageForEntityNamed(entity().name(), session());
            }
            
            lpi.setDataSource(queryDataSource);
            lpi.setNextPage(context().page());
            
            // remove old values for next iteration
            displayGroup.queryMatch().removeAllObjects();
            displayGroup.queryOperator().removeAllObjects();
            result = (WOComponent)lpi;
        }
        return result;
    }
    
    public WOComponent showRegularQueryAction() {
        QueryPageInterface qpi = null;
        if(queryConfigurationName() != null) {
            qpi = (QueryPageInterface)D2W.factory().pageForConfigurationNamed(queryConfigurationName(), session());
        }  else {
            qpi = D2W.factory().queryPageForEntityNamed(entity().name(), session());
        }
        return (WOComponent)qpi;
    }
}
