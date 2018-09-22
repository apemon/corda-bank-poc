package net.apemon.poc.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

// family of schema
object AccountTransferSchema

// AccountTransfer schema
object AccountTransferSchemaV1: MappedSchema(
        schemaFamily = AccountTransferSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentAccountTransfer::class.java)
) {
    @Entity
    @Table(name = "account_transfer_states")
    class PersistentAccountTransfer (
            @Column(name = "debtor_name")
            var debtorName: String,

            @Column(name = "debtor_acct")
            var debtorAcct: String,

            @Column(name = "creditor_name")
            var creditorName: String,

            @Column(name = "creditor_acct")
            var creditorAcct: String,

            @Column(name = "amount")
            var amount: Long,

            @Column(name = "currency")
            var currency: String,

            @Column(name = "linear_id")
            var linearIId: UUID


    ): PersistentState() {
        constructor(): this("","","","",0,"",UUID.randomUUID())
    }
}