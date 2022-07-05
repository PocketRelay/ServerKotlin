package com.jacobtread.kme.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MessagesTable : IntIdTable("messages") {

    // The different message types
    const val MENU_TABBED_TYPE: Byte = 0
    const val MENU_SCROLLING_TYPE: Byte = 1
    const val MULTIPLAYER_PROMOTION: Byte = 8

    val endDate = long("end_date")
    val image = varchar("image", 120)
    val message = text("message")
    val title = varchar("title", 255)
    val priority = short("priority")
    val type = byte("type")
}