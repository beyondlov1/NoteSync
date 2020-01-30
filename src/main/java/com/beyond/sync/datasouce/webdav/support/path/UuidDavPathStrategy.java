package com.beyond.sync.datasouce.webdav.support.path;


import com.beyond.sync.utils.OkWebDavUtil;

import java.util.ArrayList;
import java.util.List;

import static com.beyond.sync.constant.WebDavConst.DAV_DATA_DIR;

/**
 * @author: beyond
 * @date: 2019/8/3
 */

public class UuidDavPathStrategy implements DavPathStrategy {

    private static final int DEFAULT_DEPTH = 0;
    private static final int DEFAULT_WIDTH = 2;

    private String server;

    private Class clazz;

    private int width = DEFAULT_WIDTH;
    private int depth = DEFAULT_DEPTH;

    public UuidDavPathStrategy(String server, Class clazz) {
        this.server = server;
        this.clazz = clazz;
    }

    @Override
    public String getStorageDir(String id) {
        if (depth > 32) {
            throw new RuntimeException("depth is too big");
        }
        List<String> dirs = new ArrayList<>();
        dirs.add(clazz.getSimpleName().toUpperCase());
        dirs.add(DAV_DATA_DIR);
        for (int i = 0; i < depth; i++) {
            dirs.add(id.substring(i, i + width));
        }
        return OkWebDavUtil.concat(dirs.toArray(new String[0]));
    }

    @Override
    public String getStorageDirUrl(String id) {
        return OkWebDavUtil.concat(server, getStorageDir(id));
    }

    @Override
    public String[] getAllStorageDirs(String type) {
        return new String[]{OkWebDavUtil.concat(clazz.getSimpleName().toUpperCase(), DAV_DATA_DIR)};
    }
}
