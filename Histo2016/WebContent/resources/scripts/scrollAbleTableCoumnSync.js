// closing overlay on return
function scrollAbleTableCoumnSync(id, scrollbarWidth) {
	// Change the selector if needed
	var $table = $(id);
	var $bodyCells = $table.find('tbody tr:first').children()
	// column with array
	var colWidth = $bodyCells.map(function() {
		return $(this).width();
	}).get();

	// extend last column
	colWidth[colWidth.length-1] = colWidth[colWidth.length-1] + scrollbarWidth;

	// Set the width of thead columns
	$table.find('thead tr').children().each(function(i, v) {
		$(v).width(colWidth[i]);
	});
}