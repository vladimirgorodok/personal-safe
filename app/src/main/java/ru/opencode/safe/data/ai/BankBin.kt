package ru.opencode.safe.data.ai

/**
 * Определение банка-эмитента карты по BIN (первым цифрам номера).
 * Таблица неполная и приблизительная: результат трактуется как предположение,
 * точный банк виден на самой карте или в мобильном приложении банка.
 */
object BankBin {

    data class Issuer(val name: String, val domain: String, val manageUrl: String, val cardUrl: String)

    private val sber = Issuer("Сбербанк", "sberbank.ru", "https://online.sberbank.ru/", "https://www.sberbank.ru/ru/person/bank_cards")
    private val tbank = Issuer("Т-Банк (Тинькофф)", "tinkoff.ru", "https://www.tinkoff.ru/mybank/", "https://www.tbank.ru/cards/")
    private val alfa = Issuer("Альфа-Банк", "alfabank.ru", "https://ib.alfabank.ru/", "https://alfabank.ru/get-money/credit-cards/")
    private val vtb = Issuer("ВТБ", "vtb.ru", "https://www.vtb.ru/lk/", "https://www.vtb.ru/personal/karty/")
    private val gpb = Issuer("Газпромбанк", "gazprombank.ru", "https://web.gazprombank.ru/", "https://www.gazprombank.ru/personal/cards/")

    /** 4-значные префиксы BIN. */
    private val byFour: Map<String, Issuer> = mapOf(
        "4276" to sber, "4279" to sber, "5469" to sber, "5484" to sber, "5336" to sber, "4817" to sber, "6762" to sber,
        "5536" to tbank, "4377" to tbank, "5213" to tbank,
        "4154" to alfa, "4584" to alfa,
        "4272" to vtb, "4627" to vtb,
        "4890" to gpb,
    )

    /** 6-значные уточнения (приоритетнее). */
    private val bySix: Map<String, Issuer> = mapOf(
        "427601" to sber, "427602" to sber, "427603" to sber, "427604" to sber,
        "427901" to sber, "427902" to sber,
        "546901" to sber, "546905" to sber,
        "553691" to tbank, "553609" to tbank, "553649" to tbank,
        "415402" to alfa,
        "427201" to vtb,
    )

    fun issuerFor(digits: String): Issuer? {
        val clean = digits.filter { it.isDigit() }
        if (clean.length < 6) return null
        bySix[clean.take(6)]?.let { return it }
        return byFour[clean.take(4)]
    }
}
