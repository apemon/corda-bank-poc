package net.apemon.poc.contract

import kotlinx.html.InputType
import net.apemon.poc.state.ProxyNameState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

@LegalProseReference(uri = "<prose_contract_uri>")
class ProxyNameContract : Contract {
    companion object {
        @JvmStatic
        val PROXYNAME_CONTRACT_ID = "net.apemon.poc.contract.ProxyNameContract"
    }

    interface Commands: CommandData {
        class Issue : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<ProxyNameContract.Commands>()
        when(command.value){
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing an ProxyName." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing an ProxyName." using (tx.outputs.size == 1)
                val proxyName = tx.outputStates.single() as ProxyNameState
                "Namespace and identifier cannot be null" using (!proxyName.namespace.isNullOrEmpty() && !proxyName.identifier.isNullOrEmpty())
                "Only issuer can sign ProxyName issue transaction" using (command.signers.toSet() == proxyName.participants.map { it.owningKey }.toSet())
            }
        }
    }

}