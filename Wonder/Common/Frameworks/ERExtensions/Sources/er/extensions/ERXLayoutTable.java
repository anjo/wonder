package er.extensions;

import java.util.Enumeration;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

/**
 * Table that can handle cell with row and colspans. Very useful with D2W to make more advance layouts.<br />
 * 
 */

public class ERXLayoutTable extends WOComponent {

	static final Logger log = Logger.getLogger(ERXLayoutTable.class);
	
	NSArray _list;
	NSArray _colCounts;
	int _maxColumns;
    public int currentRow;
    public int currentCol;
    public int currentItemIndex;

    public ERXLayoutTable(WOContext aContext)  {
        super(aContext);
        _resetInternalCaches();
    }

    public boolean isStateless() {
        return true;
    }
    
    protected int intForBinding(String name, int defaultValue) {
        Object valueStr = valueForBinding(name);
        if (valueStr != null) {
            try {
                int value = Integer.parseInt(valueStr.toString());
                return value;
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Problem parsing int from " + name + " binding "+e);
            }
        }
        return defaultValue;
    }

    public int cellRowSpan() {
    	return intForBinding("cellRowSpan", 1);
    }

    public int cellColSpan() {
    	return intForBinding("cellColSpan", 1);
    }

    public int maxColumns()  {
        if (_maxColumns == -1) {
            Object maxStr = valueForBinding("maxColumns");
            if (maxStr != null) {
                try {
                    _maxColumns = Integer.parseInt(maxStr.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("WOTable - problem parsing int from maxColumns binding "+e);
                }
            }
            if (_maxColumns <= 0)
                _maxColumns=1;
       }
        return _maxColumns;
    }
  
    public NSArray colCounts()  {
    	if (_colCounts==null) {
    		_colCounts = (NSArray)valueForBinding("colCounts");
    		if(_colCounts == null) {
    			NSMutableArray colCounts = new NSMutableArray();
    			int maxColumns = maxColumns();
    			int currentRow = 0;
    			int currentCol = 0;
    			int index = 0;
    			int total = 0;
    			for (Enumeration iter = list().objectEnumerator(); iter.hasMoreElements();) {
    				Object item = (Object) iter.nextElement();
    				setValueForBinding(item, "item");
    				setValueForBinding(ERXConstant.integerForInt(currentRow), "row");
    				setValueForBinding(ERXConstant.integerForInt(currentCol), "col");
    				setValueForBinding(ERXConstant.integerForInt(index), "index");
    				int rowSpan = cellRowSpan();
    				int colSpan = cellColSpan();
    				// fill up rows until enough are present
    				// this currently doesn't work when a col has eaten all
    				for(int i = colCounts.count(); i < rowSpan + currentRow; i++) {
    					colCounts.addObject(new Integer(maxColumns));
    					//log.info("Added: " + item + " " + colCounts);
    				}
    				//log.info("Start: " + item + "  " + currentRow + "/" + rowSpan + " " + currentCol + "/" + colSpan + " " + colCounts);
    				for(int i = currentRow; i < currentRow + rowSpan; i++) {
    					int currentMaxColumns = ((Integer) colCounts.objectAtIndex(i)).intValue();
    					currentMaxColumns = currentMaxColumns - (colSpan - (i == currentRow ? 1 : 0));
    					colCounts.replaceObjectAtIndex(new Integer(currentMaxColumns), i);
    					//log.info("Curr: " + item + "  " + i + "/" + rowSpan + " " + currentMaxColumns + "/" + colSpan + " " + colCounts);
    				}
    				//log.info("Intern: " + item + "  " + currentRow + "/" + rowSpan + " " + currentCol + "/" + colSpan + " " + colCounts);
    				int currentRowMaxColums = ((Integer) colCounts.objectAtIndex(currentRow)).intValue();
    				total += rowSpan * colSpan;
    				currentCol += colSpan;
    				if(currentCol >= currentRowMaxColums) {
    					currentRow++;
    					currentCol = 0;
    					//log.info("Bumping row: " + item + " " + currentRow + " " + colCounts);
    				}
    	   			index = index + 1;
    			}
    			if(total > maxColumns * colCounts.count()) {
    				colCounts.addObject(new Integer(total - maxColumns * colCounts.count()));
    			}
    			//log.info("Result: " + colCounts);
    			_colCounts = colCounts;
    		}
    	}
    	return _colCounts;
    }
    
    public NSArray list()  {
    	if (_list==null) {
    		_list = (NSArray)valueForBinding("list");
    	}
        return _list;
    }


    public int rowCount()  {
        return colCounts().count();
    }

    public int colCount()  {
        return ((Number) colCounts().objectAtIndex(currentRow)).intValue();
    }

    public void setCurrentRow(int newValue) {
        currentRow=newValue;
        currentCol=-1;
    }

    public void pushItem()  {
        NSArray aList = list();
        int index = currentItemIndex;
        Object item = index < aList.count() ? aList.objectAtIndex(index) : null;
        // log.debug("Index: " + currentItemIndex + ": " + item + " -> " + currentRow + "/" + currentCol);
        setValueForBinding(item, "item");
        setValueForBinding(ERXConstant.integerForInt(currentRow), "row");
        setValueForBinding(ERXConstant.integerForInt(currentCol), "col");
        setValueForBinding(ERXConstant.integerForInt(index), "index");
        currentItemIndex++;
    }

    public void setCurrentCol(int newValue){
    	currentCol=newValue;
    	// log.debug("Index: " + newValue);
    	if(colCount() != newValue) {
    		pushItem();
    	} else {
    		setValueForBinding(null, "item");
    	}
    }

    protected void _resetInternalCaches() {
        _list=null;
        currentCol=-1;
        currentRow=-1;
        currentItemIndex = 0;
        _maxColumns = -1;
        _colCounts = null;
    }

    public void takeValuesFromRequest(WORequest aRequest, WOContext aContext)  {
        _resetInternalCaches();
        super.takeValuesFromRequest(aRequest, aContext);
     }

    public void reset() {
        _resetInternalCaches();
        setValueForBinding(null, "item");
   }
}