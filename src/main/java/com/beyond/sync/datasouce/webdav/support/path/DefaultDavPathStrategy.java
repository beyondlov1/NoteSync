package com.beyond.sync.datasouce.webdav.support.path;


import com.beyond.sync.utils.OkWebDavUtil;
import org.apache.commons.lang3.StringUtils;

import static com.beyond.sync.constant.WebDavConst.DAV_DATA_DIR;

public class DefaultDavPathStrategy implements DavPathStrategy {

    private String server;

    private String[] paths;

    private Class clazz;

    public DefaultDavPathStrategy(String server,Class clazz, String... paths) {
        this.server = server;
        this.clazz = clazz;
        this.paths = new String[]{
                OkWebDavUtil.concat(clazz.getSimpleName().toUpperCase(),DAV_DATA_DIR,"SLICE1"),
                OkWebDavUtil.concat(clazz.getSimpleName().toUpperCase(),DAV_DATA_DIR,"SLICE2")
        };
    }

    @Override
    public String getStorageDir(String id) {
        if (paths.length == 0) {
            throw new RuntimeException("url不能为空");
        }
        if (paths.length == 1) {
            if (paths[0].endsWith("/")) {
                return StringUtils.substringBeforeLast(paths[0], "/");
            }
            return paths[0];
        }

        int index = Math.abs(id.hashCode()) % paths.length;
        if (paths[index].endsWith("/")) {
            return StringUtils.substringBeforeLast(paths[index], "/");
        }
        return paths[index];
    }

    @Override
    public String getStorageDirUrl(String id) {
        return OkWebDavUtil.concat(server, getStorageDir(id));
    }

    @Override
    public String[] getAllStorageDirs(String type) {
        return new String[]{OkWebDavUtil.concat(clazz.getSimpleName().toUpperCase(),DAV_DATA_DIR)};
    }
}
