package com.planify.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    onWikilinkClick: ((String) -> Unit)? = null
) {
    val lines = markdown.split("\n")
    Column(modifier = modifier) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = parseInline(line.removePrefix("### "), onWikilinkClick),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInline(line.removePrefix("## "), onWikilinkClick),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = parseInline(line.removePrefix("# "), onWikilinkClick),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Text(
                        text = parseInline("  \u2022 ${line.drop(2)}", onWikilinkClick),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("> ") -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = parseInline(line.removePrefix("> "), onWikilinkClick),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                line.startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = codeLines.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                line.isBlank() -> {
                    // spacing
                }
                else -> {
                    Text(
                        text = parseInline(line, onWikilinkClick),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            i++
        }
    }
}

private fun parseInline(
    text: String,
    onWikilinkClick: ((String) -> Unit)?
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val wikilinkMatch = Regex("\\[\\[([^\\]]+)\\]\\]").find(remaining)
            val boldMatch = Regex("\\*\\*(.+?)\\*\\*").find(remaining)
            val italicMatch = Regex("\\*(.+?)\\*").find(remaining)
            val codeMatch = Regex("`([^`]+)`").find(remaining)

            val candidates = listOfNotNull(
                wikilinkMatch?.let { "wikilink" to it },
                boldMatch?.let { "bold" to it },
                italicMatch?.let { "italic" to it },
                codeMatch?.let { "code" to it }
            )

            if (candidates.isEmpty()) {
                append(remaining)
                break
            }

            val (type, match) = candidates.minBy { it.second.range.first }
            if (match.range.first > 0) {
                append(remaining.substring(0, match.range.first))
            }

            when (type) {
                "bold" -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1])
                    }
                }
                "italic" -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groupValues[1])
                    }
                }
                "code" -> {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )) {
                        append(match.groupValues[1])
                    }
                }
                "wikilink" -> {
                    val title = match.groupValues[1]
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(title)
                    }
                }
            }
            remaining = remaining.substring(match.range.last + 1)
        }
    }
}
