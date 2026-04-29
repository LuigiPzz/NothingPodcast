import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.util.Scanner

fun main() {
    val searchUrl = "https://itunes.apple.com/search?term=digitalia&media=podcast&limit=1"
    val searchConn = URL(searchUrl).openConnection() as HttpsURLConnection
    searchConn.setRequestProperty("User-Agent", "Mozilla/5.0")
    val searchResult = searchConn.inputStream.bufferedReader().readText()
    
    val regex = Regex("\"feedUrl\":\"([^\"]+)\"")
    val match = regex.find(searchResult)
    if (match != null) {
        val feedUrl = match.groupValues[1]
        println("Feed URL: \$feedUrl")
        
        val feedConn = URL(feedUrl).openConnection() as HttpsURLConnection
        feedConn.setRequestProperty("User-Agent", "Mozilla/5.0")
        val feedContent = feedConn.inputStream.bufferedReader().readText()
        
        val buttonRegex = Regex("<button[^>]*>")
        val buttons = buttonRegex.findAll(feedContent).toList()
        println("Found \${buttons.size} buttons")
        if (buttons.isNotEmpty()) {
            println("First button: \${buttons[0].value}")
        }
    } else {
        println("Feed URL not found")
    }
}
