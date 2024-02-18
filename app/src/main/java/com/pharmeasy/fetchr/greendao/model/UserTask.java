package com.pharmeasy.fetchr.greendao.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Property;

@Entity(nameInDb = "USER_TASK")
public class UserTask {
    @Index(unique = true)
    @Id(autoincrement = true)
    @Property(nameInDb = "_id")
    private Long _id=null;

    @NotNull
    @Property(nameInDb = "uid")
    private String uid=null;

    @NotNull
    @Property(nameInDb = "type")
    private String type=null;
    @Property(nameInDb = "taskId")
    private Long taskId;
    @Property(nameInDb = "trayId")
    private String trayId=null;
    @Property(nameInDb = "issueTrayId")
    private String issueTrayId=null;
    @Property(nameInDb = "reference")
    private String reference=null;
    @Property(nameInDb = "referenceType")
    private String referenceType=null;

    @NotNull
    @Property(nameInDb = "status")
    private String status=null;
    @Property(nameInDb = "dnd")
    private Integer dnd=0;

    @Property(nameInDb = "source")
    private String source=null;
    @Property(nameInDb = "ucode")
    private String ucode=null;
    @Property(nameInDb = "name")
    private String name=null;
    @Property(nameInDb = "binId")
    private String binId=null;
    @Property(nameInDb = "trayPickedZone")
    private String trayPickedZone = null;
    @Property(nameInDb = "taskType")
    private String taskType = null;
    @Property(nameInDb = "nearExpiry")
    private Boolean nearExpiry = false;

    @Generated(hash = 1465624816)
    public UserTask(Long _id, @NotNull String uid, @NotNull String type,
            Long taskId, String trayId, String issueTrayId, String reference,
            String referenceType, @NotNull String status, Integer dnd,
            String source, String ucode, String name, String binId,
            String trayPickedZone, String taskType, Boolean nearExpiry) {
        this._id = _id;
        this.uid = uid;
        this.type = type;
        this.taskId = taskId;
        this.trayId = trayId;
        this.issueTrayId = issueTrayId;
        this.reference = reference;
        this.referenceType = referenceType;
        this.status = status;
        this.dnd = dnd;
        this.source = source;
        this.ucode = ucode;
        this.name = name;
        this.binId = binId;
        this.trayPickedZone = trayPickedZone;
        this.taskType = taskType;
        this.nearExpiry = nearExpiry;
    }
    @Generated(hash = 841106868)
    public UserTask() {
    }
    public Long get_id() {
        return this._id;
    }
    public void set_id(Long _id) {
        this._id = _id;
    }
    public String getUid() {
        return this.uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public String getType() {
        return this.type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public Long getTaskId() {
        return this.taskId;
    }
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    public String getTrayId() {
        return this.trayId;
    }
    public void setTrayId(String trayId) {
        this.trayId = trayId;
    }
    public String getIssueTrayId() {
        return this.issueTrayId;
    }
    public void setIssueTrayId(String issueTrayId) {
        this.issueTrayId = issueTrayId;
    }
    public String getReference() {
        return this.reference;
    }
    public void setReference(String reference) {
        this.reference = reference;
    }
    public String getReferenceType() {
        return this.referenceType;
    }
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Integer getDnd() {
        return this.dnd;
    }
    public void setDnd(Integer dnd) {
        this.dnd = dnd;
    }
    public String getSource() {
        return this.source;
    }
    public void setSource(String source) {
        this.source = source;
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
    public String getBinId() {
        return this.binId;
    }
    public void setBinId(String binId) {
        this.binId = binId;
    }
    public String getTrayPickedZone() {
        return this.trayPickedZone;
    }
    public void setTrayPickedZone(String trayPickedZone) {
        this.trayPickedZone = trayPickedZone;
    }

    public String getTaskType() {
        return this.taskType;
    }
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    public Boolean getIsNearExpiry() {
        return this.nearExpiry;
    }
    public void setIsNearExpiry(Boolean isNearExpiry) {
        this.nearExpiry = isNearExpiry;
    }
    public Boolean getNearExpiry() {
        return this.nearExpiry;
    }
    public void setNearExpiry(Boolean nearExpiry) {
        this.nearExpiry = nearExpiry;
    }

    public static class Builder{
        private Long _id=null;
        private String uid=null;
        private String type=null;
        private Long taskId;
        private String trayId=null;
        private String issueTrayId=null;
        private String reference=null;
        private String referenceType=null;
        private String status=null;
        private Integer dnd=0;
        private String source=null;
        private String ucode=null;
        private String name=null;
        private String binId=null;
        private String trayPickedZone=null;
        private String taskType=null;
        private Boolean nearExpiry = false;

        public Builder(String uid){
            this.uid=uid;
        }

        public Builder withType(String type){
            this.type=type;
            return this;
        }

        public Builder withTaskId(Long taskId){
            this.taskId=taskId;
            return this;
        }

        public Builder withTrayId(String trayId){
            this.trayId=trayId;
            return this;
        }

        public Builder withIssueTrayId(String issueTrayId){
            this.issueTrayId=issueTrayId;
            return this;
        }

        public Builder withReference(String reference){
            this.reference=reference;
            return this;
        }

        public Builder withReferenceType(String referenceType){
            this.referenceType=referenceType;
            return this;
        }

        public Builder withStatus(String status){
            this.status=status;
            return this;

        }

        public Builder withDnd(Integer dnd){
            this.dnd=dnd;
            return this;
        }

        public Builder withSource(String source){
            this.source=source;
            return this;
        }

        public Builder withUcode(String ucode){
            this.ucode=ucode;
            return this;
        }

        public Builder withName(String name){
            this.name=name;
            return this;
        }
        public Builder withBinId(String binId){
            this.binId=binId;
            return this;
        }

        public Builder withTrayPickedZone(String trayPickedZone){
            this.trayPickedZone=trayPickedZone;
            return this;
        }

        public Builder withTaskType(String taskType){
            this.taskType=taskType;
            return this;
        }

        public Builder withNearExpiry(boolean nearExpiry){
            this.nearExpiry=nearExpiry;
            return this;
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "_id=" + _id +
                    ", uid='" + uid + '\'' +
                    ", type='" + type + '\'' +
                    ", taskId=" + taskId +
                    ", trayId='" + trayId + '\'' +
                    ", issueTrayId='" + issueTrayId + '\'' +
                    ", reference='" + reference + '\'' +
                    ", referenceType='" + referenceType + '\'' +
                    ", status='" + status + '\'' +
                    ", dnd=" + dnd +
                    ", source='" + source + '\'' +
                    ", ucode='" + ucode + '\'' +
                    ", name='" + name + '\'' +
                    ", binId='" + binId + '\'' +
                    ", trayPickedZone='" + trayPickedZone + '\'' +
                    ", taskType='" + taskType + '\'' +
                    ", nearExpiry=" + nearExpiry +
                    '}';
        }

        public UserTask build(){
            UserTask userTask = new UserTask();
            userTask.uid=this.uid;
            userTask.type=this.type;
            userTask.taskId=this.taskId;
            userTask.trayId=this.trayId;
            userTask.issueTrayId=this.issueTrayId;
            userTask.reference=this.reference;
            userTask.referenceType=this.referenceType;
            userTask.status=this.status;
            userTask.dnd=this.dnd;
            userTask.source=this.source;
            userTask.ucode=this.ucode;
            userTask.name=this.name;
            userTask.binId=this.binId;
            userTask.trayPickedZone=this.trayPickedZone;
            userTask.taskType=this.taskType;
            userTask.nearExpiry=this.nearExpiry;

            return userTask;
        }
    }
}
