create sequence xx_log_sequence increment by 1 start with 1;

CREATE TABLE IF NOT EXISTS xx_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date TIMESTAMP NOT NULL,
    modify_date TIMESTAMP NOT NULL,
    content CLOB,
    ip VARCHAR(255) NOT NULL,
    operation VARCHAR(255) NOT NULL,
    operator VARCHAR(255),
    parameter CLOB,
    PRIMARY KEY (id)
);
COMMENT ON TABLE xx_log IS '日志';