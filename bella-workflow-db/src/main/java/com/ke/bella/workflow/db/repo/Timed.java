package com.ke.bella.workflow.db.repo;

import java.time.LocalDateTime;

public interface Timed {
    LocalDateTime getCtime();

    void setCtime(LocalDateTime ctime);

    LocalDateTime getMtime();

    void setMtime(LocalDateTime mtime);
}
