package com.availelabs.availe.xmpp.session

class XmppStreamBuffer {
    private val buffer = StringBuilder()

    fun append(text: String) {
        buffer.append(text)
    }

    fun extractNextStanza(): String? {
        while (true) {
            if (buffer.isEmpty()) return null

            var leadingIndex = 0
            while (leadingIndex < buffer.length && buffer[leadingIndex].isWhitespace()) {
                leadingIndex++
            }
            if (leadingIndex > 0) {
                buffer.deleteRange(0, leadingIndex)
            }

            if (buffer.isEmpty()) return null

            if (buffer.startsWith("<?")) {
                val piEnd = buffer.indexOf("?>", startIndex = 2)
                if (piEnd == -1) return null
                buffer.deleteRange(0, piEnd + 2)
                continue
            }

            if (buffer.startsWith("<!--")) {
                val commentEnd = buffer.indexOf("-->", startIndex = 4)
                if (commentEnd == -1) return null
                buffer.deleteRange(0, commentEnd + 3)
                continue
            }

            break
        }

        if (buffer.startsWith("</")) {
            val tagEnd = buffer.indexOf('>', startIndex = 2)
            if (tagEnd == -1) return null
            val stanza = buffer.substring(0, tagEnd + 1)
            buffer.deleteRange(0, tagEnd + 1)
            return stanza
        }

        if (buffer.startsWith("<stream:stream") || buffer.startsWith("<open")) {
            val tagEnd = buffer.indexOf('>', startIndex = 1)
            if (tagEnd == -1) return null
            val stanza = buffer.substring(0, tagEnd + 1)
            buffer.deleteRange(0, tagEnd + 1)
            return stanza
        }

        var depth = 0
        var isInTag = false
        var isInQuote = false
        var quoteChar: Char? = null
        var currentTagIsOpening = false
        var currentTagIsClosing = false

        var index = 0
        while (index < buffer.length) {
            val char = buffer[index]

            if (isInTag) {
                if (char == '"' || char == '\'') {
                    if (!isInQuote) {
                        isInQuote = true
                        quoteChar = char
                    } else if (quoteChar == char) {
                        isInQuote = false
                        quoteChar = null
                    }
                } else if (!isInQuote && char == '>') {
                    val isSelfClosing = currentTagIsOpening && buffer.isSelfClosingAt(index)
                    if (isSelfClosing) {
                        depth--
                    }

                    isInTag = false

                    if ((currentTagIsClosing || isSelfClosing) && depth == 0) {
                        val stanza = buffer.substring(0, index + 1)
                        buffer.deleteRange(0, index + 1)
                        return stanza
                    }

                    currentTagIsOpening = false
                    currentTagIsClosing = false
                }

                index++
                continue
            }

            if (char != '<') {
                index++
                continue
            }

            if (buffer.startsWith("<![CDATA[", index)) {
                val cdataEnd = buffer.indexOf("]]>", startIndex = index + 9)
                if (cdataEnd == -1) return null
                index = cdataEnd + 3
                continue
            }

            if (buffer.startsWith("<!--", index)) {
                val commentEnd = buffer.indexOf("-->", startIndex = index + 4)
                if (commentEnd == -1) return null
                index = commentEnd + 3
                continue
            }

            if (buffer.startsWith("<?", index)) {
                val piEnd = buffer.indexOf("?>", startIndex = index + 2)
                if (piEnd == -1) return null
                index = piEnd + 2
                continue
            }

            isInTag = true
            isInQuote = false
            quoteChar = null

            if (buffer.startsWith("</", index)) {
                depth--
                currentTagIsClosing = true
                currentTagIsOpening = false
            } else {
                depth++
                currentTagIsOpening = true
                currentTagIsClosing = false
            }

            index++
        }

        return null
    }

    private fun CharSequence.isSelfClosingAt(tagEndIndex: Int): Boolean {
        var index = tagEndIndex - 1
        while (index >= 0 && this[index].isWhitespace()) {
            index--
        }
        return index >= 0 && this[index] == '/'
    }
}