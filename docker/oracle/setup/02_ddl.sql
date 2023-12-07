alter session set container = XEPDB1;

-- SEQUENCES
CREATE SEQUENCE JAWS_OWNER.PRIORITY_ID
    INCREMENT BY 1
    START WITH 1
    NOCYCLE
    NOCACHE
    ORDER;

CREATE SEQUENCE JAWS_OWNER.COMPONENT_ID
    INCREMENT BY 1
    START WITH 1
    NOCYCLE
    NOCACHE
    ORDER;

CREATE SEQUENCE JAWS_OWNER.TEAM_ID
    INCREMENT BY 1
    START WITH 1
    NOCYCLE
    NOCACHE
    ORDER;

CREATE SEQUENCE JAWS_OWNER.ACTION_ID
    INCREMENT BY 1
    START WITH 1
    NOCYCLE
    NOCACHE
    ORDER;

CREATE SEQUENCE JAWS_OWNER.ALARM_ID
    INCREMENT BY 1
    START WITH 1
    NOCYCLE
	NOCACHE
	ORDER;

CREATE SEQUENCE JAWS_OWNER.LOCATION_ID
    INCREMENT BY 1
    START WITH 1
    NOCYCLE
    NOCACHE
    ORDER;

-- TABLES
CREATE TABLE JAWS_OWNER.TEAM
(
    TEAM_ID INTEGER NOT NULL,
    NAME    VARCHAR2(64 CHAR) NOT NULL,
    CONSTRAINT TEAM_PK PRIMARY KEY (TEAM_ID),
    CONSTRAINT TEAM_AK1 UNIQUE (NAME)
);

CREATE TABLE JAWS_OWNER.COMPONENT
(
    COMPONENT_ID INTEGER NOT NULL,
    TEAM_ID      INTEGER NOT NULL,
    NAME         VARCHAR2(64 CHAR) NOT NULL,
    CONSTRAINT COMPONENT_PK PRIMARY KEY (COMPONENT_ID),
    CONSTRAINT COMPONENT_FK1 FOREIGN KEY (TEAM_ID) REFERENCES JAWS_OWNER.TEAM (TEAM_ID) ON DELETE SET NULL,
    CONSTRAINT COMPONENT_AK1 UNIQUE (NAME)
);

CREATE TABLE JAWS_OWNER.PRIORITY
(
    PRIORITY_ID INTEGER NOT NULL,
    NAME        VARCHAR2(64 CHAR) NOT NULL,
    WEIGHT      INTEGER NULL,
    CONSTRAINT PRIORITY_PK PRIMARY KEY (PRIORITY_ID),
    CONSTRAINT PRIORITY_AK1 UNIQUE (NAME)
);

CREATE TABLE JAWS_OWNER.ACTION
(
    ACTION_ID         INTEGER NOT NULL,
    COMPONENT_ID      INTEGER NOT NULL,
    PRIORITY_ID       INTEGER NOT NULL,
    NAME              VARCHAR2(64 CHAR) NOT NULL,
    CORRECTIVE_ACTION CLOB NOT NULL,
    RATIONALE         CLOB NOT NULL,
    FILTERABLE        CHAR(1 CHAR) DEFAULT 'Y' NOT NULL,
    LATCHABLE         CHAR(1 CHAR) DEFAULT 'N' NOT NULL,
    ON_DELAY_SECONDS  INTEGER NULL,
    OFF_DELAY_SECONDS INTEGER NULL,
    CONSTRAINT ACTION_PK PRIMARY KEY (ACTION_ID),
    CONSTRAINT ACTION_FK1 FOREIGN KEY (COMPONENT_ID) REFERENCES JAWS_OWNER.COMPONENT (COMPONENT_ID) ON DELETE SET NULL,
    CONSTRAINT ACTION_FK3 FOREIGN KEY (PRIORITY_ID) REFERENCES JAWS_OWNER.PRIORITY (PRIORITY_ID) ON DELETE SET NULL,
    CONSTRAINT ACTION_AK1 UNIQUE (NAME),
    CONSTRAINT ACTION_CK1 CHECK (FILTERABLE IN ('Y', 'N')),
    CONSTRAINT ACTION_CK2 CHECK (LATCHABLE IN ('Y', 'N'))
);


CREATE TABLE JAWS_OWNER.ALARM
(
    ALARM_ID             INTEGER NOT NULL,
    ACTION_ID            INTEGER NOT NULL,
    NAME                 VARCHAR2(64 CHAR) NOT NULL,
    DEVICE               VARCHAR2(64 CHAR) NULL,
    SCREEN_COMMAND       VARCHAR2(512 CHAR) NULL,
    MASKED_BY            VARCHAR2(64 CHAR) NULL,
    CONSTRAINT ALARM_PK PRIMARY KEY (ALARM_ID),
    CONSTRAINT ALARM_AK1 UNIQUE (NAME),
    CONSTRAINT ALARM_FK1 FOREIGN KEY (ACTION_ID) REFERENCES JAWS_OWNER.ACTION (ACTION_ID) ON DELETE SET NULL
);

CREATE TABLE JAWS_OWNER.LOCATION
(
    LOCATION_ID INTEGER NOT NULL,
    PARENT_ID   INTEGER NULL,
    NAME        VARCHAR2(64 CHAR) NOT NULL,
    WEIGHT      INTEGER NULL,
    CONSTRAINT LOCATION_PK PRIMARY KEY (LOCATION_ID),
    CONSTRAINT LOCATION_FK1 FOREIGN KEY (PARENT_ID) REFERENCES JAWS_OWNER.LOCATION (LOCATION_ID) ON DELETE SET NULL,
    CONSTRAINT LOCATION_AK1 UNIQUE (NAME)
);

CREATE TABLE JAWS_OWNER.ALARM_LOCATION
(
    ALARM_ID    INTEGER NOT NULL,
    LOCATION_ID INTEGER NOT NULL,
    CONSTRAINT ALARM_LOCATION_PK PRIMARY KEY (ALARM_ID, LOCATION_ID)
);

CREATE TABLE JAWS_OWNER.ALARM_SOURCE_EPICS
(
    ALARM_ID INTEGER NOT NULL,
    PV       VARCHAR2(64 CHAR) NOT NULL,
    CONSTRAINT ALARM_SOURCE_EPICS_PK PRIMARY KEY (ALARM_ID)
);
