package com.asurint.keystone.kafkaretrytest.configuration


import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.RecoverableDataAccessException
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ConsumerRecordRecoverer
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryListener
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.util.backoff.FixedBackOff
import java.util.function.BiFunction


const val BACK_OFF_PERIOD: Long = 1000L // should not be longer than max.poll.interval.ms
const val MAX_ATTEMPTS: Int = 1

private fun buildProducerRecord(key: String?, value: String, topic: String): ProducerRecord<String?, String>? {
//add headers if needed
    return ProducerRecord(topic, null, key, value, null)
}

@Configuration
@EnableKafka
class KafkaConsumerConfiguration {
    private val logger: Logger = LoggerFactory.getLogger(KafkaConsumerConfiguration::class.java)

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String?, String>

    @Value("\${topics.retry}")
    private val retryTopic: String? = "local.accounts.retry"

    @Value("\${topics.dlt}")
    private val deadLetterTopic: String? = "local.accounts"


    var consumerRecordRecoverer = ConsumerRecordRecoverer { consumerRecord: ConsumerRecord<*, *>?, e: Exception ->
        logger.info("Exception in consumerRecordRecoverer : {} ", e.message, e)
        val record = consumerRecord as ConsumerRecord<String?, String>?
        if (e.cause is RecoverableDataAccessException) {
            //recovery logic
            logger.info("Inside Recovery")
            var producerRecord = record?.let { buildProducerRecord(it.key(), it.value(), retryTopic!!) }
           // if (producerRecord != null) {
            //    kafkaTemplate.send(producerRecord)
            //}
             //failureService.saveFailedRecord(record, e, com.learnkafka.config.LibraryEventsConsumerConfig.RETRY)
        } else {
            // non-recovery logic
            logger.info("Inside Non-Recovery")
           // var producerRecord = record?.let { buildProducerRecord(it.key(), it.value(), deadLetterTopic!!) }
           // if (producerRecord != null) {
            //    kafkaTemplate.send(producerRecord)
           // }
            //failureService.saveFailedRecord(record, e, com.learnkafka.config.LibraryEventsConsumerConfig.DEAD)
        }
    }



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
        val errorHandler1 = DefaultErrorHandler(
            consumerRecordRecoverer,
            expBackOff
        )

        //exceptionsToIgnoreList.forEach(errorHandler::addNotRetryableExceptions);
        //exceptionsToRetryList.forEach(errorHandler::addRetryableExceptions)
        errorHandler1
            .setRetryListeners(RetryListener { record: ConsumerRecord<*, *>?, ex: Exception, deliveryAttempt: Int ->
                logger.info( "Failed Record in Retry Listener, Exception : {} , deliveryAttempt : {} ",
                    ex.message,
                    deliveryAttempt)
            })
        return errorHandler1
    }
    @Bean
    fun consumerFactory(properties: KafkaProperties): ConsumerFactory<String?, String?> =
        DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
    @Bean
    fun kafkaListenerContainerFactory(properties: KafkaProperties): ConcurrentKafkaListenerContainerFactory<String?, String?> {
        val factory = ConcurrentKafkaListenerContainerFactory<String?, String?>()
        factory.consumerFactory = consumerFactory(properties)
        factory.setCommonErrorHandler(errorHandler(properties)!!)
        return factory
    }
/*
    @Bean
    fun kafkaListenerContainerFactory(
        configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
        kafkaConsumerFactory: ConsumerFactory<Any?, Any?>?
    ): ConcurrentKafkaListenerContainerFactory<*, *>? {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        configurer.configure(factory, kafkaConsumerFactory)
        factory.setConcurrency(1)
        factory.setCommonErrorHandler(errorHandler()!!)
        // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory
    }*/
}