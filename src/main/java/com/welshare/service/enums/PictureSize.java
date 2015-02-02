package com.welshare.service.enums;

public enum PictureSize {
    SMALL("_small"), LARGE("_large");

    private String suffix;

    private PictureSize(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }
}
