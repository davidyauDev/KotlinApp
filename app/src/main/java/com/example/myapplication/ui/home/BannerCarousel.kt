package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val bannerImages  = listOf(
        "https://www.udacity.com/blog/wp-content/uploads/2018/05/Kotlin-Udacity-Google.png",
    "https://www.udacity.com/blog/wp-content/uploads/2018/05/Kotlin-Udacity-Google.png",
    "https://www.udacity.com/blog/wp-content/uploads/2018/05/Kotlin-Udacity-Google.png"
    )

@Composable
fun BannerCarousel() {
    val pagerState = rememberPagerState()

    // State for images fetched from API
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Fetch banners from API on composition
    LaunchedEffect(Unit) {
        // start auto scroll coroutine; launched child will be cancelled automatically with this scope
        launch {
            while (true) {
                delay(4000)
                if (images.isNotEmpty()) {
                    val next = (pagerState.currentPage + 1) % images.size
                    pagerState.animateScrollToPage(next)
                }
            }
        }

        // Por ahora comentamos la lógica de llamada al API y usamos las rutas estáticas definidas en `bannerImages`.
        // Esto facilita pruebas locales y evita depender del endpoint remoto temporalmente.
        /*
        try {
            isLoading = true

            // obtain token from DataStore via UserPreferences
            val userPreferences = UserPreferences(context)
            val token = try { userPreferences.userToken.first() } catch (e: Exception) { "" }

            val api = if (!token.isNullOrBlank()) {
                RetrofitClient.apiWithToken { token }
            } else {
                RetrofitClient.apiWithoutToken
            }

            val response = api.getBanners()

            if (response.isSuccessful) {
                val body = response.body()
                val urls = body?.data?.mapNotNull { it.image_url }?.filter { it.isNotBlank() } ?: emptyList()
                images = if (urls.isNotEmpty()) urls else bannerImages
            } else {
                val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                errorMsg = "Error fetching banners: ${response.code()} ${response.message()} - ${errorBody ?: "no error body"}"
                images = bannerImages
            }
        } catch (e: Exception) {
            errorMsg = e.localizedMessage ?: "Unknown error"
            images = bannerImages
        } finally {
            isLoading = false
        }
        */

        // Uso temporal de las imágenes locales
        images = bannerImages
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // margen elegante
    ) {
        // If still loading and no images yet, show a loader
        if (isLoading && images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            HorizontalPager(
                count = images.size,
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
                        model = images.getOrNull(page),
                        contentDescription = "Banner $page",
                        contentScale = ContentScale.Crop,
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

            // show error text if any
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg ?: "",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
