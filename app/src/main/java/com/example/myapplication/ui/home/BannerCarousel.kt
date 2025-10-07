package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay

val bannerImages  = listOf(
    "https://v1.padlet.pics/3/image.webp?t=c_limit%2Cdpr_1%2Ch_951%2Cw_1340&url=https%3A%2F%2Fu1.padletusercontent.com%2Fuploads%2Fpadlet-uploads-usc1%2F4079103017%2Fd0b46eaec5e27332d97c6d839a136c0a%2FCUMPLEA_OS__41_.png%3Fexpiry_token%3D5WaHZRdGG3LkUVQGy3SZ-zdRtq89aJeottSBaF_Hii8dmxJqYDvE2-MDbblcM-ZrVekXW99RReKkJFIoMoKiownz0e6GqrC5X-n1hRaRzh8NNmtfEdbIXoQUFmVz0tcsdjfe9KVrmRhg-HlJe_mKm06W6Xu57aGsvPyvbar9UWyfqvIcH_43IMA_CBGlQ5uU6QcVIP-RLhtjyZo8WEWiZ8aynjvgusNNjv51yDpNjWc%3D",
    "https://v1.padlet.pics/3/image.webp?t=c_limit%2Cdpr_1%2Ch_951%2Cw_1340&url=https%3A%2F%2Fu1.padletusercontent.com%2Fuploads%2Fpadlet-uploads-usc1%2F4079103017%2Fd0b46eaec5e27332d97c6d839a136c0a%2FCUMPLEA_OS__41_.png%3Fexpiry_token%3D5WaHZRdGG3LkUVQGy3SZ-zdRtq89aJeottSBaF_Hii8dmxJqYDvE2-MDbblcM-ZrVekXW99RReKkJFIoMoKiownz0e6GqrC5X-n1hRaRzh8NNmtfEdbIXoQUFmVz0tcsdjfe9KVrmRhg-HlJe_mKm06W6Xu57aGsvPyvbar9UWyfqvIcH_43IMA_CBGlQ5uU6QcVIP-RLhtjyZo8WEWiZ8aynjvgusNNjv51yDpNjWc%3D",
    "https://v1.padlet.pics/3/image.webp?t=c_limit%2Cdpr_1%2Ch_951%2Cw_1340&url=https%3A%2F%2Fu1.padletusercontent.com%2Fuploads%2Fpadlet-uploads-usc1%2F4079103017%2Fd0b46eaec5e27332d97c6d839a136c0a%2FCUMPLEA_OS__41_.png%3Fexpiry_token%3D5WaHZRdGG3LkUVQGy3SZ-zdRtq89aJeottSBaF_Hii8dmxJqYDvE2-MDbblcM-ZrVekXW99RReKkJFIoMoKiownz0e6GqrC5X-n1hRaRzh8NNmtfEdbIXoQUFmVz0tcsdjfe9KVrmRhg-HlJe_mKm06W6Xu57aGsvPyvbar9UWyfqvIcH_43IMA_CBGlQ5uU6QcVIP-RLhtjyZo8WEWiZ8aynjvgusNNjv51yDpNjWc%3D"
)

@Composable
fun BannerCarousel() {
    val pagerState = rememberPagerState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % bannerImages.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // margen elegante
    ) {
        HorizontalPager(
            count = bannerImages.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // Relación 16:9 para evitar distorsión
        ) { page ->
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                AsyncImage(
                    model = bannerImages[page],
                    contentDescription = "Banner $page",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }


        Spacer(modifier = Modifier.height(8.dp))

        HorizontalPagerIndicator(
            pagerState = pagerState,
            activeColor = Color.Red,
            inactiveColor = Color.LightGray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
