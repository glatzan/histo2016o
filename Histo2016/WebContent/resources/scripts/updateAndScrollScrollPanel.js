/*
 * Array of id;;classToFocus
 */
function updateAndAutoScrollToSelectedElement() {

	if (arguments.length == 1)
		speed = 100;

	for (var i = 0; i < arguments.length; i++) {

		var res = arguments[i].split(";;");

		var primfcid = res[0].replace(':', '\\:');
		var idDataTbl = '#' + primfcid;

		$(idDataTbl).data('jsp').reinitialise();

		if (res.length == 2) {
			var element = $(idDataTbl).find('tr.' + res[1]);
			if (element.length > 0) {
				$(idDataTbl).data('jsp').scrollToElement(element, true)
			}
		}

	}
}
