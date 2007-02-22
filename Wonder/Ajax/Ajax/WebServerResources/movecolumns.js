//----------------------------------------------//
//	Created by: Romulo do Nascimento Ferreira	//
//	Email: romulo.nf@gmail.com					//
//----------------------------------------------//

// NOTICE: This code may be used to any purpose without further
// permission from the author. You may remove this notice from the
// final code, however its appreciated if you keep the author name/email.
// Email me if theres something needing improvement

// Chuck Hill: Translated to English, made many improvements, error condition handling


var ajaxGrid_drag = false;

/*
 * Adds drag and drop support to the table
 */
function ajaxGrid_init(table) {
	table.onselectstart = function () { return false; } 
	table.onmousedown = function () { return false; }
	table.onmouseup = drop;
	
	numberOfColumns = table.rows[0].cells.length;
	tableRows = table.getElementsByTagName("TR");

	for (row=0; row < tableRows.length; row++) {
		tds = tableRows[row].cells;
		for (col=1; col<tds.length; col++) {
			drag(tds[col]);
			tds[col].onmouseover = paint;
			tds[col].onmouseout = paint;
			tds[col].originalClassName=tds[col].className;  // Save this to restore after a select and drag
		}
	}
}



/**
 * Adds the dynamically generated drag handler to the passed object.
 * The drag handler sets the style of the column being dragged and captures its index. It rejects 
 * the drag if started over an <a> tag (this caused problems with the sort column links).
 */
function drag(obj){
	if(!obj) return;
	obj.onmousedown = function(ev) {
		if (!ev) ev=window.event
	    if (ev.target) target = ev.target
	    else if (ev.srcElement) target=ev.srcElement
	    if ( ! isIgnoredElement(target))
	    {
		    columnAtual = cellIndex(this)
			for (x=0; x<tableRows.length; x++) {
				tableRows[x].cells[columnAtual].className ="ajaxGridSelected";
			}
			ajaxGrid_drag = true
			recordColumn(this);
		}
		return false;
	}
}
		
		
		
/**
 * Returns true if the passed element is one that mouse click should be ignored for.  This
 * allows the element to be accessed and prevents clicks on it from being used to re-order rows.
 */
function isIgnoredElement(element) {
	nodeName = element.nodeName.toLowerCase();
	return nodeName == 'a' ||
	       nodeName == 'input' ||
	       nodeName == 'button' ||
	       nodeName == 'select' ||
	       nodeName == 'textarea';
	       nodeName == 'input' ||
	       nodeName == 'input';
}


/*
 * Captures the index of the column being dragged
 */
function recordColumn(obj) {
	columnIndex =  cellIndex(obj) 
	return columnIndex
}



/*
 * Performs the actual re-ordering of the table in the browser and notifies the server of the change
 */
function orderTd (obj) {
	destinationIndex =  cellIndex(obj);
	
	// Error and no-op handling
	// A destinationIndex of -1 means not a table cell and 0 means the left most column which is not dropable
	if (destinationIndex < 1 || columnIndex == destinationIndex) return
	updateServerColumnOrder('sourceColumn=' + columnIndex + '&destinationColumn=' + destinationIndex);

	for (x=0; x<tableRows.length; x++) {
		tds = tableRows[x].cells
		var cell = tableRows[x].removeChild(tds[columnIndex])
		if (destinationIndex >= numberOfColumns || destinationIndex + 1 >= numberOfColumns) {
			tableRows[x].appendChild(cell)
		}
		else {
			tableRows[x].insertBefore(cell, tds[destinationIndex])
		}
	}
}



/*
 * Drop handler.  Calls orderTd() to perform the acutal re-order and then resets the style
 */
function drop(e) {
    if ( ! ajaxGrid_drag) return;
	if (!e) e=window.event
	if (e.target) target = e.target
	else if (e.srcElement) target=e.srcElement
	orderTd(target)
	ajaxGrid_drag = false
	
	for(x=0; x<tableRows.length; x++) {
		for (y=1; y<tableRows[x].cells.length; y++) {
		tableRows[x].cells[y].className=tableRows[x].cells[y].originalClassName;
		}
	}
}



/*
 * Sets and resets the style as the drag operation moves over other columns
 */
function paint(e) {
	if (!e) e=window.event
	ev = e.type
	if (ev == "mouseover") {
		if (ajaxGrid_drag) {
			for (x=0; x<tableRows.length; x++) {
				if (this.className !="ajaxGridSelected") {
					tableRows[x].cells[cellIndex(this)].className="ajaxGridHover"
				}
			}
		}
	}
	
	else if (ev == "mouseout") {
		for (x=0; x<tableRows.length; x++) {
			if (this.className !="ajaxGridSelected") {
				tableRows[x].cells[cellIndex(this)].className=tableRows[x].cells[cellIndex(this)].originalClassName
			}
		}
	}
}


/*
 * Returns the cell index (index in the row, zero based) of el if it is a <td> or <th> tag. If el is not one of those tags,
 * returns the cell index of the closest containing <td> or <th> tag.
 */
function cellIndex(el) {
    var ci = -1;
    td = el;
    if (el.nodeName.toLowerCase() !='td' && el.nodeName.toLowerCase() !='th') {
    	td = ascendDOM(el, 'th');
    	if (td == null) td = ascendDOM(el, 'td');
    }
    parent_row = ascendDOM(td, 'tr');
    for (var i = 0; i < parent_row.cells.length; i++) {
        if (td === parent_row.cells[i]) {
            ci = i;
        }
    }
    return ci;
}



/*
 * Walks up the DOM tree from e and returns the closest element with a nodeName of target.
 * Returns null if no such node is found.
 */
function ascendDOM(e,target) {
    while (e.nodeName.toLowerCase() !=target &&
           e.nodeName.toLowerCase() !='html')
        e=e.parentNode;
    return (e.nodeName.toLowerCase() == 'html')? null : e;
}