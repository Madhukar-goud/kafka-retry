package com.asurint.keystone.kafkaretrytest.configuration


import com.course.avro.data.GetClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.RecoverableDataAccessException
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.*
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.util.backoff.FixedBackOff


const val BACK_OFF_PERIOD: Long = 1000L // should not be longer than max.poll.interval.ms
const val MAX_ATTEMPTS: Int = 1

private fun buildProducerRecord(key: String?, value: GetClient, topic: String): ProducerRecord<String?, GetClient>? {
//add headers if needed
    return ProducerRecord(topic, null, key, value, null)
}

@Configuration
@EnableKafka
class KafkaConsumerConfiguration {
    private val logger: Logger = LoggerFactory.getLogger(KafkaConsumerConfiguration::class.java)

    @Value("\${topics.retry}")
    private val retryTopic: String? = "local.accounts.retry"

    @Value("\${topics.dlt}")
    private val deadLetterTopic: String? = "local.accounts.dlq"

    var consumerRecordRecoverer = ConsumerRecordRecoverer { consumerRecord: ConsumerRecord<*, *>?, e: Exception ->
        logger.info("Exception in consumerRecordRecoverer : {} ", e.message, e)
        val record = consumerRecord as ConsumerRecord<String?, GetClient>?
        if (e.cause is RecoverableDataAccessException) {
            //recovery logic
            logger.info("Inside Recovery")
            var producerRecord = record?.let { buildProducerRecord(it.key(), it.value(), retryTopic!!) }
            if (producerRecord != null) {
                kafkaTemplate().send(producerRecord)
            }
            //failureService.saveFailedRecord(record, e, com.learnkafka.config.LibraryEventsConsumerConfig.RETRY)
           // failureService!!.saveFailedRecord(record, e, retryTopic)
        } else {
            // non-recovery logic
            logger.info("Inside Non-Recovery")
            /*var producerRecord = record?.let { buildProducerRecord(it.key(), it.value(), deadLetterTopic!!) }
            if (producerRecord != null) {
                kafkaTemplate().send(producerRecord)
            }*/
            //failureService.saveFailedRecord(record, e, com.learnkafka.config.LibraryEventsConsumerConfig.DEAD)
        }
    }

    @Bean
    fun producerFactory(): DefaultKafkaProducerFactory<String, GetClient> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:29092"
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        configProps[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://localhost:8085"
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String?, GetClient> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun errorHandler(kafkaProperties: KafkaProperties): DefaultErrorHandler? {
        /* val exceptionsToIgnoreList: Unit = List.of(
            IllegalArgumentException::class.java
        )
        val exceptionsToRetryList: Unit = List.of(
            RecoverableDataAccessException::class.java
        )*/

        val fixedBackOff = FixedBackOff(1000L, 1)
        val expBackOff = ExponentialBackOffWithMaxRetries(MAX_ATTEMPTS)
        expBackOff.initialInterval = 1000L
        expBackOff.multiplier = 4.0
        expBackOff.maxInterval = 40000L

        var defaultKafkaProducerFactory: DefaultKafkaProducerFactory<String, String> =
            DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties())

        val errorHandler1 = DefaultErrorHandler(
            consumerRecordRecoverer,
            expBackOff
        )
        //val errorHandler1 = DefaultErrorHandler(DeadLetterPublishingRecoverer(kafkaTemplate()), expBackOff)

        //exceptionsToIgnoreList.forEach(errorHandler::addNotRetryableExceptions);
        //exceptionsToRetryList.forEach(errorHandler::addRetryableExceptions)
        errorHandler1
            .setRetryListeners(RetryListener { record: ConsumerRecord<*, *>?, ex: Exception, deliveryAttempt: Int ->
                logger.info(
                    "Failed Record in Retry Listener, Exception : {} , deliveryAttempt : {} ",
                    ex.message,
                    deliveryAttempt
                )
            })
        return errorHandler1
    }

    @Bean
    fun consumerFactory(properties: KafkaProperties): ConsumerFactory<String?, GetClient?>  {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:29092"
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "events-listener-group"
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer::class.java)
        //configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer::class.java)
        configProps[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = StringDeserializer::class.java
        configProps[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = KafkaAvroDeserializer::class.java
        configProps[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "http://localhost:8085"
        configProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        return DefaultKafkaConsumerFactory(configProps)
    }


    @Bean
    fun kafkaListenerContainerFactory(properties: KafkaProperties): ConcurrentKafkaListenerContainerFactory<String?, GetClient?> {
        val factory = ConcurrentKafkaListenerContainerFactory<String?, GetClient?>()
        factory.consumerFactory = consumerFactory(properties)
        factory.setCommonErrorHandler(errorHandler(properties)!!)
        return factory
    }
}
