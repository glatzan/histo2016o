/**
 * disable button, args... widgetVar
 * 
 * @param disable
 * @returns
 */
function disableButton(disable) {

	for (var i = 1; i < arguments.length; i++) {
		if (disable)
			PF(arguments[i]).disable();
		else
			PF(arguments[i]).enable();
	}

}

/**
 * Displays a growl message,even from a diaglo
 * 
 * @param name
 * @returns
 */
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
		else {

		}
	}
}

/**
 * Sets the scrollspeet to an custom scrollbar
 * 
 * @param id
 * @param speed
 * @returns
 */
function setScrollPanelScrollSpeed(id, speed) {

	if (arguments.length == 1)
		speed = 100;

	var idDataTbl = getAlteredID(id);

	$(idDataTbl).jScrollPane({
		mouseWheelSpeed : speed
	});
}

/**
 * function is triggered by the backend to show a dialog, workaround
 * 
 * @param btn
 * @returns
 */
function clickButtonFromBean(btn) {
	$(getAlteredID(btn)).click();
}

/**
 * function is executed by backend, workaround
 * 
 * @param btn
 * @returns
 */
function executeFunctionFromBean(btn) {
	btn();
}

/**
 * Generates a pf id form a normal id text:test
 * 
 * @param str
 * @returns
 */
function getAlteredID(str) {
	// for selection in JQuery the ids with : must be endoded with \\:
	var primfcid = str.replace(':', '\\:');
	var idDataTbl = '#' + primfcid;

	return idDataTbl;
}