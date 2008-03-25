package er.extensions;



import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

/**
 * <p>
 * ERXFlickrBatchNavigation is a batch navigation component that provides 
 * pagination that behaves like the paginator on Flickr.com.
 * </p>
 * 
 * <p>
 * Include ERXFlickrBatchNavigation.css in ERExtensions for a default stylesheet
 * that looks (very) similar to Flickr.
 * </p>
 * <p>Can also be used for pagination on the parent component, where the objects being paginated may be POJOs in an array,
 * or where paging all the objects in the allObjects array is not feasible due to memory requirements.</p>
 * 
 * @author mschrag
 * @author rob, cug (non displayGroup batching)
 * 
 * @binding displayGroup the display group to paginate
 * @binding displayName the name of the items that are being display ("photo", "bug", etc)
 * @binding showPageRange if true, the page of items on the page is shown, for example "(1-7 of 200 items)"
 * @binding small if true, a compressed page count style is used 
 * 
 * @binding parentActionName (if you don't provide a displayGroup) the action to be executed on the parent component to get the next batch of items.
 * @binding currentBatchIndex (if you don't provide a displayGroup) used to get and set on the parent component the selected page index
 * @binding maxNumberOfObjects (if you don't provide a displayGroup) used to get the total number of objects that are being paginated.
 * @binding numberOfObjectsPerBatch (if you don't provide a displayGroup) the number of objects per batch (page)
 */
public class ERXFlickrBatchNavigation extends WOComponent {
	private int _lastPageCount;
	private int _lastPageSize;
	private int _lastCurrentPageNumber;
	private NSMutableArray<PageNumber> _pageNumbers;
	private PageNumber _repetitionPageNumber; 
	
	//Note: Lazily Cached
	private String _parentActionName;

	public ERXFlickrBatchNavigation(WOContext context) {
		super(context);
		_lastPageCount = -1;
		_lastCurrentPageNumber = -1;
		_lastPageSize = -1;
	}

	@Override
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}

	public NSArray objects() {
		NSArray objects = null;
		if(displayGroup() != null){
			if (displayGroup() instanceof ERXDisplayGroup) {
				ERXDisplayGroup dg = (ERXDisplayGroup) displayGroup();
				objects =  dg.filteredObjects();
			} else {
				objects =  displayGroup().allObjects();
			}
		} 
		return objects;
	}
	
	public WODisplayGroup displayGroup() {
		return (WODisplayGroup) valueForBinding("displayGroup");
	}

	public void setRepetitionPageNumber(PageNumber repetitionPageNumber) {
		_repetitionPageNumber = repetitionPageNumber;
	}

	public PageNumber repetitionPageNumber() {
		return _repetitionPageNumber;
	}
	
	public boolean hasMultiplePages() {
		return batchCount() > 1;
	}
		
	public boolean hasPreviousPage() {
		return currentBatchIndex() > 1;
	}

	public WOActionResults previousPage() {
		WOActionResults previousPage = null;
		if(displayGroup() != null){
			WODisplayGroup displayGroup = displayGroup();
			displayGroup.displayPreviousBatch();
		} else if(parentActionName() != null){
			Integer previousBatchIndex = new Integer((currentBatchIndex() - 1));
			if(!(previousBatchIndex.intValue()  > 0)){
				previousBatchIndex = new Integer(1);
			} 
			setValueForBinding(previousBatchIndex, "currentBatchIndex");
			previousPage = performParentAction(parentActionName());
		}
		return previousPage;
	}

	public boolean hasNextPage() {
		return currentBatchIndex() < batchCount();
	}

	public WOActionResults nextPage() {
		WOActionResults nextPage = null;
		if(displayGroup() != null){
			WODisplayGroup displayGroup = displayGroup();
			displayGroup.displayNextBatch();
		} else if(parentActionName() != null){
			Integer nextBatchIndex = new Integer((currentBatchIndex() + 1));
			int pageCount  = batchCount();
			if((nextBatchIndex.intValue()  > pageCount)){
				nextBatchIndex = new Integer(pageCount);
			} 
			setValueForBinding(nextBatchIndex, "currentBatchIndex");
			nextPage = performParentAction(parentActionName());
		}
		return nextPage;
	}

	public WOActionResults selectPage() {
		WOActionResults selectPage = null;
		Integer pageNumber = _repetitionPageNumber.pageNumber();
		if (pageNumber != null) {
			if (displayGroup() != null) {
				displayGroup().setCurrentBatchIndex(pageNumber.intValue());
			} else {
				setValueForBinding(pageNumber, "currentBatchIndex");
				selectPage = performParentAction(parentActionName());
			}
		}
		return selectPage;
	}

	public String displayName() {
		String displayName = (String) valueForBinding("displayName");
		if (displayName == null) {
			displayName = ERXLocalizer.currentLocalizer().localizedStringForKey("ERXFlickrBatchNavigation.item");
		}
		return displayName;
	}
	
	public Integer displayNameCount(){
		Integer displayNameCount = new Integer(0);
		if(displayGroup() != null){
			NSArray objects = objects();
			if(objects != null && objects.count() > 0){
				displayNameCount = new Integer(objects.count());
			}
		} else {
			displayNameCount = new Integer(maxNumberOfObjects());
		}
		
		return displayNameCount;
	}

	public boolean isCurrentPageNumber() {
		Integer pageNumber = _repetitionPageNumber.pageNumber();
		return pageNumber != null && pageNumber.intValue() == _lastCurrentPageNumber;
	}

	public NSArray<PageNumber> pageNumbers() {
		int pageCount = batchCount();
		int currentPageNumber = currentBatchIndex();
		int pageSize = numberOfObjectsPerBatch();
		if (_lastPageCount != pageCount || _lastCurrentPageNumber != currentPageNumber || _lastPageSize != pageSize) {
			_pageNumbers = new NSMutableArray<PageNumber>();

			int nearEdgeCount;
			int endCount;
			int nearCount;
			int minimumCount;

			if (ERXComponentUtilities.booleanValueForBinding(this, "small", false)) {
				nearEdgeCount = 1;
				endCount = 1;
				nearCount = 0;
				minimumCount = 5;
			}
			else {
				nearEdgeCount = 8;
				endCount = 2;
				nearCount = 3;
				minimumCount = 15;
			}
			
			if (pageCount <= minimumCount) {
				addPageNumbers(1, pageCount);
			}
			else if (currentPageNumber <= nearEdgeCount) {
				addPageNumbers(1, Math.max(nearEdgeCount - 1, currentPageNumber + nearCount));
				addEllipsis();
				addPageNumbers(pageCount - endCount + 1, pageCount);
			}
			else if (currentPageNumber > pageCount - nearEdgeCount) {
				addPageNumbers(1, endCount);
				addEllipsis();
				addPageNumbers(Math.min(pageCount - nearEdgeCount + 2, currentPageNumber - nearCount), pageCount);
			}
			else {
				addPageNumbers(1, endCount);
				if (currentPageNumber - nearCount > (endCount + 1)) {
					addEllipsis();
				}
				addPageNumbers(Math.max(endCount + 1, currentPageNumber - nearCount), Math.min(currentPageNumber + nearCount, pageCount - endCount));
				if (currentPageNumber + nearCount < pageCount - endCount) {
					addEllipsis();
				}
				addPageNumbers(pageCount - endCount + 1, pageCount);
			}

			_lastPageCount = pageCount;
			_lastCurrentPageNumber = currentPageNumber;
			_lastPageSize = pageSize;
		}
		return _pageNumbers;
	}
	
	protected void addEllipsis() {
		_pageNumbers.addObject(new PageNumber(null, true));
		
	}
	protected void addPageNumbers(int startIndex, int endIndex) {
		for (int pageNumber = startIndex; pageNumber <= endIndex; pageNumber++) {
			_pageNumbers.addObject(new PageNumber(Integer.valueOf(pageNumber), false));
		}
	}

	public static class PageNumber {
		private Integer _pageNumber;
		private boolean _ellipsis;

		public PageNumber(Integer pageNumber, boolean ellipsis) {
			_pageNumber = pageNumber;
			_ellipsis = ellipsis;
		}

		public Integer pageNumber() {
			return _pageNumber;
		}

		public boolean isEllipsis() {
			return _ellipsis;
		}
	}
	
	public int batchCount(){
		int batchCount = 0;
		if(displayGroup() != null){
			batchCount = displayGroup().batchCount();
		} else {
			int numberOfObjectsPerBatch = numberOfObjectsPerBatch();
			int maxNumberOfObjects = maxNumberOfObjects();
			if (!(numberOfObjectsPerBatch == 0)){	
				if (maxNumberOfObjects == 0)
					batchCount = 1;
				else
					batchCount = (maxNumberOfObjects - 1) / numberOfObjectsPerBatch + 1;
			}
		}
		return batchCount;	
	}
	
	public int numberOfObjectsPerBatch(){
		int numberOfObjects = 0;
		if(displayGroup() != null){
			numberOfObjects = displayGroup().numberOfObjectsPerBatch();
		} else if(hasBinding("numberOfObjectsPerBatch")){
			Integer numberOfObjectsPerBatch = (Integer) valueForBinding("numberOfObjectsPerBatch");
			if(numberOfObjectsPerBatch != null && numberOfObjectsPerBatch.intValue() > 0){
				numberOfObjects = numberOfObjectsPerBatch.intValue();
			} 
		}
		return numberOfObjects;
	}
	
	public int maxNumberOfObjects() {
		int maxNumber = 0; 
		if (hasBinding("maxNumberOfObjects")) {
			Integer maxNumberOfObjects = (Integer) valueForBinding("maxNumberOfObjects");
			if (maxNumberOfObjects != null && maxNumberOfObjects.intValue() > 0) {
				maxNumber = maxNumberOfObjects.intValue();
			} 
		} 
		return maxNumber;
	}
	
	public int currentBatchIndex(){
		int index = 1;
		if(displayGroup() != null){
			index = displayGroup().currentBatchIndex();
		} else if(hasBinding("currentBatchIndex")){
			Integer currentBatchIndex = (Integer) valueForBinding("currentBatchIndex");
			if(currentBatchIndex != null && currentBatchIndex.intValue() > 1){
				index = currentBatchIndex.intValue();
			}
		}
		return index;
	}
	
	public String parentActionName(){
		if(_parentActionName == null){
			_parentActionName = (String) valueForBinding("parentActionName");
		}
		return _parentActionName;
	}
	
	public int firstIndex(){
		int firstIndex = 0;
		if(displayGroup() != null){
			firstIndex  = displayGroup().indexOfFirstDisplayedObject();
		} else {
			int currentBatchIndex = currentBatchIndex();
			int numberOfObjectsPerBatch = numberOfObjectsPerBatch();
			firstIndex = (currentBatchIndex * numberOfObjectsPerBatch) - (numberOfObjectsPerBatch - 1); 
		}
		return firstIndex;
	}
	
	public int lastIndex(){
		int lastIndex = 0;
		if(displayGroup() != null){
			lastIndex  = displayGroup().indexOfLastDisplayedObject();
		} else {
			int currentBatchIndex = currentBatchIndex();
			int numberOfObjectsPerBatch = numberOfObjectsPerBatch();
			lastIndex = currentBatchIndex * numberOfObjectsPerBatch;
		}
		return lastIndex;
	}
}