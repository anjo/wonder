package er.rest;

import com.webobjects.eocontrol.EOClassDescription;
import com.webobjects.eocontrol.EOEditingContext;

/**
 * ERXAbstractRestDelegate is the default implementation of the IERXRestDelegate interface that can handle looking up delegates for non-eo classes, etc.
 * 
 * @author mschrag
 */
public abstract class ERXAbstractRestDelegate implements IERXRestDelegate {
	private EOEditingContext _editingContext;
	private boolean _createMissingObjects;
	
	public ERXAbstractRestDelegate() {
	}
	
	public void setCreateMissingObjects(boolean createMissingObjects) {
		_createMissingObjects = createMissingObjects;
	}
	
	public boolean shouldCreateMissingObjects() {
		return _createMissingObjects;
	}

	public ERXAbstractRestDelegate(EOEditingContext editingContext) {
		_editingContext = editingContext;
	}
	
	public void setEditingContext(EOEditingContext editingContext) {
		_editingContext = editingContext;
	}
	
	public EOEditingContext editingContext() {
		return _editingContext;
	}
	
	public Object primaryKeyForObject(Object obj) {
		Object pkValue;
		EOClassDescription classDescription = ERXRestClassDescriptionFactory.classDescriptionForObject(obj);
		if (_isDelegateForEntity(classDescription)) {
			pkValue = _primaryKeyForObject(classDescription, obj);
		}
		else {
			IERXRestDelegate delegate = IERXRestDelegate.Factory.delegateForEntityNamed(classDescription.entityName(), _editingContext);
			if (delegate == null || delegate.getClass() == getClass()) {
				throw new UnsupportedOperationException("Unable to create an instance of the entity '" + classDescription.entityName() + "'.");
			}
			else {
				pkValue = delegate.primaryKeyForObject(obj);
			}
		}
		return pkValue;
	}

	public Object createObjectOfEntityWithID(EOClassDescription entity, Object id) {
		Object obj;
		if (_isDelegateForEntity(entity)) {
			obj = _createObjectOfEntityWithID(entity, id);
		}
		else {
			IERXRestDelegate delegate = IERXRestDelegate.Factory.delegateForEntityNamed(entity.entityName(), _editingContext);
			if (delegate == null) {
				// MS: Is this still necessary?
				if (entity instanceof BeanInfoClassDescription) {
					obj = ((BeanInfoClassDescription) entity).createInstance();
				}
				else {
					throw new UnsupportedOperationException("Unable to create an instance of the entity '" + entity + "'.");
				}
			}
			else if (getClass() == delegate.getClass()) {
				obj = _createObjectOfEntityWithID(entity, id);
			}
			else {
				obj = delegate.createObjectOfEntityWithID(entity, id);
			}
		}
		return obj;
	}

	public Object objectOfEntityWithID(EOClassDescription entity, Object id) {
		Object obj;
		if (_isDelegateForEntity(entity)) {
			if (id == null) {
				obj = createObjectOfEntityWithID(entity, id);
			}
			else {
				obj = _fetchObjectOfEntityWithID(entity, id);
				if (obj == null && shouldCreateMissingObjects()) {
					obj = createObjectOfEntityWithID(entity, id);
				}
			}
		}
		else {
			IERXRestDelegate delegate = IERXRestDelegate.Factory.delegateForEntityNamed(entity.entityName(), _editingContext);
			if (delegate.getClass() == getClass()) {
				throw new UnsupportedOperationException("There is no delegate for the entity '" + entity.entityName() + "'.");
			}
			obj = delegate.objectOfEntityWithID(entity, id);
		}
		return obj;
	}

	protected abstract boolean _isDelegateForEntity(EOClassDescription entity);

	protected abstract Object _createObjectOfEntityWithID(EOClassDescription entity, Object id);

	protected abstract Object _primaryKeyForObject(EOClassDescription entity, Object obj);
	
	protected abstract Object _fetchObjectOfEntityWithID(EOClassDescription entity, Object id);
}