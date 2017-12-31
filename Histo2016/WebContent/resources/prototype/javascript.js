// disable loginbutton 
function disableButton(btn, disable) {
	if (disable)
		PF(btn).disable();
	else
		PF(btn).enable();

}

// sets the position to the top of the p:selectCheckboxMenu
(function() {
	PrimeFaces.widget.SelectCheckboxMenu.prototype.alignPanel = function() {
		var fixedPosition = this.panel.css('position') == 'fixed', win = $(window), positionOffset = fixedPosition ? '-'
				+ win.scrollLeft() + ' -' + win.scrollTop()
				: null, panelStyle = this.panel.attr('style');

		this.panel.css({
			'left' : '',
			'top' : '',
			'z-index' : ++PrimeFaces.zindex
		});

		if (this.panel.parent().attr('id') === this.id) {
			this.panel.css({
				left : 0,
				top : this.jq.innerHeight()
			});
		} else {
			this.panel.position({
				my : 'left bottom',
				at : 'left top',
				of : this.jq,
				offset : positionOffset
			});
		}

	};
})();


// show object methodes
var keys = Object.keys(myObject);