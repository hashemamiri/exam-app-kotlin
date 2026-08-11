package ir.exam.app.ui.print

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ir.exam.app.domain.model.A4Page
import ir.exam.app.domain.model.QuestionPrintBlock
import ir.exam.app.ui.math.NativeMathText

@Composable
fun A4Preview(page: A4Page, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(210f / 297f)
            .background(Color.White)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("برگه A4 — صفحه ${page.number}", color = Color.Black)
            page.blocks.forEach { block ->
                if (block is QuestionPrintBlock) {
                    Column(
                        Modifier.fillMaxWidth().border(1.dp, Color.Gray).padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("سؤال ${block.row} · ${block.score} نمره", color = Color.Black)
                        NativeMathText(block.htmlFreeText, color = Color.Black)
                    }
                }
            }
        }
    }
}
