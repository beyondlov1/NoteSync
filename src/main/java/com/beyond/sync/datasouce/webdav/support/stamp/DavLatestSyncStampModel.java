package com.beyond.sync.datasouce.webdav.support.stamp;


import com.beyond.sync.datasouce.webdav.DavDataSourceProperty;
import com.beyond.sync.datasouce.webdav.support.DavClient;
import com.beyond.sync.entity.SyncStamp;
import com.beyond.sync.utils.OkWebDavUtil;

import java.util.Map;

/**
 * @author: beyond
 * @date: 2019/8/9
 */

public class DavLatestSyncStampModel extends AbstractDavSyncStampModel {

    public DavLatestSyncStampModel(DavClient client, DavDataSourceProperty property, Class clazz) {
        super(client, property, clazz);
    }

    @Override
    protected String getUrl(String oppositeKey) {
        String clazzUpCase = clazz.getSimpleName().toUpperCase();
        return OkWebDavUtil.concat(property.getServer(),
                clazzUpCase, property.getSyncStampDir(), property.getLatestSyncStampFileName()+".stamp");
    }

    @Override
    public Map<String, SyncStamp> findAllConnectMe() {
        throw new RuntimeException("not supported");
    }
}
