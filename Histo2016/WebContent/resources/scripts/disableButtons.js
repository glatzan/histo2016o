// disable loginbutton 
function disableButton(disable) {
	
	for(var i = 1; i < arguments.length; i++) {
		if (disable)
			PF(arguments[i]).disable();
		else
			PF(arguments[i]).enable();
    }
	
}