package com.beyond.sync.datasouce.sql;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class TraceLog {
    public static final String UPDATE = "update";
    public static final String ADD = "add";

    @Id
    private String id;
    private String documentId;
    private String refPath;
    private String refServer;
    private String operation;
    private Date operationTime;
    private Date createTime;
    private String source;
    private String type;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getRefPath() {
        return refPath;
    }

    public void setRefPath(String refPath) {
        this.refPath = refPath;
    }

    public String getRefServer() {
        return refServer;
    }

    public void setRefServer(String refServer) {
        this.refServer = refServer;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Date getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(Date operationTime) {
        this.operationTime = operationTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
