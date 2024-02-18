package com.pharmeasy.fetchr.greendao.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;

@Entity(nameInDb = "TASK_ITEM")
public class TaskItem {
    @Id(autoincrement = true)
    @Property(nameInDb = "_id")
    private Long _id;
    @Property(nameInDb = "taskId")
    private Long taskId;
    @Property(nameInDb = "id")
    private String id;
    @NotNull
    @Property(nameInDb = "ucode")
    private String ucode;
    @NotNull
    @Property(nameInDb = "name")
    private String name;
    @Property(nameInDb = "packQuantity")
    private Integer packQuantity;
    @Property(nameInDb = "packForm")
    private String packForm;
    @Property(nameInDb = "batchNumber")
    private String batchNumber;
    @Property(nameInDb = "binId")
    private String binId;
    @Property(nameInDb = "barCode")
    private String barCode;
    @Property(nameInDb = "expiryDate")
    private String expiryDate;
    @Property(nameInDb = "status")
    private String status;
    @Property(nameInDb = "mrp")
    private Double mrp;
    @Property(nameInDb = "returnReason")
    private String returnReason;
    @Property(nameInDb = "trayId")
    private String trayId;
    @Property(nameInDb = "orderedQuantity")
    private Integer orderedQuantity;
    @Property(nameInDb = "processed")
    private Integer processed;
    @Property(nameInDb = "sync")
    private Integer sync;
    @Property(nameInDb = "quantity")
    private Integer quantity;
    @Property(nameInDb = "refrigerated")
    private String refrigerated;
    @Property(nameInDb = "batchId")
    private Long batchId;
    @Property(nameInDb = "createdTime")
    private Long createdTime;
    @Generated(hash = 959159634)
    public TaskItem(Long _id, Long taskId, String id, @NotNull String ucode,
            @NotNull String name, Integer packQuantity, String packForm, String batchNumber,
            String binId, String barCode, String expiryDate, String status, Double mrp,
            String returnReason, String trayId, Integer orderedQuantity, Integer processed,
            Integer sync, Integer quantity, String refrigerated, Long batchId,
            Long createdTime) {
        this._id = _id;
        this.taskId = taskId;
        this.id = id;
        this.ucode = ucode;
        this.name = name;
        this.packQuantity = packQuantity;
        this.packForm = packForm;
        this.batchNumber = batchNumber;
        this.binId = binId;
        this.barCode = barCode;
        this.expiryDate = expiryDate;
        this.status = status;
        this.mrp = mrp;
        this.returnReason = returnReason;
        this.trayId = trayId;
        this.orderedQuantity = orderedQuantity;
        this.processed = processed;
        this.sync = sync;
        this.quantity = quantity;
        this.refrigerated = refrigerated;
        this.batchId = batchId;
        this.createdTime = createdTime;
    }
    @Generated(hash = 910645620)
    public TaskItem() {
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
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getUcode() {
        return this.ucode;
    }
    public void setUcode(String ucode) {
        this.ucode = ucode;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getPackQuantity() {
        return this.packQuantity;
    }
    public void setPackQuantity(Integer packQuantity) {
        this.packQuantity = packQuantity;
    }
    public String getPackForm() {
        return this.packForm;
    }
    public void setPackForm(String packForm) {
        this.packForm = packForm;
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
    public String getExpiryDate() {
        return this.expiryDate;
    }
    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Double getMrp() {
        return this.mrp;
    }
    public void setMrp(Double mrp) {
        this.mrp = mrp;
    }
    public String getReturnReason() {
        return this.returnReason;
    }
    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }
    public String getTrayId() {
        return this.trayId;
    }
    public void setTrayId(String trayId) {
        this.trayId = trayId;
    }
    public Integer getOrderedQuantity() {
        return this.orderedQuantity;
    }
    public void setOrderedQuantity(Integer orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
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
    public void setSync(Integer sync) {
        this.sync = sync;
    }
    public Integer getQuantity() {
        return this.quantity;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public String getRefrigerated() {
        return this.refrigerated;
    }
    public void setRefrigerated(String refrigerated) {
        this.refrigerated = refrigerated;
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

    @Override
    public String toString() {
        return "TaskItem{" +
                "_id=" + _id +
                ", taskId=" + taskId +
                ", id='" + id + '\'' +
                ", ucode='" + ucode + '\'' +
                ", name='" + name + '\'' +
                ", packQuantity=" + packQuantity +
                ", packForm='" + packForm + '\'' +
                ", batchNumber='" + batchNumber + '\'' +
                ", binId='" + binId + '\'' +
                ", barCode='" + barCode + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", status='" + status + '\'' +
                ", mrp=" + mrp +
                ", returnReason='" + returnReason + '\'' +
                ", trayId='" + trayId + '\'' +
                ", orderedQuantity=" + orderedQuantity +
                ", processed=" + processed +
                ", sync=" + sync +
                ", quantity=" + quantity +
                ", refrigerated='" + refrigerated + '\'' +
                ", batchId=" + batchId +
                ", createdTime=" + createdTime +
                '}';
    }

}
