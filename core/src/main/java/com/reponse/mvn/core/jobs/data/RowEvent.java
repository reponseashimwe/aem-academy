package com.reponse.mvn.core.jobs.data;

public final class RowEvent {
    public final int    row;
    public final String title;
    public final String status;
    public final String message;

    public RowEvent(int row, String title, String status, String message) {
        this.row     = row;
        this.title   = title;
        this.status  = status;
        this.message = message;
    }
}
