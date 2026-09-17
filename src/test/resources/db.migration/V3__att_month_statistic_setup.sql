-- Month-statistic history for the enrichment consumer.
-- User 7 exists ONLY in this table: it has no att_work_sessions and no att_user_leave_days rows, so
-- StatisticInfoService.resolveTargetUserIds() never sees it and no HTTP flow can overwrite this history.
--
-- Anchor month is 2024-01. The consumer query is
-- findTop7...MonthStartDateLessThanEqualOrderBy...Desc(tenantId, userId, monthStart), so the expected
-- window is 2024-01 (the target itself) + the 6 months before it (2023-12 .. 2023-07).
INSERT INTO att_month_statistic (tenant_id, user_id, month_start_date, work_days, overtime_hours,
                                 total_work_hours, vacation_days, sick_days, updated_by, sys_update_date)
VALUES
    -- 7th-oldest prior month: falls outside the top-7 window and must never be returned
    (2, 7, DATE '2023-06-01', 19, 1.0, 150.0, 0.0, 0.0, 'test', TIMESTAMP '2023-07-01 00:00:00Z'),
    (2, 7, DATE '2023-07-01', 20, 1.5, 155.0, 0.0, 0.0, 'test', TIMESTAMP '2023-08-01 00:00:00Z'),
    (2, 7, DATE '2023-08-01', 21, 2.0, 160.0, 0.0, 0.0, 'test', TIMESTAMP '2023-09-01 00:00:00Z'),
    (2, 7, DATE '2023-09-01', 20, 2.5, 162.0, 0.0, 0.0, 'test', TIMESTAMP '2023-10-01 00:00:00Z'),
    (2, 7, DATE '2023-10-01', 22, 3.0, 170.0, 0.0, 0.0, 'test', TIMESTAMP '2023-11-01 00:00:00Z'),
    (2, 7, DATE '2023-11-01', 20, 2.0, 160.0, 0.0, 0.0, 'test', TIMESTAMP '2023-12-01 00:00:00Z'),
    -- previous month: non-zero vacation/sick so the value-mapping assertions cover every field
    (2, 7, DATE '2023-12-01', 21, 3.0, 168.0, 2.0, 1.5, 'test', TIMESTAMP '2024-01-01 00:00:00Z'),
    -- the target month itself: in production the HTTP flow computes and persists this row before publishing
    (2, 7, DATE '2024-01-01', 22, 4.0, 175.0, 1.0, 0.5, 'test', TIMESTAMP '2024-02-01 00:00:00Z'),
    -- after the target month: must never be pulled into the history, regardless of table order
    (2, 7, DATE '2024-02-01', 18, 0.5, 140.0, 0.0, 0.0, 'test', TIMESTAMP '2024-03-01 00:00:00Z');

-- Scoping noise: the same months under the wrong tenant and the wrong user. The consumer query is scoped by
-- tenant + user, so none of these may ever surface in the enrichment history. Their values are deliberately
-- unlike the rows above so a leak fails loudly instead of passing by coincidence.
INSERT INTO att_month_statistic (tenant_id, user_id, month_start_date, work_days, overtime_hours,
                                 total_work_hours, vacation_days, sick_days, updated_by, sys_update_date)
VALUES
    -- wrong tenant, same user
    (3, 7, DATE '2023-12-01', 1, 99.0, 999.0, 9.0, 9.0, 'test', TIMESTAMP '2024-01-01 00:00:00Z'),
    (3, 7, DATE '2024-01-01', 2, 99.0, 999.0, 9.0, 9.0, 'test', TIMESTAMP '2024-02-01 00:00:00Z'),
    -- same tenant, wrong user
    (2, 8, DATE '2023-12-01', 3, 99.0, 999.0, 9.0, 9.0, 'test', TIMESTAMP '2024-01-01 00:00:00Z'),
    (2, 8, DATE '2024-01-01', 4, 99.0, 999.0, 9.0, 9.0, 'test', TIMESTAMP '2024-02-01 00:00:00Z');
