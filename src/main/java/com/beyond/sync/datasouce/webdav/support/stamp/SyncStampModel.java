package com.beyond.sync.datasouce.webdav.support.stamp;


import com.beyond.sync.entity.SyncStamp;

import java.io.IOException;
import java.util.Map;

/**
 * @author: beyond
 * @date: 2019/8/3
 */

public interface SyncStampModel {
    void update(SyncStamp syncStamp, String oppositeKey) throws IOException;
    SyncStamp retrieve(String oppositeKey) throws IOException;
    Map<String,SyncStamp> findAllConnectMe() throws IOException;
}
