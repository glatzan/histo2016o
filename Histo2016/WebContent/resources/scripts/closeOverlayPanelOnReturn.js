// closing overlay on return
function closeOverlayPanelOnReturn(e, id) {
	if (e.keyCode == 13) {
		PF(id).hide();
		return false;
	}
}