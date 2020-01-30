package com.beyond.sync.datasouce.webdav;


import static com.beyond.sync.constant.WebDavConst.*;

/**
 * @author: beyond
 * @date: 2019/8/9
 */

public class DavDataSourceProperty {

    private String username;
    private String password;
    private String server;
    private String lockDir = DAV_LOCK_DIR;
    private String syncStampDir = DAV_STAMP_DIR;
    private String dataDir = DAV_DATA_DIR;
    private boolean needExecutorService = true;
    private String latestSyncStampFileName = DAV_STAMP_LATEST_NAME;
    private String baseSyncStampFilePrefix = DAV_STAMP_BASE_PREFIX;
    private String filesDir = DAV_FILES_DIR;

    public DavDataSourceProperty(String username, String password, String server) {
        this.username = username;
        this.password = password;
        this.server = server;
    }

    public String getLatestSyncStampFileName() {
        return latestSyncStampFileName;
    }

    public void setLatestSyncStampFileName(String latestSyncStampFileName) {
        this.latestSyncStampFileName = latestSyncStampFileName;
    }

    public String getBaseSyncStampFilePrefix() {
        return baseSyncStampFilePrefix;
    }

    public void setBaseSyncStampFilePrefix(String baseSyncStampFilePrefix) {
        this.baseSyncStampFilePrefix = baseSyncStampFilePrefix;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getLockDir() {
        return lockDir;
    }

    public void setLockDir(String lockDir) {
        this.lockDir = lockDir;
    }

    public String getSyncStampDir() {
        return syncStampDir;
    }

    public void setSyncStampDir(String syncStampDir) {
        this.syncStampDir = syncStampDir;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public boolean isNeedExecutorService() {
        return needExecutorService;
    }

    public void setNeedExecutorService(boolean needExecutorService) {
        this.needExecutorService = needExecutorService;
    }

    public String getFilesDir() {
        return filesDir;
    }

    public void setFilesDir(String filesDir) {
        this.filesDir = filesDir;
    }
}
