package org.histo.ui;

public class Page {

    private String name;
    private String page;

    public Page(String name, String page){
	this.name = name;
	this.page = page;
    }
    
    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getPage() {
	return page;
    }

    public void setPage(String page) {
	this.page = page;
    }

}
