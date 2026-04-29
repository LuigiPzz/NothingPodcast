package com.example.nothingpodcast.util

import com.example.nothingpodcast.domain.model.Podcast
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object OpmlManager {
    
    /**
     * Generates an OPML XML string from a list of podcasts.
     */
    fun generateOpml(podcasts: List<Podcast>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<opml version=\"2.0\">\n")
        sb.append("  <head>\n")
        sb.append("    <title>Nothing Podcast Subscriptions</title>\n")
        sb.append("    <dateCreated>${java.util.Date()}</dateCreated>\n")
        sb.append("  </head>\n")
        sb.append("  <body>\n")
        sb.append("    <outline text=\"feeds\" title=\"feeds\">\n")
        podcasts.forEach { p ->
            val title = escapeXml(p.title)
            val feedUrl = escapeXml(p.feedUrl)
            sb.append("      <outline type=\"rss\" text=\"$title\" title=\"$title\" xmlUrl=\"$feedUrl\" />\n")
        }
        sb.append("    </outline>\n")
        sb.append("  </body>\n")
        sb.append("</opml>")
        return sb.toString()
    }

    private fun escapeXml(s: String): String = s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    /**
     * Parses an OPML file from an InputStream and returns a list of feed URLs.
     */
    fun parseOpml(inputStream: InputStream): List<String> {
        val feeds = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "outline") {
                    // Some OPMLs use xmlUrl, some use url
                    val xmlUrl = parser.getAttributeValue(null, "xmlUrl") 
                        ?: parser.getAttributeValue(null, "url")
                    
                    if (!xmlUrl.isNullOrBlank()) {
                        feeds.add(xmlUrl)
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return feeds
    }
}
