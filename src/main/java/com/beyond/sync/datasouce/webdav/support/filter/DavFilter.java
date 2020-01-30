package com.beyond.sync.datasouce.webdav.support.filter;


import com.github.sardine.DavResource;

public interface DavFilter {
    /**
     * 返回true为过滤掉， 返回false则不过滤
     * @param davResource
     * @return
     */
    boolean filter(DavResource davResource);
}
