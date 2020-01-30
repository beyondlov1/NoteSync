package com.beyond.sync;

public interface Lock {

    boolean tryLock();

    boolean tryLock(Long time);

    boolean isLocked();

    boolean release();
}
