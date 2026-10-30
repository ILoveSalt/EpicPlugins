import kotlin.random.*

//Все участники
// Имя -> Рейтинг
val gladiators = mutableMapOf<String, Int>(
    "Сердюк Михали" to 1000,
    "Степанов Владислав" to 1000,
    "Бурцев Владислав" to 1000,
    "Зиновьев Данил" to 1000,
    "Остроух Кирилл" to 1000,
    "Руслан Майер" to 1000,
    "Лукянцев Михаил" to 1000,
    "Всеволод Зерняков" to 1000,
    "Шкебеде" to 1000,
    "Бурмалдрун" to 1000,
    "Фог" to 1000,
    "Пакость" to 1000,
    "Оу Май Гадность" to 1000,
    "Туберкулез" to 1000,
    "Пал Лаич" to 1000,
    "Бурмалдрель" to 1000,
    "MC Оксимирон" to 1000,
    "Шляпа" to 1000,
    "Мелстрой" to 1000,
    "Александр Гнилозуб" to 1000,
    "Василий Селёдка" to 1000,
    "Евгений Камчатка" to 1000,
    "Дмитрий Каторга" to 1000,
    "Шопот" to 1000,
    "Боленьк" to 1000,
    "Херобрин" to 1000,
    "Ла-Манш" to 1000,
    "Поез" to 1000,
    "Могность" to 1000,
    "ТруАдамость" to 1000,
    "Саб3" to 1000,

)

// Имя -> Список характеристик: [Сила, Ловкость, Здоровье]
val stats = mutableMapOf<String, MutableList<Int>>()

// История матчей
val battleHistory = mutableListOf<String>()

fun main() {
// Генерация характеристик для всех гладиаторов при старте
    for (name in gladiators.keys) {
        stats[name] = generateRandomStats()
    }

    var isRunning = true
    while (isRunning) {
        println("\n===Гладиаторские Сражения===")
        println("1 Сражения 1х1")
        println("2 Сражения 2х2")
        println("3 Рейтинги")
        println("4 История матчей")
        println("5 Найти игрока")
        println("6 Подробная статистика")
        println("0 Выход")
        print("Выберите пункт меню: ")

        val choice = readln().toIntOrNull() ?: -1

        when (choice) {
            1 -> startBattle1x1()
            2 -> startBattle2x2()
            3 -> showRatings()
            4 -> showHistory()
            5 -> findGladiator()
            6 -> showFullStats()
            0 -> {
                println("Выход из игры. До встречи на арене!")
                isRunning = false
            }
            else -> println("Ошибка: неверный пункт меню. Попробуйте еще раз.")
        }
    }
}

fun generateRandomStats(): MutableList<Int> {
    return mutableListOf(
        Random.nextInt(5..40), // Сила [0]
        Random.nextInt(3..30), // Ловкость [1]
        Random.nextInt(35..90) // Здоровье [2]
    )
}

//Функция выбора гладиатора
fun selectGladiatorByName(prompt: String, excludeNames: List<String> = emptyList()): String {
    val availableGladiators = gladiators.keys.filter { it !in excludeNames }

    while (true) {
        println("\nДоступные гладиаторы:")
        for (i in availableGladiators.indices) {
            val name = availableGladiators[i]
            println("${i + 1}. $name (Рейтинг: ${gladiators[name]})")
        }
        print("$prompt: ")
        val input = readln().toIntOrNull()

        if (input == null) {
            println("Ошибка: введите числовое значение.")
            continue
        }

        val actualIndex = input - 1
        if (actualIndex !in availableGladiators.indices) {
            println("Ошибка: гладиатора с таким номером нет в списке доступных.")
            continue
        }

        return availableGladiators[actualIndex]
    }
}

// Бой 1 на 1
fun startBattle1x1() {
    if (gladiators.size < 2) {
        println("Недостаточно гладиаторов для проведения боя!")
        return
    }

    val name1 = selectGladiatorByName("Выберите первого бойца (введите номер)")
    val name2 = selectGladiatorByName("Выберите второго бойца (введите номер)", listOf(name1))

    println("\n--- Бой 1х1: $name1 против $name2 ---")

// Получаем характеристики по имени
    val stats1 = stats[name1]!!
    val stats2 = stats[name2]!!

    var hp1 = stats1[2]
    var hp2 = stats2[2]
    val str1 = stats1[0]
    val agi1 = stats1[1]
    val str2 = stats2[0]
    val agi2 = stats2[1]

    while (hp1 > 0 && hp2 > 0) {
// Ход первого бойца
        if (Random.nextInt(1..100) <= (50 + agi1 - agi2)) {
            val damage = Random.nextInt(1..str1)
            hp2 -= damage
            println("$name1 наносит $damage урона. У $name2 осталось $hp2 HP.")
        } else {
            println("$name1 промахнулся!")
        }

        if (hp2 <= 0) break

// Ход второго бойца
        if (Random.nextInt(1..100) <= (50 + agi2 - agi1)) {
            val damage = Random.nextInt(1..str2)
            hp1 -= damage
            println("$name2 наносит $damage урона. У $name1 осталось $hp1 HP.")
        } else {
            println("$name2 промахнулся!")
        }
    }
    // Обработка результатов боя и изменение рейтинга при значении оставшегося хп победителя
    if (hp1 > 0) {
        println("\nПобедил $name1! (Осталось HP: $hp1)")
        val newRating = gladiators[name2]!! - hp1
        gladiators[name2] = newRating
        println("$name2 теряет $hp1 рейтинга. (Новый рейтинг: $newRating)")
        battleHistory.add("1х1: $name1 победил $name2. $name2 потерял $hp1 рейтинга.")
    } else {
        println("\nПобедил $name2! (Осталось HP: $hp2)")
        val newRating = gladiators[name1]!! - hp2
        gladiators[name1] = newRating
        println("$name1 теряет $hp2 рейтинга. (Новый рейтинг: $newRating)")
        battleHistory.add("1х1: $name2 победил $name1. $name1 потерял $hp2 рейтинга.")
    }
}

// Бой 2 на 2
fun startBattle2x2() {
    if (gladiators.size < 4) {
        println("Недостаточно гладиаторов для боя 2х2!")
        return
    }

    val selected = mutableListOf<String>()

    println("\n--- Формирование Команды 1 ---")
    selected.add(selectGladiatorByName("Выберите 1-го бойца для Команды 1", selected))
    selected.add(selectGladiatorByName("Выберите 2-го бойца для Команды 1", selected))

    println("\n--- Формирование Команны 2 ---")
    selected.add(selectGladiatorByName("Выберите 1-го бойца для Команды 2", selected))
    selected.add(selectGladiatorByName("Выберите 2-го бойца для Команды 2", selected))

    val t1p1 = selected[0]
    val t1p2 = selected[1]
    val t2p1 = selected[2]
    val t2p2 = selected[3]

    val team1Names = "$t1p1 и $t1p2"
    val team2Names = "$t2p1 и $t2p2"

    println("\n--- Бой 2х2: [$team1Names] против [$team2Names] ---")

// Расчет суммарных и средних характеристик команд
    var team1Hp = stats[t1p1]!![2] + stats[t1p2]!![2]
    var team2Hp = stats[t2p1]!![2] + stats[t2p2]!![2]

    val team1Str = (stats[t1p1]!![0] + stats[t1p2]!![0]) / 2
    val team1Agi = (stats[t1p1]!![1] + stats[t1p2]!![1]) / 2
    val team2Str = (stats[t2p1]!![0] + stats[t2p2]!![0]) / 2
    val team2Agi = (stats[t2p1]!![1] + stats[t2p2]!![1]) / 2

    while (team1Hp > 0 && team2Hp > 0) {
        if (Random.nextInt(1..100) <= (50 + team1Agi - team2Agi)) {
            val damage = Random.nextInt(5..team1Str)
            team2Hp -= damage
            println("Команда 1 наносит $damage урона. У Команды 2 осталось $team2Hp HP.")
        }
        if (team2Hp <= 0) break

        if (Random.nextInt(1..100) <= (50 + team2Agi - team1Agi)) {
            val damage = Random.nextInt(5..team2Str)
            team1Hp -= damage
            println("Команда 2 наносит $damage урона. У Команды 1 осталось $team1Hp HP.")
        }
    }

    if (team1Hp > 0) {
        println("\nПобедила Команда 1 [$team1Names]! (Оставшееся HP: $team1Hp)")
        gladiators[t2p1] = gladiators[t2p1]!! - team1Hp
        gladiators[t2p2] = gladiators[t2p2]!! - team1Hp
        battleHistory.add("2х2: [$team1Names] победили [$team2Names]. Проигравшие потеряли по $team1Hp рейтинга.")
    } else {
        println("\nПобедила Команда 2 [$team2Names]! (Оставшееся HP: $team2Hp)")
        gladiators[t1p1] = gladiators[t1p1]!! - team2Hp
        gladiators[t1p2] = gladiators[t1p2]!! - team2Hp
        battleHistory.add("2х2: [$team2Names] победили [$team1Names]. Проигравшие потеряли по $team2Hp рейтинга.")
    }
}

// Вывод рейтингов
fun showRatings() {
    println("\n--- Рейтинги Гладиаторов ---")
    var counter = 1
    for ((name, rating) in gladiators) {
        println("${counter++}. $name — Рейтинг: $rating")
    }
}

// Просмотр истории матчей
fun showHistory() {
    println("\n--- История Матчей ---")
    if (battleHistory.isEmpty()) {
        println("История пуста. На арене еще никто не сражался.")
    } else {
        for (event in battleHistory) {
            println(event)
        }
    }
}

// Поиск игрока по имени
fun findGladiator() {
    print("\nВведите имя гладиатора для поиска: ")
    val searchName = readln().trim()

// Ищем ключ без учета регистра
    val actualKey = gladiators.keys.find { it.lowercase() == searchName.lowercase() }
    if (actualKey != null) {
        val gladiatorStats = stats[actualKey]!!
        println("\nГладиатор найден!")
        println("Имя: $actualKey")
        println("Рейтинг: ${gladiators[actualKey]}")
        println("Характеристики -> Сила: ${gladiatorStats[0]}, Ловкость: ${gladiatorStats[1]}, Здоровье: ${gladiatorStats[2]}")
    } else {
        println("Гладиатор с именем \"$searchName\" не найден.")
    }
}

// Подробная статистика всех бойцов
fun showFullStats() {
    println("\n--- Подробная статистика всех гладиаторов ---")
    for ((name, rating) in gladiators) {
        val gladiatorStats = stats[name]!!
        val str = gladiatorStats[0]
        val agi = gladiatorStats[1]
        val hp = gladiatorStats[2]
        println("Имя: $name | Сила: $str | Ловкость: $agi | Здоровье: $hp | Рейтинг: $rating")
    }
}