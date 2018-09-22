package net.apemon.poc.contract

import net.apemon.poc.state.AccountTransferState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.loggerFor
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.issuedBy
import net.corda.finance.utils.sumCash
import net.corda.finance.utils.sumCashBy

class AccountTransferContract: Contract {
    companion object {
        @JvmStatic
        val ACCOUNT_TRANSFER_CONTRACT_ID = "net.apemon.poc.contract.AccountTransferContract"
    }

    interface Commands: CommandData {
        class Transfer: TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<AccountTransferContract.Commands>()
        when(command.value) {
            is Commands.Transfer -> requireThat {
                val transfer = tx.groupStates<AccountTransferState, UniqueIdentifier> { it.linearId }.single()
                //loggerFor<AccountTransferContract>().info("tx input: " + transfer.inputs.first().toString())
                "No inputs should be consumed when transfer" using (transfer.inputs.isEmpty())
                "Only one output state should be created" using (transfer.outputs.size == 1)
                // Check there are output cash states.
                val cash = tx.outputsOfType<Cash.State>()
                "There must be output cash" using (cash.isNotEmpty())
                val output = transfer.outputs.single() as AccountTransferState
                "Debtor and Creditor must not be the same party" using (output.debtor != output.creditor)
                val acceptableCash = cash.filter { it.owner == output.creditor }
                val sumAcceptableCash = acceptableCash.sumCash().withoutIssuer()
                //val issuerBankPartyRef = OpaqueBytes.of(0)
                //val issuePartyAndRef = PartyAndReference(output.issuer, issuerBankPartyRef)
                //"The balance must greater than transfer amount" using (sumAcceptableCash >= output.amount.issuedBy(issuePartyAndRef))
                "Only participants must sign transaction" using (command.signers.toSet() == output.participants.map { it.owningKey }.toSet())
            }
        }
    }
}