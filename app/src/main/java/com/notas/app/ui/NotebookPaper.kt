package com.notas.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notas.app.ui.theme.LocalNotebookColors
import kotlin.math.roundToInt

/** Distância entre as pautas. Todo texto sobre a folha usa esta altura de linha. */
val RuleHeight = 32.sp

/** Posição da margem vermelha, a partir da esquerda. */
val MarginWidth = 64.dp

private val TextStart = 14.dp
private val TextEnd = 20.dp

/**
 * Altura da pauta em pixels inteiros. Arredondar evita que texto e linhas se
 * desencontrem aos poucos quando a escala da fonte dá um valor quebrado.
 */
@Composable
fun rulePx(): Int = with(LocalDensity.current) { RuleHeight.toPx().roundToInt() }

/** Altura de [lines] pautas em dp (acompanha o tamanho de fonte escolhido). */
@Composable
fun ruleDp(lines: Int = 1): Dp = with(LocalDensity.current) { (rulePx() * lines).toDp() }

/** Recuo do texto: começa logo depois da margem. */
fun Modifier.afterMargin() = padding(start = MarginWidth + TextStart, end = TextEnd)

/** Encaixa o texto na pauta, com a base das letras sobre a linha. */
@Composable
fun TextStyle.onRule(): TextStyle = copy(
    lineHeight = with(LocalDensity.current) { rulePx().toSp() },
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Bottom,
        trim = LineHeightStyle.Trim.None,
    ),
)

/** Fundo de papel com as pautas. [scrollOffset] faz as linhas rolarem junto com o conteúdo. */
@Composable
fun Modifier.paperRules(scrollOffset: () -> Int = { 0 }): Modifier {
    val rule = LocalNotebookColors.current.rule
    val paper = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    val rulePx = rulePx().toFloat()
    val stroke = with(density) { 1.dp.toPx() }

    return drawBehind {
        drawRect(paper)
        var y = rulePx - (scrollOffset() % rulePx) - stroke / 2
        while (y <= size.height) {
            drawLine(rule, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            y += rulePx
        }
    }
}

/** Margem dupla vermelha, desenhada por cima de tudo na altura inteira da tela. */
@Composable
fun Modifier.paperMargin(): Modifier {
    val margin = LocalNotebookColors.current.margin
    val paper = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    val x = with(density) { MarginWidth.toPx() }
    val stroke = with(density) { 1.dp.toPx() }

    return drawWithContent {
        drawRect(paper)
        drawContent()
        drawLine(margin, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
        drawLine(margin, Offset(x + stroke * 3, 0f), Offset(x + stroke * 3, size.height), strokeWidth = stroke)
    }
}

/**
 * Uma linha do caderno, com exatamente uma pauta de altura.
 * [margin] fica à esquerda da margem vermelha (datas, símbolos); [content], depois dela.
 */
@Composable
fun NotebookLine(
    modifier: Modifier = Modifier,
    margin: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth().height(ruleDp())) {
        Box(
            Modifier.width(MarginWidth).padding(end = 10.dp),
            contentAlignment = Alignment.BottomEnd,
            content = margin,
        )
        Box(
            Modifier.weight(1f).padding(start = TextStart, end = TextEnd),
            contentAlignment = Alignment.BottomStart,
            content = content,
        )
    }
}
