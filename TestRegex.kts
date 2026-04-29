import java.io.File
import kotlin.text.Regex

fun main() {
    val xml = File("digitalia_rss.xml").readText()
    val htmlChapterRegex = Regex("data-start-time=\"(\\d+)\"\\s*data-title=\"([^\"]+)\"")
    val matches = htmlChapterRegex.findAll(xml).toList()
    println("Found \${matches.size} chapters.")
    matches.take(5).forEach { match ->
        println("\${match.groupValues[1]} -> \${match.groupValues[2]}")
    }
}
