package com.asurint.keystone.kafkaretrytest.configuration

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import java.lang.Exception


class GlobalErrorHandler : CommonErrorHandler {
    private val logger: Logger = LoggerFactory.getLogger(GlobalErrorHandler::class.java)

    override fun handleRecord(
        thrownException: Exception,
        record: ConsumerRecord<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer
    ) {
        logger.info("Global error handler for message: {}", record.value().toString())
    }

    override fun handleOtherException(
        thrownException: Exception,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        batchListener: Boolean
    ) {
        logger.info("IN +++Global error handler for message: {}")
    }
}