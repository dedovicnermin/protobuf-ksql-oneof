# Using protobuf's `oneof`
```protobuf
syntax = "proto3";
package io.dedovicnermin.protobuf;

import "Customer.proto";
import "Order.proto";
import "Product.proto";

message AllTypes {
  oneof msg {
    Customer customer = 1;
    Product product = 2;
    Order order = 3;
  }
}
```

Steps:
- `docker compose up -d`
  - reach broker via cli with bootstrap server `localhost:29092` 
- `mvn clean install`
- `mvn schema-registry:download && mvn schema-registry:register`
  - this might return an error depending on if SR/Broker are ready to accept requests  

- run `ProtobufProducer.main()` 
  - easiest way is via IDE or
  - `java -jar target/protobuf-ksql-oneof-1.0-SNAPSHOT-jar-with-dependencies.jar`

- log into ksql ([binary install here](https://docs.confluent.io/platform/current/ksqldb/installing.html#installing-ksqldb))
  -`ksql http://ksqldb-server:8088`

- execute the following :
  - `set 'auto.offset.reset' = 'earliest';`
  - `list topics;`
  - `print alltypes;` (print topic)

#### BASE (execute me)
```
CREATE STREAM ALL_TYPES WITH (
    KAFKA_TOPIC = 'alltypes',
    VALUE_FORMAT='PROTOBUF'
);
```

> NOTE: when querying the above in ksql, only the correct event type is populated with struct, the other struct's are shown as null. View below (truncated)

```
select * from all_types emit changes;
+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|MSG_0                                                                                                                                                                                                                       |
+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|{CUSTOMER=null, PRODUCT={PRODUCT_ID=0, PRODUCT_NAME=Product0}, ORDER=null}                                                                                                                                                  |
|{CUSTOMER={CUSTOMER_ID=0, CUSTOMER_NAME=FIRST_LAST:0, CUSTOMER_EMAIL=0@email.com, CUSTOMER_ADDRESS=0 W. Blvd}, PRODUCT=null, ORDER=null}                                                                                    |
|{CUSTOMER=null, PRODUCT=null, ORDER={ORDER_ID=99, ORDER_DATE=99/99/99, ORDER_AMOUNT=99, PRODUCTS=[{PRODUCT_ID=99, PRODUCT_NAME=Product99}], CUSTOMER={CUSTOMER_ID=99, CUSTOMER_NAME=FIRST_LAST:99, CUSTOMER_EMAIL=99@email.c|
|om, CUSTOMER_ADDRESS=99 W. Blvd}}}                                                                                                                                                                                          |


```
 

`DESCRIBE ALL_TYPES` RETURNS:
```
 Field | Type
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 MSG_0 | STRUCT<CUSTOMER STRUCT<CUSTOMER_ID BIGINT, CUSTOMER_NAME VARCHAR(STRING), CUSTOMER_EMAIL VARCHAR(STRING), CUSTOMER_ADDRESS VARCHAR(STRING)>, PRODUCT STRUCT<PRODUCT_ID BIGINT, PRODUCT_NAME VARCHAR(STRING)>, ORDER STRUCT<ORDER_ID INTEGER, ORDER_DATE VARCHAR(STRING), ORDER_AMOUNT INTEGER, PRODUCTS ARRAY<STRUCT<PRODUCT_ID BIGINT, PRODUCT_NAME VARCHAR(STRING)>>, CUSTOMER STRUCT<CUSTOMER_ID BIGINT, CUSTOMER_NAME VARCHAR(STRING), CUSTOMER_EMAIL VARCHAR(STRING), CUSTOMER_ADDRESS VARCHAR(STRING)>>>
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
```
 

#### BASE->CUSTOMERS ONLY
```
CREATE STREAM CUSTOMERS WITH (KAFKA_TOPIC='customers-ksql', VALUE_FORMAT='PROTOBUF')
    AS SELECT MSG_0->CUSTOMER AS CUSTOMER
    FROM ALL_TYPES 
    WHERE MSG_0->CUSTOMER IS NOT NULL 
    EMIT CHANGES;
```

```
+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|CUSTOMER                                                                                                                                                                                                                                                    |
+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|{CUSTOMER_ID=0, CUSTOMER_NAME=FIRST_LAST:0, CUSTOMER_EMAIL=0@email.com, CUSTOMER_ADDRESS=0 W. Blvd}                                                                                                                                                         |
|{CUSTOMER_ID=1, CUSTOMER_NAME=FIRST_LAST:1, CUSTOMER_EMAIL=1@email.com, CUSTOMER_ADDRESS=1 W. Blvd}                                                                                                                                                         |
|{CUSTOMER_ID=2, CUSTOMER_NAME=FIRST_LAST:2, CUSTOMER_EMAIL=2@email.com, CUSTOMER_ADDRESS=2 W. Blvd}                                                                                                                                                         |
```


##### CUSTOMERS_PARSED
```
CREATE STREAM CUSTOMERS_P WITH (KAFKA_TOPIC='customers-parsed', VALUE_FORMAT='PROTOBUF') AS SELECT
    CUSTOMER->CUSTOMER_ID AS ID,
    CUSTOMER->CUSTOMER_NAME AS NAME,
    CUSTOMER->CUSTOMER_EMAIL AS EMAIL,
    CUSTOMER->CUSTOMER_ADDRESS AS ADDRESS
FROM CUSTOMERS
EMIT CHANGES;
```

```
SELECT * FROM CUSTOMERS_P EMIT CHANGES LIMIT 3;
+-------------------------------------------------------------+-------------------------------------------------------------+-------------------------------------------------------------+-------------------------------------------------------------+
|ID                                                           |NAME                                                         |EMAIL                                                        |ADDRESS                                                      |
+-------------------------------------------------------------+-------------------------------------------------------------+-------------------------------------------------------------+-------------------------------------------------------------+
|0                                                            |FIRST_LAST:0                                                 |0@email.com                                                  |0 W. Blvd                                                    |
|1                                                            |FIRST_LAST:1                                                 |1@email.com                                                  |1 W. Blvd                                                    |
|2                                                            |FIRST_LAST:2                                                 |2@email.com                                                  |2 W. Blvd                                                    |
```

#### BASE->PRODUCTS ONLY
```
CREATE STREAM PRODUCTS 
  WITH (KAFKA_TOPIC='products-ksql', VALUE_FORMAT='PROTOBUF') AS
  SELECT MSG_0->PRODUCT AS PRODUCT 
FROM ALL_TYPES
  WHERE MSG_0->PRODUCT IS NOT NULL
  EMIT CHANGES;
```

```
SELECT * FROM PRODUCTS EMIT CHANGES LIMIT 3;
+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|PRODUCT                                                                                                                                                                                                                                                     |
+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
|{PRODUCT_ID=0, PRODUCT_NAME=Product0}                                                                                                                                                                                                                       |
|{PRODUCT_ID=1, PRODUCT_NAME=Product1}                                                                                                                                                                                                                       |
|{PRODUCT_ID=2, PRODUCT_NAME=Product2}                                                                                                                                                                                                                       |
```


#### PRODUCTS PARSED
```
CREATE STREAM PRODUCTS_P 
WITH (KAFKA_TOPIC='products-parsed', VALUE_FORMAT='PROTOBUF') 
  AS SELECT PRODUCT->PRODUCT_ID AS ID, PRODUCT->PRODUCT_NAME AS NAME
FROM PRODUCTS
EMIT CHANGES;
```


```
SELECT * FROM PRODUCTS_P EMIT CHANGES LIMIT 3;
+-----------------------------------------------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------+
|ID                                                                                                                           |NAME                                                                                                                         |
+-----------------------------------------------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------+
|0                                                                                                                            |Product0                                                                                                                     |
|1                                                                                                                            |Product1                                                                                                                     |
|2                                                                                                                            |Product2                                                                                                                     |
```