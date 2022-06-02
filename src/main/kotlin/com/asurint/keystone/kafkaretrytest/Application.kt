package com.asurint.keystone.kafkaretrytest

import com.asurint.keystone.kafkaretrytest.configuration.KafkaProducerConfiguration
import com.course.avro.data.GetClient
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class Application

fun main(args: Array<String>) {
	//runApplication<Application>(*args)
	val ctx: ApplicationContext = SpringApplication.run(com.asurint.keystone.kafkaretrytest.Application::class.java, *args)
	val testKafkaProducer: KafkaProducerConfiguration = ctx.getBean(KafkaProducerConfiguration::class.java)

	var client = GetClient.newBuilder().setFullName("testname").setActive(true).setMaritalStatus("Married").build()
	//testKafkaProducer.sendMessage(client, "local.accounts")

}
