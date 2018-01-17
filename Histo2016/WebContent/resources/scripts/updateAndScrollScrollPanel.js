function updateAndAutoScrollToSelectedElement(idOfScrollPane) {
	updateAndAutoScrollToSelectedElement(idOfScrollPane, null)
}

function updateAndAutoScrollToSelectedElement(idOfScrollPane,
		classOfSelectedElement) {

	var primfcid = idOfScrollPane.replace(':', '\\:');
	var idDataTbl = '#' + primfcid;

	$(idDataTbl).data('jsp').reinitialise();

	if (classOfSelectedElement != null) {
		$(idDataTbl).data('jsp').scrollToElement(
				$(idDataTbl).find('tr.' + classOfSelectedElement), true)
	}
}
