package com.glasslist.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

data class PhoneModel(val brand: String, val group: String, val name: String)

private fun modelKey(model: PhoneModel) = "${model.brand}|${model.group}|${model.name}" 

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val jsonText = assets.open("glass_list.json").bufferedReader().use { it.readText() }
        val source = JSONArray(jsonText)
        val allModels = buildList {
            for (i in 0 until source.length()) {
                val entry = source.getJSONObject(i)
                val entryBrand = entry.optString("brand").trim().uppercase()
                val entryGroup = entry.optString("group").trim().uppercase()
                val models = entry.optJSONArray("models") ?: JSONArray()
                for (j in 0 until models.length()) {
                    models.optString(j).split('/', '\n', '\r')
                        .map { it.trim().replace(Regex("\\s+"), " ") }
                        .filter { it.isNotBlank() }
                        .forEach { add(PhoneModel(entryBrand, entryGroup, it)) }
                }
            }
        }.distinctBy { "${it.brand}|${it.group}|${it.name.lowercase()}" }
        setContent { GlassListScreen(allModels) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassListScreen(data: List<PhoneModel>) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("glass_list", Context.MODE_PRIVATE) }
    val models = remember { mutableStateListOf<PhoneModel>().apply {
        addAll(data)
        val saved = runCatching { JSONArray(prefs.getString("added_models", "[]")) }.getOrNull() ?: JSONArray()
        for (i in 0 until saved.length()) {
            val o = saved.optJSONObject(i) ?: continue
            val m = PhoneModel(o.optString("brand"), o.optString("group"), o.optString("name"))
            if (m.brand.isNotBlank() && m.group.isNotBlank() && m.name.isNotBlank() && none { modelKey(it) == modelKey(m) }) add(m)
        }
    } }
    val favourites = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("favourites", emptySet()) ?: emptySet()) } }
    val stock = remember { mutableStateMapOf<String, Int>().apply {
        val saved = runCatching { JSONObject(prefs.getString("stock", "{}")) }.getOrNull() ?: JSONObject()
        saved.keys().forEach { key -> put(key, saved.optInt(key, 0)) }
    } }
    var query by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("ALL") }
    var group by remember { mutableStateOf("ALL") }
    var onlyFav by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var stockTarget by remember { mutableStateOf<PhoneModel?>(null) }
    var newBrand by remember { mutableStateOf("") }
    var newGroup by remember { mutableStateOf("G1") }
    var newName by remember { mutableStateOf("") }
    var newStock by remember { mutableStateOf("1") }
    var stockInput by remember { mutableStateOf("") }

    fun saveModels() {
        val arr = JSONArray()
        models.filter { m -> data.none { modelKey(it) == modelKey(m) } }.forEach { m ->
            arr.put(JSONObject().put("brand", m.brand).put("group", m.group).put("name", m.name))
        }
        prefs.edit().putString("added_models", arr.toString()).apply()
    }
    fun saveStock() {
        val obj = JSONObject()
        stock.forEach { (key, value) -> obj.put(key, value) }
        prefs.edit().putString("stock", obj.toString()).apply()
    }

    val brands = listOf("ALL") + models.map { it.brand }.distinct().sorted()
    val groups = listOf("ALL") + models.map { it.group }.distinct().sortedBy { it.drop(1).toIntOrNull() ?: 0 }
    val filtered = models.filter {
        (brand == "ALL" || it.brand == brand) && (group == "ALL" || it.group == group) &&
        (!onlyFav || favourites.contains(modelKey(it))) &&
        (query.isBlank() || it.name.contains(query.trim(), true) || it.brand.contains(query.trim(), true) || it.group.contains(query.trim(), true))
    }.sortedWith(compareBy<PhoneModel> { it.brand }.thenBy { it.group }.thenBy { it.name })
    val soldOutCount = models.count { stock[modelKey(it)] == 0 }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF3156D8), background = Color(0xFFF5F6FA), surface = Color.White)) {
        Scaffold(containerColor = Color(0xFFF5F6FA), topBar = {
            CenterAlignedTopAppBar(title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GLASS LIST", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("PHONE MODEL & STOCK MANAGER", fontSize = 10.sp, color = Color.Gray, letterSpacing = 1.sp)
                }
            }, actions = {
                TextButton(onClick = { showAdd = true }) { Text("+ ADD") }
                TextButton(onClick = { onlyFav = !onlyFav }) { Text(if (onlyFav) "★" else "☆") }
            })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⌕", fontSize = 23.sp, color = Color.Gray)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true, decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search phone model or brand...", color = Color.Gray, fontSize = 14.sp)
                        inner()
                    })
                    if (query.isNotEmpty()) Text("✕", Modifier.clickable { query = "" }, color = Color.Gray)
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL ${models.size} MODELS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("$soldOutCount SOLD OUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD04444))
                }
                Spacer(Modifier.height(8.dp))
                Text("BRAND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    brands.take(4).forEach { value -> FilterChip(selected = brand == value, onClick = { brand = value }, label = { Text(value, fontSize = 10.sp) }) }
                    if (brands.size > 4) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(selected = brand !in brands.take(4), onClick = { expanded = true }, label = { Text("More ▾", fontSize = 10.sp) })
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                brands.drop(4).forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { brand = value; expanded = false }) }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("GROUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    var groupExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { groupExpanded = true }) { Text(group, fontWeight = FontWeight.Bold) }
                        DropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                            groups.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { group = value; groupExpanded = false }) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${filtered.size} SHOWN", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filtered, key = { modelKey(it) }) { item ->
                        val key = modelKey(item)
                        val qty = stock[key]
                        val isOut = qty == 0
                        Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF20243A))
                                Spacer(Modifier.height(4.dp))
                                Text("${item.brand} • ${item.group}", fontSize = 10.sp, color = Color.Gray)
                                Spacer(Modifier.height(5.dp))
                                Text(when { qty == null -> "STOCK NOT SET"; isOut -> "SOLD OUT"; else -> "AVAILABLE: $qty" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = when { qty == null -> Color.Gray; isOut -> Color(0xFFD04444); else -> Color(0xFF16834A) })
                            }
                            Text(if (favourites.contains(key)) "★" else "☆", Modifier.clickable {
                                if (favourites.contains(key)) favourites.remove(key) else favourites.add(key)
                                prefs.edit().putStringSet("favourites", favourites.toSet()).apply()
                            }.padding(5.dp), fontSize = 19.sp, color = Color(0xFFE5A000))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("STOCK", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("−", Modifier.clickable { if (qty != null && qty > 0) { stock[key] = qty - 1; saveStock() } }.padding(horizontal = 7.dp, vertical = 5.dp), fontSize = 18.sp, color = Color(0xFFD04444))
                                    Text(qty?.toString() ?: "?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("+", Modifier.clickable { stock[key] = (qty ?: 0) + 1; saveStock() }.padding(horizontal = 7.dp, vertical = 5.dp), fontSize = 18.sp, color = Color(0xFF16834A))
                                }
                                Text("SET", Modifier.clickable { stockInput = qty?.toString() ?: "0"; stockTarget = item }.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 9.sp, color = Color(0xFF3156D8), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(5.dp))
                            Text("COPY", Modifier.background(Color(0xFFEFF2FF), RoundedCornerShape(8.dp)).clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Phone model", item.name))
                                Toast.makeText(context, "Copied: ${item.name}", Toast.LENGTH_SHORT).show()
                            }.padding(horizontal = 9.dp, vertical = 9.dp), color = Color(0xFF3156D8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (filtered.isEmpty()) item { Text("No models found. Try another search.", Modifier.fillMaxWidth().padding(30.dp), color = Color.Gray) }
                }
            }
        }
    }

    if (showAdd) AlertDialog(
        onDismissRequest = { showAdd = false },
        title = { Text("Add New Glass Model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = newBrand, onValueChange = { newBrand = it }, label = { Text("Brand (e.g. VIVO)") }, singleLine = true)
                OutlinedTextField(value = newGroup, onValueChange = { newGroup = it }, label = { Text("Group (e.g. G11)") }, singleLine = true)
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Phone model name") }, singleLine = true)
                OutlinedTextField(value = newStock, onValueChange = { newStock = it.filter(Char::isDigit) }, label = { Text("Starting stock quantity") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { TextButton(onClick = {
            val m = PhoneModel(newBrand.trim().uppercase(), newGroup.trim().uppercase(), newName.trim())
            val qty = newStock.toIntOrNull()
            if (m.brand.isBlank() || m.group.isBlank() || m.name.isBlank() || qty == null || qty < 0) {
                Toast.makeText(context, "Enter valid brand, group, model and stock", Toast.LENGTH_SHORT).show()
            } else if (models.any { modelKey(it).equals(modelKey(m), true) }) {
                Toast.makeText(context, "This model already exists", Toast.LENGTH_SHORT).show()
            } else {
                models.add(m); stock[modelKey(m)] = qty; saveModels(); saveStock()
                newBrand = ""; newGroup = "G1"; newName = ""; newStock = "1"; showAdd = false
                Toast.makeText(context, "Model added", Toast.LENGTH_SHORT).show()
            }
        }) { Text("ADD MODEL") } },
        dismissButton = { TextButton(onClick = { showAdd = false }) { Text("CANCEL") } }
    )

    stockTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { stockTarget = null },
            title = { Text("Update Stock") },
            text = {
                Column {
                    Text(target.name, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = stockInput, onValueChange = { stockInput = it.filter(Char::isDigit) }, label = { Text("Available quantity (0 = Sold Out)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = { TextButton(onClick = {
                val qty = stockInput.toIntOrNull()
                if (qty == null) Toast.makeText(context, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
                else { stock[modelKey(target)] = qty; saveStock(); stockTarget = null }
            }) { Text("SAVE") } },
            dismissButton = { TextButton(onClick = { stockTarget = null }) { Text("CANCEL") } }
        )
    }
}
