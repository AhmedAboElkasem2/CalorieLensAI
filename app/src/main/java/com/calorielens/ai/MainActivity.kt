package com.calorielens.ai

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.IOException
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalorieLensFreeApp(this) }
    }
}

data class Food(
    val name: String,
    val aliases: List<String>,
    val visionKeywords: List<String>,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val defaultGrams: Double
)

data class MealLine(
    val id: Int,
    val food: Food,
    val gramsText: String
)

private val foods = listOf(
    Food("أرز أبيض مطبوخ", listOf("رز", "ارز", "rice"), listOf("rice"), 130.0, 2.4, 28.2, 0.3, 180.0),
    Food("أرز بسمتي مطبوخ", listOf("بسمتي", "basmati"), listOf("rice"), 121.0, 3.5, 25.2, 0.4, 180.0),
    Food("مكرونة مطبوخة", listOf("مكرونه", "مكرونة", "pasta", "spaghetti"), listOf("pasta", "spaghetti", "noodle"), 158.0, 5.8, 30.9, 0.9, 180.0),
    Food("عيش بلدي", listOf("عيش", "خبز", "بلدي", "bread"), listOf("bread", "flatbread"), 250.0, 8.5, 52.0, 1.3, 90.0),
    Food("توست أبيض", listOf("توست", "toast"), listOf("toast", "bread"), 266.0, 8.9, 49.4, 3.3, 30.0),

    Food("فول مدمس", listOf("فول", "beans", "fava"), listOf("bean", "beans"), 110.0, 7.6, 19.7, 0.4, 180.0),
    Food("طعمية / فلافل", listOf("طعميه", "طعمية", "فلافل", "falafel"), listOf("falafel"), 333.0, 13.3, 31.8, 17.8, 90.0),
    Food("كشري", listOf("كشري", "koshari", "koshary"), listOf("rice", "pasta", "lentil"), 160.0, 5.0, 30.0, 2.5, 350.0),
    Food("عدس مطبوخ", listOf("عدس", "lentil"), listOf("lentil", "soup"), 116.0, 9.0, 20.1, 0.4, 180.0),
    Food("شوربة عدس", listOf("شوربة عدس", "lentil soup"), listOf("soup", "lentil"), 75.0, 4.0, 12.0, 1.5, 250.0),

    Food("صدر فراخ مشوي", listOf("فراخ", "دجاج", "صدر فراخ", "chicken breast"), listOf("chicken", "poultry"), 165.0, 31.0, 0.0, 3.6, 150.0),
    Food("ورك فراخ مطهو", listOf("ورك", "فخدة", "chicken thigh"), listOf("chicken", "poultry"), 209.0, 26.0, 0.0, 10.9, 150.0),
    Food("فراخ مقلية", listOf("فراخ مقلية", "fried chicken"), listOf("fried chicken", "chicken"), 260.0, 24.0, 8.0, 15.0, 150.0),
    Food("لحم بقري مشوي", listOf("لحمة", "لحم", "beef", "steak"), listOf("beef", "steak", "meat"), 250.0, 26.0, 0.0, 17.0, 150.0),
    Food("كفتة", listOf("كفتة", "kofta"), listOf("meatball", "meat"), 250.0, 20.0, 3.0, 18.0, 150.0),
    Food("كباب", listOf("كباب", "kebab"), listOf("kebab", "meat"), 230.0, 25.0, 2.0, 14.0, 150.0),
    Food("كبدة مطهية", listOf("كبدة", "liver"), listOf("liver", "meat"), 175.0, 26.0, 5.0, 5.0, 120.0),
    Food("سمك مشوي", listOf("سمك", "fish"), listOf("fish", "seafood"), 150.0, 24.0, 0.0, 5.0, 180.0),
    Food("تونة بالماء", listOf("تونه", "تونة", "tuna"), listOf("tuna", "fish"), 116.0, 25.5, 0.0, 0.8, 120.0),
    Food("جمبري مطهو", listOf("جمبري", "shrimp", "prawn"), listOf("shrimp", "prawn", "seafood"), 99.0, 24.0, 0.2, 0.3, 150.0),

    Food("بيض مسلوق", listOf("بيض", "egg", "boiled egg"), listOf("egg"), 155.0, 13.0, 1.1, 11.0, 50.0),
    Food("أومليت", listOf("اومليت", "أومليت", "omelet", "omelette"), listOf("omelet", "egg"), 154.0, 11.0, 2.0, 11.0, 120.0),
    Food("جبنة قريش", listOf("جبنه قريش", "جبنة قريش", "cottage cheese"), listOf("cheese"), 98.0, 11.1, 3.4, 4.3, 100.0),
    Food("جبنة بيضاء / فيتا", listOf("جبنه", "فيتا", "feta", "cheese"), listOf("cheese", "feta"), 264.0, 14.0, 4.1, 21.0, 50.0),
    Food("زبادي", listOf("زبادي", "yogurt", "yoghurt"), listOf("yogurt"), 61.0, 3.5, 4.7, 3.3, 170.0),
    Food("لبن كامل الدسم", listOf("لبن", "حليب", "milk"), listOf("milk"), 61.0, 3.2, 4.8, 3.3, 240.0),

    Food("بطاطس مسلوقة", listOf("بطاطس", "potato"), listOf("potato"), 87.0, 1.9, 20.1, 0.1, 180.0),
    Food("بطاطس مقلية", listOf("بطاطس مقلية", "فرنش فرايز", "fries", "french fries"), listOf("french fries", "fries"), 312.0, 3.4, 41.4, 15.0, 120.0),
    Food("بطاطا حلوة", listOf("بطاطا", "sweet potato"), listOf("sweet potato"), 90.0, 2.0, 20.7, 0.2, 180.0),
    Food("خضار مشكل مطبوخ", listOf("خضار", "vegetables", "mixed vegetables"), listOf("vegetable"), 65.0, 3.0, 12.0, 1.0, 200.0),
    Food("سلطة خضراء", listOf("سلطة", "salad"), listOf("salad", "vegetable"), 25.0, 1.2, 5.0, 0.2, 200.0),
    Food("طماطم", listOf("طماطم", "tomato"), listOf("tomato"), 18.0, 0.9, 3.9, 0.2, 100.0),
    Food("خيار", listOf("خيار", "cucumber"), listOf("cucumber"), 15.0, 0.7, 3.6, 0.1, 100.0),
    Food("بامية مطبوخة", listOf("بامية", "okra"), listOf("okra", "vegetable"), 65.0, 2.0, 10.0, 2.0, 200.0),
    Food("ملوخية مطبوخة", listOf("ملوخية", "molokhia"), listOf("soup", "leaf vegetable"), 70.0, 3.0, 7.0, 4.0, 200.0),

    Food("محشي ورق عنب", listOf("ورق عنب", "محشي", "grape leaves"), listOf("stuffed", "rice"), 150.0, 3.0, 23.0, 5.0, 180.0),
    Food("محشي كوسة", listOf("محشي كوسة", "كوسة", "stuffed zucchini"), listOf("stuffed", "zucchini"), 135.0, 4.0, 20.0, 4.5, 220.0),
    Food("شاورما فراخ", listOf("شاورما", "shawarma"), listOf("shawarma", "chicken"), 215.0, 23.0, 4.0, 12.0, 150.0),
    Food("برجر لحم", listOf("برجر", "burger", "hamburger"), listOf("hamburger", "burger"), 250.0, 17.0, 24.0, 10.0, 200.0),
    Food("بيتزا جبنة", listOf("بيتزا", "pizza"), listOf("pizza"), 266.0, 11.0, 33.0, 10.0, 200.0),

    Food("تفاح", listOf("تفاح", "apple"), listOf("apple"), 52.0, 0.3, 13.8, 0.2, 180.0),
    Food("موز", listOf("موز", "banana"), listOf("banana"), 89.0, 1.1, 22.8, 0.3, 120.0),
    Food("برتقال", listOf("برتقال", "orange"), listOf("orange", "citrus"), 47.0, 0.9, 11.8, 0.1, 180.0),
    Food("تمر", listOf("تمر", "dates"), listOf("date", "dried fruit"), 277.0, 1.8, 75.0, 0.2, 25.0),

    Food("بسبوسة", listOf("بسبوسه", "بسبوسة", "basbousa"), listOf("cake", "dessert"), 350.0, 5.0, 48.0, 16.0, 100.0),
    Food("كنافة", listOf("كنافه", "كنافة", "kunafa"), listOf("dessert", "pastry"), 360.0, 6.0, 45.0, 18.0, 100.0),

    Food("زيت طهي", listOf("زيت", "oil"), listOf(), 884.0, 0.0, 0.0, 100.0, 5.0),
    Food("سمن / زبدة", listOf("سمن", "زبدة", "butter", "ghee"), listOf("butter"), 717.0, 0.9, 0.1, 81.0, 5.0),
    Food("عسل نحل", listOf("عسل", "honey"), listOf("honey"), 304.0, 0.3, 82.4, 0.0, 20.0),
    Food("سكر", listOf("سكر", "sugar"), listOf("sugar"), 387.0, 0.0, 100.0, 0.0, 10.0)
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CalorieLensFreeApp(context: Context) {
    var firstImage by remember { mutableStateOf<Uri?>(null) }
    var secondImage by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("اختار صورة واضغط تحليل مجاني") }
    var detected by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Food>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var nextId by remember { mutableStateOf(1) }
    val meal = remember { mutableStateListOf<MealLine>() }

    val firstPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        firstImage = it
        suggestions = emptyList()
        detected = ""
        info = if (it == null) "اختار صورة واضغط تحليل مجاني" else "الصورة الأساسية جاهزة"
    }

    val secondPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        secondImage = it
        if (it != null) info = "الصورة الثانية جاهزة"
    }

    fun addFood(food: Food) {
        meal.add(MealLine(nextId++, food, formatGrams(food.defaultGrams)))
    }

    val searchResults = remember(search) {
        val q = search.trim().lowercase(Locale.ROOT)
        if (q.length < 2) emptyList()
        else foods.filter { food ->
            food.name.lowercase(Locale.ROOT).contains(q) ||
                food.aliases.any { it.lowercase(Locale.ROOT).contains(q) }
        }.take(10)
    }

    val totalKcal = meal.sumOf { line ->
        grams(line.gramsText) * line.food.kcalPer100 / 100.0
    }
    val totalProtein = meal.sumOf { line ->
        grams(line.gramsText) * line.food.proteinPer100 / 100.0
    }
    val totalCarbs = meal.sumOf { line ->
        grams(line.gramsText) * line.food.carbsPer100 / 100.0
    }
    val totalFat = meal.sumOf { line ->
        grams(line.gramsText) * line.food.fatPer100 / 100.0
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF66E0A3),
            background = Color(0xFF0B0F14),
            surface = Color(0xFF141A21)
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Calorie Lens AI • Free") }) }
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
                    Text("حلل أكلك بدون API وبدون اشتراك", fontSize = 22.sp)
                    Text(
                        "التعرف الأساسي على الصورة بيتم على الموبايل نفسه. الصورة لا تقدر تقيس الوزن بدقة، لذلك أكد نوع الأكل وعدّل الجرامات للحصول على نتيجة أدق.",
                        color = Color.LightGray
                    )

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("✓ مجاني لكل تحليل")
                            Text("✓ بدون OpenAI API Key")
                            Text("✓ نموذج التعرف على الصور مدمج داخل التطبيق")
                            Text("✓ قاعدة سعرات محلية وأكلات مصرية")
                        }
                    }

                    Button(
                        onClick = { firstPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (firstImage == null) "اختار صورة الأكلة" else "✓ الصورة الأساسية جاهزة")
                    }

                    OutlinedButton(
                        onClick = { secondPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (secondImage == null) "أضف زاوية ثانية — اختياري" else "✓ الصورة الثانية جاهزة")
                    }

                    Button(
                        enabled = firstImage != null && !busy,
                        onClick = {
                            val image = firstImage ?: return@Button
                            busy = true
                            info = "جاري التحليل على الجهاز..."
                            detected = ""
                            suggestions = emptyList()

                            analyzeImagesOffline(
                                context = context,
                                first = image,
                                second = secondImage,
                                onSuccess = { labels ->
                                    busy = false
                                    val useful = labels
                                        .sortedByDescending { it.confidence }
                                        .distinctBy { it.text.lowercase(Locale.ROOT) }
                                        .take(8)

                                    detected = useful.joinToString(" • ") {
                                        "${it.text} ${(it.confidence * 100).toInt()}%"
                                    }

                                    suggestions = matchFoods(labels)
                                    info = if (suggestions.isEmpty()) {
                                        "الصورة اتقرت، لكن مفيش أكلة محددة بثقة كافية. استخدم البحث اليدوي تحت."
                                    } else {
                                        "دي اقتراحات الذكاء المحلي. ضيف الصحيح منها فقط."
                                    }
                                },
                                onError = { message ->
                                    busy = false
                                    info = "حصل خطأ: $message"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (busy) "جاري التحليل..." else "حلل الصورة مجانًا")
                    }

                    Text(info, color = if (info.startsWith("حصل خطأ")) MaterialTheme.colorScheme.error else Color.LightGray)

                    if (detected.isNotBlank()) {
                        Text("اللي النموذج شافه في الصورة:", fontSize = 16.sp)
                        Text(detected, color = Color.Gray)
                    }

                    if (suggestions.isNotEmpty()) {
                        HorizontalDivider()
                        Text("اقتراحات من الصورة", fontSize = 20.sp)
                        suggestions.forEach { food ->
                            FoodSuggestionCard(food = food, onAdd = { addFood(food) })
                        }
                    }

                    HorizontalDivider()
                    Text("ابحث عن أي أكلة يدويًا", fontSize = 20.sp)
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("مثال: رز  فراخ  فول  كشري  زيت") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    searchResults.forEach { food ->
                        FoodSuggestionCard(food = food, onAdd = { addFood(food) })
                    }

                    if (meal.isNotEmpty()) {
                        HorizontalDivider()
                        Text("وجبتك", fontSize = 22.sp)
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("${totalKcal.toInt()} kcal", fontSize = 28.sp)
                                Text(
                                    "بروتين ${"%.1f".format(totalProtein)} جم  •  كارب ${"%.1f".format(totalCarbs)} جم  •  دهون ${"%.1f".format(totalFat)} جم"
                                )
                            }
                        }

                        meal.forEachIndexed { index, line ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(line.food.name, fontSize = 18.sp)
                                    OutlinedTextField(
                                        value = line.gramsText,
                                        onValueChange = { value ->
                                            val cleaned = value.filter { it.isDigit() || it == '.' }
                                            meal[index] = line.copy(gramsText = cleaned)
                                        },
                                        label = { Text("الوزن بالجرام") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    val itemKcal = grams(line.gramsText) * line.food.kcalPer100 / 100.0
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${itemKcal.toInt()} kcal")
                                        TextButton(onClick = { meal.removeAt(index) }) {
                                            Text("حذف")
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            "القيم الغذائية تقريبية لأنها تختلف حسب الوصفة وطريقة التسوية. لو عندك ميزان مطبخ، تعديل الجرامات هو أكبر حاجة ترفع دقة السعرات.",
                            color = Color.Gray
                        )

                        OutlinedButton(
                            onClick = { meal.clear() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("امسح الوجبة وابدأ من جديد")
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun FoodSuggestionCard(food: Food, onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(food.name, fontSize = 17.sp)
                Text(
                    "${food.kcalPer100.toInt()} kcal لكل 100 جم • الكمية المقترحة ${formatGrams(food.defaultGrams)} جم",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            Button(onClick = onAdd) {
                Text("إضافة")
            }
        }
    }
}

private fun analyzeImagesOffline(
    context: Context,
    first: Uri,
    second: Uri?,
    onSuccess: (List<ImageLabel>) -> Unit,
    onError: (String) -> Unit
) {
    val options = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.35f)
        .build()
    val labeler = ImageLabeling.getClient(options)

    try {
        val firstInput = InputImage.fromFilePath(context, first)
        labeler.process(firstInput)
            .addOnSuccessListener { firstLabels ->
                if (second == null) {
                    labeler.close()
                    onSuccess(firstLabels)
                } else {
                    try {
                        val secondInput = InputImage.fromFilePath(context, second)
                        labeler.process(secondInput)
                            .addOnSuccessListener { secondLabels ->
                                labeler.close()
                                onSuccess(firstLabels + secondLabels)
                            }
                            .addOnFailureListener { error ->
                                labeler.close()
                                onError(error.message ?: "تعذر تحليل الصورة الثانية")
                            }
                    } catch (e: IOException) {
                        labeler.close()
                        onError(e.message ?: "تعذر فتح الصورة الثانية")
                    }
                }
            }
            .addOnFailureListener { error ->
                labeler.close()
                onError(error.message ?: "تعذر تحليل الصورة")
            }
    } catch (e: IOException) {
        labeler.close()
        onError(e.message ?: "تعذر فتح الصورة")
    }
}

private fun matchFoods(labels: List<ImageLabel>): List<Food> {
    val scores = mutableMapOf<Food, Float>()

    labels.forEach { label ->
        val text = label.text.lowercase(Locale.ROOT).trim()
        if (text.length < 3) return@forEach

        foods.forEach { food ->
            val matched = food.visionKeywords.any { keyword ->
                val k = keyword.lowercase(Locale.ROOT)
                text == k || text.contains(k)
            }
            if (matched) {
                val existing = scores[food] ?: 0f
                if (label.confidence > existing) scores[food] = label.confidence
            }
        }
    }

    return scores.entries
        .sortedByDescending { it.value }
        .map { it.key }
        .distinctBy { it.name }
        .take(8)
}

private fun grams(text: String): Double = text.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

private fun formatGrams(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(Locale.US, value)
