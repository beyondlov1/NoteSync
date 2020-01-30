package com.beyond.sync.datasouce.webdav.support.lock;


import com.alibaba.fastjson.JSONObject;
import com.beyond.sync.Lock;
import com.beyond.sync.datasouce.webdav.support.DavClient;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;

public class DavLock implements Lock {

    private String url;

    private DavClient client;

    private String id;  // 防止其他 dataSource 释放自己的 lock

    public DavLock(DavClient client, String url,String id) {
        this.url = url;
        this.client = client;
        this.id = id;
    }

    public boolean tryLock() {
        return tryLock(-1L);
    }

    @Override
    public boolean tryLock(Long time) {
        try {
            if (isLocked()) {
                return false;
            }
            String json = JSONObject.toJSONString(new LockTimeUnit(new Date(), time,id));
            client.put(url, json);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean isLocked() {
        try {
            if (client.exists(url)){
                String json = client.get(url);
                LockTimeUnit lockTimeUnit = JSONObject.parseObject(json, LockTimeUnit.class);
                if (lockTimeUnit == null){
                    return false;
                }
                if (StringUtils.equals(lockTimeUnit.getId(),id)){
                    return false;
                }
                return !lockTimeUnit.expired();
            }else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean release() {
        try {
            String json = client.get(url);
            LockTimeUnit lockTimeUnit = JSONObject.parseObject(json, LockTimeUnit.class);
            if (StringUtils.equals(lockTimeUnit.getId(),id)){
                client.delete(url);
                return true;
            }else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class LockTimeUnit {
        private String id;
        private Date lastLockTime;
        private Long lockPeriod;

        public LockTimeUnit() {
        }

        LockTimeUnit(Date lastLockTime, Long lockPeriod, String id) {
            this.lastLockTime = lastLockTime;
            this.lockPeriod = lockPeriod;
            this.id = id;
        }

        public Date getLastLockTime() {
            return lastLockTime;
        }

        public void setLastLockTime(Date lastLockTime) {
            this.lastLockTime = lastLockTime;
        }

        public Long getLockPeriod() {
            return lockPeriod;
        }

        public void setLockPeriod(Long lockPeriod) {
            this.lockPeriod = lockPeriod;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        boolean expired() {
            return lastLockTime.getTime() != -1L && System.currentTimeMillis() - lastLockTime.getTime() > lockPeriod;
        }
    }
}
