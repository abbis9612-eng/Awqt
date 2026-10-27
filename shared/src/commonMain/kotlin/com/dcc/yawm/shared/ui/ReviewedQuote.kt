package com.dcc.yawm.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcc.yawm.shared.content.ReviewedTexts

/** A reviewed text with its source and grade; shows nothing until the reviewer approves it. */
@Composable
fun ReviewedQuote(id: String) {
    val t = ReviewedTexts.byId(id) ?: return
    val pal = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Txt(t.text, size = 17.sp, lineHeight = 1.8f)
        Txt("${t.source} · ${t.grade}", size = 13.5.sp, color = pal.grey, lineHeight = 1.5f)
        if (t.note != null) Txt(t.note, size = 13.5.sp, color = pal.grey, lineHeight = 1.5f)
    }
}
