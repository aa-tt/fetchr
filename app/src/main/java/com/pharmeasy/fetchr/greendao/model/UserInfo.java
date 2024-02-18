package com.pharmeasy.fetchr.greendao.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;

@Entity(nameInDb="USER_INFO")
public class UserInfo {
    @NotNull
    @Property(nameInDb = "_id")
    private Long _id;
    @NotNull
    @Property(nameInDb = "status")
    private String status;
    @Property(nameInDb = "breakStarted")
    private int breakStarted;
    @Property(nameInDb = "scannerEnabled")
    private int scannerEnabled;
    @Generated(hash = 366205254)
    public UserInfo(@NotNull Long _id, @NotNull String status, int breakStarted,
            int scannerEnabled) {
        this._id = _id;
        this.status = status;
        this.breakStarted = breakStarted;
        this.scannerEnabled = scannerEnabled;
    }
    @Generated(hash = 1279772520)
    public UserInfo() {
    }
    public Long get_id() {
        return this._id;
    }
    public void set_id(Long _id) {
        this._id = _id;
    }
    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public int getBreakStarted() {
        return this.breakStarted;
    }
    public void setBreakStarted(int breakStarted) {
        this.breakStarted = breakStarted;
    }
    public int getScannerEnabled() {
        return this.scannerEnabled;
    }
    public void setScannerEnabled(int scannerEnabled) {
        this.scannerEnabled = scannerEnabled;
    }

}
