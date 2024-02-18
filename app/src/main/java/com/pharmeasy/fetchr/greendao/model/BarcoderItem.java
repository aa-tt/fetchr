package com.pharmeasy.fetchr.greendao.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;

@Entity(nameInDb = "BARCODER_ITEM")
public class BarcoderItem {
    @Id(autoincrement = true)
    @Property(nameInDb = "_id")
    private Long _id;
    @Property(nameInDb = "taskId")
    private Long taskId;
    @NotNull
    @Property(nameInDb = "ucode")
    private String ucode;
    @Property(nameInDb = "batchNumber")
    private String batchNumber;
    @Property(nameInDb = "binId")
    private String binId;
    @Property(nameInDb = "packForm")
    private String packForm;
    @Property(nameInDb = "barCode")
    private String barCode;
    @Property(nameInDb = "status")
    private String status;
    @Property(nameInDb = "returnReason")
    private String returnReason;
    @Property(nameInDb = "trayId")
    private String trayId;
    @Property(nameInDb = "processed")
    private Integer processed;
    @Property(nameInDb = "expiryDate")
    private String expiryDate;
    @Property(nameInDb = "mrp")
    private Double mrp;
    @Property(nameInDb = "sync")
    private Integer sync;
    @Property(nameInDb = "quantity")
    private Integer quantity;
    private Long batchId;
    @Property(nameInDb = "createdTime")
    private Long createdTime;
    @NotNull
    @Property(nameInDb = "name")
    private String name;
    @Generated(hash = 1864736937)
    public BarcoderItem(Long _id, Long taskId, @NotNull String ucode, String batchNumber, String binId,
            String packForm, String barCode, String status, String returnReason, String trayId,
            Integer processed, String expiryDate, Double mrp, Integer sync, Integer quantity,
            Long batchId, Long createdTime, @NotNull String name) {
        this._id = _id;
        this.taskId = taskId;
        this.ucode = ucode;
        this.batchNumber = batchNumber;
        this.binId = binId;
        this.packForm = packForm;
        this.barCode = barCode;
        this.status = status;
        this.returnReason = returnReason;
        this.trayId = trayId;
        this.processed = processed;
        this.expiryDate = expiryDate;
        this.mrp = mrp;
        this.sync = sync;
        this.quantity = quantity;
        this.batchId = batchId;
        this.createdTime = createdTime;
        this.name = name;
    }
    @Generated(hash = 663463657)
    public BarcoderItem() {
    }
    public Long get_id() {
        return this._id;
    }
    public void set_id(Long _id) {
        this._id = _id;
    }
    public Long getTaskId() {
        return this.taskId;
    }
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    public String getUcode() {
        return this.ucode;
    }
    public void setUcode(String ucode) {
        this.ucode = ucode;
    }
    public String getBatchNumber() {
        return this.batchNumber;
    }
    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }
    public String getBinId() {
        return this.binId;
    }
    public void setBinId(String binId) {
        this.binId = binId;
    }
    public String getBarCode() {
        return this.barCode;
    }
    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }
    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getReturnReason() { return this.returnReason; }
    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }
    public String getTrayId() {
        return this.trayId;
    }
    public void setTrayId(String trayId) {
        this.trayId = trayId;
    }
    public Integer getProcessed() {
        return this.processed;
    }
    public void setProcessed(Integer processed) {
        this.processed = processed;
    }
    public Integer getSync() {
        return this.sync;
    }
    public void setSync(Integer sync) { this.sync = sync; }
    public Integer getQuantity() { return this.quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public Long getBatchId() {
        return this.batchId;
    }
    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }
    public Long getCreatedTime() {
        return this.createdTime;
    }
    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }
    public String getExpiryDate() {
        return expiryDate;
    }
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
    public Double getMrp() {
        return this.mrp;
    }
    public void setMrp(Double mrp) {
        this.mrp = mrp;
    }
    public String getPackForm() {
        return this.packForm;
    }
    public void setPackForm(String packForm) {
        this.packForm = packForm;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
