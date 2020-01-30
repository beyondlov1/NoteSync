package com.beyond.sync;


import com.beyond.sync.entity.SyncState;

import java.util.List;

public interface SyncStateManager {

    void saveAll(List<SyncState> successSyncStates);

    List<SyncState> findAll();

    void deleteConnectedEachOtherIn(List<String> keys, Class clazz);
}
