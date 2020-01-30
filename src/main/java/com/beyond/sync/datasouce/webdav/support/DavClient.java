package com.beyond.sync.datasouce.webdav.support;

import com.beyond.sync.datasouce.webdav.support.filter.DavFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface DavClient {
    void put(String url, String content) throws IOException;
    void upload(String path, String url) throws IOException;
    void upload(File file, String url) throws IOException;
    void upload(File file, String url, String contentType) throws IOException;
    String get(String url) throws IOException;
    void download(String url, String path) throws IOException;
    void download(String url, File file) throws IOException;
    List<String> listAllFileName(String dirUrl) throws IOException;
    List<String> listAllFilePath(String dirUrl) throws IOException;
    List<String> listAllFileUrl(String dirUrl) throws IOException;
    List<String> listAllFileName(String dirUrl, DavFilter filter) throws IOException;
    List<String> listAllFilePath(String dirUrl, DavFilter filter) throws IOException;
    List<String> listAllFileUrl(String dirUrl, DavFilter filter) throws IOException;
    void delete(String url) throws IOException;
    boolean exists(String url) throws IOException;
    boolean mkDirQuietly(String dirUrl);
}
