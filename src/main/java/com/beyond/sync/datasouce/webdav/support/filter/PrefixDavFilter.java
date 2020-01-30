package com.beyond.sync.datasouce.webdav.support.filter;


import com.github.sardine.DavResource;

/**
 * @author: beyond
 * @date: 2019/8/9
 */

public class PrefixDavFilter implements DavFilter {
    private final String prefix;

    public PrefixDavFilter(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean filter(DavResource davResource) {
        return !davResource.getName().startsWith(prefix);
    }
}
