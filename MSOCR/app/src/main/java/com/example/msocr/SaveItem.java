package com.example.msocr;

public class SaveItem {

    private byte[] image;
    private String text;
    private String trans;

    public byte[] getImage() {
        return image;
    }

    public String getText1() {
        return text;
    }

    public String getText2() {
        return trans;
    }

    public SaveItem(byte[] image, String text, String trans) {
        this.image = image;
        this.text = text;
        this.trans = trans;
    }
}
