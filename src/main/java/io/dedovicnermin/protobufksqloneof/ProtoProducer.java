package io.dedovicnermin.protobufksqloneof;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.dedovicnermin.protobuf.AllTypesOuterClass;
import io.dedovicnermin.protobuf.CustomerOuterClass;
import io.dedovicnermin.protobuf.OrderOuterClass;
import io.dedovicnermin.protobuf.ProductOuterClass;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ProtoProducer {


    public static void main(String[] args) {
        final Properties properties = new Properties();
        try (final InputStream inputStream = ProtoProducer.class.getClassLoader().getResourceAsStream("producer.properties")) {
            properties.load(inputStream);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
        } catch (IOException e) {
            throw new RuntimeException("Issue during property loading",e);
        }

        final String topic = "alltypes";


        System.out.println("\tProducing products...\t");
        try (final KafkaProducer<String, AllTypesOuterClass.AllTypes> producer = new KafkaProducer<>(properties)) {
            int i = 0;
            while (i < 100) {
                final AllTypesOuterClass.AllTypes allTypes = AllTypesOuterClass.AllTypes.newBuilder()
                        .setProduct(
                                ProductOuterClass.Product.newBuilder()
                                        .setProductId(i)
                                        .setProductName("Product" + i).build()
                        ).build();
                final ProducerRecord<String, AllTypesOuterClass.AllTypes> record = new ProducerRecord<>(topic, allTypes);
                producer.send(record, ((recordMetadata, e) -> System.out.println(recordMetadata.toString())));
                i++;
            }
        }

        System.out.println("\tProducing customers...\t");
        try (final KafkaProducer<String, AllTypesOuterClass.AllTypes> producer = new KafkaProducer<>(properties)) {
            int i = 0;
            while (i < 100) {
                final AllTypesOuterClass.AllTypes customer = AllTypesOuterClass.AllTypes.newBuilder()
                        .setCustomer(
                                CustomerOuterClass.Customer.newBuilder()
                                        .setCustomerId(i)
                                        .setCustomerAddress(i + " W. Blvd")
                                        .setCustomerEmail(i + "@email.com")
                                        .setCustomerName("FIRST_LAST:" + i)
                                        .build()

                        ).build();
                final ProducerRecord<String, AllTypesOuterClass.AllTypes> record = new ProducerRecord<>(topic, customer);
                producer.send(record, ((recordMetadata, e) -> System.out.println(recordMetadata.toString())));
                i++;
            }
        }


        System.out.println("\tProducing Orders...\t");
        try (final KafkaProducer<String, AllTypesOuterClass.AllTypes> producer = new KafkaProducer<>(properties)) {
            int i = 0;
            while (i < 100) {
                final CustomerOuterClass.Customer customer = CustomerOuterClass.Customer.newBuilder()
                        .setCustomerId(i)
                        .setCustomerAddress(i + " W. Blvd")
                        .setCustomerEmail(i + "@email.com")
                        .setCustomerName("FIRST_LAST:" + i)
                        .build();
                final ProductOuterClass.Product product = ProductOuterClass.Product.newBuilder()
                        .setProductId(i)
                        .setProductName("Product" + i).build();
                final OrderOuterClass.Order order = OrderOuterClass.Order.newBuilder()
                        .setOrderId(i)
                        .setCustomer(customer)
                        .setOrderAmount(i)
                        .setOrderDate(i + "/" + i + "/" + i)
                        .addProducts(product)
                        .build();
                final AllTypesOuterClass.AllTypes build = AllTypesOuterClass.AllTypes.newBuilder()
                        .setOrder(order).build();
                final ProducerRecord<String, AllTypesOuterClass.AllTypes> record = new ProducerRecord<>(topic, build);
                producer.send(record, ((recordMetadata, e) -> System.out.println(recordMetadata.toString())));
                i++;
            }
        }


        System.out.println("DONE");

    }
}
