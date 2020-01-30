package com.beyond.sync;

public interface Synchronizer<T> {

    String SYNC_STRATEGY_POINT = "point";
    String SYNC_STRATEGY_MULTI = "multi";

    boolean sync() throws Exception;
}
