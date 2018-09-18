package net.apemon.poc.state

import net.apemon.poc.schema.ProxyNameSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class ProxyNameState(val namespace: String,
                          val identifier: String,
                          val account: String,
                          val issuer: Party,
                          override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState, QueryableState {

    override val participants: List<Party>
        get() = listOf(issuer)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ProxyNameSchemaV1 -> ProxyNameSchemaV1.PersistentProxyName(
                    this.namespace,
                    this.identifier,
                    this.issuer.name.toString(),
                    this.account,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ProxyNameSchemaV1)
}