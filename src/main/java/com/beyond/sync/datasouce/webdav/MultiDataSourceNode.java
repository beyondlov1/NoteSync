package com.beyond.sync.datasouce.webdav;


import com.beyond.sync.datasouce.MultiDataSource;
import com.beyond.sync.entity.SyncStamp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiDataSourceNode<T> {

    private MultiDataSourceNode<T> parent;
    private List<MultiDataSourceNode<T>> children = new ArrayList<>();
    private MultiDataSource<T> dataSource;
    private List<T> modifiedData = new ArrayList<T>();

    public static <T> MultiDataSourceNode<T> of(MultiDataSource<T> dataSource) {
        MultiDataSourceNode<T> node = new MultiDataSourceNode<T>();
        node.setDataSource(dataSource);
        return node;
    }


    public void getAllChildren(List<MultiDataSourceNode<T>> result) {
        for (MultiDataSourceNode<T> child : children) {
            result.add(child);
            child.getAllChildren(result);
        }
    }

    public List<T> getChildrenModifiedData() {
        List<T> childrenModifiedData = new ArrayList<T>();
        for (MultiDataSourceNode<T> child : children) {
            childrenModifiedData.addAll(child.getModifiedData());
            childrenModifiedData.addAll(child.getChildrenModifiedData());
        }
        return childrenModifiedData;
    }

    public void initModifiedData() throws Exception {
        MultiDataSource<T> parentDataSource = parent.getDataSource();
        SyncStamp lastSyncStamp = this.dataSource.getLastSyncStamp(parentDataSource);
        if (this.dataSource.isChanged(parentDataSource)) {
            modifiedData = this.dataSource.getChangedData(lastSyncStamp, parentDataSource);
        }
    }

    public void saveData(List<T> data, String... oppositeKeys) throws Exception {
        Set<T> notMineData = new HashSet<>(data);
        notMineData.removeAll(modifiedData);
        if (notMineData.isEmpty()) {
            return;
        }

        dataSource.saveAll(new ArrayList<>(notMineData), oppositeKeys);
    }

    public MultiDataSourceNode<T> addChild(MultiDataSourceNode<T> node) {
        node.setParent(this);
        children.add(node);
        return this;
    }


    public List<MultiDataSourceNode<T>> getChildren() {
        return children;
    }

    public void setChildren(List<MultiDataSourceNode<T>> children) {
        this.children = children;
        for (MultiDataSourceNode<T> child : children) {
            child.setParent(this);
        }
    }

    public MultiDataSource<T> getDataSource() {
        return dataSource;
    }

    public void setDataSource(MultiDataSource<T> dataSource) {
        this.dataSource = dataSource;
    }

    public MultiDataSourceNode<T> getParent() {
        return parent;
    }

    public void setParent(MultiDataSourceNode<T> parent) {
        this.parent = parent;
    }

    public List<T> getModifiedData() {
        return modifiedData;
    }

    public void setModifiedData(List<T> modifiedData) {
        this.modifiedData = modifiedData;
    }
}
