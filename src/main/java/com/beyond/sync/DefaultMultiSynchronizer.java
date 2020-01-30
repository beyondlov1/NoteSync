package com.beyond.sync;


import com.beyond.sync.datasouce.MultiDataSource;
import com.beyond.sync.datasouce.webdav.MultiDataSourceNode;
import com.beyond.sync.entity.SyncStamp;
import com.beyond.sync.entity.SyncState;
import com.beyond.sync.entity.Traceable;
import com.beyond.sync.utils.SyncUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: beyond
 * @date: 2019/8/9
 */
//TODO: STATE
public class DefaultMultiSynchronizer<T extends Traceable> implements Synchronizer<T> {

    private long remoteLockTimeOutMills = 30 * 60000L; // 30min

    private java.util.concurrent.locks.Lock syncLock;

    private List<MultiDataSource<T>> dataSources;
    private String[] dataSourceKeys;

    private ExecutorService executorService;
    private ExecutorService executorServiceExtra;  // 防止多线程嵌套时死锁

    private final SyncStateManager syncStateManager;

    private Date syncTimeStart;

    public DefaultMultiSynchronizer(List<MultiDataSource<T>> dataSources,
                                    ExecutorService executorService,
                                    SyncStateManager syncStateManager) {
        this.dataSources = dataSources;
        this.executorService = executorService;
        this.executorServiceExtra = new ThreadPoolExecutor(
                9, 60,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        this.syncLock = new ReentrantLock();
        ArrayList<String> keyList = new ArrayList<>(dataSources.size());
        for (MultiDataSource<T> dataSource : dataSources) {
            keyList.add(dataSource.getKey());
        }
        this.dataSourceKeys = keyList.toArray(new String[0]);
        this.syncStateManager = syncStateManager;
    }

    @Override
    public boolean sync() throws Exception {
        if (syncLock.tryLock()) {
            try {
                doSync();
            } catch (Exception e) {
                long delay = (new Random().nextInt(20) + 35) * 60 * 1000;
                retryIfNecessary(delay);
                e.printStackTrace();
            } finally {
                syncLock.unlock();
            }
        } else {
            System.out.println("同步正在进行, 本次同步取消"); 
        }
        return true;
    }

    private void doSync() throws Exception {

        syncTimeStart = new Date();

        List<MultiDataSource<T>> dataSources = new ArrayList<>(this.dataSources);

        initDataSourceAttachmentHelper(dataSources);

        System.out.println( "initSyncStamps:" + new Date());
        initSyncStamps(dataSources);

        System.out.println( "chooseRoot:" + new Date());
        MultiDataSourceNode<T> root = chooseRoot(dataSources);

        System.out.println( "constructTree:" + new Date());
        constructTree(root, dataSources);

        System.out.println( "initModifiedDataAndRemoveInValidNodes:" + new Date());
        initModifiedDataAndRemoveInValidNodes(root);

        System.out.println( "getChildrenModifiedData:" + new Date());
        List<T> childrenModifiedData = root.getChildrenModifiedData();
        childrenModifiedData.addAll(root.getModifiedData());
        System.out.println( "modifiedData:" + childrenModifiedData);


        if (!remoteLock()) {
            releaseLock();
            return;
        }

        System.out.println( "handleSingles:" + new Date());
        SyncStamp singlesSyncStamp = handleSingles(root, childrenModifiedData, dataSources);

        System.out.println( "getAllChildrenNodes:" + new Date());
        List<MultiDataSourceNode<T>> childrenNodes = getAllChildrenNodes(root);

        childrenNodes.add(root);

        System.out.println( "saveAll:" + new Date());
        List<MultiDataSourceNode<T>> successNodes = saveAll(childrenModifiedData, childrenNodes);

        System.out.println( "saveSyncStamps:" + new Date());
        SyncStamp uniteSyncStamp = getUniteSyncStamp(childrenModifiedData, singlesSyncStamp, dataSources);
        saveSyncStamps(successNodes, uniteSyncStamp);

        List<SyncState> beforeClear = syncStateManager.findAll();
        System.out.println(beforeClear);

        System.out.println( "clearSyncState:" + new Date());
        clearSyncState(successNodes, root);

        List<SyncState> afterClear = syncStateManager.findAll();
        System.out.println(afterClear);

        System.out.println( "end:" + new Date());

        releaseLock();
    }

    private void initRootModifiedData(MultiDataSourceNode<T> root) throws Exception {
        // root 的 ModifiedData 是根据与 children 中最新同步的比较
        List<MultiDataSourceNode<T>> children = root.getChildren();
        SyncStamp latestSyncStamp = SyncStamp.ZERO;
        MultiDataSourceNode<T> latestSyncChild = null;
        for (MultiDataSourceNode<T> child : children) {
            if (child.getDataSource().getLastSyncStamp(root.getDataSource()).getLastSyncTimeEnd().after(latestSyncStamp.getLastSyncTimeEnd())) {
                latestSyncStamp = child.getDataSource().getLastSyncStamp(root.getDataSource());
                latestSyncChild = child;
            }
        }
        List<T> changedData = root.getDataSource().getChangedData(latestSyncStamp, latestSyncChild == null ? null : latestSyncChild.getDataSource());
        root.setModifiedData(changedData);
    }

    private void initDataSourceAttachmentHelper(List<MultiDataSource<T>> dataSources) {
//        AttachmentHelper attachmentHelper = new AttachmentHelper(executorService);
//        for (MultiDataSource<T> dataSource : dataSources) {
//            if (dataSource instanceof AttachmentHelperAware) {
//                ((AttachmentHelperAware) dataSource).setAttachmentHelper(attachmentHelper);
//            }
//        }
    }

    private void retryIfNecessary(long delay) {
//        SyncRetryService.retryIfNecessary(MyApplication.getInstance(), delay);
//        SyncRetryService.addFailCount();
    }

    private void clearSyncState(List<MultiDataSourceNode<T>> successNodes, MultiDataSourceNode<T> root) {

        if (dataSources == null || dataSources.isEmpty()) {
            return;
        }

        List<MultiDataSource<T>> successDataSources = extract(successNodes);

        ArrayList<MultiDataSourceNode<T>> allConnectedNodes = new ArrayList<>();
        root.getAllChildren(allConnectedNodes);
        List<MultiDataSource<T>> allConnectedDataSource = extract(allConnectedNodes);

        ArrayList<MultiDataSource<T>> inValidDataSource = new ArrayList<>(dataSources);
        inValidDataSource.removeAll(allConnectedDataSource);

        List<MultiDataSource<T>> nonRecordDataSource = new ArrayList<>(successDataSources);
        nonRecordDataSource.addAll(inValidDataSource);

        List<String> keys = new ArrayList<>(nonRecordDataSource.size());
        for (MultiDataSource<T> dataSource : nonRecordDataSource) {
            keys.add(dataSource.getKey());
        }

        syncStateManager.deleteConnectedEachOtherIn(keys, dataSources.get(0).clazz());
    }

    private List<MultiDataSource<T>> extract(List<MultiDataSourceNode<T>> nodes) {
        List<MultiDataSource<T>> dataSources = new ArrayList<>(nodes.size());
        for (MultiDataSourceNode<T> node : nodes) {
            dataSources.add(node.getDataSource());
        }
        return dataSources;
    }

    private void releaseLock() {
        releaseLock(dataSources);
    }

    private void releaseLock(List<MultiDataSource<T>> dataSources) {
        SyncUtils.blockExecute(executorService,
                new SyncUtils.ParamCallable<MultiDataSource<T>, Void>() {
                    @Override
                    public Void call(MultiDataSource<T> singleExecutor) throws Exception {
                        singleExecutor.release();
                        return null;
                    }
                }, null,
                dataSources);
    }

    private boolean remoteLock() {
        return remoteLock(dataSources);
    }

    private boolean remoteLock(List<MultiDataSource<T>> dataSources) {
        final boolean[] locked = {true};
        SyncUtils.blockExecute(executorService,
                new SyncUtils.ParamCallable<MultiDataSource<T>, Boolean>() {
                    @Override
                    public Boolean call(MultiDataSource<T> singleExecutor) throws Exception {
                        return singleExecutor.tryLock(remoteLockTimeOutMills);
                    }
                }, new SyncUtils.Handler<MultiDataSource<T>, Boolean>() {
                    @Override
                    public void handle(MultiDataSource<T> param, Boolean result) throws Exception {
                        locked[0] = result && locked[0];
                    }
                }, null,
                dataSources);
        return locked[0];
    }

    private SyncStamp getUniteSyncStamp(List<T> childrenModifiedData, SyncStamp singlesSyncStamp, List<MultiDataSource<T>> dataSources) {
        if (childrenModifiedData.isEmpty()) {
            if (!dataSources.isEmpty()) {
                return singlesSyncStamp;
            }
            return null;
        }
        return SyncStamp.create(
                getLatestLastModifyTime(childrenModifiedData),
                syncTimeStart,
                new Date());
    }

    private SyncStamp handleSingles(final MultiDataSourceNode<T> root, final List<T> childrenModifiedData, List<MultiDataSource<T>> dataSources) throws Exception {
        if (dataSources.isEmpty()) {
            return null;
        }
        if (childrenModifiedData.isEmpty()) {
            if (!remoteLock(dataSources)) {
                releaseLock(dataSources);
                throw new RuntimeException("singles syncLock failed");
            }
        }
        final List<T> rootAll = root.getDataSource().getChangedData(SyncStamp.ZERO, null);
        SyncUtils.blockExecute(executorService
                , new SyncUtils.ParamCallable<MultiDataSource<T>, List<T>>() {
                    @Override
                    public List<T> call(MultiDataSource<T> singleExecutor) throws Exception {
                        return singleExecutor.getChangedData(SyncStamp.ZERO, root.getDataSource());
                    }
                }, new SyncUtils.Handler<MultiDataSource<T>, List<T>>() {
                    @Override
                    public void handle(MultiDataSource<T> param, List<T> result) throws Exception {
                        result.removeAll(rootAll);
                        childrenModifiedData.addAll(result);
                        // 保存root的全量
                        param.saveAll(rootAll, dataSourceKeys);
                        // 添加到树
                        MultiDataSourceNode<T> node = MultiDataSourceNode.of(param);
                        node.setModifiedData(result);
                        root.addChild(node);

                    }
                }, null, dataSources);
        return SyncStamp.create(
                getLatestLastModifyTime(rootAll),
                syncTimeStart,
                new Date());
    }

    private List<MultiDataSourceNode<T>> getAllChildrenNodes(MultiDataSourceNode<T> root) {
        List<MultiDataSourceNode<T>> nodes = new ArrayList<>();
        root.getAllChildren(nodes);
        return nodes;
    }

    private void saveSyncStamps(final List<MultiDataSourceNode<T>> nodes, final SyncStamp syncStamp) {
        if (syncStamp == null) {
            return;
        }
        SyncUtils.blockExecute(executorService,
                new SyncUtils.ParamCallable<MultiDataSourceNode<T>, Void>() {
                    @Override
                    public Void call(final MultiDataSourceNode<T> singleExecutor) throws Exception {
                        final MultiDataSource<T> dataSource = singleExecutor.getDataSource();
                        SyncUtils.blockExecute(executorServiceExtra,
                                new SyncUtils.ParamCallable<MultiDataSourceNode<T>, Void>() {
                                    @Override
                                    public Void call(MultiDataSourceNode<T> successNode) throws Exception {
                                        if (singleExecutor == successNode) {
                                            return null;
                                        }
                                        dataSource.updateLastSyncStamp(syncStamp, successNode.getDataSource());
                                        dataSource.updateLatestSyncStamp(syncStamp);
                                        return null;
                                    }
                                }, null, nodes);

                        return null;
                    }
                }, null, nodes);
    }

    private Date getLatestLastModifyTime(List<T> childrenModifiedData) {
        Date latestTime = null;
        for (T localDatum : childrenModifiedData) {
            if (latestTime == null) {
                latestTime = localDatum.getLastModifyTime();
                continue;
            }
            if (localDatum.getLastModifyTime().compareTo(latestTime) > 0) {
                latestTime = localDatum.getLastModifyTime();
            }
        }

        if (latestTime == null) {
            latestTime = new Date(0);
        }
        return latestTime;
    }

    private List<MultiDataSourceNode<T>> saveAll(final List<T> modifiedData, List<MultiDataSourceNode<T>> childrenNodes) {
        final List<MultiDataSourceNode<T>> successNodes = new ArrayList<>();
        SyncUtils.blockExecute(executorService,
                new SyncUtils.ParamCallable<MultiDataSourceNode<T>, Void>() {
                    @Override
                    public Void call(MultiDataSourceNode<T> singleExecutor) throws Exception {
                        singleExecutor.saveData(modifiedData, dataSourceKeys);
                        return null;
                    }
                }, new SyncUtils.Handler<MultiDataSourceNode<T>, Void>() {
                    @Override
                    public void handle(MultiDataSourceNode<T> param, Void result) {
                        successNodes.add(param);
                    }
                }, null, childrenNodes);
        return successNodes;
    }

    private void initModifiedDataAndRemoveInValidNodes(MultiDataSourceNode<T> root) throws Exception {
        ArrayList<MultiDataSourceNode<T>> allChildren = new ArrayList<MultiDataSourceNode<T>>();
        root.getAllChildren(allChildren);
        SyncUtils.blockExecute(executorService,
                new SyncUtils.ParamCallable<MultiDataSourceNode<T>, Void>() {
                    public Void call(MultiDataSourceNode<T> singleExecutor) throws Exception {
                        singleExecutor.initModifiedData();
                        return null;
                    }
                }, new SyncUtils.ParamCallable<MultiDataSourceNode<T>, Void>() {
                    public Void call(MultiDataSourceNode<T> singleExecutor) throws Exception {
                        // 把子节点从树中移除
                        singleExecutor.getParent().getChildren().remove(singleExecutor);
                        singleExecutor.setParent(null);
                        return null;
                    }
                }, allChildren);
        initRootModifiedData(root);
    }

    private void initSyncStamps(List<MultiDataSource<T>> list) {
        for (MultiDataSource<T> dataSource : list) {
            dataSource.setChosenKey(null);
            dataSource.setSyncStampsCache(new LinkedHashMap<String, SyncStamp>());
        }
        SyncUtils.blockExecute(executorService,
                new SyncUtils.ParamCallable<MultiDataSource<T>, Void>() {
                    public Void call(MultiDataSource<T> singleExecutor) throws Exception {
                        singleExecutor.initLastSyncStamps();
                        return null;
                    }
                }, new SyncUtils.ParamCallable<MultiDataSource<T>, Void>() {
                    public Void call(MultiDataSource<T> singleExecutor) throws Exception {
                        System.out.println(singleExecutor);
                        return null;
                    }
                }, list);
    }

    private MultiDataSourceNode<T> chooseRoot(List<MultiDataSource<T>> dataSources) {
        initSyncKeyForSync(dataSources);
        MultiDataSource<T> rootDataSource = getMaxConnectedDataSource(dataSources);
        MultiDataSourceNode<T> rootNode = MultiDataSourceNode.of(rootDataSource);
        dataSources.remove(rootDataSource);
        return rootNode;
    }

    private void initSyncKeyForSync(List<MultiDataSource<T>> list) {
        for (MultiDataSource<T> dataSource : list) {
            chooseInternal(list, dataSource);
        }
    }

    private void chooseInternal(List<MultiDataSource<T>> list, MultiDataSource<T> dataSource) {
        Map<String, SyncStamp> syncStamps = dataSource.getSyncStampsCache();
        for (String key : syncStamps.keySet()) {
            boolean found = false;
            for (MultiDataSource<T> ds : list) {
                if (ds.getKey().equals(key)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                dataSource.setChosenKey(key);
                break;
            }
        }
    }

    private MultiDataSource<T> getMaxConnectedDataSource(List<MultiDataSource<T>> list) {
        MultiDataSource<T> result = list.get(0);
        int resultCount = getConnectedDataSourceCount(result, list);
        for (MultiDataSource<T> dataSource : list) {
            int count = getConnectedDataSourceCount(dataSource, list);
            if (count > resultCount) {
                result = dataSource;
                resultCount = count;
            }
        }
        return result;
    }

    private int getConnectedDataSourceCount(MultiDataSource<T> dataSource, List<MultiDataSource<T>> list) {
        int count = 0;
        for (MultiDataSource<T> ds : list) {
            if (dataSource.getKey().equals(ds.getChosenKey())) {
                count++;
            }
        }
        return count;
    }

    private void constructTree(MultiDataSourceNode<T> root, List<MultiDataSource<T>> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            return;
        }
        Iterator<MultiDataSource<T>> iterator = dataSources.iterator();
        while (iterator.hasNext()) {
            MultiDataSource<T> next = iterator.next();
            if (root.getDataSource().getKey().equals(next.getChosenKey())) {
                root.addChild(MultiDataSourceNode.of(next));
                iterator.remove();
            }
        }
        for (MultiDataSourceNode<T> child : root.getChildren()) {
            constructTree(child, dataSources);
        }
    }

}
