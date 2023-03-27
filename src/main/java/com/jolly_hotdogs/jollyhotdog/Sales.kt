package com.jolly_hotdogs.jollyhotdog

import java.time.LocalDateTime

class Sales(
    var branch: String,
    var name: String,
    var quantity: Int,
    val type: ItemType,
    val price: Double,
    var transactionDate: LocalDateTime
)