package net.apemon.poc.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

// family of schemas
object ProxyNameSchema

// ProxyName Schema
object ProxyNameSchemaV1: MappedSchema(
        schemaFamily = ProxyNameSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentProxyName::class.java)
){
    @Entity
    @Table(name = "proxyname_states")
    class PersistentProxyName(
            @Column(name = "namespace")
            var namespace: String,

            @Column(name = "identifier")
            var identifier: String,

            @Column(name = "issuer")
            var issuerName: String,

            @Column(name = "account")
            var accountNo: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ): PersistentState(){
        constructor(): this("","","","",UUID.randomUUID())
    }
}

