package com.mycompany.javafxapplication1;

public class UserAcl {

    private String filename;
    private String owner;
    private int readable;
    private int writable;


    public UserAcl(String filename, String owner, int readable, int writable) {

        this.filename = filename;
        this.owner = owner;
        this.readable = readable;
        this.writable = writable;
    }





    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getReadable() {
        return readable;
    }

    public void setReadable(int readable) {
        this.readable = readable;
    }

    public int getWritable() {
        return writable;
    }

    public void setWritable(int writable) {
        this.writable = writable;
    }
}
