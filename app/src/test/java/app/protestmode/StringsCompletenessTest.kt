package app.protestmode

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class StringsCompletenessTest {
    private val res = File("src/main/res")
    private val locales = listOf("fr", "es", "it")
    private val formatArgument = Regex("""%\d\$[sd]""")

    private fun strings(folder: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$folder/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .filter { it.getAttribute("translatable") != "false" }
            .associate { it.getAttribute("name") to it.textContent }
    }

    private fun plurals(folder: String): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$folder/strings.xml"))
        val nodes = document.getElementsByTagName("plurals")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("name") }.toSet()
    }

    @Test
    fun everyLanguageDefinesEveryString() {
        val english = strings("values")
        assertTrue(english.isNotEmpty())
        locales.forEach { locale ->
            assertEquals(locale, english.keys, strings("values-$locale").keys)
            assertEquals(locale, plurals("values"), plurals("values-$locale"))
        }
    }

    @Test
    fun formatArgumentsMatchEnglish() {
        val english = strings("values")
        locales.forEach { locale ->
            strings("values-$locale").forEach { (key, text) ->
                assertEquals(
                    "$locale/$key",
                    formatArgument.findAll(english.getValue(key)).map { it.value }.sorted().toList(),
                    formatArgument.findAll(text).map { it.value }.sorted().toList()
                )
            }
        }
    }

    @Test
    fun localeConfigListsEveryLanguage() {
        val config = File(res, "xml/locales_config.xml").readText()
        (listOf("en") + locales).forEach { assertTrue(it, "android:name=\"$it\"" in config) }
    }
}
