package net.apemon.poc.api

import net.apemon.poc.flow.ProxyNameIssueFlow
import net.apemon.poc.schema.ProxyNameSchemaV1
import net.apemon.poc.state.ProxyNameState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("proxy")
class ProxyNameAPI(val rpcOps: CordaRPCOps) {

    private val me = rpcOps.nodeInfo().legalIdentities.first()

    @POST
    @Path("issue")
    fun issueProxyName(request: IssueRequest): Response {
        try{
            val state = ProxyNameState(request.namespace,
                    request.identifier,
                    request.account,
                    me)
            val result = rpcOps.startFlow(::ProxyNameIssueFlow, state).returnValue.get()
            return Response
                    .status(Response.Status.OK)
                    .entity(result.tx.outputs.single().data.toString())
                    .build()
        } catch (e: Exception){
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.printStackTrace())
                    .build()
        }
    }

    @GET
    @Path("names")
    @Produces(MediaType.APPLICATION_JSON)
    fun getNames(): List<StateAndRef<ContractState>> {
        return rpcOps.vaultQueryBy<ProxyNameState>().states
    }

    @GET
    @Path("name/{namespace}/{identifier}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getName(@PathParam(value = "namespace") namespace: String,
                @PathParam(value = "identifier") identifier: String): Response{
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = builder {
            var namespaceType = ProxyNameSchemaV1.PersistentProxyName::namespace.equal(namespace)
            val customNamespaceCriteria = QueryCriteria.VaultCustomQueryCriteria(namespaceType)
            var identifierType = ProxyNameSchemaV1.PersistentProxyName::identifier.equal(identifier)
            val customIdentifierCriteria = QueryCriteria.VaultCustomQueryCriteria(identifierType)
            val criteria = generalCriteria.and(customNamespaceCriteria).and(customIdentifierCriteria)
            val results = rpcOps.vaultQueryBy<ProxyNameState>(criteria).states
            return Response.ok(results).build()
        }
    }

    data class IssueRequest(
            val namespace: String,
            val identifier: String,
            val account: String
    )
}