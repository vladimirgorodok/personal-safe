package ru.opencode.safe.data.model

import kotlin.math.abs

object Defaults {

    fun seedFor(text: String): Long = abs(text.lowercase().hashCode()).toLong()

    fun defaultCategories(): List<Category> = listOf(
        Category(name = "Токены", type = CategoryType.TOKEN, description = "Токены доступа, JWT, ключи авторизации", colorSeed = seedFor("Токены"), isSystem = true),
        Category(name = "Пароли", type = CategoryType.PASSWORD, description = "Пароли от сайтов и сервисов", colorSeed = seedFor("Пароли"), isSystem = true),
        Category(name = "API-ключи", type = CategoryType.API_KEY, description = "Ключи для интеграций и разработки", colorSeed = seedFor("API-ключи"), isSystem = true),
        Category(name = "ID-карты", type = CategoryType.ID_CARD, description = "Удостоверения и ID-карты", colorSeed = seedFor("ID-карты"), isSystem = true),
        Category(name = "Банковские карты", type = CategoryType.BANK_CARD, description = "Номера карт, реквизиты", colorSeed = seedFor("Банковские карты"), isSystem = true),
        Category(name = "Паспорт", type = CategoryType.PASSPORT, description = "Паспортные данные", colorSeed = seedFor("Паспорт"), isSystem = true),
        Category(name = "Аккаунты", type = CategoryType.ACCOUNT, description = "Логины и доступы к сервисам", colorSeed = seedFor("Аккаунты"), isSystem = true),
        Category(name = "Wi-Fi", type = CategoryType.WIFI, description = "Доступы к беспроводным сетям", colorSeed = seedFor("Wi-Fi"), isSystem = true),
        Category(name = "PIN-коды", type = CategoryType.PIN, description = "Короткие числовые коды", colorSeed = seedFor("PIN-коды"), isSystem = true),
        Category(name = "Заметки", type = CategoryType.NOTE, description = "Прочие защищённые записи", colorSeed = seedFor("Заметки"), isSystem = true),
    )

    /** Сопоставление типа с категорией из числа системных. */
    fun systemCategoryName(type: CategoryType): String = when (type) {
        CategoryType.TOKEN -> "Токены"
        CategoryType.PASSWORD -> "Пароли"
        CategoryType.API_KEY -> "API-ключи"
        CategoryType.ID_CARD -> "ID-карты"
        CategoryType.BANK_CARD -> "Банковские карты"
        CategoryType.PASSPORT -> "Паспорт"
        CategoryType.ACCOUNT -> "Аккаунты"
        CategoryType.WIFI -> "Wi-Fi"
        CategoryType.PIN -> "PIN-коды"
        CategoryType.LICENSE -> "Лицензии"
        CategoryType.CERTIFICATE -> "Сертификаты"
        CategoryType.NOTE -> "Заметки"
        CategoryType.OTHER -> "Другое"
    }
}
