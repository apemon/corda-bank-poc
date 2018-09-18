package com.template

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals

class DriverBasedTest {
    private val centralBank = TestIdentity(CordaX500Name("CentralBank", "Bangkok", "TH"))
    private val bankA = TestIdentity(CordaX500Name("BankA", "Bangkok", "TH"))

    @Test
    fun `node test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (centralBankHandle, bankAHandle) = startNodes(centralBank, bankA)

        // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
        // nodes have started and can communicate.

        // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
        // and other important metrics to ensure that your CorDapp is working as intended.
        assertEquals(centralBank.name, centralBankHandle.resolveName(centralBank.name))
        assertEquals(bankA.name, bankAHandle.resolveName(bankA.name))
    }

    @Test
    fun `node webserver test`() = withDriver {
        // This test starts each node's webserver and makes an HTTP call to retrieve the body of a GET endpoint on
        // the node's webserver, to verify that the nodes' webservers have started and have loaded the API.
        startWebServers(centralBank, bankA).forEach {
            val request = Request.Builder()
                .url("http://${it.listenAddress}/api/bank/issue")
                .build()
            val response = OkHttpClient().newCall(request).execute()
        }
    }

    // region Utility functions

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true)
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()

    // Starts multiple webservers simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startWebServers(vararg identities: TestIdentity) = startNodes(*identities)
        .map { startWebserver(it) }
        .waitForAll()

    // endregion
}