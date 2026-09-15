package l1

fun main ()
{

    val green = "\u001B[35m"
    val reset = "\u001B[0m"
    val cyan = "\u001B[36m"
    val bold = "\u001B[1m"
    val purple = "\u001B[32m"
    println("Анкета питомца")
    println("Какой у вас питомец: ")

    val namePet: String = readln()

    println("Как вы назвали питомцу: ")

    val nameTag: String = readln()

    println("Какого пола ваш питомец: ")

    val sex: String = readln()

    println("Сколько лет вашему питомцу:")

    val age: String = readln()

    println("""
        $purple$bold--------------------------------------$reset
        $purple$bold|        Анкета питомца              |$reset
        $purple$bold--------------------------------------$reset
        $cyan  Питомец:$reset  $bold$namePet$reset
        $cyan  Кличка:$reset   $bold$nameTag$reset
        $cyan  Пол:$reset      $bold$sex$reset
        $cyan  Возраст:$reset  $bold$age$reset 
        $purple$bold--------------------------------------$reset
        
        
    """.trimIndent())


}