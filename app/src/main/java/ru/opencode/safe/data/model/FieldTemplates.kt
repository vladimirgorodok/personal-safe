package ru.opencode.safe.data.model

/**
 * Типовые поля записи по её типу.
 *
 * Нужны, чтобы у банковской карты сразу были «Срок действия», «CVV/CVC», «PIN», «Держатель»,
 * у паспорта — «Кем выдан», «Код подразделения», у Wi-Fi — «Сеть» и «Пароль». Каждое такое
 * поле скрыто глазом и копируется одной кнопкой, поэтому держать их отдельно удобнее, чем
 * писать всё одной строкой.
 *
 * Основное значение записи (номер карты, сам токен) в шаблон не входит — оно и так копируется.
 */
object FieldTemplates {

    private val templates: Map<CategoryType, List<String>> = mapOf(
        CategoryType.BANK_CARD to listOf(
            "Срок действия", "CVV/CVC", "PIN", "Держатель", "Банк", "Платёжная система",
        ),
        CategoryType.PASSPORT to listOf(
            "Серия и номер", "Дата рождения", "Место рождения", "Дата выдачи", "Кем выдан", "Код подразделения",
        ),
        CategoryType.ID_CARD to listOf(
            "Номер документа", "Дата рождения", "Дата выдачи", "Кем выдан", "Срок действия",
        ),
        CategoryType.PASSWORD to listOf(
            "Логин", "Пароль", "Сайт", "Код восстановления",
        ),
        CategoryType.ACCOUNT to listOf(
            "Логин", "Пароль", "E-mail", "Телефон", "Код восстановления",
        ),
        CategoryType.WIFI to listOf(
            "Сеть (SSID)", "Пароль", "Тип шифрования",
        ),
        CategoryType.TOKEN to listOf(
            "Токен", "Логин", "Срок действия",
        ),
        CategoryType.API_KEY to listOf(
            "Секрет", "Endpoint", "Логин",
        ),
        CategoryType.PIN to listOf(
            "PIN", "PUK", "Логин",
        ),
        CategoryType.LICENSE to listOf(
            "Номер лицензии", "Ключ активации", "Дата выдачи", "Срок действия",
        ),
        CategoryType.CERTIFICATE to listOf(
            "Серийный номер", "Отпечаток", "Срок действия",
        ),
    )

    /** Все типовые подписи для типа записи (может быть пусто — тогда подсказывать нечего). */
    fun labels(type: CategoryType): List<String> = templates[type].orEmpty()

    fun hasTemplate(type: CategoryType): Boolean = labels(type).isNotEmpty()

    /**
     * Подписи, которых в записи ещё нет. Сравнение без учёта регистра и лишних пробелов,
     * чтобы «CVV/CVC» и «cvv/cvc» не задваивались.
     */
    fun suggested(type: CategoryType, existing: List<SecretField>): List<String> {
        val taken = existing.map { it.label.trim().lowercase() }.toSet()
        return labels(type).filter { it.trim().lowercase() !in taken }
    }

    /** Заготовки полей для новой записи: подписи заполнены, значения — пользователь вводит сам. */
    fun seed(type: CategoryType): List<SecretField> = labels(type).map { SecretField(label = it) }

    /**
     * Короткая подпись для строки записи в списке: «+3 значения», или null, если заполненных
     * дополнительных полей нет. Нужна, чтобы наличие полей было видно, не открывая запись.
     */
    fun filledSummary(fields: List<SecretField>): String? {
        val count = fields.count { it.value.isNotBlank() }
        if (count == 0) return null
        val word = when {
            count % 10 == 1 && count % 100 != 11 -> "значение"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "значения"
            else -> "значений"
        }
        return "+$count $word"
    }
}
