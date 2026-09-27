package com.calorielens.ai

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalorieLensApp(this) }
    }
}

data class FoodItem(
    val name: String,
    var grams: Double,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double
)

data class Analysis(
    val mealName: String,
    val items: List<FoodItem>,
    val minCalories: Int,
    val maxCalories: Int,
    val confidence: Int,
    val note: String
)

private fun prefs(context: Context) = EncryptedSharedPreferences.create(
    context,
    "secure_settings",
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieLensApp(context: Context) {
    val scope = rememberCoroutineScope()
    val p = remember { prefs(context) }
    var apiKey by remember { mutableStateOf(p.getString("openai_key", "") ?: "") }
    var firstImage by remember { mutableStateOf<Uri?>(null) }
    var secondImage by remember { mutableStateOf<Uri?>(null) }
    var plateDiameter by remember { mutableStateOf("25") }
    var notes by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf<Analysis?>(null) }

    val firstPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        firstImage = it
    }
    val secondPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        secondImage = it
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF66E0A3),
            background = Color(0xFF0B0F14),
            surface = Color(0xFF141A21)
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Calorie Lens AI") })
            }
        ) { pad ->
            Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .background(Color(0xFF0B0F14))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("حلّل أكلك بالذكاء الاصطناعي", fontSize = 22.sp)
                Text(
                    "الصورة وحدها تقدير وليست ميزانًا. إضافة قطر الطبق وصورة ثانية وملاحظات الزيت ترفع الدقة.",
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it.trim()
                        p.edit().putString("openai_key", apiKey).apply()
                    },
                    label = { Text("OpenAI API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(onClick = { firstPicker.launch("image/*") }, Modifier.fillMaxWidth()) {
                    Text(if (firstImage == null) "اختار / صوّر صورة الأكلة" else "✓ الصورة الأساسية جاهزة")
                }
                OutlinedButton(onClick = { secondPicker.launch("image/*") }, Modifier.fillMaxWidth()) {
                    Text(if (secondImage == null) "أضف زاوية ثانية — اختياري" else "✓ الصورة الثانية جاهزة")
                }

                OutlinedTextField(
                    value = plateDiameter,
                    onValueChange = { plateDiameter = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("قطر الطبق بالسنتيمتر — اختياري") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات: زيت، صوص، طريقة التسوية…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Button(
                    enabled = !busy && firstImage != null && apiKey.isNotBlank(),
                    onClick = {
                        busy = true
                        error = ""
                        analysis = null
                        scope.launch {
                            try {
                                analysis = analyzeMeal(
                                    context, apiKey, firstImage!!, secondImage,
                                    plateDiameter, notes
                                )
                            } catch (e: Exception) {
                                error = e.message ?: "حصل خطأ غير معروف"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (busy) "جاري التحليل…" else "حلّل الوجبة")
                }

                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }

                analysis?.let { a ->
                    HorizontalDivider()
                    Text(a.mealName, fontSize = 22.sp)
                    val kcal = a.items.sumOf { it.grams * it.kcalPer100 / 100.0 }
                    val protein = a.items.sumOf { it.grams * it.proteinPer100 / 100.0 }
                    val carbs = a.items.sumOf { it.grams * it.carbsPer100 / 100.0 }
                    val fat = a.items.sumOf { it.grams * it.fatPer100 / 100.0 }

                    Text("السعرات المحسوبة: ${kcal.toInt()} kcal", fontSize = 20.sp)
                    Text("النطاق المتوقع من الصورة: ${a.minCalories}–${a.maxCalories} kcal")
                    Text("الثقة: ${a.confidence}%")
                    Text("Protein ${"%.1f".format(protein)}g   •   Carbs ${"%.1f".format(carbs)}g   •   Fat ${"%.1f".format(fat)}g")

                    a.items.forEachIndexed { idx, item ->
                        var g by remember(a, idx) { mutableStateOf(item.grams.toString()) }
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.name, fontSize = 17.sp)
                                OutlinedTextField(
                                    value = g,
                                    onValueChange = { v ->
                                        g = v.filter { c -> c.isDigit() || c == '.' }
                                        g.toDoubleOrNull()?.let { item.grams = it }
                                        analysis = a.copy(items = a.items.toList())
                                    },
                                    label = { Text("الجرام — عدّله لو عارف الوزن الحقيقي") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("${(item.grams * item.kcalPer100 / 100).toInt()} kcal")
                            }
                        }
                    }
                    if (a.note.isNotBlank()) Text("ملاحظة التحليل: ${a.note}", color = Color.LightGray)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private suspend fun analyzeMeal(
    context: Context,
    apiKey: String,
    image1: Uri,
    image2: Uri?,
    plateDiameter: String,
    notes: String
): Analysis = withContext(Dispatchers.IO) {
    val content = JSONArray()
    content.put(JSONObject().put("type", "input_text").put(
        "text",
        """حلل الوجبة من الصور بأقصى دقة عملية ممكنة.
تعرف على الأطعمة المصرية والعربية جيدًا.
استخدم قطر الطبق كمرجع للحجم إن كان متاحًا: $plateDiameter سم.
ملاحظات المستخدم: $notes
لا تدّعِ دقة غير ممكنة من الصورة. قدّر المكونات والأوزان ثم القيم الغذائية لكل 100 جم.
أرجع JSON فقط بالشكل:
{"meal_name":"", "items":[{"name":"","grams":0,"kcal_per_100g":0,"protein_per_100g":0,"carbs_per_100g":0,"fat_per_100g":0}], "min_calories":0,"max_calories":0,"confidence":0,"note":""}
confidence رقم من 0 إلى 100."""
    ))
    content.put(imagePart(context, image1))
    if (image2 != null) content.put(imagePart(context, image2))

    val bodyJson = JSONObject()
        .put("model", "gpt-5.6-sol")
        .put("store", false)
        .put("input", JSONArray().put(
            JSONObject()
                .put("role", "user")
                .put("content", content)
        ))
        .put("reasoning", JSONObject().put("effort", "high"))
        .put("text", JSONObject().put("format",
            JSONObject()
                .put("type", "json_schema")
                .put("name", "meal_analysis")
                .put("strict", true)
                .put("schema", schema())
        ))

    val req = Request.Builder()
        .url("https://api.openai.com/v1/responses")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

    val response = OkHttpClient.Builder().build().newCall(req).execute()
    val raw = response.body?.string().orEmpty()
    if (!response.isSuccessful) throw IOException("OpenAI API: ${response.code} — $raw")

    val root = JSONObject(raw)
    val output = root.getJSONArray("output")
    var jsonText: String? = null
    for (i in 0 until output.length()) {
        val o = output.getJSONObject(i)
        val c = o.optJSONArray("content") ?: continue
        for (j in 0 until c.length()) {
            val part = c.getJSONObject(j)
            if (part.optString("type") == "output_text") jsonText = part.optString("text")
        }
    }
    val j = JSONObject(jsonText ?: throw IOException("لم يصل رد صالح من النموذج"))
    val arr = j.getJSONArray("items")
    val items = mutableListOf<FoodItem>()
    for (i in 0 until arr.length()) {
        val x = arr.getJSONObject(i)
        items += FoodItem(
            x.getString("name"),
            x.getDouble("grams"),
            x.getDouble("kcal_per_100g"),
            x.getDouble("protein_per_100g"),
            x.getDouble("carbs_per_100g"),
            x.getDouble("fat_per_100g")
        )
    }
    Analysis(
        j.getString("meal_name"), items,
        j.getInt("min_calories"), j.getInt("max_calories"),
        j.getInt("confidence"), j.optString("note")
    )
}

private fun imagePart(context: Context, uri: Uri): JSONObject {
    val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return JSONObject()
        .put("type", "input_image")
        .put("image_url", "data:image/jpeg;base64,$b64")
        .put("detail", "high")
}

private fun schema(): JSONObject {
    fun num() = JSONObject().put("type", "number")
    val item = JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("properties", JSONObject()
            .put("name", JSONObject().put("type", "string"))
            .put("grams", num())
            .put("kcal_per_100g", num())
            .put("protein_per_100g", num())
            .put("carbs_per_100g", num())
            .put("fat_per_100g", num())
        )
        .put("required", JSONArray(listOf("name","grams","kcal_per_100g","protein_per_100g","carbs_per_100g","fat_per_100g")))
    return JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("properties", JSONObject()
            .put("meal_name", JSONObject().put("type", "string"))
            .put("items", JSONObject().put("type", "array").put("items", item))
            .put("min_calories", JSONObject().put("type", "integer"))
            .put("max_calories", JSONObject().put("type", "integer"))
            .put("confidence", JSONObject().put("type", "integer"))
            .put("note", JSONObject().put("type", "string"))
        )
        .put("required", JSONArray(listOf("meal_name","items","min_calories","max_calories","confidence","note")))
}
