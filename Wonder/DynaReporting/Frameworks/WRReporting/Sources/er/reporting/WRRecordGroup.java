package er.reporting;

import er.grouping.*;
import java.lang.*;
import java.util.*;
import java.io.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import er.extensions.*;
import ognl.webobjects.*;

/**
 * Contains a cell in the table that {@link WRReport} has set up.
 * You should subclass this class to implement additional behaviour
 * or cell-level rendering, like colors that depend on the current group.
 * Additionally, you must specify the components name in the bindings
 * for the report component as the <code>reportComponentName</code>
 * binding.
 */

public class WRRecordGroup extends WOComponent  {
    /** logging support */
    private static final ERXLogger log = ERXLogger.getLogger(WRRecordGroup.class,"components");

    String _totalToShow;
    DRRecordGroup _recordGroup;
    String _displayType;
    String _noTotalLabel;
    Boolean _showAsTable;
    Boolean _showHeadings;
    Boolean _showRecordTable;
    DRReportModel _model;
    NSArray _colors;
    String _reportStyle;
    NSDictionary _totalDict;
    NSDictionary _coordinates;
    int _totalCount = -1;

    public DRRecord record;
    public DRValue value;
    public DRValue totalValue;

    String bgcolor;

    public WRRecordGroup(WOContext c){
        super(c);
    }

    /** Resets cached values. */
    public void reset() {
        _totalToShow = null;
        _recordGroup = null;
        _displayType = null;
        _noTotalLabel = null;
        _showAsTable = null;
        _showHeadings = null;
        _showRecordTable = null;
        _model = null;
        _colors = null;
        _reportStyle = null;
        _coordinates = null;
        _totalDict = null;
        _totalCount = -1;
    }

    
    /** Holds the cached dictionary of formatters */
    static private NSMutableDictionary _formatDict = new NSMutableDictionary();

    /** @return the dictionary of cached formatters */
    static public NSMutableDictionary formatDict(){
        return _formatDict;
    }

    /**
     * Pushes a custom formatter into the dictionary of formatters.
     * You'd use this to implement special display that can't be achieved
     * with a simple {@link NSNumberFormatter} by using a unique format string.
     */
    static public void setFormatterForFormat(NSNumberFormatter v, String frmt) {
        synchronized(_formatDict) {
            formatDict().setObjectForKey(v, frmt);
        }
    }

    /** Retreives the formatter for the given format, creating and caching one if needed. */
    static public NSNumberFormatter formatterForFormat(String frmt) {
        synchronized(_formatDict) {
            if(frmt == null) frmt = "#,###0.00";
            NSNumberFormatter v = (NSNumberFormatter)formatDict().objectForKey(frmt);
            if(v == null) {
                v = ERXNumberFormatter.numberFormatterForPattern(frmt);
                setFormatterForFormat(v, frmt);
            }
            return v;
        }
    }

    /** Component does not synchronize with bindings, and your subclasses should be doing so, too. */
    public final boolean synchronizesVariablesWithBindings() {
        return false;
    }

    /** Component is stateless, and your subclasses should be so, too. */
    public boolean isStateless() {
        return true;
    }
    
    public void sleep() {
        //this.reset();
    }

    public void appendToResponse(WOResponse r, WOContext c) {
        super.appendToResponse(r, c);
    }


    /** Returns the {@link DRRecordGroup} from the bindings. */
    public DRRecordGroup recordGroup() {
        if (_recordGroup == null) {
            _recordGroup = (DRRecordGroup)this.valueForBinding("recordGroup");
        }
        return _recordGroup;
    }

    /**
     * Returns the display type from the bindings.
     * If not set, then <code>TOTALS</code> is used as the default,
     * meaning that ???
     */
    public String displayType() {
        if (_displayType == null) {
            _displayType = (String)this.valueForBinding("displayType");
            if (_displayType == null) {
                _displayType = "TOTALS";
            }
        }
        return _displayType;
    }

    public String noTotalLabel() {
        if (_noTotalLabel == null) {
            _noTotalLabel = (String)this.valueForBinding("noTotalLabel");
            if (_noTotalLabel == null) {
                _noTotalLabel = "-";
            }
        }
        return _noTotalLabel;
    }

    public String reportStyle() {
        if (_reportStyle == null) {
            _reportStyle = (String)this.valueForBinding("reportStyle");
            if (_reportStyle == null) {
                _reportStyle = "NESTED_CELLS";
            }
        }
        return _reportStyle;
    }


    public NSArray records() {
        return this.recordGroup().sortedRecordList();
    }


    public NSArray totals() {
        //log.debug( "entered");
        if(recordGroup() == null)
            return NSArray.EmptyArray;
        return this.recordGroup().totalList();
    }

    public double totalValueTotal() {
        if(totalValue != null) {
            if(totalValue.key().indexOf("~") == 0) {
                return DRValueConverter.converter().doubleForValue(WOOgnl.factory().getValue(totalValue.key().substring(1), recordGroup().rawRecordList()));
            } else {
                String totalKey = (String)totalValue.attribute().userInfo().objectForKey("total");
                if(totalKey != null) {
                    return DRValueConverter.converter().doubleForValue(recordGroup().rawRecordList().valueForKeyPath(totalKey));
                }
            }
            return totalValue.total();
        }
        return 0;
    }

    public boolean nototals() {
        if (this.totals().count() > 0) {
            return false;
        }
        return true;
    }


    public String toString() {
        return this.recordGroup().totals().toString();
    }

    public DRReportModel model() {
        if (_model == null) {
            _model = (DRReportModel)this.valueForBinding("model");
        }
        return _model;
    }

    public Boolean booleanValueForBinding(String name) {
        boolean flag = ERXValueUtilities.booleanValue(valueForBinding(name));
        return flag ? Boolean.TRUE : Boolean.FALSE;
    }

    public boolean showAsTable() {
        if (_showAsTable == null) {
            _showAsTable = booleanValueForBinding("showAsTable");
        }
        return _showAsTable.booleanValue();
    }

    public boolean showHeadings() {
        if (showSingleValue()) {
            return false;
        }
        if (_showHeadings == null) {
            _showHeadings = booleanValueForBinding("showHeadings");
        }
        return _showHeadings.booleanValue();
    }

    public boolean showHeadingsForTotalsOrTable() {
        return showHeadings();
    }


    public boolean showRecordTable() {
        if(_showRecordTable == null) {
            _showRecordTable = "TABLE".equals(displayType()) ? Boolean.TRUE: Boolean.FALSE;
        }
        return _showRecordTable.booleanValue();
    }


    public boolean showSingleValue() {
        if ("SINGLE_TOTAL".equals(displayType())) {
            return true;
        }
        return false;
    }


    public boolean showTotalsOnly() {
        if ("TOTALS".equals(displayType())) {
            return true;
        }
        return false;
    }


    public boolean showTotalsOnlyAsTable() {
        if (this.showTotalsOnly() && this.showAsTable()) {
            return true;
        }
        return false;
    }


    public boolean showTotalsOnlyAsCells() {
        if (this.showTotalsOnly() && !this.showAsTable()) {
            return true;
        }
        return false;
    }


    public boolean showRecordTableAsCells() {
        if (this.showRecordTable() && !this.showAsTable()) {
            return true;
        }
        return false;
    }


    public boolean showRecordTableAsTable() {
        if (this.showRecordTable() && this.showAsTable()) {
            return true;
        }
        return false;
    }


    public boolean totalsOnly() {
        return showTotalsOnly();
    }


    public String totalToShow() {
        if (_totalToShow == null) {
            _totalToShow = (String)this.valueForBinding("totalToShow");
        }
        return _totalToShow;
    }


    public String singleTotal() {
        if(recordGroup() == null)
            return noTotalLabel();
        String totalKey = this.totalToShow();

        double doubleValue = 0.0;

        if(totalKey != null) {
            if(totalKey.indexOf("~") == 0) {
                doubleValue = DRValueConverter.converter().doubleForValue(WOOgnl.factory().getValue(totalKey.substring(1), recordGroup().rawRecordList()));
            } else if(totalKey.indexOf("@") == 0) {
                doubleValue = DRValueConverter.converter().doubleForValue(recordGroup().rawRecordList().valueForKeyPath(totalKey));
                if(doubleValue == 0.0 && totalKey.indexOf("@count") == 0) {
                    return this.noTotalLabel();
                }
            } else {
                NSArray tots = this.recordGroup().totalList();

                if (tots != null && tots.count() > 0) {
                    DRValue v = this.recordGroup().totalForKey(this.totalToShow());
                    if(v != null) {
                        doubleValue = v.total();
                    } else {
                        return this.noTotalLabel();
                    }
                }
            }
        }
        Number nm = new Double(doubleValue);
        NSNumberFormatter formatter = WRRecordGroup.formatterForFormat((String)valueForBinding("formatForSingleTotal"));
        return formatter.format(nm);
    }


    public int nototalsrowspan() {
        return this.model().flatAttributeListTotal().count();
    }


    public boolean hDimsUsed() {
        if (this.model().hList().count() > 0) {
            return true;
        }
        return false;
    }


    public boolean useVerticalReportStyle() {
        String style = this.reportStyle();
        if ("VERTICAL_ROWS".equals(style)) {
            return true;
        }
        return false;
    }

    public NSDictionary totalDict() {
        if (_totalDict == null) {
            _totalDict = (NSDictionary)this.valueForBinding("totalDict");
        }
        return _totalDict;
    }

    public NSDictionary coordinates() {
        if (_coordinates == null) {
            _coordinates = (NSDictionary)this.valueForBinding("coordinates");
        }
        return _coordinates;
    }

    public NSArray colors() {
        if (_colors == null) {
            _colors = (NSArray)this.valueForBinding("colors");
            if(_colors == null) {
                _colors = NSArray.EmptyArray;
            }
        }
        return _colors;
    }

    public int totalCount() {
        if(_totalCount == -1) {
            NSDictionary d = this.totalDict();
            if (d == null) {
                _totalCount = 0;
            } else {
                _totalCount = d.allKeys().count();
            }
        }
        return _totalCount;
    }

    public boolean isNotTotalGroup() {
        if (this.totalCount() > 0) {
            return false;
        }
        return true;
    }

    public NSDictionary attributeListDict() {
        if (this.totalsOnly()) {
            return this.model().flatAttributeListTotalDict();
        }

        return this.model().flatAttributeDepthDict();
    }


    public int depthCount() {
        return this.attributeListDict().allKeys().count();
    }


    public int colspanForAllAttribs() {
        return this.model().flatAttributeList().count();
    }

    /*
     public void takeValuesFromRequest(WORequest r, WOContext c) {
         //Abort call to super to save all this processing time
     }
     */

    public NSArray recordFlatValueList() {
        return record.flatValueList();
    }
}