var ERMSubmitLink = {
	submit: function (fieldName, additionalFunction) {
		var hf = window.document.getElementById(fieldName);
		hf.value = fieldName;
		if (additionalFunction) {
			eval(additionalFunction);
		}
		hf.form.submit();
	}
};

var ERMSL = ERMSubmitLink;