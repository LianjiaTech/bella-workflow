ALTER TABLE workflow_run
    ADD INDEX idx_workflow_trigger (`workflow_id`, `workflow_run_id`, `trigger_from`),
    ALGORITHM INPLACE,
    LOCK = NONE;
