package com.example.starter.handlers

import com.example.starter.db.FortuneRepository
import com.example.starter.models.Fortune
import io.vertx.core.http.HttpServerRequest
import org.apache.logging.log4j.kotlin.Logging

class FortuneHandler(private val repository: FortuneRepository) : AbstractHandler() {
    fun templateFortunes(req: HttpServerRequest) {
        repository
            .selectFortunes()
            .onSuccess { fortunes ->
                val list = fortunes + Fortune(0, "Additional fortune added at request time.")
                list.sorted()

                val html = renderFortunes(list)
                req.html().end(html)
            }
            .onFailure {
                logger.error(SOMETHING_WENT_WRONG, it)
                req.error()
            }
    }

    private companion object : Logging {
        private const val HEADER =
            "<!DOCTYPE html><html><head><title>Fortunes</title></head><body><table>" +
                    "<tr><th>id</th><th>message</th></tr>"
        private const val FOOTER = "</table></body></html>"

        private fun renderFortunes(fortunes: Array<Fortune>): String {
            val sb = StringBuilder(256 + fortunes.size * 64)
            sb.append(HEADER)
            for (f in fortunes) {
                sb.append("<tr><td>")
                    .append(f.id)
                    .append("</td><td>")
                    .append(escapeHtml(f.message))
                    .append("</td></tr>")
            }
            sb.append(FOOTER)
            return sb.toString()
        }

        private fun escapeHtml(text: String): String {
            var i = 0
            val len = text.length
            while (i < len) {
                when (text[i]) {
                    '<', '>', '&', '"' -> return buildEscaped(text, i)
                }
                i++
            }
            return text
        }

        private fun buildEscaped(text: String, start: Int): String {
            val sb = StringBuilder(text.length + 16)
            sb.append(text, 0, start)
            for (i in start until text.length) {
                when (val c = text[i]) {
                    '<' -> sb.append("&lt;")
                    '>' -> sb.append("&gt;")
                    '&' -> sb.append("&amp;")
                    '"' -> sb.append("&quot;")
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}
