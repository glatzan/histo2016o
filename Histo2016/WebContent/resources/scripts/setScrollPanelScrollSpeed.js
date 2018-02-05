function setScrollPanelScrollSpeed(id, speed) {

	if (arguments.length == 1)
		speed = 100;

	var primfcid = id.replace(':', '\\:');
	var idDataTbl = '#' + primfcid;

	$(idDataTbl).jScrollPane({
		mouseWheelSpeed : speed
	});
}


// 	<!-- scrollspeed of scrollpane -->
// <script type="text/javascript">
// $(document).ready(function () {
// 	 setScrollPanelScrollSpeed("navigationForm:patientNavigationScroll");
// });
// </script>