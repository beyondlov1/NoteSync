package com.beyond.sync.datasouce.webdav.support.filter;


import com.github.sardine.DavResource;

import java.util.Date;

public class AfterModifiedTimeDavFilter implements DavFilter {

    private Date date;

    public AfterModifiedTimeDavFilter(Date date) {
        this.date = date;
    }

    @Override
    public boolean filter( DavResource davResource) {
        return davResource.getModified() == null || !davResource.getModified().after(date);
    }
}
