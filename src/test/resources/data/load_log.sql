INSERT INTO xx_log (ID,CREATE_DATE,MODIFY_DATE,CONTENT,IP,OPERATION,OPERATOR,PARAMETER) SELECT * FROM CSVREAD('src/test/resources/data/imports/XX_LOG.csv', null, 'UTF-8', ',');