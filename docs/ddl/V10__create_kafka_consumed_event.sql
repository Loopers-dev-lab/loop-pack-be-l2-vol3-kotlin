CREATE TABLE kafka_consumed_event (
    event_id       VARCHAR(36)  NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    handled_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id, consumer_group)
);
