package net.apemon.poc.api

import net.corda.core.internal.x500Name
import net.corda.core.messaging.CordaRPCOps
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("bank")
class BankAPI(val rpcOps: CordaRPCOps) {

    private val me = rpcOps.nodeInfo().legalIdentities.first().name;
    private val myLegalName = me.x500Name;

    fun X500Name.toDisplayString() : String  = BCStyle.INSTANCE.toString(this)

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoamo() = mapOf("me" to myLegalName.toDisplayString());


}