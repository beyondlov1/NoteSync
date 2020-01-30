package com.beyond.sync.datasouce;


import com.beyond.sync.datasouce.DataSource;
import com.beyond.sync.entity.SyncStamp;

import java.io.IOException;
import java.util.Map;

public interface MultiDataSource<T>  extends DataSource<T> {
    void setChosenKey(String key);
    String getChosenKey();
    void initLastSyncStamps() throws IOException;
    Map<String, SyncStamp> getSyncStampsCache();
    void setSyncStampsCache(Map<String, SyncStamp> syncStamps);
}
