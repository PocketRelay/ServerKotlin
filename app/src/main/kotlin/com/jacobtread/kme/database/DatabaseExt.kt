package com.jacobtread.kme.database

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.transactions.transaction

fun <V : IntEntityClass<T>, T> V.byId(id: Int): T? {
    return transaction { findById(id) }
}

fun <V : IntEntityClass<T>, T> V.byId(id: Long): T? = byId(id.toInt())
fun <V : IntEntityClass<T>, T> V.byId(id: ULong): T? = byId(id.toInt())

/**
 * Represents a function which when provided an expression builder
 * produces a boolean operation
 */
typealias BooleanOperation = SqlExpressionBuilder.() -> Op<Boolean>

@Suppress("NOTHING_TO_INLINE")
inline fun <V : EntityClass<*, T>, T> V.firstOrNullSafe(noinline op: BooleanOperation): T? {
    return transaction {
        firstOrNull(op)
    }
}

/**
 * findFirst wraps the underlying find function on EntityClasses
 * to allow the finding the first entity matching the op expression
 * or null if nothing matched.
 *
 * This function is expected to be wrapped in a transaction
 *
 * @param V The enitty class type
 * @param T The mapped entity
 * @param op The operation to determine whether to use it
 * @receiver Usees the entity class type as the reciever in order to use the "find" function
 * @return The first matching entity or null if none match
 */
fun <V : EntityClass<*, T>, T> V.firstOrNull(op: BooleanOperation): T? {
    return find(op).limit(1).firstOrNull()
}

/**
 * updateOrCreate Attempts to find the first entity that matches the
 * boolean operation and then run the update function on that entity
 * but if that entity is not found the update function will instead
 * be run on a freshly created class
 *
 * This function is expected to be wrapped in a transaction
 *
 * @param V The enitty class type
 * @param T The mapped entity
 * @param findOp The operation for finding the entity
 * @param update The function which applys the updates to the entity
 * @receiver Usees the entity class type as the reciever in order to use the "find" function
 * @return The entity that was updated or the newly created entity
 */
fun <V : EntityClass<*, T>, T> V.updateOrCreate(findOp: BooleanOperation, update: T.() -> Unit): T {
    return transaction {
        val value = firstOrNull(findOp)
        if (value != null) {
            value.apply(update)
            value
        } else {
            new(update)
        }
    }
}