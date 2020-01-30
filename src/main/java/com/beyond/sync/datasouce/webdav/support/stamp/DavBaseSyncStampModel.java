package com.beyond.sync.datasouce.webdav.support.stamp;


import com.beyond.sync.datasouce.webdav.DavDataSourceProperty;
import com.beyond.sync.datasouce.webdav.support.DavClient;
import com.beyond.sync.datasouce.webdav.support.stamp.AbstractDavSyncStampModel;
import com.beyond.sync.entity.SyncStamp;
import com.beyond.sync.utils.OkWebDavUtil;
import com.beyond.sync.utils.SyncUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author: beyond
 * @date: 2019/8/9
 */

public class DavBaseSyncStampModel extends AbstractDavSyncStampModel {

    public DavBaseSyncStampModel(DavClient client, DavDataSourceProperty property, Class clazz) {
        super(client, property, clazz);
    }

    @Override
    protected String getUrl(String oppositeKey) {
        String clazzUpCase = clazz.getSimpleName().toUpperCase();
        return OkWebDavUtil.concat(property.getServer(), clazzUpCase, property.getSyncStampDir(),
                property.getBaseSyncStampFilePrefix() + SyncUtils.base64Encode(oppositeKey) + ".stamp");
    }

    @Override
    public Map<String, SyncStamp> findAllConnectMe() throws IOException {
        throw new RuntimeException("not supported");
    }
}
