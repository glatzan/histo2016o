// funktion is triggert by the backend to show a dialog, workaround
function clickButtonFromBean(btn) {
	$(getAlteredID(btn)).click();
}

function executeFunctionFromBean(btn) {
	btn();
}

// formID:elementID
function getAlteredID(str) {
	// for selection in JQuery the ids with : must be endoded with \\:
	var primfcid = str.replace(':', '\\:');
	var idDataTbl = '#' + primfcid;

	return idDataTbl;
}