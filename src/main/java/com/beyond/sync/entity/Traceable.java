package com.beyond.sync.entity;

import java.util.Date;

public interface Traceable extends Element {

    Date getCreateTime();

    void setCreateTime(Date createTime);

    Date getLastModifyTime();

    void setLastModifyTime(Date lastModifyTime);

    Integer getVersion();

    void setVersion(Integer version);
}
