package com.beyond.sync.utils;

import java.util.UUID;

/**
 * @author: beyond
 * @date: 2019/2/1
 */

public class IDUtil {
    public static String uuid(){
        return UUID.randomUUID().toString().replace("-", "");
    }
}
