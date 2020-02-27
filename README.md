## Tiny JPA: 一个基于JPA2.2规范的微服框架
## 设计目的
 - 提供一些基于JDBC底层数据库属性的访问工具。
 - 提供一个泛用JPA实体访问REST服务。
 - 提供JPA实体实装时经常使用的创建日期，更新日期，排序属性的基类。
 - 提供高速加载和输出CSV数据文件的控制接口(API)。

## Usage

### 1. How to use JDBC Database Tool (java)
```java
SchemaParser parser = new SchemaParser(connection);
Schema schema = parser.parse("PUBLIC");
List<Table> tables = schema.getTables();
Table table = tables.get(0);
List<Column> columns = table.getColumns();
Column column = columns.get(0);
PrimaryKey primaryKey = column.getPrimaryKey();
String columnName = column.getColumnName();
String type = column.getTypeName();
int size = column.getColumnSize();
boolean isKey = column.isPrimaryKey();
```


### 2. Access JPA entities using REST services
 - Sample configuration file : application-dao.yml

```txt
handler:
  dao:
    class:    net.tiny.dao.EntityService
    path:     /dao/v1
    auth:     ${auth.base}
    pattern:  .*/classes/, .*/test-classes/, .*/your-entity.*[.]jar
    entities: your.package.entity.*
    level:    INFO
auth:
  base:
    class:    net.tiny.ws.auth.SimpleAuthenticator
    encode:   false
    username: user
    password: password
```

 - Access REST API

```txt
 GET    /dao/v1/{entity}/{id}
 GET    /dao/v1/{entity}/list?size=99
 GET    /dao/v1/{entity}/count
 POST   /dao/v1/{entity}
 PUT    /dao/v1/{entity}/{id}
 DELETE /dao/v1/{entity}/{id}

 Run a command line:
 curl -u user:password -v http://localhost:8080//dao/v1/table_name/1234
```

### 3. Import and Export CSV data
 - Import CSV data sample (java)

```java
CsvImporter.Options options = new CsvImporter.Options("XX_LOG.csv", "xx_log")
        .verbose(true)
        .truncated(true)
        .skip(1);
CsvImporter.load(connection, options);
```

 - Batch import CSV data set to tables (java)

```java
CsvImporter.load(conn, "imports/csv");
```

 - Prepare batch import 'imports/csv/table-ordering.txt' file and it's csv files

```txt
table_name_1
table_name_2
```

### 4. How to implement a DAO service class

 - Step.1: Implement a entity class extends BaseEntity or OrderEntity

```java
@Entity
@Table(name = "xx_log")
public class Log extends BaseEntity {
    /** ID */
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "logSequenceGenerator")
    @SequenceGenerator(name = "logSequenceGenerator", sequenceName = "xx_log_sequence", allocationSize=1)
    @Id
    @Column(name = "id")
    private Long id;
    //...
}
```

 - Step.2: Append a define of entity class in 'META-INF/persistence.xml'

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence
    xmlns="http://xmlns.jcp.org/xml/ns/persistence"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence    http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd"
    version="2.1">
    <persistence-unit name="persistenceUnit" transaction-type="RESOURCE_LOCAL">
        <mapping-file>META-INF/orm.xml</mapping-file>
        <!-- Basic Entities -->
        <class>net.tiny.dao.converter.LocalDateAttributeConverter</class>
        <class>net.tiny.dao.converter.LocalDateTimeAttributeConverter</class>
        <class>net.tiny.dao.entity.BaseEntity</class>
        <class>net.tiny.dao.entity.OrderEntity</class>
        <class>net.tiny.dao.entity.LockableEntity</class>

        <!-- Application Entities -->
        <class>your.package.entity.Log</class>
    </persistence-unit>
</persistence>
```

 - Step.3: Implement a custom dao class (Optional)

```java
public class LogDao extends BaseDao<Log, Long> {
    // Append your code here
}
```

 - Step.4: Implement a service class using dao

```java
public class LogService extends BaseService<Log> {
    public LogService(ServiceContext c) {
        super(c, Account.class);
    }
    public LogService(BaseService<?> base) {
        super(base, Account.class);
    }

    // Append your code here
    public Optional<Log> findByDate(Date date) {
        if (date == null) {
            return Optional.empty();
        }
        try {
           Log log = super.dao()
                    .getNamedQuery("Log.findByDate")
                    .setParameter("date", date)
                    .getSingleResult();
            return Optional.of(log);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
```


### 5. JPA support configuration file sample

```yaml
logging:
  handler:
    output: none
  level:
    all: INFO
main:
  - ${launcher.http}
daemon: true
executor: ${pool}
callback: ${service.context}
pool:
  class:   net.tiny.service.PausableThreadPoolExecutor
  size:    5
  max:     10
  timeout: 3
service:
  context:
    class: net.tiny.service.ServiceLocator
# HTTP Server launcher
launcher:
  http:
    class: net.tiny.ws.Launcher
    builder:
      port: 8092
      backlog: 10
      stopTimeout: 1
      executor: ${pool}
      handlers:
        - ${handler.sys}
        - ${handler.health}
        - ${handler.dao}
handler:
  sys:
    class:   net.tiny.ws.ControllableHandler
    path:    /sys
    auth:    ${auth.base}
    filters: ${filter.logger}
  dao:
    class:    net.tiny.dao.EntityService
    path:     /dao/v1
    auth:     ${auth.base}
    filters:  ${filter.jpa}, ${filter.logger}
    entities: your.entity.*
    pattern:  .*/classes/, .*/your-entity.*[.]jar
filter:
   logger:
     class: net.tiny.ws.AccessLogger
     out:   stdout
   jpa:
     class: net.tiny.dao.HttpTransactionFilter
     producer: ${jpa.producer}
auth:
  base:
    class:    net.tiny.ws.auth.SimpleAuthenticator
    encode:   false
    username: user
    password: password
# JPA
jpa:
  producer:
    class: net.tiny.dao.EntityManagerProducer
    properties: ${jpa.properties}
# JPA2 Properties
  properties:
    javax:
      persistence:
         jdbc:
           driver:   org.h2.Driver
           url:      jdbc:h2:tcp://127.0.0.1:9092/h2
           user:     sa
           password: sa
#           user:     ${${vcap.alias}.cf.username}
#           password: ${${vcap.alias}.cf.password}
           show_sql: true
         provider: org.eclipse.persistence.jpa.PersistenceProvider
         validation:
           mode: auto
         lock:
           timeout: 1000
         query:
           timeout: 1000
         schema-generation:
           database:
             action: create
           create-source: metadata
#Supported platforms : JavaDB Derby Oracle MySQL4 PostgreSQL SQLServer DB2 DB2Mainframe Sybase H2 HSQL
#Others available : Informix TimesTen Attunity SQLAnyWhere DBase Cloudscape PointBase
  eclipselink:
    target-database: HSQL
    logging:
      level: INFO
      level:
        sql: FINE
      parameters: true
    jdbc:
      connection_pool:
        default:
          initial: 2
          min:     2
          max:     5
    weaving:
      changetracking: false
```

## More Detail, See The Samples

---
Email   : wuweibg@gmail.com
