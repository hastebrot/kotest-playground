package playground

import com.google.protobuf.util.JsonFormat
import playground.proto.ProtoProduct

fun main() {
    val product = ProtoProduct.newBuilder().build()

    val jsonPrinter = JsonFormat.printer().includingDefaultValueFields()
    jsonPrinter.print(product).println
}

val <T> T?.println get() = println(this)
