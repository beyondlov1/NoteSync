package com.beyond.sync.datasouce.webdav;


import com.beyond.sync.Lock;
import com.beyond.sync.SyncStateManager;
import com.beyond.sync.datasouce.DataSource;
import com.beyond.sync.datasouce.MultiDataSource;
import com.beyond.sync.datasouce.webdav.support.filter.AfterModifiedTimeDavFilter;
import com.beyond.sync.datasouce.webdav.support.DavClient;
import com.beyond.sync.datasouce.webdav.support.filter.DavFilter;
import com.beyond.sync.datasouce.webdav.support.filter.PrefixDavFilter;
import com.beyond.sync.datasouce.webdav.support.lock.DavLock;
import com.beyond.sync.datasouce.webdav.support.path.DavPathStrategy;
import com.beyond.sync.datasouce.webdav.support.path.UuidDavPathStrategy;
import com.beyond.sync.datasouce.webdav.support.stamp.DavBaseSyncStampModel;
import com.beyond.sync.datasouce.webdav.support.stamp.DavLatestSyncStampModel;
import com.beyond.sync.datasouce.webdav.support.stamp.SyncStampModel;
import com.beyond.sync.entity.SyncStamp;
import com.beyond.sync.entity.SyncState;
import com.beyond.sync.entity.Traceable;
import com.beyond.sync.utils.JsonUtil;
import com.beyond.sync.utils.OkWebDavUtil;
import com.beyond.sync.utils.SyncUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.beyond.sync.constant.WebDavConst.DAV_STAMP_DIR;

/**
 * @author: beyond
 * @date: 2019/8/9
 */

public class DefaultMultiDavDataSource<T extends Traceable> implements MultiDataSource<T> {

    private String chosenKey;

    private Map<String, SyncStamp> syncStamps = new LinkedHashMap<>();

    private ExecutorService executorService;

    private DavPathStrategy davPathStrategy;

    private Lock lock;

    private SyncStampModel baseSyncStampModel;

    private SyncStampModel latestSyncStampModel;

    // ----------- required ---------- //

    private DavClient davClient;

    private DavDataSourceProperty property;

    private Class<T> clazz;

    private SyncStateManager syncStateManager;

    public DefaultMultiDavDataSource(DavClient davClient,
                                     DavDataSourceProperty property,
                                     Class<T> clazz,
                                     SyncStateManager syncStateManager) {
        this.davClient = davClient;
        this.property = property;
        this.clazz = clazz;
        this.syncStateManager = syncStateManager;
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
        final String prefix = property.getBaseSyncStampFilePrefix();
        List<String> urls = davClient.listAllFileUrl(
                OkWebDavUtil.concat(property.getServer(), clazz().getSimpleName().toUpperCase(), DAV_STAMP_DIR),
                new PrefixDavFilter(prefix));
        if (urls.isEmpty()) {
            syncStamps.clear();
            chosenKey = null;
        }
        SyncUtils.blockExecute(executorService,
                new SyncUtils.ParamCallable<String, SyncStamp>() {
                    @Override
                    public SyncStamp call(String url) throws Exception {
                        return JsonUtil.decode(davClient.get(url), SyncStamp.class);
                    }
                }, new SyncUtils.Handler<String, SyncStamp>() {
                    @Override
                    public void handle(String param, SyncStamp result) throws IOException {
                        String key = StringUtils.substring(param, param.indexOf(prefix) + 11,
                                param.lastIndexOf("."));
                        key = SyncUtils.base64Decode(key);
                        syncStamps.put(key, result);
                    }
                }, null, urls);
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
        if (property.isNeedExecutorService()) {
            executorService = new ThreadPoolExecutor(
                    17, 60,
                    60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>());
        }

        this.baseSyncStampModel = new DavBaseSyncStampModel(davClient, property, clazz);
        this.latestSyncStampModel = new DavLatestSyncStampModel(davClient, property, clazz);
        this.davPathStrategy = new UuidDavPathStrategy(property.getServer(), clazz);
        this.lock = new DavLock(davClient,
                OkWebDavUtil.concat(property.getServer(),
                        clazz.getSimpleName().toUpperCase(),
                        property.getLockDir(),
                        clazz.getSimpleName().toUpperCase() + ".lock"),
                getChosenKey()); // FIXME?
    }


    @Override
    public String getKey() {
        return property.getServer();
    }

    @Override
    public void saveAll(List<T> ts, String... oppositeKeys) throws Exception {
        if (executorService == null) {
            singleThreadSaveAll(ts, oppositeKeys);
            return;
        }
        multiThreadSaveAll(ts, oppositeKeys);
    }

    private void multiThreadSaveAll(List<T> ts, final String[] oppositeKeys) throws Exception {
        mkDirForMultiThread();
        CompletionService<T> completionService = new ExecutorCompletionService<>(executorService);
        for (final T t : ts) {
            completionService.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    save(t, oppositeKeys);
                    return t;
                }
            });
        }

        try {
            for (int i = 0; i < ts.size(); i++) {
                Future<T> future = completionService.take();
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void mkDirForMultiThread() {
        String[] paths = davPathStrategy.getAllStorageDirs(clazz().getSimpleName().toLowerCase());
        for (String path : paths) {
            davClient.mkDirQuietly(OkWebDavUtil.concat(property.getServer(), path));
            davClient.mkDirQuietly(OkWebDavUtil.concat(property.getServer(), path, "FILES"));
        }
    }

    private void singleThreadSaveAll(List<T> ts, String[] oppositeKey) throws Exception {
        for (T t : ts) {
            try {
                save(t, oppositeKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void save(T t, String[] oppositeKeys) throws IOException {
        if (davClient.exists(getDocumentUrl(t))) {
            T remoteT = JsonUtil.decode((davClient.get(getDocumentUrl(t))), clazz());
            if (t.getLastModifyTime().after(remoteT.getLastModifyTime())
                    || (t.getVersion() == null ? 0 : t.getVersion()) > (remoteT.getVersion() == null ? 0 : remoteT.getVersion())) {
                update(t);
            }
        } else {
            add(t);
        }

        saveSuccessState(t, oppositeKeys);
    }

    private String getDocumentUrl(T t) {
        return OkWebDavUtil.concat(
                OkWebDavUtil.concat(property.getServer(),
                        davPathStrategy.getStorageDir(t.getId())),
                t.getId());
    }

    protected void saveSuccessState(T t, String[] oppositeKeys) {

        List<SyncState> syncStateInfos = new ArrayList<>(oppositeKeys.length);
        for (String oppositeKey : oppositeKeys) {
            SyncState syncStateInfo = SyncState.create();
            syncStateInfo.setDocumentId(t.getId());
            syncStateInfo.setLocal(oppositeKey);
            syncStateInfo.setServer(getKey());
            syncStateInfo.setState(1);
            syncStateInfo.setType(clazz().getSimpleName().toLowerCase());
            syncStateInfos.add(syncStateInfo);
        }

        syncStateManager.saveAll(syncStateInfos);
    }

    protected void add(T t) throws IOException {
        davClient.put(getDocumentUrl(t), JsonUtil.encode(t, clazz()));
    }

    protected void update(T t) throws IOException {
        davClient.put(getDocumentUrl(t), JsonUtil.encode(t, clazz()));
    }

    @Override
    public List<T> selectAll() throws Exception {
        return selectByModifiedDate(null);
    }

    @Override
    public boolean isChanged(DataSource<T> targetDataSource) throws IOException {
        SyncStamp baseSyncStamp = baseSyncStampModel.retrieve(targetDataSource.getKey());
        SyncStamp latestSyncStamp = getLatestSyncStamp();

        Date correspondLastModifyTime = baseSyncStamp.getLastModifyTime();
        Date latestLastModifyTime = latestSyncStamp.getLastModifyTime();
        Date correspondLastSyncTimeEnd = baseSyncStamp.getLastSyncTimeEnd();
        Date latestLastSyncTimeEnd = latestSyncStamp.getLastSyncTimeEnd();

        return !(DateUtils.isSameInstant(correspondLastModifyTime, latestLastModifyTime)
                && DateUtils.isSameInstant(correspondLastSyncTimeEnd, latestLastSyncTimeEnd));
    }

    @Override
    public List<T> getChangedData(SyncStamp syncStamp, @Nullable DataSource<T> targetDataSource) throws IOException {
        return selectByModifiedDate(syncStamp.getLastSyncTimeEnd());
    }

    private List<T> selectByModifiedDate(Date date) throws IOException {

        DavFilter davFilter = null;
        if (date != null) {
            davFilter = new AfterModifiedTimeDavFilter(date);
        }

        List<String> modifiedIds = getModifiedIds(davFilter);
        /**
         * 单线程方法
         */
        if (executorService == null) {
            return singleThreadRetrieve(modifiedIds);
        }

        /**
         * 多线程方法
         */
        return multiThreadRetrieve(modifiedIds);
    }

    private List<String> getModifiedIds(DavFilter davFilter) throws IOException {
        List<String> ids = new ArrayList<>();
        String[] allPaths = davPathStrategy.getAllStorageDirs(clazz().getSimpleName().toLowerCase());
        for (String path : allPaths) {
            ids.addAll(davClient.listAllFileName(OkWebDavUtil.concat(property.getServer(), path), davFilter));
        }
        return ids;
    }

    private List<T> multiThreadRetrieve(List<String> ids) throws IOException {
        List<Future<T>> resultFutures = new ArrayList<>();
        for (final String id : ids) {
            Future<T> future = executorService.submit(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    if (id.contains(".")) {
                        return null;
                    }
                    T t = clazz().newInstance();
                    t.setId(id);
                    return select(t);
                }
            });
            resultFutures.add(future);
        }

        List<T> result = new ArrayList<>();
        try {
            for (Future<T> future : resultFutures) {
                T t = future.get();
                if (t != null) {
                    result.add(t);
                }
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<T> singleThreadRetrieve(List<String> ids) throws IOException {
        List<T> result = new ArrayList<>();
        for (String id : ids) {
            if (id.contains(".")) {
                continue;
            }
            try {
                T t = clazz().newInstance();
                t.setId(id);
                result.add(select(t));
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private T select(T t) throws IOException {
        return JsonUtil.decode(davClient.get(getDocumentUrl(t)), clazz());
    }


    @Override
    public SyncStamp getLastSyncStamp(DataSource<T> targetDataSource) throws IOException {
        return baseSyncStampModel.retrieve(targetDataSource.getKey());
    }

    @Override
    public void updateLastSyncStamp(SyncStamp syncStamp, DataSource<T> targetDataSource) throws IOException {
        baseSyncStampModel.update(syncStamp, targetDataSource.getKey());
    }

    @Override
    public SyncStamp getLatestSyncStamp() throws IOException {
        return latestSyncStampModel.retrieve(null);
    }

    @Override
    public void updateLatestSyncStamp(SyncStamp syncStamp) throws IOException {
        latestSyncStampModel.update(syncStamp, null);
    }

    @Override
    public Class<T> clazz() {
        return this.clazz;
    }

    @Override
    public boolean tryLock(Long time) {
        return lock.tryLock(time);
    }

    @Override
    public boolean isLocked() {
        return lock.isLocked();
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public boolean release() {
        return lock.release();
    }
}
