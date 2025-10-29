package com.example.myapplication.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.NavItem

@Composable
fun BottomNavBar(
    navItemList: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFF8F8F8),
        tonalElevation = 4.dp,
        contentColor = Color.DarkGray
    ) {
        navItemList.forEachIndexed { index, navItem ->
            val isSelected = index == selectedIndex
            val animatedScale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1f,
                label = "ScaleOnSelect"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.label,
                        tint = if (isSelected)
                            Color(0xFF0051A8) // Azul corporativo
                        else
                            Color.Gray,
                        modifier = Modifier.scale(animatedScale)
                    )
                },
                label = {
                    Text(
                        text = navItem.label,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected)
                            Color(0xFF0051A8)
                        else
                            Color.Gray
                    )
                },
                alwaysShowLabel = true,
                interactionSource = remember { MutableInteractionSource() }
            )
        }
    }
}
