package com.beyond.sync.datasouce.sql;

import com.beyond.sync.datasouce.DataSource;
import com.beyond.sync.datasouce.MultiDataSource;
import com.beyond.sync.datasouce.webdav.support.stamp.SyncStampModel;
import com.beyond.sync.entity.SyncStamp;
import com.beyond.sync.entity.Traceable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class DefaultSqlDataSource<T extends Traceable> implements MultiDataSource<T> {

    private String chosenKey;

    private Map<String, SyncStamp> syncStamps = new LinkedHashMap<>();

    private TraceableModel<T> traceableModel;

    private SyncStampModel baseSyncStampModel;

    private SqlLogModel sqlLogModel;

    private Class<T> clazz;

    private String myKey;

    public DefaultSqlDataSource(TraceableModel<T> traceableModel,
                                SyncStampModel baseSyncStampModel,
                                SqlLogModel sqlLogModel,
                                Class<T> clazz,
                                String myKey) {

        this.traceableModel = traceableModel;
        this.baseSyncStampModel = baseSyncStampModel;
        this.sqlLogModel = sqlLogModel;
        this.clazz = clazz;
        this.myKey = myKey;
        init();
    }

    @Override
    public void setChosenKey(String key) {
        this.chosenKey = key;
    }

    @Override
    public String getChosenKey() {
        return chosenKey;
    }

    @Override
    public void initLastSyncStamps() throws IOException {
        syncStamps = baseSyncStampModel.findAllConnectMe();
    }

    @Override
    public Map<String, SyncStamp> getSyncStampsCache() {
        return syncStamps;
    }

    @Override
    public void setSyncStampsCache(Map<String, SyncStamp> syncStamps) {
        this.syncStamps = syncStamps;
    }

    @Override
    public void init() {

    }

    @Override
    public String getKey() {
        return myKey;
    }

    @Override
    public void saveAll(List<T> ts, String... oppositeKeys) throws Exception {
        Map<String, T> map = new HashMap<>(ts.size());
        for (T t : ts) {
            map.put(t.getId(), t);
        }
        List<T> tList = traceableModel.findAllById(map.keySet());
        Map<String, T> localMap = new HashMap<>(tList.size());

        for (T localNote : tList) {
            localMap.put(localNote.getId(), localNote);
        }

        List<T> addList = new ArrayList<>();
        List<T> updateList = new ArrayList<>();
        for (String id : map.keySet()) {
            if (localMap.containsKey(id)) {
                if (map.get(id).getLastModifyTime().after(localMap.get(id).getLastModifyTime())
                        || (map.get(id).getVersion() == null ? 0 : map.get(id).getVersion())
                        > (localMap.get(id).getVersion() == null ? 0 : localMap.get(id).getVersion())) {
                    updateList.add(map.get(id));
                }
            } else {
                addList.add(map.get(id));
            }
        }
        traceableModel.addAll(addList);
        traceableModel.updateAll(updateList);
    }

    @Override
    public List<T> selectAll() throws Exception {
        return traceableModel.findAll();
    }

    @Override
    public boolean isChanged(DataSource<T> targetDataSource) throws IOException {
        return !getChangedData(getLastSyncStamp(targetDataSource),targetDataSource).isEmpty();
    }

    @Override
    public List<T> getChangedData(SyncStamp syncStamp, @Nullable DataSource<T> targetDataSource) throws IOException {
        // 日志的操作时间大于上次同步成功的最后修改时间
        // 会改变lastModifyTime类型的, 如 add, update
        List<TraceLog> lastModifyTimeChangeableLogs = sqlLogModel.getAllWhereOperationTimeAfter(
                syncStamp.getLastModifyTime() == null ? new Date(0) : syncStamp.getLastModifyTime());

        // 日志的操作时间大于上次同步成功的同步开始时间 , 并且source是不是对方dataSource的
        // 改变priority这种不更改lastModifyTime的, 如 改变priority
        List<TraceLog> lastModifyTimeUnchangeableLogs = null;
        if (targetDataSource != null){
            lastModifyTimeUnchangeableLogs = sqlLogModel.getAllWithoutSourceWhereCreateTimeAfter(
                    syncStamp.getLastSyncTimeStart() == null ? new Date(0) : syncStamp.getLastSyncTimeStart(),
                    targetDataSource.getKey());
        }
        List<TraceLog> modifiedLogs = new ArrayList<>();
        modifiedLogs.addAll(lastModifyTimeChangeableLogs);
        if (lastModifyTimeUnchangeableLogs!=null){
            modifiedLogs.addAll(lastModifyTimeUnchangeableLogs);
        }
        if (modifiedLogs.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> ids = new HashSet<>(modifiedLogs.size());
        for (TraceLog modifiedLog : modifiedLogs) {
            ids.add(modifiedLog.getDocumentId());
        }

        return traceableModel.findAllById(new ArrayList<>(ids));
    }

    @Override
    public SyncStamp getLastSyncStamp(DataSource<T> targetDataSource) throws IOException {
        return baseSyncStampModel.retrieve(targetDataSource.getKey());
    }

    @Override
    public void updateLastSyncStamp(SyncStamp syncStamp, DataSource<T> targetDataSource) throws IOException {
        baseSyncStampModel.update(syncStamp,targetDataSource.getKey());
    }

    @Override
    public SyncStamp getLatestSyncStamp() throws Exception {
        List<T> list = selectAll();
        if (list != null && !list.isEmpty()) {
            Collections.sort(list, new Comparator<T>() {
                @Override
                public int compare(T o1, T o2) {
                    return (int) (o1.getLastModifyTime().getTime() - o2.getLastModifyTime().getTime());
                }
            });
            T latestNote = list.get(list.size() - 1);
            return SyncStamp.create(latestNote.getLastModifyTime(), latestNote.getLastModifyTime());
        } else {
            return SyncStamp.ZERO;
        }
    }

    @Override
    public void updateLatestSyncStamp(SyncStamp syncStamp) throws IOException {
        //do nothing
    }

    @Override
    public Class<T> clazz() {
        return clazz;
    }

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public boolean tryLock(Long time) {
        return true;
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean release() {
        return true;
    }
}
