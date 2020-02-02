# A Common library template project

JDepend: Summary
项目名称 含义
TC: 类的总数
AC: 抽象类的数量
CC: 实装类的数量
AC: 向心度（外部依赖类的数量）
EC: 离心度（依赖外部类的数量）
A : 抽象度
I : 不稳定度
D : 距离（抽象性和稳定性的平衡指标）
V :

maven-jdepend-plugin:register:
maven-checkstyle-plugin:register:
maven-javadoc-plugin:register:
maven-jxr-plugin:register:
maven-license-plugin:register:
maven-changes-plugin:register:
maven-changelog-plugin:register:
maven-developer-activity-plugin:register:
maven-file-activity-plugin:register:
maven-junit-report-plugin:register:
maven-tasklist-plugin:register:


mvn --version
mvn -B archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DgroupId=net.tiny -DartifactId=tiny-boot
mvn validate
mvn compile
mvn test
mvn package -Dmaven.test.skip
mvn install
mvn deploy
mvn clean
mvn site
mvn dependency:tree
mvn dependency:copy-dependencies -DoutputDirectory=lib
mvn assembly:assembly -DdescriptorId=bin
mvn javadoc:javadoc
mvn dependency:sources
mvn dependency:tree
mvn license:license-list
mvn license:update-file-header

mvn -q -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -Dorg.slf4j.simpleLogger.log.net.sourceforge.pmd=error clean test

mvn -Duser.timezone=JST clean jacoco:prepare-agent test jacoco:report site
mvn -Duser.timezone=JST clean clover:setup test clover:clover clover:aggregate site
mvn -Pdevelopment -s settings.xml -q clean clover:setup test clover:aggregate clover:clover site