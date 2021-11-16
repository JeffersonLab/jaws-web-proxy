package org.jlab.jaws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.jlab.jaws.entity.*;
import org.jlab.jaws.eventsource.*;
import org.jlab.jaws.json.*;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Path("/sse")
@WebListener
public class SSE implements ServletContextListener {
    // max 10 concurrent users
    private ExecutorService exec = Executors.newFixedThreadPool(10);
    private Sse sse;

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        System.err.println("Attempting to stop executor service");

        exec.shutdownNow();

        try {
            exec.awaitTermination(8, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Timeout while awaiting shutdown");
            e.printStackTrace();
        }
    }

    @Context
    public void setSse(Sse sse) {
        this.sse = sse;
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listen(@Context final SseEventSink sink,
                       @QueryParam("classIndex") @DefaultValue("-1") long classIndex,
                       @QueryParam("registrationIndex") @DefaultValue("-1") long registrationIndex,
                       @QueryParam("effectiveIndex") @DefaultValue("-1") long effectiveIndex) {
        System.err.println("Proxy connected: classIndex: " + classIndex +
                ", registrationIndex: " + registrationIndex +
                ", effectiveIndex: " + effectiveIndex);

        exec.execute(new Runnable() {

            @Override
            public void run() {
                final Properties classProps = getClassProps(JaxRSApp.BOOTSTRAP_SERVERS, JaxRSApp.SCHEMA_REGISTRY);
                final Properties registrationProps = getRegistrationProps(JaxRSApp.BOOTSTRAP_SERVERS, JaxRSApp.SCHEMA_REGISTRY);
                final Properties effectiveProps = getEffectiveProps(JaxRSApp.BOOTSTRAP_SERVERS, JaxRSApp.SCHEMA_REGISTRY);

                try (
                        EventSourceTable2<String, AlarmClass> classTable = new EventSourceTable2<>(classProps, classIndex);
                        EventSourceTable2<String, AlarmRegistration> registrationTable = new EventSourceTable2<>(registrationProps, registrationIndex);
                        EventSourceTable2<String, EffectiveRegistration> effectiveTable = new EventSourceTable2<>(effectiveProps, effectiveIndex);
                ) {

                    classTable.addListener(new EventSourceListener<String, AlarmClass>() {
                        @Override
                        public void initialState(LinkedHashMap<String, EventSourceRecord<String, AlarmClass>> records) {
                            sendClassRecords(sink, records.values());
                            sink.send(sse.newEvent("class-highwatermark", ""));
                        }

                        @Override
                        public void changes(LinkedHashMap<String, EventSourceRecord<String, AlarmClass>> records) {
                            sendClassRecords(sink, records.values());
                        }

                    });

                    registrationTable.addListener(new EventSourceListener<String, AlarmRegistration>() {
                        @Override
                        public void initialState(LinkedHashMap<String, EventSourceRecord<String, AlarmRegistration>> records) {
                            sendRegistrationRecords(sink, records.values());
                            sink.send(sse.newEvent("registration-highwatermark", ""));
                        }

                        @Override
                        public void changes(LinkedHashMap<String, EventSourceRecord<String, AlarmRegistration>> records) {
                            sendRegistrationRecords(sink, records.values());
                        }

                    });

                    effectiveTable.addListener(new EventSourceListener<String, EffectiveRegistration>() {
                        @Override
                        public void initialState(LinkedHashMap<String, EventSourceRecord<String, EffectiveRegistration>> records) {
                            sendEffectiveRecords(sink, records.values());
                            sink.send(sse.newEvent("effective-highwatermark", ""));
                        }

                        @Override
                        public void changes(LinkedHashMap<String, EventSourceRecord<String, EffectiveRegistration>> records) {
                            sendEffectiveRecords(sink, records.values());
                        }

                    });

                    classTable.start();
                    registrationTable.start();
                    effectiveTable.start();


                    try {
                        while (!sink.isClosed()) {
                            sink.send(sse.newEvent("ping", ":"));  // Actively check for connection
                            System.err.println("Looping checking for disconnect");
                            Thread.sleep(5000);
                        }
                    } catch (InterruptedException e) {
                        System.err.println("SSE Thread interrupted, shutting down");
                        e.printStackTrace();
                    }
                }

                System.err.println("Proxy disconnected");
            }
        });
    }

    private Properties getClassProps(String servers, String registry) {
        final Properties props = new Properties();

        final SpecificAvroSerde<AlarmClass> VALUE_SERDE = new SpecificAvroSerde<>();

        props.put(EventSourceConfig.EVENT_SOURCE_GROUP, "web-admin-gui-" + Instant.now().toString() + "-" + Math.random());
        props.put(EventSourceConfig.EVENT_SOURCE_TOPIC, JaxRSApp.CLASSES_TOPIC);
        props.put(EventSourceConfig.EVENT_SOURCE_BOOTSTRAP_SERVERS, servers);
        props.put(EventSourceConfig.EVENT_SOURCE_KEY_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(EventSourceConfig.EVENT_SOURCE_VALUE_DESERIALIZER, VALUE_SERDE.deserializer().getClass().getName());

        // Deserializer specific configs
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, registry);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");

        return props;
    }

    private Properties getRegistrationProps(String servers, String registry) {
        final Properties props = new Properties();

        final SpecificAvroSerde<AlarmRegistration> VALUE_SERDE = new SpecificAvroSerde<>();

        props.put(EventSourceConfig.EVENT_SOURCE_GROUP, "web-admin-gui-" + Instant.now().toString() + "-" + Math.random());
        props.put(EventSourceConfig.EVENT_SOURCE_TOPIC, JaxRSApp.REGISTRATION_TOPIC);
        props.put(EventSourceConfig.EVENT_SOURCE_BOOTSTRAP_SERVERS, servers);
        props.put(EventSourceConfig.EVENT_SOURCE_KEY_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(EventSourceConfig.EVENT_SOURCE_VALUE_DESERIALIZER, VALUE_SERDE.deserializer().getClass().getName());

        // Deserializer specific configs
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, registry);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");

        return props;
    }

    private Properties getEffectiveProps(String servers, String registry) {
        final Properties props = new Properties();

        final SpecificAvroSerde<EffectiveRegistration> VALUE_SERDE = new SpecificAvroSerde<>();

        props.put(EventSourceConfig.EVENT_SOURCE_GROUP, "web-admin-gui-" + Instant.now().toString() + "-" + Math.random());
        props.put(EventSourceConfig.EVENT_SOURCE_TOPIC, JaxRSApp.EFFECTIVE_TOPIC);
        props.put(EventSourceConfig.EVENT_SOURCE_BOOTSTRAP_SERVERS, servers);
        props.put(EventSourceConfig.EVENT_SOURCE_KEY_DESERIALIZER, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(EventSourceConfig.EVENT_SOURCE_VALUE_DESERIALIZER, VALUE_SERDE.deserializer().getClass().getName());

        // Deserializer specific configs
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, registry);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");

        return props;
    }

    private void sendClassRecords(SseEventSink sink, Collection<EventSourceRecord<String, AlarmClass>> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        ObjectMapper mapper = new ObjectMapper();

        mapper.addMixIn(AlarmClass.class, AlarmClassMixin.class);

        for (EventSourceRecord<String, AlarmClass> record : records) {
            String key = record.getKey();
            AlarmClass value = record.getValue();

            String jsonValue = null;

            if (value != null) {
                try {
                    jsonValue = mapper.writeValueAsString(value);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String recordString = "{\"key\": \"" + key + "\", \"value\": " + jsonValue + ", \"offset\": " + record.getOffset() + "},";

            builder.append(recordString);
        }

        int i = builder.lastIndexOf(",");

        if(i == -1) {
            builder.append("]");
        } else {
            builder.replace(i, i + 1, "]");
        }

        sink.send(sse.newEvent("class", builder.toString()));
    }

    private void sendRegistrationRecords(SseEventSink sink, Collection<EventSourceRecord<String, AlarmRegistration>> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        ObjectMapper mapper = new ObjectMapper();

        mapper.addMixIn(AlarmRegistration.class, AlarmRegistrationMixin.class);
        mapper.addMixIn(SimpleProducer.class, SimpleProducerMixin.class);
        mapper.addMixIn(EPICSProducer.class, EPICSProducerMixin.class);
        mapper.addMixIn(CALCProducer.class, CALCProducerMixin.class);

        for (EventSourceRecord<String, AlarmRegistration> record : records) {
            String key = record.getKey();
            AlarmRegistration value = record.getValue();

            String jsonValue = null;

            if (value != null) {
                try {
                    jsonValue = mapper.writeValueAsString(value);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String recordString = "{\"key\": \"" + key + "\", \"value\": " + jsonValue + ", \"offset\": " + record.getOffset() + "},";

            builder.append(recordString);
        }

        int i = builder.lastIndexOf(",");

        if(i == -1) {
            builder.append("]");
        } else {
            builder.replace(i, i + 1, "]");
        }

        sink.send(sse.newEvent("registration", builder.toString()));
    }

    private void sendEffectiveRecords(SseEventSink sink, Collection<EventSourceRecord<String, EffectiveRegistration>> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        ObjectMapper mapper = new ObjectMapper();

        mapper.addMixIn(EffectiveRegistration.class, EffectiveRegistrationMixin.class);
        mapper.addMixIn(AlarmRegistration.class, AlarmRegistrationMixin.class);
        mapper.addMixIn(SimpleProducer.class, SimpleProducerMixin.class);
        mapper.addMixIn(EPICSProducer.class, EPICSProducerMixin.class);
        mapper.addMixIn(CALCProducer.class, CALCProducerMixin.class);

        for (EventSourceRecord<String, EffectiveRegistration> record : records) {
            String key = record.getKey();
            EffectiveRegistration value = record.getValue();

            String jsonValue = null;

            if (value != null) {
                try {
                    jsonValue = mapper.writeValueAsString(value);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String recordString = "{\"key\": \"" + key + "\", \"value\": " + jsonValue + ", \"offset\": " + record.getOffset() + "},";

            builder.append(recordString);
        }

        int i = builder.lastIndexOf(",");

        if(i == -1) {
            builder.append("]");
        } else {
            builder.replace(i, i + 1, "]");
        }

        sink.send(sse.newEvent("effective", builder.toString()));
    }
}
