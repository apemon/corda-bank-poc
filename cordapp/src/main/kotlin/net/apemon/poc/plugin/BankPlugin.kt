package net.apemon.poc.plugin

import net.apemon.poc.api.BankAPI
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class BankPlugin: WebServerPluginRegistry{
    override  val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::BankAPI))

    override val staticServeDirs: Map<String, String> = mapOf(
        "bank" to javaClass.classLoader.getResource("bankWeb").toExternalForm()
    )
}