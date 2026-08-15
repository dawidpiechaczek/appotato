package com.appotato.features.pantry.implementation.data

import com.appotato.features.pantry.implementation.PantryItem
import com.appotato.features.pantry.implementation.ProductCategory
import com.appotato.shared.database.PantryItemEntity
import kotlinx.datetime.LocalDate

internal fun PantryItemEntity.toDomain(): PantryItem = PantryItem(
    id = id,
    name = name,
    expiresOn = LocalDate.fromEpochDays(expiresOnEpochDay.toInt()),
    category = ProductCategory.fromCode(category),
    quantity = quantity,
    barcode = barcode
)

internal fun PantryItem.toEntity(): PantryItemEntity = PantryItemEntity(
    id = id,
    name = name,
    expiresOnEpochDay = expiresOn.toEpochDays().toLong(),
    category = category.code,
    quantity = quantity,
    barcode = barcode
)
