package com.beyond.sync.entity;


import com.beyond.sync.utils.IDUtil;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class SyncState {

    public static final Integer NONE = 0;
    public static final Integer SUCCESS = 1;
    public static final Integer FAIL = 2;

    @Id
    private String id;
    private String documentId;
    private String local;
    private String server;
    private String type;
    private Integer state;

    public static SyncState create(){
        SyncState syncState = new SyncState();
        syncState.setId(IDUtil.uuid());
        syncState.setState(SUCCESS);
        return syncState;
    }

    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getDocumentId() {
        return this.documentId;
    }
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    public String getServer() {
        return this.server;
    }
    public void setServer(String server) {
        this.server = server;
    }
    public Integer getState() {
        return this.state;
    }
    public void setState(Integer state) {
        this.state = state;
    }
    public String getLocal() {
        return this.local;
    }
    public void setLocal(String local) {
        this.local = local;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @Override
    public String toString() {
        return "SyncState{" +
                "id='" + id + '\'' +
                ", documentId='" + documentId + '\'' +
                ", local='" + local + '\'' +
                ", server='" + server + '\'' +
                ", type='" + type + '\'' +
                ", state=" + state +
                '}';
    }
}
