package com.welshare.dao;

public class Paging {
    private int page;
    private int resultsPerPage;

    public Paging(int page, int resultsPerPage) {
        super();
        this.page = page;
        this.resultsPerPage = resultsPerPage;
    }

    public int getPage() {
        return page;
    }
    public void setPage(int page) {
        this.page = page;
    }
    public int getResultsPerPage() {
        return resultsPerPage;
    }
    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }
}
