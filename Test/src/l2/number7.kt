package l2

fun main()
{

    val green = "\u001B[32m"
    val reset = "\u001B[0m"
    val cyan = "\u001B[36m"
    val bold = "\u001B[1m"
    val purple = "\u001B[35m"

    println("Конвертер времени")

    println("Введите любое кол-во секунд: ")
    val score: Long = readln().trim().toLong()

    val hours: Long = score / 3600
    val minuts: Long = (score % 3600) / 60
    val seconds: Long = score % 60

    println("""
        $purple$bold--------------------------------------$reset
        $purple$bold|     Конвертор времени              |$reset
        $purple$bold--------------------------------------$reset
        $purple$bold|$green Часов: $reset  $bold$hours$reset,$green Минут: $reset $bold$minuts$reset,$green Секунд: $reset$seconds$reset $purple$bold |$reset
        $purple$bold--------------------------------------$reset
        
        
    """.trimIndent())

}