package net.apemon.poc.flow

import co.paralleluniverse.fibers.Suspendable
import net.apemon.poc.contract.AccountTransferContract
import net.apemon.poc.state.AccountTransferState
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.internal.declaredField
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.getCashBalance
import net.corda.finance.contracts.getCashBalances

@InitiatingFlow
@StartableByRPC
class AccountTransferFlow(val state: AccountTransferState):FlowLogic<SignedTransaction>(){
    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("begin account transfer flow")
        logger.info(state.toString())
        // get notary
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // build command
        val command = Command(AccountTransferContract.Commands.Transfer(), state.participants.map { it.owningKey })
        // build transaction
        val builder = TransactionBuilder(notary = notary)
        // check the cash asset that we have enough for transfer
        val amount = state.amount
        val cashBalance = serviceHub.getCashBalance(amount.token)
        logger.info("cash balance: " + cashBalance.token)

        if(cashBalance < amount) {
            throw IllegalArgumentException("You has only $cashBalance but attempt to transfer $amount")
        }
        // get some cash from vault and add a spend to transaction builder
        val (_, cashKeys) = Cash.generateSpend(serviceHub, builder, amount, ourIdentityAndCert, state.creditor)
        logger.info("key to sign: " + cashKeys.toString())
        // add command and output
        builder.addCommand(command)
        builder.addOutputState(state, AccountTransferContract.ACCOUNT_TRANSFER_CONTRACT_ID)
        // verify and sign
        logger.debug("begin verify")
        builder.verify(serviceHub)
        val myKeysToSign = (cashKeys.toSet() + ourIdentity.owningKey).toList()
        val ptx = serviceHub.signInitialTransaction(builder, myKeysToSign)
        // initial session
        val counterPartySession = initiateFlow(state.creditor)
        // Sending other party our identities so they are aware of anonymous public keys
        subFlow(IdentitySyncFlow.Send(counterPartySession, ptx.tx))
        // collect other signatures
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(counterPartySession),myOptionalKeys = myKeysToSign))
        return subFlow(FinalityFlow(stx))
    }
}

@InitiatedBy(AccountTransferFlow::class)
class AccountTransferFlowResponder(val flowSession: FlowSession): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        logger.info("receive flow: " + flowSession.toString())
        // receive anonymous identities
        subFlow(IdentitySyncFlow.Receive(flowSession))
        logger.info("identity flow receive: " + flowSession.toString())
        // signing transaction
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) {

            }
        }
        subFlow(signedTransactionFlow)
    }
}