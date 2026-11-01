INSERT INTO att_work_sessions (id, user_id, tenant_id, work_date, open_session_date, close_session_date)
VALUES (1001, 2, 2, DATE '2024-01-02', TIMESTAMP '2024-01-02 09:00:00Z',
        TIMESTAMP '2024-01-02 17:30:00Z'),
       (1002, 2, 2, DATE '2024-01-03', TIMESTAMP '2024-01-03 09:00:00Z',
        TIMESTAMP '2024-01-03 17:00:00Z'),
       (1003, 2, 2, DATE '2024-01-04', TIMESTAMP '2024-01-04 09:00:00Z',
        NULL);

INSERT INTO att_work_sessions (id, user_id, tenant_id, work_date, open_session_date, close_session_date)
VALUES (1005, 2, 2, CURRENT_DATE, DATEADD(HOUR, 9, CAST(CURRENT_DATE AS TIMESTAMP)),
        NULL);

INSERT INTO att_work_sessions (id, user_id, tenant_id, work_date, open_session_date, close_session_date)
VALUES (1004, 1, 1, DATE '2024-01-10', TIMESTAMP '2024-01-10 08:00:00Z',
        TIMESTAMP '2024-01-10 16:00:00Z');