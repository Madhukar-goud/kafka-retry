package com.asurint.keystone.kafkaretrytest.service

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Printed
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class PromotionUppercaseStream {
    @Bean
    fun kstreamPromotionUppercase(builder: StreamsBuilder): KStream<String, String> {
        val sourceStream = builder.stream("local.accounts", Consumed.with(Serdes.String(), Serdes.String()))
        val uppercaseStream = sourceStream.mapValues { s: String ->
            s.uppercase(
                Locale.getDefault()
            )
        }
        uppercaseStream.to("local.accounts.uppercase")
        sourceStream.print(Printed.toSysOut<String, String>().withLabel("Original stream"))
        uppercaseStream.print(Printed.toSysOut<String, String>().withLabel("Uppercase stream"))
        return sourceStream
    }
}