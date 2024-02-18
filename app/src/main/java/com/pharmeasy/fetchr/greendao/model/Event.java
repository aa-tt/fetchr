package com.pharmeasy.fetchr.greendao.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.Property;

@Entity(nameInDb = "EVENT")
public class Event {
    @Index(unique = true)
    @Id(autoincrement = true)
    @Property(nameInDb = "_id")
    private Long _id;
    @Property(nameInDb = "action")
    private String action;
    @Property(nameInDb = "category")
    private String category;
    @Property(nameInDb = "meta")
    private String meta;
    @Property(nameInDb = "user")
    private String user;
    @Property(nameInDb = "createdOn")
    private String createdOn;
    @Property(nameInDb = "processed")
    private Integer processed;
    @Property(nameInDb = "referenceId")
    private String referenceId;
    @Property(nameInDb = "jsonData")
    private String jsonData;
    @Generated(hash = 261922390)
    public Event(Long _id, String action, String category, String meta, String user,
            String createdOn, Integer processed, String referenceId,
            String jsonData) {
        this._id = _id;
        this.action = action;
        this.category = category;
        this.meta = meta;
        this.user = user;
        this.createdOn = createdOn;
        this.processed = processed;
        this.referenceId = referenceId;
        this.jsonData = jsonData;
    }
    @Generated(hash = 344677835)
    public Event() {
    }
    public Long get_id() {
        return this._id;
    }
    public void set_id(Long _id) {
        this._id = _id;
    }
    public String getAction() {
        return this.action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public String getCategory() {
        return this.category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getMeta() {
        return this.meta;
    }
    public void setMeta(String meta) {
        this.meta = meta;
    }
    public String getUser() {
        return this.user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getCreatedOn() {
        return this.createdOn;
    }
    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }
    public Integer getProcessed() {
        return this.processed;
    }
    public void setProcessed(Integer processed) {
        this.processed = processed;
    }
    public String getReferenceId() {
        return this.referenceId;
    }
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    public String getJsonData() {
        return this.jsonData;
    }
    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    public static class Builder{

        private String action;
        private String category=null;
        private String meta=null;
        private String user=null;
        private String createdOn=null;
        private Integer processed=-1;
        private String referenceId=null;
        private String jsonData=null;

        public Builder(String action){
            this.action=action;
        }

        public Builder withCategory(String category){
            this.category=category;
            return this;
        }

        public Builder withMeta(String meta){
            this.meta=meta;
            return this;
        }

        public Builder withUser(String user){
            this.user=user;
            return this;
        }

        public Builder withCreatedOn(String createdOn){
            this.createdOn=createdOn;
            return this;
        }

        public Builder withProcessed(Integer processed){
            this.processed=processed;
            return this;
        }

        public Builder withReferenceId(String referenceId){
            this.referenceId=referenceId;
            return this;
        }

        public Builder withJsonData(String jsonData){
            this.jsonData=jsonData;
            return this;

        }


        public Event build(){
            Event event = new Event();
            event.action=this.action;
            event.category=this.category;
            event.meta=this.meta;
            event.user=this.user;
            event.createdOn=this.createdOn;
            event.processed=this.processed;
            event.referenceId=this.referenceId;
            event.jsonData=this.jsonData;

            return event;

        }
    }
}
