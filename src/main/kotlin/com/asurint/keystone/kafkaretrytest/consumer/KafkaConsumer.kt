package com.asurint.keystone.kafkaretrytest.consumer

import com.course.avro.data.GetClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.RecoverableDataAccessException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

//@Component
class KafkaConsumer() {

    private val logger: Logger = LoggerFactory.getLogger(KafkaConsumer::class.java)

    //@KafkaListener(topics = ["local.accounts"])
    fun listenRetry(record: ConsumerRecord<String, GetClient>) {
        logger.info("received 'test-retry' record! (value: ${record.value()})")
        throw RecoverableDataAccessException("Temporary Network Issue")
       // logger.info("correlation id : ${record.value().metadata.correlationId}");
       /* val libraryEventOptional: Optional<LibraryEvent> =
            libraryEventsRepository.findById(libraryEvent.getLibraryEventId())
        require(libraryEventOptional.isPresent()) { "Not a valid library Event" }
        log.info("Validation is successful for the library Event : {} ", libraryEventOptional.get())
        *///exceptionService.withRecord(record).iFailButWillRecover()
    }
}