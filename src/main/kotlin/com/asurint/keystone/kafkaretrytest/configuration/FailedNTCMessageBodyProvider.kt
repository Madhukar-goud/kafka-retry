package com.asurint.keystone.kafkaretrytest.configuration

import com.course.avro.data.GetClient
import org.apache.kafka.common.header.Headers
import java.util.function.BiFunction


class FailedNTCMessageBodyProvider :
    BiFunction<ByteArray?, Headers?, GetClient> {
    override fun apply(t: ByteArray?, u: Headers?): GetClient {
        return NTCBadMessageBody(t)
    }
}

class NTCBadMessageBody(val failedDecode: ByteArray?) : GetClient()

