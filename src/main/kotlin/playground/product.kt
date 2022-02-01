package playground

import com.google.protobuf.util.JsonFormat

fun main() {
    val product = ProtoProduct.newBuilder().build()

    val jsonPrinter = JsonFormat.printer().includingDefaultValueFields()
    jsonPrinter.print(product).println
}

val <T> T?.println get() = println(this)
