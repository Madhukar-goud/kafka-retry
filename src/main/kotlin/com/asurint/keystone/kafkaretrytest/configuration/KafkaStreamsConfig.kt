package com.asurint.keystone.kafkaretrytest.configuration

import com.course.avro.data.GetClient
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration

@Configuration
@EnableKafkaStreams
class KafkaStreamConfig {
    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun kafkaStreamsConfig(): KafkaStreamsConfiguration {
        val props = HashMap<String, Any>()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "kafka-stream-" + System.currentTimeMillis()
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:29092"
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        props[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = "3000"
        props[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = StreamsConfig.EXACTLY_ONCE_V2
        props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
        return KafkaStreamsConfiguration(props)
    }
}
