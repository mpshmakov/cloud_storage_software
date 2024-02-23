package com.mycompany.javafxapplication1;

import java.util.Date;

public class recovery {

    private String filename;
    private String owner;
    private Date deletedDate;



    public recovery(String filename, String owner, Date deletedDate) {

        this.filename = filename;
        this.owner = owner;
        this.deletedDate = deletedDate;

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

    public Date getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
    }


}
