package net.apemon.poc.state

import net.apemon.poc.schema.AccountTransferSchemaV1
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

data class AccountTransferState(val debtor: Party,
                                val creditor: Party,
                                val debtorAcct: String,
                                val creditorAcct: String,
                                val amount: Amount<Currency>,
                                override val linearId: UniqueIdentifier = UniqueIdentifier()):LinearState, QueryableState {

    override val participants: List<Party>
        get() = listOf(debtor, creditor)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is AccountTransferSchemaV1 -> AccountTransferSchemaV1.PersistentAccountTransfer(
                    this.debtor.name.toString(),
                    this.debtorAcct,
                    this.creditor.name.toString(),
                    this.creditorAcct,
                    this.amount.quantity,
                    this.amount.token.toString(),
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecoginised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AccountTransferSchemaV1)

}