package com.beyond.sync.utils;


import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class OkWebDavUtil {

    private final static Map<String,Boolean> IS_DIR_EXIST = new ConcurrentHashMap<>();

    /**
     * dirUrl要以/结尾
     * @param client
     * @param dirUrl
     * @throws IOException
     */
    public static void mkRemoteDirQuietly(Sardine client, String dirUrl) throws IOException {
        if (IS_DIR_EXIST.get(dirUrl)!=null&& IS_DIR_EXIST.get(dirUrl)){
            return;
        }
        //获取文件夹路径
        String parentUrl = StringUtils.substringBeforeLast(dirUrl, "/");
        String root = getRootUrl(dirUrl);
        if (!urlEquals(parentUrl, root)) {
            mkRemoteDirQuietly(client,parentUrl);
        }
        try {
            client.createDirectory(dirUrl);
            IS_DIR_EXIST.put(dirUrl,true);
        }catch (SardineException e){
            //ignore
        }
    }

    public static boolean urlEquals(String url1, String url2) {
        return  StringUtils.equalsIgnoreCase(
                url1.replace("/", ""),
                url2.replace("/", ""));
    }


    public static void mkDir(String rootUrl, String dirUrl,Sardine sardine) throws IOException {
        String parentUrl = StringUtils.substringBeforeLast(dirUrl, "/");
        if (!OkWebDavUtil.urlEquals(parentUrl, rootUrl)) {
            mkDir(rootUrl,parentUrl,sardine);
        }
        if (!sardine.exists(dirUrl)){
            sardine.createDirectory(dirUrl);
        }
    }

    public static String concat(String...strings){
        String result = null;
        for (String string : strings) {
            if (result == null){
                result = string;
                continue;
            }
            result = concat(result,string);
        }
        return result;
    }
    public static String concat(String root, String path) {
        String url = null;
        if ((!root.endsWith("/") && path.startsWith("/"))
                || root.endsWith("/") && !path.startsWith("/")) {
            url = root + path;
        } else if (root.endsWith("/") && path.startsWith("/")) {
            url = StringUtils.substringBeforeLast(root, "/") + path;
        } else if (!root.endsWith("/") && !path.startsWith("/")) {
            url = root + "/" + path;
        }
        return url;
    }

    public static String getRootUrl(String dirUrl) {
        URI uri = URI.create(dirUrl);
        return uri.getScheme() + "://" + uri.getAuthority();
    }

}
