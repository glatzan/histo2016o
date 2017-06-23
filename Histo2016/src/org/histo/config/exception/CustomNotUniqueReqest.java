package org.histo.config.exception;

public class CustomNotUniqueReqest extends RuntimeException {
	
	private static final long serialVersionUID = -4899647186394820187L;

	private boolean closeDialog;
	
	public CustomNotUniqueReqest() {
		this(false);
	}
	
	public CustomNotUniqueReqest(boolean closeDialog) {
		super("Not unique reqest!");
		setCloseDialog(closeDialog);
	}
	
	public boolean isCloseDialog() {
		return closeDialog;
	}
	public void setCloseDialog(boolean closeDialog) {
		this.closeDialog = closeDialog;
	}
}
