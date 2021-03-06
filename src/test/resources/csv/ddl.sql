CREATE TABLE LOG_MESSAGE (
    MESSAGE_ID CHAR(8) NOT NULL,
    MESSAGE VARCHAR(1024) NOT NULL,
    CONSTRAINT LOG_MESSAGE_PK PRIMARY KEY (MESSAGE_ID)
);

CREATE TABLE APPLICATION_MESSAGE (
    MESSAGE_ID CHAR(8) NOT NULL,
    MESSAGE VARCHAR(1024) NOT NULL,
    CLIENT_ENABLED CHAR(1) DEFAULT '0' NOT NULL,
    SUBSYSTEM_ID CHAR(4) NOT NULL,
    LAST_UPDATE TIMESTAMP NOT NULL,
    CONSTRAINT APPLICATION_MESSAGE_PK PRIMARY KEY (MESSAGE_ID)
);

DROP TABLE APPLICATION_MESSAGE;
DROP TABLE LOG_MESSAGE;

ALTER TABLE APPLICATION_MESSAGE ALTER COLUMN CLIENT_ENABLED BOOLEAN;

