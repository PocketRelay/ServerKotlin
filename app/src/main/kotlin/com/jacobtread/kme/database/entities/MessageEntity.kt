package com.jacobtread.kme.database.entities

import com.jacobtread.kme.database.tables.MessagesTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MessageEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MessageEntity>(MessagesTable) {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("MM:dd:yyyy")

        fun createMessageMap(): LinkedHashMap<String, String> {
            return transaction {
                val out = LinkedHashMap<String, String>()
                val messages = all()
                val locales = arrayOf("de", "es", "fr", "it", "ja", "pl", "ru")
                messages.forEachIndexed { i, message ->
                    val index = i + 1
                    out["MSG_${index}_endDate"] = DATE_FORMAT.format(LocalDate.ofEpochDay(message.endDate))
                    out["MSG_${index}_image"] = message.image
                    out["MSG_${index}_message"] = message.message
                    locales.forEach { locale ->
                        out["MSG_${index}_message_$locale"] = message.message
                    }
                    out["MSG_${index}_priority"] = message.priority.toString()
                    out["MSG_${index}_title"] = message.title
                    locales.forEach { locale ->
                        out["MSG_${index}_title_$locale"] = message.title
                    }
                    out["MSG_${index}_trackingId"] = message.id.value.toString()
                    out["MSG_${index}_type"] = message.type.toString()
                }
                out
            }
        }

        fun create(
            title: String,
            message: String,
            image: String = "Promo_n7.dds",
            priority: Short = 0,
            type: Byte = MessagesTable.MENU_SCROLLING_TYPE,
            endDate: LocalDate = LocalDate.now().plusDays(15),
        ) {
            val timestamp = endDate.toEpochDay()
            transaction {
                new {
                    this.endDate = timestamp
                    this.image = image
                    this.title = title
                    this.message = message
                    this.priority = priority
                    this.type = type
                }
            }
        }
    }

    var endDate by MessagesTable.endDate
    var image by MessagesTable.image
    var message by MessagesTable.message
    var title by MessagesTable.title
    var priority by MessagesTable.priority
    var type by MessagesTable.type
}