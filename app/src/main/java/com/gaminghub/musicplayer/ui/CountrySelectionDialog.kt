package com.gaminghub.musicplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gaminghub.musicplayer.ui.theme.MusifyCardBg
import com.gaminghub.musicplayer.ui.theme.MusifyDarkBg
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

data class CountryItem(val code: String, val name: String, val flag: String)

val availableCountries = listOf(
    CountryItem("IN", "India", "????"),
    CountryItem("US", "United States", "????"),
    CountryItem("GB", "United Kingdom", "????"),
    CountryItem("CA", "Canada", "????"),
    CountryItem("AU", "Australia", "????"),
    CountryItem("DE", "Germany", "????"),
    CountryItem("FR", "France", "????"),
    CountryItem("BR", "Brazil", "????"),
    CountryItem("JP", "Japan", "????"),
    CountryItem("KR", "South Korea", "????"),
    CountryItem("MX", "Mexico", "????"),
    CountryItem("ES", "Spain", "????"),
    CountryItem("IT", "Italy", "????"),
    CountryItem("ID", "Indonesia", "????"),
    CountryItem("NG", "Nigeria", "????"),
    CountryItem("PK", "Pakistan", "????"),
    CountryItem("BD", "Bangladesh", "????"),
    CountryItem("RU", "Russia", "????"),
    CountryItem("TR", "Turkey", "????"),
    CountryItem("AE", "United Arab Emirates", "????"),
    CountryItem("SA", "Saudi Arabia", "????"),
    CountryItem("PH", "Philippines", "????"),
    CountryItem("AR", "Argentina", "????"),
    CountryItem("NL", "Netherlands", "????"),
    CountryItem("SE", "Sweden", "????"),
    CountryItem("ZA", "South Africa", "????")
)

@Composable
fun CountrySelectionDialog(
    selectedCountry: String,
    onCountrySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) availableCountries
        else availableCountries.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = MusifyDarkBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, MusifyGlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Country",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MusifyGlassSurface)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MusifyGreen,
                        unfocusedBorderColor = MusifyGlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MusifyGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Country List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCountries) { country ->
                        val isSelected = country.name.equals(selectedCountry, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onCountrySelected(country.name)
                                    onDismiss()
                                },
                            color = if (isSelected) MusifyGreen.copy(alpha = 0.2f) else MusifyCardBg,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MusifyGreen) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(country.flag, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = country.name,
                                        color = if (isSelected) MusifyGreen else Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MusifyGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
