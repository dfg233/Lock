package com.dfg233.lock.capability;

import com.dfg233.lock.data.LockData;

public interface ILockable {
    // 获取锁数据
    LockData getLockData();

    // 设置锁数据
    void setLockData(LockData data);

    // 是否已安装锁
    boolean hasLock();
}
