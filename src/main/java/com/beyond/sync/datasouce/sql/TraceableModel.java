package com.beyond.sync.datasouce.sql;

import java.util.Collection;
import java.util.List;

public interface TraceableModel<T> {
    List<T> findAllById(Collection<String> keySet);

    void addAll(List<T> addList);

    void updateAll(List<T> updateList);

    List<T> findAll();
}
