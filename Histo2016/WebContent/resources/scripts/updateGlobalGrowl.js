function updateGlobalGrowl(name) {

	var windowElement = window;

	var i = 0;

	// searching for growl with name, going up the parent docuemntes (max 10
	// times) to finde it
	do {
		i++;
	} while (PrimeFaces.widgets[name] == null
			&& (windowElement != windowElement.parent)
			&& (windowElement = windowElement.parent) != null && i < 10)

	var growl = windowElement.PF(name);

	if (growl != null) {
		if (arguments.length == 2)
			growl.renderMessage(arguments[1])
		else if (arguments.length == 4)
			growl.renderMessage({
				'summary' : arguments[1],
				'detail' : arguments[2],
				'severity' : arguments[3]
			});
		else{
			
		}
	}
}