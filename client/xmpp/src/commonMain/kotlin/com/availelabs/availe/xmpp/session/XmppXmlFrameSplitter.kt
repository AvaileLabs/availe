package com.availelabs.availe.xmpp.session

internal fun splitTopLevelXmlElements(rawText: String): List<String> {
    val text = rawText.trim()
    if (text.isBlank()) return emptyList()

    val elements = mutableListOf<String>()
    var depth = 0
    var elementStartIndex: Int? = null

    var index = 0
    while (index < text.length) {
        val ch = text[index]
        if (ch != '<') {
            index++
            continue
        }

        if (text.startsWith("<?", index)) {
            val end = text.indexOf('>', startIndex = index + 2)
            if (end == -1) return listOf(text)
            index = end + 1
            continue
        }

        if (text.startsWith("<!--", index)) {
            val end = text.indexOf("-->", startIndex = index + 4)
            if (end == -1) return listOf(text)
            index = end + 3
            continue
        }

        if (text.startsWith("<!", index)) {
            val end = text.indexOf('>', startIndex = index + 2)
            if (end == -1) return listOf(text)
            index = end + 1
            continue
        }

        val tagEndIndex = text.indexOf('>', startIndex = index + 1)
        if (tagEndIndex == -1) return listOf(text)

        val isEndTag = text.startsWith("</", index)
        val tagBody = text.substring(index + if (isEndTag) 2 else 1, tagEndIndex)
        val isSelfClosing = !isEndTag && tagBody.trimEnd().endsWith("/")

        if (!isEndTag) {
            if (depth == 0) {
                elementStartIndex = index
            }

            if (isSelfClosing) {
                val start = elementStartIndex ?: index
                elements += text.substring(start, tagEndIndex + 1)
                elementStartIndex = null
            } else {
                depth++
            }
        } else {
            depth = (depth - 1).coerceAtLeast(0)
            if (depth == 0) {
                val start = elementStartIndex
                if (start != null) {
                    elements += text.substring(start, tagEndIndex + 1)
                }
                elementStartIndex = null
            }
        }

        index = tagEndIndex + 1
    }

    return elements.ifEmpty { listOf(text) }
}