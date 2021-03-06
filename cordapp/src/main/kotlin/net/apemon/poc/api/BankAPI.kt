package net.apemon.poc.api

import kotlinx.html.attributesMapOf
import net.apemon.poc.flow.AccountTransferFlow
import net.apemon.poc.state.AccountTransferState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.internal.x500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.getCashBalances
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.flows.CashPaymentFlow
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import java.util.*
import javax.inject.Qualifier
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("bank")
class BankAPI(val rpcOps: CordaRPCOps) {

    private val me = rpcOps.nodeInfo().legalIdentities.first().name;
    private val myLegalName = me.x500Name;

    fun X500Name.toDisplayString() : String  = BCStyle.INSTANCE.toString(this)

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoamo() = mapOf("me" to myLegalName.toDisplayString());

    @GET
    @Path("balance")
    @Produces(MediaType.APPLICATION_JSON)
            // Display cash balances.
    fun getCashBalances() = rpcOps.getCashBalances()

    @GET
    @Path("accountTransfer")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAccountTransfer(): List<StateAndRef<ContractState>> {
        // Filter by state type: Cash.
        return rpcOps.vaultQueryBy<AccountTransferState>().states
    }

    @GET
    @Path("cash")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCash(): List<StateAndRef<ContractState>> {
        // Filter by state type: Cash.
        return rpcOps.vaultQueryBy<Cash.State>().states
    }

    @POST
    @Path("issue")
    fun issue(request: RequestParam): Response {
        val issueAmount = Amount(request.amount.toLong() * 100, Currency.getInstance(request.currency))

        val issuerBankPartyRef = OpaqueBytes.of(0)
        val notaryParty = rpcOps.notaryIdentities().first()
        try {
            val cashState = rpcOps.startFlow(::CashIssueFlow, issueAmount, issuerBankPartyRef, notaryParty).returnValue.get()
            return Response
                    .status(Response.Status.OK)
                    .entity((cashState.stx.tx.outputs.single().data as Cash.State).toString())
                    .build()
        } catch(e: Exception){
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.printStackTrace())
                    .build()
        }
    }

    @POST
    @Path("transfer")
    fun transfer(request: RequestParam): Response {
        val transferAmount = Amount(request.amount.toLong() * 100, Currency.getInstance(request.currency))

        val targetParty = rpcOps.partiesFromName(request.to, true).first()
        val issuerParty = rpcOps.partiesFromName("CentralBank", true)
        val paymentRequest = CashPaymentFlow.PaymentRequest(
                transferAmount, targetParty, true, issuerParty
        )
        try {
            val cashState = rpcOps.startFlow(::CashPaymentFlow, paymentRequest).returnValue.get()
            return Response
                    .status(Response.Status.OK)
                    .entity((cashState.stx.tx.outputs.single().data as Cash.State).toString())
                    .build()
        } catch(e: Exception){
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.printStackTrace())
                    .build()
        }
    }

    @POST
    @Path("accountTransfer")
    fun accountTransfer(request: AccountTransferRequest): Response {
        val transferAmount = Amount(request.amount.toLong() * 100, Currency.getInstance(request.currency))

        val targetParty = rpcOps.partiesFromName(request.creditor, true).first()
        val state = AccountTransferState(
                debtor = rpcOps.nodeInfo().legalIdentities.first(),
                debtorAcct = request.debtorAcct,
                creditor = targetParty,
                creditorAcct = request.creditorAcct,
                amount = transferAmount
        )
        try {
            val trx = rpcOps.startFlow(::AccountTransferFlow, state).returnValue.get()
            return Response
                    .status(Response.Status.OK)
                    .entity((trx.tx.outputs.single().data as AccountTransferState).toString())
                    .build()
        } catch(e: Exception){
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.printStackTrace())
                    .build()
        }
    }

    data class RequestParam(
            val to: String,
            val amount: Int,
            val currency: String
    )

    data class AccountTransferRequest(
            val debtor: String,
            val debtorAcct: String,
            val creditor: String,
            val creditorAcct: String,
            val amount: Int,
            val currency: String
    )
}