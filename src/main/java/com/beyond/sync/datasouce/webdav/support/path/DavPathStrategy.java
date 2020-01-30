package com.beyond.sync.datasouce.webdav.support.path;

public interface DavPathStrategy {
    String getStorageDir(String id);
    String getStorageDirUrl(String id);
    String[] getAllStorageDirs(String type);
}
