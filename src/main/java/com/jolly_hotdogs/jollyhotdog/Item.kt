package com.jolly_hotdogs.jollyhotdog

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import java.time.LocalDateTime

class Item(
    name: String?,
    var quantity: Int,
    var type: ItemType,
    var price: Double,
    var lastTransaction: LocalDateTime
) {
    val name: StringProperty

    init {
        this.name = SimpleStringProperty(name)
    }

    fun getName(): String {
        return name.get()
    }

    fun setName(name: String) {
        this.name.set(name)
    }

    fun nameProperty(): StringProperty {
        return name
    }

    fun toCsvString(): String {
        return "$name,$quantity,$type,$price,$lastTransaction"
    }
}