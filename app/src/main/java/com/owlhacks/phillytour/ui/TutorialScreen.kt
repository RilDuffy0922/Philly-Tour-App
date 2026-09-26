package com.owlhacks.phillytour.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.owlhacks.phillytour.R
import kotlinx.coroutines.launch

private data class TutorialPage(val title: String, val text: String, val icon: ImageVector?)

private val PAGES = listOf(
    TutorialPage(
        "Meet your otter guide",
        "I'll take you along Philadelphia's rivers and share fun facts about the places we pass.",
        null
    ),
    TutorialPage(
        "Pick a tour",
        "Choose the Schuylkill or the Delaware. When you start, the stop closest to you becomes stop 1.",
        Icons.Filled.Map
    ),
    TutorialPage(
        "Tap Start tour and follow me",
        "I'll chat about the next stop as you go. When you arrive, you'll hear its story and get a trivia question.",
        Icons.Filled.LocationOn
    ),
    TutorialPage(
        "Make it yours",
        "Not interested in a stop? Tap Skip. Want to try it from your couch? Open the ⋮ menu to set your " +
            "location or run a demo. Change my voice in Settings.",
        Icons.Filled.Tune
    ),
)

/** A short first-run walkthrough. Shown once automatically, and any time from the "How it works" button. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGES.lastIndex

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = PAGES[index]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (page.icon != null) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.otter),
                        contentDescription = null,
                        modifier = Modifier.size(140.dp).clip(CircleShape)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = page.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(PAGES.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (isLast) onDone() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLast) "Get started" else "Next")
            }
            if (!isLast) {
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Skip")
                }
            }
        }
    }
}
