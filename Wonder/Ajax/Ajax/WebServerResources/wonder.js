Object.extend(String.prototype, {
	addQueryParameters: function(additionalParameters) {
		if (additionalParameters) {
			return this + (this.match(/\?/) ? '&' : '?') + additionalParameters;
		}
		else {
			return this;
		}
	}
});

Object.extend(Event, {
	keyValue: function(event) {
		var keynum;
		if (window.event) {
			keynum = event.keyCode;
		}
		else if (event.which) {
			keynum = event.which;
		}
		else {
			keynum = event.keyCode;
		}
		return keynum;
	}
});

Object.extend(Form, {
  serializeWithoutSubmits: function(form) {
    var elements = Form.getElements($(form));
    var queryComponents = new Array();

    for (var i = 0; i < elements.length; i++) {
			if (elements[i].type != 'submit') {
	      var queryComponent = Form.Element.serialize(elements[i]);
	      if (queryComponent) {
	        queryComponents.push(queryComponent);
				}
			}
    }

    return queryComponents.join('&');
  }
});

var AjaxInPlace = {
	saveFunctionName : function(id) {
		return "window." + id + "Save";
	},
	
	cancelFunctionName : function(id) {
		return "window." + id + "Cancel";
	},
	
	editFunctionName : function(id) {
		return "window." + id + "Edit";
	},
	
	cleanupEdit : function(id) {
		var saveFunctionName = this.saveFunctionName(id);
		var cancelFunctionName = this.cancelFunctionName(id);
		if (typeof eval(saveFunctionName) != 'undefined') { eval(saveFunctionName + " = null"); }
		if (typeof eval(cancelFunctionName) != 'undefined') { eval(cancelFunctionName + " = null"); }
	},
	
	cleanupView : function(id) {
		var editFunctionName = this.editFunctionName(id);
		if (typeof eval(editFunctionName) != 'undefined') { eval(editFunctionName + " = null"); }
	}
};

var AjaxOptions = {
	options : function(additionalOptions) {
		var options = { method: 'get', asynchronous: true, evalScripts: true };
		Object.extend(options, additionalOptions || {});
		return options;
	}
}

var AjaxUpdateContainer = {
	register : function(id, options) {
		if (!options) {
			options = "{}";
		}
		eval(id + "Update = function() { AjaxUpdateContainer.update('" + id + "', " + options + ") }");
	},
	
	update : function(id, options) {
		new Ajax.Updater(id, $(id).getAttribute('updateUrl'), AjaxOptions.options(options));
	}
};

var AjaxUpdateLink = {
	updateFunc : function(id, options, elementID) {
		var updateFunction = function(queryParams) {
			AjaxUpdateLink.update(id, options, elementID, queryParams);
		};
		return updateFunction;
	},
	
	update : function(id, options, elementID, queryParams) {
		var actionUrl = $(id).getAttribute('updateUrl').sub('[^/]+$', elementID) + '?__updateID=' + id;
		actionUrl = actionUrl.addQueryParameters(queryParams);
		new Ajax.Updater(id, actionUrl, AjaxOptions.options(options));
	}
};