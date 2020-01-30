package com.beyond.sync.datasouce.sql;

import java.util.Date;
import java.util.List;

public interface SqlLogModel {

    List<TraceLog> getAllWhereOperationTimeAfter(Date date);

    List<TraceLog> getAllWithoutSourceWhereCreateTimeAfter(Date date, String key);
}
