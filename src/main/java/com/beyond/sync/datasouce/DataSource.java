package com.beyond.sync.datasouce;


import com.beyond.sync.Lock;
import com.beyond.sync.entity.SyncStamp;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public interface DataSource<T> extends Lock {

    void init();

    String getKey();

    void saveAll(List<T> tList, String... oppositeKeys) throws Exception;

    List<T> selectAll() throws Exception;

    boolean isChanged(DataSource<T> targetDataSource) throws Exception;

    List<T> getChangedData(SyncStamp syncStamp, @Nullable DataSource<T> targetDataSource) throws Exception;

    SyncStamp getLastSyncStamp(DataSource<T> targetDataSource) throws Exception;

    void updateLastSyncStamp(SyncStamp syncStamp, DataSource<T> targetDataSource) throws Exception;

    SyncStamp getLatestSyncStamp() throws Exception;

    void updateLatestSyncStamp(SyncStamp syncStamp) throws Exception;

    Class<T> clazz();

}
