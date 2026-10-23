package com.reportboi.offline

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color as AColor
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Forest = Color(0xFF174A38)
private val Green = Color(0xFF25845B)
private val Mint = Color(0xFFEAF3E8)
private val CanvasBg = Color(0xFFF7F6EF)
private val Ink = Color(0xFF20352B)
private val Muted = Color(0xFF68786D)
private val Border = Color(0xFFE1E7DE)
private val SoftGold = Color(0xFFD8B777)
private const val PREFS = "report_boi_local"
private const val DATA_KEY = "report_data"

private enum class FieldType { NUMBER, TEXT, CHECK }
private data class FieldSpec(val key: String, val label: String, val unit: String = "", val type: FieldType = FieldType.NUMBER)
private data class FieldGroup(val title: String, val fields: List<FieldSpec>)
private data class NavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private fun field(key: String, label: String, unit: String = "", type: FieldType = FieldType.NUMBER) = FieldSpec(key, label, unit, type)

private val planGroups = listOf(
    FieldGroup("কুরআন অধ্যয়ন", listOf(field("quranDays","মোট দিন","দিন"), field("quranAvgAyat","গড় আয়াত","আয়াত/দিন"), field("quranSurahNames","সূরার নাম","",FieldType.TEXT), field("quranDars","দারস প্রস্তুত","টি"), field("quranMemorizedAyat","মুখস্থ আয়াত","টি"), field("quranMeaningSurah","অর্থসহ সূরা","",FieldType.TEXT))),
    FieldGroup("হাদিস অধ্যয়ন", listOf(field("hadithDays","মোট দিন","দিন"), field("hadithAvg","গড় হাদিস","টি/দিন"), field("hadithBooks","গ্রন্থ / বিষয়","",FieldType.TEXT), field("hadithDars","দারস প্রস্তুত","টি"), field("hadithMemorized","মুখস্থ হাদিস","টি"), field("hadithSubject","বিষয়","",FieldType.TEXT))),
    FieldGroup("সাহিত্য অধ্যয়ন", listOf(field("literatureTotalPages","মোট পৃষ্ঠা","পৃষ্ঠা"), field("literatureIslamicPages","ইসলামী সাহিত্য","পৃষ্ঠা"), field("literatureOtherPages","অন্যান্য সাহিত্য","পৃষ্ঠা"), field("literatureBooks","বইয়ের নাম","",FieldType.TEXT), field("bookNotes","বই নোট","টি"), field("discussionNotes","আলোচনা নোট","টি"))),
    FieldGroup("পাঠ্যপুস্তক ও নামাজ", listOf(field("studyDays","পড়াশোনার মোট দিন","দিন"), field("studyAvgHours","গড় পড়াশোনা","ঘণ্টা/দিন"), field("classesTotal","মোট ক্লাস","টি"), field("classesTargetAttendance","লক্ষ্য উপস্থিতি","টি"), field("jamaatTarget","জামাতে নামাজের লক্ষ্য","ওয়াক্ত"), field("qazaTarget","কাজা নামাজের লক্ষ্য","ওয়াক্ত"))),
    FieldGroup("যোগাযোগের লক্ষ্য", listOf(field("contactMember","সদস্য","জন"), field("contactSathi","সাথী","জন"), field("contactWorker","কর্মী","জন"), field("contactSupporter","সমর্থক","জন"), field("contactFriend","বন্ধু","জন"), field("contactWellWisher","শুভাকাঙ্ক্ষী","জন"), field("contactSchoolStudent","স্কুলছাত্র","জন"), field("contactTalentedStudent","মেধাবী ছাত্র","জন"), field("contactTeacher","শিক্ষক","জন"), field("contactMuharrama","মুহাররমা","জন"), field("contactVIP","ভিআইপি","জন"))),
    FieldGroup("সাংগঠনিক দায়িত্ব পালন", listOf(field("orgDays","মোট দিন","দিন"), field("orgAvgHours","গড় সময়","ঘণ্টা/দিন"), field("dawahDays","দাওয়াতি কাজ","দিন"), field("otherOrgDays","অন্যান্য সাংগঠনিক কাজ","দিন"), field("dawahHours","দাওয়াতি কাজের সময়","ঘণ্টা"), field("otherOrgHours","অন্যান্য কাজের সময়","ঘণ্টা"))),
    FieldGroup("বিতরণের লক্ষ্য", listOf(field("distIslamicLiterature","ইসলামী সাহিত্য","টি"), field("distKishoreMagazine","কিশোর পত্রিকা","টি"), field("distYW","Y.W.","টি"), field("distStudentNews","ছাত্রসংবাদ","টি"), field("distEnglishMagazine","ইংরেজি পত্রিকা","টি"), field("distIntro","পরিচিতি","টি"), field("distClassRoutine","ক্লাস রুটিন","টি"), field("distStickerCard","স্টিকার/কার্ড","টি"), field("distGiftMessageEmail","উপহার/মেসেজ/ই-মেইল","টি"))),
    FieldGroup("বৃদ্ধির লক্ষ্য", listOf(field("growthMember","সদস্য","জন"), field("growthCandidate","সদস্যপ্রার্থী","জন"), field("growthSathi","সাথী","জন"), field("growthSathiCandidate","সাথীপ্রার্থী","জন"), field("growthWorker","কর্মী","জন"), field("growthSupporter","সমর্থক","জন"), field("growthFriend","বন্ধু","জন"), field("growthWellWisher","শুভাকাঙ্ক্ষী","জন"))),
    FieldGroup("বায়তুলমাল / আর্থিক হিসাব", listOf(field("financePaymentDate","পরিশোধের তারিখ","",FieldType.TEXT), field("financePersonalGrowth","ব্যক্তিগত বৃদ্ধি","টাকা"), field("financeTotalGrowth","মোট বৃদ্ধি","টাকা"), field("financeStudentWelfare","ছাত্রকল্যাণ","টাকা"), field("financeTableBank","টেবিল ব্যাংক","টাকা"), field("financePotCount","কলসি/হাঁড়ি","টি"))),
    FieldGroup("বিবিধ পরিকল্পনা", listOf(field("selfReviewDays","আত্মসমালোচনা","দিন"), field("exerciseDays","শরীরচর্চা","দিন"), field("magazineReadDays","পত্রিকা পাঠ","দিন"), field("relativesContacts","আত্মীয়স্বজনের সঙ্গে যোগাযোগ","জন"), field("groupDawahCount","গ্রুপ দাওয়াতি কাজ","বার"), field("dawahMessageCount","দাওয়াতি মেসেজ","টি"), field("dawahEmailCount","দাওয়াতি ই-মেইল","টি"), field("nonMuslimContacts","অমুসলিম বন্ধু যোগাযোগ","জন"), field("orgContacts","বন্ধু সংগঠনের সঙ্গে যোগাযোগ","জন/সংগঠন"), field("computerStudyDays","কম্পিউটার শিক্ষা","দিন"), field("languageStudyDays","ভাষা শিক্ষা","দিন"), field("otherActivities","অন্যান্য","",FieldType.TEXT), field("reportCount","দায়িত্বশীলকে রিপোর্ট","বার"), field("reportDate","রিপোর্টের তারিখ","",FieldType.TEXT)))
)

private val dailyGroups = listOf(
    FieldGroup("কুরআন অধ্যয়ন", listOf(field("quranAyat","পড়া আয়াত","আয়াত"), field("quranSurah","সূরার নাম","",FieldType.TEXT), field("quranMemorized","মুখস্থ আয়াত","আয়াত"), field("quranDarsDone","দারস প্রস্তুত","টি"))),
    FieldGroup("হাদিস অধ্যয়ন", listOf(field("hadithCount","পড়া হাদিস","টি"), field("hadithMemorized","মুখস্থ হাদিস","টি"), field("hadithTopic","গ্রন্থ / বিষয়","",FieldType.TEXT), field("hadithDarsDone","দারস প্রস্তুত","টি"))),
    FieldGroup("সাহিত্য ও পাঠ্যপুস্তক", listOf(field("literatureIslamic","ইসলামী সাহিত্য","পৃষ্ঠা"), field("literatureOther","অন্যান্য সাহিত্য","পৃষ্ঠা"), field("textbookHours","পড়াশোনার সময়","ঘণ্টা"), field("classesTotal","মোট ক্লাস","টি"), field("classesAttended","উপস্থিত ক্লাস","টি"))),
    FieldGroup("নামাজ", listOf(field("jamaat","জামাতে নামাজ","ওয়াক্ত"), field("qaza","কাজা নামাজ","ওয়াক্ত"))),
    FieldGroup("যোগাযোগ", listOf(field("contactMember","সদস্য","জন"), field("contactSathi","সাথী","জন"), field("contactWorker","কর্মী","জন"), field("contactSupporter","সমর্থক","জন"), field("contactFriend","বন্ধু","জন"), field("contactWellWisher","শুভাকাঙ্ক্ষী","জন"), field("contactSchoolStudent","স্কুলছাত্র","জন"), field("contactTalentedStudent","মেধাবী ছাত্র","জন"), field("contactTeacher","শিক্ষক","জন"), field("contactMuharrama","মুহাররমা","জন"), field("contactVIP","ভিআইপি","জন"))),
    FieldGroup("বিতরণ", listOf(field("distIslamicLiterature","ইসলামী সাহিত্য","টি"), field("distKishoreMagazine","কিশোর পত্রিকা","টি"), field("distYW","Y.W.","টি"), field("distStudentNews","ছাত্রসংবাদ","টি"), field("distEnglishMagazine","ইংরেজি পত্রিকা","টি"), field("distIntro","পরিচিতি","টি"), field("distClassRoutine","ক্লাস রুটিন","টি"), field("distStickerCard","স্টিকার/কার্ড","টি"), field("distGiftMessageEmail","উপহার/মেসেজ/ই-মেইল","টি"))),
    FieldGroup("সাংগঠনিক দায়িত্ব ও দাওয়াত", listOf(field("dawahHours","দাওয়াতি কাজ","ঘণ্টা"), field("otherOrgHours","অন্যান্য সাংগঠনিক কাজ","ঘণ্টা"), field("groupDawahCount","গ্রুপ দাওয়াতি কাজ","বার"), field("dawahMessageCount","দাওয়াতি মেসেজ","টি"), field("dawahEmailCount","দাওয়াতি ই-মেইল","টি"))),
    FieldGroup("বিবিধ ও অভ্যাস", listOf(field("relativesContacts","আত্মীয়স্বজনের সঙ্গে যোগাযোগ","জন"), field("nonMuslimContacts","অমুসলিম বন্ধু যোগাযোগ","জন"), field("orgContacts","বন্ধু সংগঠনের সঙ্গে যোগাযোগ","জন/সংগঠন"), field("computerStudyHours","কম্পিউটার শিক্ষা","ঘণ্টা"), field("languageStudyHours","ভাষা শিক্ষা","ঘণ্টা"), field("otherActivities","অন্যান্য কাজ","",FieldType.TEXT), field("magazineRead","পত্রিকা পাঠ করেছি","",FieldType.CHECK), field("exerciseDone","শরীরচর্চা করেছি","",FieldType.CHECK), field("selfReviewDone","আত্মসমালোচনা করেছি","",FieldType.CHECK), field("reportGiven","দায়িত্বশীলকে রিপোর্ট দিয়েছি","",FieldType.CHECK)))
)

private val totalFields = listOf(
    field("quranAyat","কুরআনের আয়াত","আয়াত"), field("quranMemorized","মুখস্থ আয়াত","আয়াত"), field("quranDarsDone","কুরআন দারস প্রস্তুত","টি"),
    field("hadithCount","পড়া হাদিস","টি"), field("hadithMemorized","মুখস্থ হাদিস","টি"), field("hadithDarsDone","হাদিস দারস প্রস্তুত","টি"),
    field("literatureIslamic","ইসলামী সাহিত্য","পৃষ্ঠা"), field("literatureOther","অন্যান্য সাহিত্য","পৃষ্ঠা"), field("textbookHours","পড়াশোনা","ঘণ্টা"),
    field("classesTotal","মোট ক্লাস","টি"), field("classesAttended","উপস্থিত ক্লাস","টি"), field("jamaat","জামাতে নামাজ","ওয়াক্ত"), field("qaza","কাজা নামাজ","ওয়াক্ত"),
    field("contactMember","সদস্য যোগাযোগ","জন"), field("contactSathi","সাথী যোগাযোগ","জন"), field("contactWorker","কর্মী যোগাযোগ","জন"),
    field("contactSupporter","সমর্থক যোগাযোগ","জন"), field("contactFriend","বন্ধু যোগাযোগ","জন"), field("contactWellWisher","শুভাকাঙ্ক্ষী যোগাযোগ","জন"),
    field("contactSchoolStudent","স্কুলছাত্র যোগাযোগ","জন"), field("contactTalentedStudent","মেধাবী ছাত্র যোগাযোগ","জন"), field("contactTeacher","শিক্ষক যোগাযোগ","জন"),
    field("contactMuharrama","মুহাররমা যোগাযোগ","জন"), field("contactVIP","ভিআইপি যোগাযোগ","জন"),
    field("distIslamicLiterature","ইসলামী সাহিত্য বিতরণ","টি"), field("distKishoreMagazine","কিশোর পত্রিকা বিতরণ","টি"), field("distYW","Y.W. বিতরণ","টি"),
    field("distStudentNews","ছাত্রসংবাদ বিতরণ","টি"), field("distEnglishMagazine","ইংরেজি পত্রিকা বিতরণ","টি"), field("distIntro","পরিচিতি বিতরণ","টি"),
    field("distClassRoutine","ক্লাস রুটিন বিতরণ","টি"), field("distStickerCard","স্টিকার/কার্ড বিতরণ","টি"), field("distGiftMessageEmail","উপহার/মেসেজ/ই-মেইল","টি"),
    field("dawahHours","দাওয়াতি কাজ","ঘণ্টা"), field("otherOrgHours","অন্যান্য সাংগঠনিক কাজ","ঘণ্টা"), field("groupDawahCount","গ্রুপ দাওয়াতি কাজ","বার"),
    field("dawahMessageCount","দাওয়াতি মেসেজ","টি"), field("dawahEmailCount","দাওয়াতি ই-মেইল","টি"), field("relativesContacts","আত্মীয়স্বজন যোগাযোগ","জন"),
    field("nonMuslimContacts","অমুসলিম বন্ধু যোগাযোগ","জন"), field("orgContacts","বন্ধু সংগঠনের সঙ্গে যোগাযোগ","জন/সংগঠন"),
    field("computerStudyHours","কম্পিউটার শিক্ষা","ঘণ্টা"), field("languageStudyHours","ভাষা শিক্ষা","ঘণ্টা")
)

private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun shiftIsoDate(date: String, offsetDays: Int): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val calendar = Calendar.getInstance()
    calendar.time = try { format.parse(date) ?: Date() } catch (_: Exception) { Date() }
    calendar.add(Calendar.DAY_OF_MONTH, offsetDays)
    val result = format.format(calendar.time)
    return if (result > todayIso()) todayIso() else result
}
private fun currentMonth(): String = todayIso().take(7)
private fun daysInMonth(month: String): Int {
    val parts = month.split("-")
    if (parts.size != 2) return 30
    val year = parts[0].toIntOrNull() ?: return 30
    val monthNumber = parts[1].toIntOrNull() ?: return 30
    if (monthNumber !in 1..12) return 30
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthNumber - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
}
private fun monthLabel(month: String): String = try {
    val date = SimpleDateFormat("yyyy-MM", Locale.US).parse(month) ?: Date()
    SimpleDateFormat("MMMM yyyy", Locale("bn", "BD")).format(date)
} catch (_: Exception) { month }
private fun numberBn(value: Number): String = NumberFormat.getInstance(Locale("bn", "BD")).format(value)
private fun newRoot() = JSONObject().put("plans", JSONObject()).put("days", JSONObject())
private fun loadRoot(context: Context): JSONObject = try {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(DATA_KEY, null)
    if (raw.isNullOrBlank()) newRoot() else JSONObject(raw).also {
        if (!it.has("plans")) it.put("plans", JSONObject())
        if (!it.has("days")) it.put("days", JSONObject())
    }
} catch (_: Exception) { newRoot() }
private fun cloneJson(value: JSONObject) = JSONObject(value.toString())
private fun jsonToMap(json: JSONObject?): Map<String, String> {
    if (json == null) return emptyMap()
    val out = linkedMapOf<String, String>()
    val keys = json.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = json.opt(key)
        out[key] = if (value == null || value == JSONObject.NULL) "" else value.toString()
    }
    return out
}
private fun mapToJson(values: Map<String, String>) = JSONObject().apply { values.forEach { (key, value) -> put(key, value) } }
private fun saveRoot(context: Context, value: JSONObject) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(DATA_KEY, value.toString()).apply()
}
private fun monthDays(root: JSONObject, month: String): List<Pair<String, Map<String, String>>> {
    val days = root.optJSONObject("days") ?: JSONObject()
    val keys = days.keys().asSequence().filter { it.startsWith(month) }.sorted().toList()
    return keys.map { it to jsonToMap(days.optJSONObject(it)) }
}
private fun totalFor(root: JSONObject, month: String, key: String): Double =
    monthDays(root, month).sumOf { it.second[key]?.toDoubleOrNull() ?: 0.0 }

private data class SummarySection(val title: String, val rows: List<Pair<String, String>>)

private fun dailyTotal(days: List<Pair<String, Map<String, String>>>, key: String): Double =
    days.sumOf { it.second[key]?.toDoubleOrNull() ?: 0.0 }

private fun dailyTotal(days: List<Pair<String, Map<String, String>>>, keys: List<String>): Double =
    days.sumOf { item -> keys.sumOf { item.second[it]?.toDoubleOrNull() ?: 0.0 } }

private fun activeDays(days: List<Pair<String, Map<String, String>>>, key: String): Int =
    days.count { (it.second[key]?.toDoubleOrNull() ?: 0.0) > 0.0 }

private fun checkedDays(days: List<Pair<String, Map<String, String>>>, key: String): Int =
    days.count { it.second[key] == "true" }

private fun averageText(days: List<Pair<String, Map<String, String>>>, key: String, unit: String): String {
    val count = activeDays(days, key)
    return if (count == 0) "—" else numberBn(dailyTotal(days, key) / count) + " " + unit
}

private fun distinctDailyText(days: List<Pair<String, Map<String, String>>>, key: String): String =
    days.mapNotNull { it.second[key]?.trim()?.takeIf { value -> value.isNotEmpty() } }
        .distinct().joinToString("، ")

private fun monthlySnapshotSections(
    days: List<Pair<String, Map<String, String>>>,
    plan: Map<String, String>
): List<SummarySection> {
    val result = mutableListOf<SummarySection>()
    fun add(title: String, rows: List<Pair<String, String>>) {
        result += SummarySection(title, rows)
    }
    fun value(key: String): String = plan[key]?.trim().orEmpty()
    fun addPlanRow(rows: MutableList<Pair<String, String>>, label: String, key: String, unit: String = "") {
        val v = value(key)
        if (v.isNotBlank()) rows += label to (v + if (unit.isNotBlank()) " " + unit else "")
    }
    fun textOrDaily(planKey: String, dailyKey: String): String =
        value(planKey).ifBlank { distinctDailyText(days, dailyKey) }.ifBlank { "—" }

    val quran = mutableListOf<Pair<String, String>>()
    quran += "মোট দিন" to (numberBn(activeDays(days, "quranAyat")) + " দিন")
    quran += "গড় আয়াত" to averageText(days, "quranAyat", "আয়াত/দিন")
    quran += "সূরার নাম" to textOrDaily("quranSurahNames", "quranSurah")
    quran += "দারস প্রস্তুত" to (numberBn(dailyTotal(days, "quranDarsDone")) + " টি")
    quran += "মুখস্থ আয়াত" to (numberBn(dailyTotal(days, "quranMemorized")) + " আয়াত")
    addPlanRow(quran, "অর্থসহ সূরা", "quranMeaningSurah")
    add("কুরআন অধ্যয়ন", quran)

    val hadith = mutableListOf<Pair<String, String>>()
    hadith += "মোট দিন" to (numberBn(activeDays(days, "hadithCount")) + " দিন")
    hadith += "গড় হাদিস" to averageText(days, "hadithCount", "টি/দিন")
    hadith += "গ্রন্থ / বিষয়" to textOrDaily("hadithBooks", "hadithTopic")
    hadith += "দারস প্রস্তুত" to (numberBn(dailyTotal(days, "hadithDarsDone")) + " টি")
    hadith += "মুখস্থ হাদিস" to (numberBn(dailyTotal(days, "hadithMemorized")) + " টি")
    addPlanRow(hadith, "বিষয়", "hadithSubject")
    add("হাদিস অধ্যয়ন", hadith)

    val literature = mutableListOf<Pair<String, String>>()
    val islamicPages = dailyTotal(days, "literatureIslamic")
    val otherPages = dailyTotal(days, "literatureOther")
    literature += "মোট পৃষ্ঠা" to (numberBn(islamicPages + otherPages) + " পৃষ্ঠা")
    literature += "ইসলামী সাহিত্য" to (numberBn(islamicPages) + " পৃষ্ঠা")
    literature += "অন্যান্য সাহিত্য" to (numberBn(otherPages) + " পৃষ্ঠা")
    addPlanRow(literature, "বইয়ের নাম", "literatureBooks")
    addPlanRow(literature, "বই নোট", "bookNotes", "টি")
    addPlanRow(literature, "আলোচনা নোট", "discussionNotes", "টি")
    add("সাহিত্য অধ্যয়ন", literature)

    val studyPrayer = mutableListOf<Pair<String, String>>()
    studyPrayer += "পড়াশোনার মোট দিন" to (numberBn(activeDays(days, "textbookHours")) + " দিন")
    studyPrayer += "গড় পড়াশোনা" to averageText(days, "textbookHours", "ঘণ্টা/দিন")
    studyPrayer += "মোট ক্লাস" to (numberBn(dailyTotal(days, "classesTotal")) + " টি")
    studyPrayer += "উপস্থিত ক্লাস" to (numberBn(dailyTotal(days, "classesAttended")) + " টি")
    studyPrayer += "জামাতে নামাজ" to (numberBn(dailyTotal(days, "jamaat")) + " ওয়াক্ত")
    studyPrayer += "কাজা নামাজ" to (numberBn(dailyTotal(days, "qaza")) + " ওয়াক্ত")
    add("পাঠ্যপুস্তক অধ্যয়ন ও নামাজ", studyPrayer)

    val contactRows = mutableListOf<Pair<String, String>>()
    listOf(
        "contactMember" to "সদস্য", "contactSathi" to "সাথী", "contactWorker" to "কর্মী",
        "contactSupporter" to "সমর্থক", "contactFriend" to "বন্ধু", "contactWellWisher" to "শুভাকাঙ্ক্ষী",
        "contactSchoolStudent" to "স্কুলছাত্র", "contactTalentedStudent" to "মেধাবী ছাত্র",
        "contactTeacher" to "শিক্ষক", "contactMuharrama" to "মুহাররমা", "contactVIP" to "ভিআইপি"
    ).forEach { (key, label) -> contactRows += label to (numberBn(dailyTotal(days, key)) + " জন") }
    add("যোগাযোগ", contactRows)

    val dawahDays = activeDays(days, "dawahHours")
    val otherOrgDays = activeDays(days, "otherOrgHours")
    val orgDays = days.count {
        (it.second["dawahHours"]?.toDoubleOrNull() ?: 0.0) > 0.0 ||
            (it.second["otherOrgHours"]?.toDoubleOrNull() ?: 0.0) > 0.0
    }
    val orgHours = dailyTotal(days, listOf("dawahHours", "otherOrgHours"))
    val orgRows = mutableListOf<Pair<String, String>>()
    orgRows += "মোট দিন" to (numberBn(orgDays) + " দিন")
    orgRows += "গড় সময়" to (if (orgDays == 0) "—" else numberBn(orgHours / orgDays) + " ঘণ্টা/দিন")
    orgRows += "দাওয়াতি কাজ" to (numberBn(dawahDays) + " দিন")
    orgRows += "দাওয়াতি কাজের গড় সময়" to averageText(days, "dawahHours", "ঘণ্টা/দিন")
    orgRows += "অন্যান্য সাংগঠনিক কাজ" to (numberBn(otherOrgDays) + " দিন")
    orgRows += "অন্যান্য কাজের গড় সময়" to averageText(days, "otherOrgHours", "ঘণ্টা/দিন")
    orgRows += "গ্রুপ দাওয়াতি কাজ" to (numberBn(dailyTotal(days, "groupDawahCount")) + " বার")
    orgRows += "দাওয়াতি মেসেজ" to (numberBn(dailyTotal(days, "dawahMessageCount")) + " টি")
    orgRows += "দাওয়াতি ই-মেইল" to (numberBn(dailyTotal(days, "dawahEmailCount")) + " টি")
    add("সাংগঠনিক দায়িত্ব পালন", orgRows)

    val distributionRows = mutableListOf<Pair<String, String>>()
    listOf(
        "distIslamicLiterature" to "ইসলামী সাহিত্য", "distKishoreMagazine" to "কিশোর পত্রিকা",
        "distYW" to "Y.W.", "distStudentNews" to "ছাত্রসংবাদ", "distEnglishMagazine" to "ইংরেজি পত্রিকা",
        "distIntro" to "পরিচিতি", "distClassRoutine" to "ক্লাস রুটিন", "distStickerCard" to "স্টিকার/কার্ড",
        "distGiftMessageEmail" to "উপহার/মেসেজ/ই-মেইল"
    ).forEach { (key, label) -> distributionRows += label to (numberBn(dailyTotal(days, key)) + " টি") }
    add("বিতরণ", distributionRows)

    val growthRows = mutableListOf<Pair<String, String>>()
    listOf(
        "growthMember" to "সদস্য", "growthCandidate" to "সদস্যপ্রার্থী", "growthSathi" to "সাথী",
        "growthSathiCandidate" to "সাথীপ্রার্থী", "growthWorker" to "কর্মী", "growthSupporter" to "সমর্থক",
        "growthFriend" to "বন্ধু", "growthWellWisher" to "শুভাকাঙ্ক্ষী"
    ).forEach { (key, label) -> addPlanRow(growthRows, label, key, "জন") }
    if (growthRows.isEmpty()) growthRows += "তথ্য" to "মাসিক পরিকল্পনায় তথ্য দেওয়া হয়নি"
    add("বৃদ্ধি", growthRows)

    val financeRows = mutableListOf<Pair<String, String>>()
    addPlanRow(financeRows, "পরিশোধের তারিখ", "financePaymentDate")
    addPlanRow(financeRows, "ব্যক্তিগত বৃদ্ধি", "financePersonalGrowth", "টাকা")
    addPlanRow(financeRows, "মোট বৃদ্ধি", "financeTotalGrowth", "টাকা")
    addPlanRow(financeRows, "ছাত্রকল্যাণ", "financeStudentWelfare", "টাকা")
    addPlanRow(financeRows, "টেবিল ব্যাংক", "financeTableBank", "টাকা")
    addPlanRow(financeRows, "কলসি/হাঁড়ি", "financePotCount", "টি")
    if (financeRows.isEmpty()) financeRows += "তথ্য" to "মাসিক পরিকল্পনায় তথ্য দেওয়া হয়নি"
    add("বায়তুলমাল / আর্থিক হিসাব", financeRows)

    val miscRows = mutableListOf<Pair<String, String>>()
    miscRows += "আত্মসমালোচনা" to (numberBn(checkedDays(days, "selfReviewDone")) + " দিন")
    miscRows += "শরীরচর্চা" to (numberBn(checkedDays(days, "exerciseDone")) + " দিন")
    miscRows += "পত্রিকা পাঠ" to (numberBn(checkedDays(days, "magazineRead")) + " দিন")
    miscRows += "আত্মীয়স্বজনের সঙ্গে যোগাযোগ" to (numberBn(dailyTotal(days, "relativesContacts")) + " জন")
    miscRows += "অমুসলিম বন্ধু যোগাযোগ" to (numberBn(dailyTotal(days, "nonMuslimContacts")) + " জন")
    miscRows += "বন্ধু সংগঠনের সঙ্গে যোগাযোগ" to (numberBn(dailyTotal(days, "orgContacts")) + " জন/সংগঠন")
    miscRows += "কম্পিউটার শিক্ষা" to (numberBn(activeDays(days, "computerStudyHours")) + " দিন")
    miscRows += "ভাষা শিক্ষা" to (numberBn(activeDays(days, "languageStudyHours")) + " দিন")
    miscRows += "দায়িত্বশীলকে রিপোর্ট" to (numberBn(checkedDays(days, "reportGiven")) + " বার")
    val reportDate = days.lastOrNull { it.second["reportGiven"] == "true" }?.first
    if (reportDate != null) miscRows += "সর্বশেষ রিপোর্টের তারিখ" to reportDate
    val otherActivities = distinctDailyText(days, "otherActivities").ifBlank { value("otherActivities") }
    if (otherActivities.isNotBlank()) miscRows += "অন্যান্য" to otherActivities
    add("বিবিধ", miscRows)

    return result
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReportBoiApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportBoiApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var root by remember { mutableStateOf(loadRoot(context)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var monthInput by remember { mutableStateOf(currentMonth()) }
    var selectedMonth by remember { mutableStateOf(currentMonth()) }
    var selectedDay by remember { mutableStateOf(todayIso()) }
    var selectedReportDate by remember { mutableStateOf(todayIso()) }
    var planValues by remember { mutableStateOf(jsonToMap(root.optJSONObject("plans")?.optJSONObject(selectedMonth))) }
    var dayValues by remember { mutableStateOf(jsonToMap(root.optJSONObject("days")?.optJSONObject(selectedDay))) }
    var dayDrafts by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }
    var pendingPdf by remember { mutableStateOf<File?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        val file = pendingPdf
        if (uri != null && file != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(file).use { input -> input.copyTo(output) }
                }
                Toast.makeText(context, "PDF সংরক্ষণ হয়েছে", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "PDF সংরক্ষণ করা যায়নি", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(selectedMonth) {
        planValues = jsonToMap(root.optJSONObject("plans")?.optJSONObject(selectedMonth))
        monthInput = selectedMonth
        if (!selectedReportDate.startsWith(selectedMonth)) {
            selectedReportDate = monthDays(root, selectedMonth).lastOrNull()?.first ?: "$selectedMonth-01"
        }
    }
    LaunchedEffect(selectedDay, root) {
        dayValues = dayDrafts[selectedDay] ?: jsonToMap(root.optJSONObject("days")?.optJSONObject(selectedDay))
    }

    val navItems = listOf(
        NavItem("হোম", Icons.Default.Home),
        NavItem("পরিকল্পনা", Icons.Default.CalendarMonth),
        NavItem("দৈনিক", Icons.Default.EditNote),
        NavItem("রিপোর্ট", Icons.Default.Assessment)
    )

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Forest, onPrimary = Color.White, secondary = Green, onSecondary = Color.White,
        background = CanvasBg, surface = Color.White, onSurface = Ink, onBackground = Ink,
        primaryContainer = Mint, onPrimaryContainer = Forest, outline = Border
    ), shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )) {
        Scaffold(
            containerColor = CanvasBg,
            topBar = {
                Column(Modifier.fillMaxWidth().background(Forest).statusBarsPadding().padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("রিপোর্ট বই", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                            Text("ব্যক্তিগত পরিকল্পনা ও হিসাব", color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
                        }
                        Column(Modifier.clip(RoundedCornerShape(50.dp)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFBFE9D0), modifier = Modifier.size(17.dp))
                            Text("অফলাইন", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White.copy(alpha = .92f), modifier = Modifier.size(20.dp))
                        OutlinedTextField(
                            value = monthInput,
                            onValueChange = { monthInput = it },
                            modifier = Modifier.weight(1f).height(52.dp),
                            singleLine = true,
                            placeholder = { Text("YYYY-MM", color = Color.White.copy(alpha = .62f), fontSize = 13.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = .36f),
                                cursorColor = Color.White, focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = .05f), unfocusedContainerColor = Color.White.copy(alpha = .03f)
                            )
                        )
                        Button(
                            onClick = {
                                if (Regex("\\d{4}-\\d{2}").matches(monthInput) && monthInput.substring(5, 7).toIntOrNull() in 1..12) selectedMonth = monthInput
                                else Toast.makeText(context, "মাস YYYY-MM ফরম্যাটে লিখুন", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Forest),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 17.dp, vertical = 10.dp)
                        ) { Text("সেট", fontWeight = FontWeight.Bold) }
                    }
                }
            },

            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(22.dp)) },
                            label = { Text(item.title, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = Mint,
                                unselectedIconColor = Muted, unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen(selectedMonth, root,
                        onDaily = { selectedDay = todayIso(); selectedMonth = todayIso().take(7); monthInput = todayIso().take(7); selectedTab = 2 },
                        onPlan = { selectedTab = 1 }, onReport = { selectedTab = 3 },
                        onOpenDay = { date -> selectedDay = date; selectedMonth = date.take(7); monthInput = date.take(7); selectedTab = 2 })
                    1 -> PlanScreen(selectedMonth, planValues, { planValues = it }) {
                        val next = cloneJson(root)
                        val plans = next.optJSONObject("plans") ?: JSONObject()
                        plans.put(selectedMonth, mapToJson(planValues)); next.put("plans", plans)
                        saveRoot(context, next); root = next
                        Toast.makeText(context, "মাসিক পরিকল্পনা সংরক্ষণ হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                    2 -> DailyScreen(
                        date = selectedDay,
                        values = dayValues,
                        savedRecordExists = root.optJSONObject("days")?.has(selectedDay) == true,
                        onPickDate = {
                            val calendar = Calendar.getInstance()
                            try { calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDay) ?: Date() } catch (_: Exception) {}
                            val dialog = DatePickerDialog(context, { _, year, month, day ->
                                val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                                selectedDay = date
                                selectedMonth = date.take(7)
                                monthInput = date.take(7)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                            dialog.datePicker.maxDate = System.currentTimeMillis()
                            dialog.show()
                        },
                        onShiftDate = { delta ->
                            val date = shiftIsoDate(selectedDay, delta)
                            selectedDay = date
                            selectedMonth = date.take(7)
                            monthInput = date.take(7)
                        },
                        onToday = {
                            val date = todayIso()
                            selectedDay = date
                            selectedMonth = date.take(7)
                            monthInput = date.take(7)
                        },
                        valuesChange = { updated -> dayValues = updated; dayDrafts = dayDrafts + (selectedDay to updated) },
                        onSave = {
                            val next = cloneJson(root)
                            val days = next.optJSONObject("days") ?: JSONObject()
                            val existed = days.has(selectedDay)
                            days.put(selectedDay, mapToJson(dayValues))
                            next.put("days", days)
                            dayDrafts = dayDrafts - selectedDay
                            saveRoot(context, next)
                            root = next
                            val message = when {
                                existed && selectedDay < todayIso() -> "আগের দিনের হিসাব আপডেট হয়েছে"
                                existed -> "দৈনিক হিসাব আপডেট হয়েছে"
                                selectedDay < todayIso() -> "আগের দিনের হিসাব সংরক্ষণ হয়েছে"
                                else -> "দৈনিক হিসাব সংরক্ষণ হয়েছে"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                        onClear = {
                            val next = cloneJson(root)
                            val days = next.optJSONObject("days") ?: JSONObject()
                            days.remove(selectedDay)
                            next.put("days", days)
                            dayDrafts = dayDrafts - selectedDay
                            saveRoot(context, next)
                            root = next
                            dayValues = emptyMap()
                            Toast.makeText(context, "এই দিনের হিসাব মুছে দেওয়া হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    )
                    3 -> ReportScreen(
                        month = selectedMonth,
                        root = root,
                        selectedDate = selectedReportDate,
                        onSelectDate = { selectedReportDate = it },
                        onSaveSnapshotPdf = {
                            try {
                                val file = PdfReport.createMonthlySnapshot(context, selectedMonth, jsonToMap(root.optJSONObject("plans")?.optJSONObject(selectedMonth)), monthDays(root, selectedMonth))
                                pendingPdf = file
                                savePdfLauncher.launch("Report-Boi-Summary-" + selectedMonth + ".pdf")
                            } catch (_: Exception) { Toast.makeText(context, "একনজরে রিপোর্ট PDF তৈরি করা যায়নি", Toast.LENGTH_LONG).show() }
                        },
                        onShareSnapshotPdf = {
                            try {
                                val file = PdfReport.createMonthlySnapshot(context, selectedMonth, jsonToMap(root.optJSONObject("plans")?.optJSONObject(selectedMonth)), monthDays(root, selectedMonth))
                                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "একনজরে মাসিক রিপোর্ট শেয়ার করুন"))
                            } catch (_: Exception) { Toast.makeText(context, "একনজরে রিপোর্ট PDF শেয়ার করা যায়নি", Toast.LENGTH_LONG).show() }
                        },
                        onPrintSnapshot = {
                            try {
                                val file = PdfReport.createMonthlySnapshot(context, selectedMonth, jsonToMap(root.optJSONObject("plans")?.optJSONObject(selectedMonth)), monthDays(root, selectedMonth))
                                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                manager.print("Report-Boi-Summary-" + selectedMonth, LocalPdfPrintAdapter(file),
                                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                        .setResolution(PrintAttributes.Resolution("report_boi", "Report Boi", 300, 300))
                                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build())
                            } catch (_: Exception) { Toast.makeText(context, "একনজরে রিপোর্ট প্রিন্ট শুরু করা যায়নি", Toast.LENGTH_LONG).show() }
                        },
                        onPickDate = {
                            val calendar = Calendar.getInstance()
                            try { calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedReportDate) ?: Date() } catch (_: Exception) {}
                            DatePickerDialog(context, { _, year, month, day ->
                                val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                                selectedReportDate = date
                                selectedMonth = date.take(7)
                                monthInput = date.take(7)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        onSaveDailyPdf = {
                            try {
                                val daily = root.optJSONObject("days")?.optJSONObject(selectedReportDate)
                                if (daily == null) Toast.makeText(context, "এই তারিখের রিপোর্ট নেই। আগে দৈনিক হিসাব সংরক্ষণ করুন।", Toast.LENGTH_LONG).show()
                                else {
                                    pendingPdf = PdfReport.createDaily(context, selectedReportDate, jsonToMap(daily))
                                    savePdfLauncher.launch("Report-Boi-" + selectedReportDate + ".pdf")
                                }
                            } catch (_: Exception) { Toast.makeText(context, "দৈনিক PDF তৈরি করা যায়নি", Toast.LENGTH_LONG).show() }
                        },
                        onShareDailyPdf = {
                            try {
                                val daily = root.optJSONObject("days")?.optJSONObject(selectedReportDate)
                                if (daily == null) Toast.makeText(context, "এই তারিখের রিপোর্ট নেই। আগে দৈনিক হিসাব সংরক্ষণ করুন।", Toast.LENGTH_LONG).show()
                                else {
                                    val file = PdfReport.createDaily(context, selectedReportDate, jsonToMap(daily))
                                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "দৈনিক PDF শেয়ার করুন"))
                                }
                            } catch (_: Exception) { Toast.makeText(context, "দৈনিক PDF শেয়ার করা যায়নি", Toast.LENGTH_LONG).show() }
                        },
                        onPrintDaily = {
                            try {
                                val daily = root.optJSONObject("days")?.optJSONObject(selectedReportDate)
                                if (daily == null) Toast.makeText(context, "এই তারিখের রিপোর্ট নেই। আগে দৈনিক হিসাব সংরক্ষণ করুন।", Toast.LENGTH_LONG).show()
                                else {
                                    val file = PdfReport.createDaily(context, selectedReportDate, jsonToMap(daily))
                                    val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                    manager.print("Report-Boi-" + selectedReportDate, LocalPdfPrintAdapter(file),
                                        PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                            .setResolution(PrintAttributes.Resolution("report_boi", "Report Boi", 300, 300))
                                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build())
                                }
                            } catch (_: Exception) { Toast.makeText(context, "দৈনিক প্রিন্ট শুরু করা যায়নি", Toast.LENGTH_LONG).show() }
                        },
                        onSaveMonthlyPdf = {
                            try {
                                val file = PdfReport.create(context, selectedMonth, jsonToMap(root.optJSONObject("plans")?.optJSONObject(selectedMonth)), monthDays(root, selectedMonth))
                                pendingPdf = file
                                savePdfLauncher.launch("Report-Boi-" + selectedMonth + ".pdf")
                            } catch (_: Exception) { Toast.makeText(context, "মাসিক PDF তৈরি করা যায়নি", Toast.LENGTH_LONG).show() }
                        },
                        onShareMonthlyPdf = {
                            try {
                                val file = PdfReport.create(context, selectedMonth, jsonToMap(root.optJSONObject("plans")?.optJSONObject(selectedMonth)), monthDays(root, selectedMonth))
                                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "মাসিক PDF শেয়ার করুন"))
                            } catch (_: Exception) { Toast.makeText(context, "মাসিক PDF শেয়ার করা যায়নি", Toast.LENGTH_LONG).show() }
                        },
                        onPrintMonthly = {
                            try {
                                val file = PdfReport.create(context, selectedMonth, jsonToMap(root.optJSONObject("plans")?.optJSONObject(selectedMonth)), monthDays(root, selectedMonth))
                                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                manager.print("Report-Boi-" + selectedMonth, LocalPdfPrintAdapter(file),
                                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                        .setResolution(PrintAttributes.Resolution("report_boi", "Report Boi", 300, 300))
                                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build())
                            } catch (_: Exception) { Toast.makeText(context, "মাসিক প্রিন্ট শুরু করা যায়নি", Toast.LENGTH_LONG).show() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(month: String, root: JSONObject, onDaily: () -> Unit, onPlan: () -> Unit, onReport: () -> Unit, onOpenDay: (String) -> Unit) {
    val days = remember(root, month) { monthDays(root, month) }
    val monthLength = daysInMonth(month).coerceAtLeast(1)
    val coverage = (days.size.toFloat() / monthLength.toFloat()).coerceIn(0f, 1f)
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "home_hero") {
            Card(colors = CardDefaults.cardColors(containerColor = Forest), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("আপনার ব্যক্তিগত ড্যাশবোর্ড", color = Color.White.copy(alpha = .76f), fontSize = 11.sp)
                            Text("আপনার মাস, আপনার অগ্রগতি", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                            Text(monthLabel(month), color = Color.White.copy(alpha = .84f), fontSize = 12.sp)
                        }
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(25.dp))
                        }
                    }
                    Divider(color = Color.White.copy(alpha = .17f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("মাসিক রিপোর্ট কভারেজ", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                            Text("${numberBn(days.size)} / ${numberBn(monthLength)} দিন", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${(coverage * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    LinearProgressIndicator(progress = { coverage }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50.dp)), color = Color(0xFFA7D9B8), trackColor = Color.White.copy(alpha = .16f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryMini("কুরআন", numberBn(totalFor(root, month, "quranAyat")), Modifier.weight(1f))
                        SummaryMini("হাদিস", numberBn(totalFor(root, month, "hadithCount")), Modifier.weight(1f))
                        SummaryMini("পড়াশোনা", numberBn(totalFor(root, month, "textbookHours")) + " ঘ.", Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "home_quick_title") {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("দ্রুত কাজ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("যেখানে কাজ থেমেছিল, সেখান থেকেই শুরু করুন", fontSize = 12.sp, color = Muted)
            }
        }
        item(key = "home_quick_row") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                QuickCard("দৈনিক হিসাব", "আজ বা আগের দিনের কাজ", Icons.Default.EditNote, onDaily, Modifier.weight(1f))
                QuickCard("মাসিক পরিকল্পনা", "লক্ষ্য নির্ধারণ", Icons.Default.CalendarMonth, onPlan, Modifier.weight(1f))
            }
        }
        item(key = "home_report_quick") { QuickCard("মাসিক রিপোর্ট", "একনজরে হিসাব, PDF ও প্রিন্ট", Icons.Default.Assessment, onReport, Modifier.fillMaxWidth()) }
        item(key = "home_recent") {
            SectionCard("সাম্প্রতিক হিসাব", "এই মাসের সর্বশেষ ৫টি রেকর্ড") {
                if (days.isEmpty()) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CanvasBg).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = Green, modifier = Modifier.size(30.dp))
                        Text("এখনো কোনো দৈনিক হিসাব নেই", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("দৈনিক ট্যাব থেকে প্রথম রিপোর্টটি তৈরি করুন।", color = Muted, fontSize = 12.sp)
                    }
                } else {
                    days.takeLast(5).asReversed().forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpenDay(item.first) }.padding(horizontal = 8.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Mint), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Forest, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.first, color = Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("কুরআন ${numberBn(item.second["quranAyat"]?.toDoubleOrNull() ?: 0.0)} আয়াত · হাদিস ${numberBn(item.second["hadithCount"]?.toDoubleOrNull() ?: 0.0)} · পড়াশোনা ${numberBn(item.second["textbookHours"]?.toDoubleOrNull() ?: 0.0)} ঘণ্টা", color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.EditNote, contentDescription = "রিপোর্ট খুলুন", tint = Green, modifier = Modifier.size(20.dp))
                        }
                        if (index < minOf(days.size, 5) - 1) Divider(color = Border.copy(alpha = .8f))
                    }
                }
            }
        }
        item(key = "home_privacy") {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Mint).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(20.dp))
                Text("আপনার সব তথ্য এই ফোনেই সংরক্ষিত থাকে।", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
@Composable
private fun SummaryMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .13f)).padding(horizontal = 11.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color.White.copy(alpha = .78f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable
private fun QuickCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp), border = BorderStroke(1.dp, Border.copy(alpha = .9f)), shape = RoundedCornerShape(19.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Mint), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Border.copy(alpha = .95f)), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.width(5.dp).height(25.dp).clip(RoundedCornerShape(50.dp)).background(Green))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (subtitle != null) Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
            Spacer(Modifier.height(if (subtitle != null) 13.dp else 11.dp))
            content()
        }
    }
}
@Composable
private fun PageHeading(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.width(5.dp).height(43.dp).clip(RoundedCornerShape(50.dp)).background(Green))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun PlanScreen(month: String, values: Map<String, String>, onChange: (Map<String, String>) -> Unit, onSave: () -> Unit) {
    val totalFields = planGroups.sumOf { it.fields.size }.coerceAtLeast(1)
    val completedFields = planGroups.sumOf { group -> group.fields.count { field ->
        val value = values[field.key].orEmpty()
        if (field.type == FieldType.CHECK) value == "true" else value.isNotBlank()
    } }
    val progress = (completedFields.toFloat() / totalFields.toFloat()).coerceIn(0f, 1f)
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "plan_heading") { PageHeading("মাসিক পরিকল্পনা", monthLabel(month) + " মাসের লক্ষ্য নির্ধারণ করুন") }
        item(key = "plan_progress") {
            SectionCard("লক্ষ্য পূরণের অগ্রগতি", "${numberBn(completedFields)} / ${numberBn(totalFields)}টি ঘর পূরণ হয়েছে") {
                Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50.dp)).background(Border)) {
                    Box(Modifier.fillMaxWidth(fraction = progress).fillMaxHeight().background(Green))
                }
                Text("${(progress * 100).toInt()}% সম্পন্ন", color = Forest, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        items(planGroups, key = { it.title }) { group -> FieldGroupCard(group, values, onChange) }
        item(key = "plan_save") { PrimaryAction("পরিকল্পনা সংরক্ষণ", Icons.Default.Save, onSave); Spacer(Modifier.height(10.dp)) }
    }
}
@Composable
private fun DailyScreen(
    date: String,
    values: Map<String, String>,
    savedRecordExists: Boolean,
    onPickDate: () -> Unit,
    onShiftDate: (Int) -> Unit,
    onToday: () -> Unit,
    valuesChange: (Map<String, String>) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    val isPastDate = date < todayIso()
    val hasSavedRecord = savedRecordExists
    var showClearConfirmation by remember(date) { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item(key = "daily_heading") {
            PageHeading("দৈনিক হিসাব", "প্রতিদিনের কাজ ও অভ্যাস লিখুন")
        }
        item(key = "daily_date") {
            SectionCard("হিসাবের তারিখ", "আজ অথবা আগের দিনের তথ্য লিখুন") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Today, contentDescription = null, tint = Green, modifier = Modifier.size(23.dp))
                    Column(Modifier.weight(1f)) {
                        Text("নির্বাচিত তারিখ", color = Muted, fontSize = 11.sp)
                        Text(date, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    TextButton(onClick = onPickDate) { Text("ক্যালেন্ডার") }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onShiftDate(-1) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)) {
                        Text("← আগের দিন", fontSize = 12.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = onToday, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)) {
                        Text("আজ", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { onShiftDate(1) }, enabled = date < todayIso(), modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)) {
                        Text("পরের দিন →", fontSize = 12.sp, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isPastDate) Mint else CanvasBg).padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(if (isPastDate) Icons.Default.CalendarMonth else Icons.Default.CheckCircle, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                isPastDate && hasSavedRecord -> "আগের দিনের রিপোর্ট পাওয়া গেছে"
                                isPastDate -> "আগের দিনের রিপোর্ট লিখছেন"
                                hasSavedRecord -> "আজকের রিপোর্ট সংরক্ষিত আছে"
                                else -> "নতুন রিপোর্ট"
                            },
                            color = Forest, fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                        )
                        Text(
                            when {
                                isPastDate && hasSavedRecord -> "তথ্য পরিবর্তন করে সংরক্ষণ করলে আগের রিপোর্ট আপডেট হবে।"
                                isPastDate -> "তথ্য লিখে সংরক্ষণ করুন; এটি ওই তারিখেই থাকবে।"
                                else -> "সংরক্ষণ করলে তথ্য এই ফোনেই থাকবে।"
                            },
                            color = Muted, fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(dailyGroups, key = { it.title }) { group -> FieldGroupCard(group, values, valuesChange) }
        item(key = "daily_actions") {
            PrimaryAction(if (hasSavedRecord) "হিসাব সংরক্ষণ / আপডেট" else "দৈনিক হিসাব সংরক্ষণ", Icons.Default.Save, onSave)
            TextButton(onClick = { showClearConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                Text("এই দিনের হিসাব মুছুন", color = Color(0xFFB0443E))
            }
            if (showClearConfirmation) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmation = false },
                    title = { Text("এই দিনের রিপোর্ট মুছে ফেলবেন?") },
                    text = { Text("এই তারিখের সংরক্ষিত তথ্য মুছে যাবে। এই কাজটি পরে ফিরিয়ে আনা যাবে না।") },
                    confirmButton = {
                        TextButton(onClick = { showClearConfirmation = false; onClear() }) {
                            Text("মুছে ফেলুন", color = Color(0xFFB0443E))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmation = false }) { Text("বাতিল") }
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
@Composable
private fun FieldGroupCard(group: FieldGroup, values: Map<String, String>, onChange: (Map<String, String>) -> Unit) {
    var expanded by remember(group.title) { mutableStateOf(group.title == "কুরআন অধ্যয়ন") }
    val completed = group.fields.count { field ->
        val value = values[field.key].orEmpty()
        if (field.type == FieldType.CHECK) value == "true" else value.isNotBlank()
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Border.copy(alpha = .95f)), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { expanded = !expanded }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Mint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = Forest, modifier = Modifier.size(21.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(group.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${numberBn(completed)} / ${numberBn(group.fields.size)}টি তথ্য পূরণ", color = Muted, fontSize = 10.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(50.dp)).background(Mint).padding(horizontal = 9.dp, vertical = 5.dp)) {
                    Text("${completed}/${group.fields.size}", color = Forest, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "সংকুচিত করুন" else "বিস্তারিত খুলুন", tint = Muted, modifier = Modifier.size(22.dp))
            }
            Box(Modifier.fillMaxWidth().padding(top = 9.dp).height(3.dp).clip(RoundedCornerShape(50.dp)).background(Border)) {
                Box(Modifier.fillMaxWidth(fraction = if (group.fields.isEmpty()) 0f else completed.toFloat() / group.fields.size).fillMaxHeight().background(Green))
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp)); Divider(color = Border); Spacer(Modifier.height(12.dp))
                group.fields.forEachIndexed { index, field ->
                    if (field.type == FieldType.CHECK) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (values[field.key] == "true") Mint else CanvasBg).clickable { onChange(values + (field.key to (if (values[field.key] == "true") "false" else "true"))) }.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = values[field.key] == "true", onCheckedChange = { onChange(values + (field.key to it.toString())) }, colors = CheckboxDefaults.colors(checkedColor = Green, uncheckedColor = Border, checkmarkColor = Color.White))
                            Text(field.label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        OutlinedTextField(
                            value = values[field.key].orEmpty(),
                            onValueChange = { onChange(values + (field.key to it)) },
                            label = { Text(field.label + if (field.unit.isNotBlank()) " (" + field.unit + ")" else "") },
                            placeholder = { Text(if (field.type == FieldType.NUMBER) "সংখ্যা লিখুন" else "এখানে লিখুন", color = Muted.copy(alpha = .72f), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = if (field.type == FieldType.NUMBER) KeyboardType.Decimal else KeyboardType.Text),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Border, focusedLabelColor = Forest, unfocusedLabelColor = Muted, cursorColor = Green, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )
                    }
                    if (index < group.fields.lastIndex) Spacer(Modifier.height(9.dp))
                }
            }
        }
    }
}
@Composable
private fun ReportScreen(
    month: String,
    root: JSONObject,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    onPickDate: () -> Unit,
    onSaveDailyPdf: () -> Unit,
    onShareDailyPdf: () -> Unit,
    onPrintDaily: () -> Unit,
    onSaveMonthlyPdf: () -> Unit,
    onShareMonthlyPdf: () -> Unit,
    onPrintMonthly: () -> Unit,
    onSaveSnapshotPdf: () -> Unit,
    onShareSnapshotPdf: () -> Unit,
    onPrintSnapshot: () -> Unit
) {
    val days = remember(root, month) { monthDays(root, month) }
    val plan = remember(root, month) { jsonToMap(root.optJSONObject("plans")?.optJSONObject(month)) }
    val selectedValues = remember(root, selectedDate) { root.optJSONObject("days")?.optJSONObject(selectedDate) }
    val summarySections = remember(days, plan) { monthlySnapshotSections(days, plan) }
    val filledPlanGroups = remember(plan) {
        planGroups.mapNotNull { group ->
            val entries = group.fields.filter { !plan[it.key].isNullOrBlank() }
            if (entries.isEmpty()) null else group to entries
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "report_heading") { PageHeading("দৈনিক ও মাসিক রিপোর্ট", monthLabel(month) + " · " + numberBn(days.size) + " দিনের রেকর্ড") }
        item(key = "summary_heading") {
            SectionCard("একনজরে মাসিক রিপোর্ট", "দৈনিক হিসাব থেকে মোট ও গড় স্বয়ংক্রিয়ভাবে হিসাব করা হয়েছে") {
                Text("কাগজের মাসিক ফর্মের প্রতিটি বিভাগ নিচে আলাদা করে দেখানো হয়েছে।", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                PrimaryAction("একনজরে মাসিক PDF সংরক্ষণ", Icons.Default.PictureAsPdf, onSaveSnapshotPdf)
                SecondaryAction("মাসিক সারাংশ শেয়ার", onShareSnapshotPdf)
                SecondaryAction("মাসিক সারাংশ প্রিন্ট", onPrintSnapshot, Icons.Default.Print)
            }
        }
        items(summarySections, key = { "summary_" + it.title }) { section ->
            SectionCard(section.title) { section.rows.forEach { (label, value) -> ReportRow(label, value) } }
        }
        item(key = "daily_report") {
            SectionCard("তারিখভিত্তিক রিপোর্ট", "প্রতিটি তারিখের রিপোর্ট আলাদা করে দেখুন, PDF করুন বা প্রিন্ট করুন") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Today, contentDescription = null, tint = Green, modifier = Modifier.size(23.dp))
                    Column(Modifier.weight(1f)) {
                        Text("নির্বাচিত তারিখ", color = Muted, fontSize = 11.sp)
                        Text(selectedDate, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    TextButton(onClick = onPickDate) { Text("তারিখ বাছুন") }
                }
                Spacer(Modifier.height(8.dp))
                Text("এই মাসের সংরক্ষিত তারিখ", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                if (days.isEmpty()) {
                    Text("এই মাসে কোনো দৈনিক হিসাব সংরক্ষণ করা হয়নি। প্রথমে “দৈনিক” ট্যাবে হিসাব লিখে সংরক্ষণ করুন।", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                } else {
                    days.forEach { item ->
                        val date = item.first
                        Row(
                            Modifier.fillMaxWidth().padding(top = 5.dp).clip(RoundedCornerShape(11.dp))
                                .background(if (date == selectedDate) Mint else Color.Transparent)
                                .clickable { onSelectDate(date) }.padding(horizontal = 10.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = if (date == selectedDate) Forest else Muted, modifier = Modifier.size(19.dp))
                            Column(Modifier.weight(1f)) {
                                Text(date, color = if (date == selectedDate) Forest else Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("কুরআন " + numberBn(item.second["quranAyat"]?.toDoubleOrNull() ?: 0.0) + " আয়াত · হাদিস " + numberBn(item.second["hadithCount"]?.toDoubleOrNull() ?: 0.0) + " টি", color = Muted, fontSize = 11.sp)
                            }
                            if (date == selectedDate) Icon(Icons.Default.CheckCircle, contentDescription = "নির্বাচিত", tint = Green, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Divider(color = Border)
                Spacer(Modifier.height(10.dp))
                Text("নির্বাচিত দিনের বিস্তারিত", color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (selectedValues == null) {
                    Text("এই তারিখে এখনো রিপোর্ট নেই। “দৈনিক” ট্যাবে গিয়ে তারিখটি নির্বাচন করে তথ্য লিখুন এবং সংরক্ষণ করুন।", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                } else {
                    var hasAny = false
                    dailyGroups.forEach { group ->
                        val filled = group.fields.filter {
                            val value = selectedValues.optString(it.key, "")
                            value.isNotBlank() && value != "false"
                        }
                        if (filled.isNotEmpty()) {
                            hasAny = true
                            Text(group.title, color = Green, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
                            filled.forEach { field ->
                                val raw = selectedValues.optString(field.key, "")
                                ReportRow(field.label, if (field.type == FieldType.CHECK) "হ্যাঁ" else raw + if (field.unit.isNotBlank()) " " + field.unit else "")
                            }
                        }
                    }
                    if (!hasAny) Text("এই তারিখে রিপোর্ট সংরক্ষিত আছে, তবে কোনো তথ্য পূরণ করা হয়নি।", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    PrimaryAction("এই দিনের PDF সংরক্ষণ", Icons.Default.PictureAsPdf, onSaveDailyPdf)
                    SecondaryAction("এই দিনের PDF শেয়ার", onShareDailyPdf)
                    SecondaryAction("এই দিনের রিপোর্ট প্রিন্ট", onPrintDaily, Icons.Default.Print)
                }
            }
        }
        item(key = "monthly_overview_card") {
            Card(colors = CardDefaults.cardColors(containerColor = Forest), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("মাসের সারাংশ", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
                    Text(monthLabel(month), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryMini("রেকর্ডের দিন", numberBn(days.size), Modifier.weight(1f))
                        SummaryMini("কুরআন", numberBn(totalFor(root, month, "quranAyat")), Modifier.weight(1f))
                        SummaryMini("পড়াশোনা", numberBn(totalFor(root, month, "textbookHours")) + " ঘ.", Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "monthly_plan_heading") {
            SectionCard("মাসিক পরিকল্পনা", "এই মাসে নির্ধারিত লক্ষ্য") {
                if (filledPlanGroups.isEmpty()) Text("এই মাসে এখনো পরিকল্পনা সংরক্ষণ করা হয়নি।", color = Muted, fontSize = 13.sp)
                else Text("পরিকল্পনার বিভাগগুলো নিচে দেখুন।", color = Muted, fontSize = 12.sp)
            }
        }
        items(filledPlanGroups, key = { "plan_" + it.first.title }) { (group, entries) ->
            SectionCard(group.title) {
                entries.forEach { field -> ReportRow(field.label, plan[field.key].orEmpty() + if (field.unit.isNotBlank()) " " + field.unit else "") }
            }
        }
        item(key = "monthly_totals") {
            SectionCard("মাসের মোট কাজ", "দৈনিক রেকর্ড থেকে হিসাব করা হয়েছে") {
                totalFields.forEach { field -> ReportRow(field.label, numberBn(totalFor(root, month, field.key)) + if (field.unit.isNotBlank()) " " + field.unit else "") }
                ReportRow("পত্রিকা পাঠ", numberBn(days.count { it.second["magazineRead"] == "true" }) + " দিন")
                ReportRow("শরীরচর্চা", numberBn(days.count { it.second["exerciseDone"] == "true" }) + " দিন")
                ReportRow("আত্মসমালোচনা", numberBn(days.count { it.second["selfReviewDone"] == "true" }) + " দিন")
            }
        }
        item(key = "daily_records_list") {
            SectionCard("দৈনিক রেকর্ডের তালিকা", "তারিখে চাপ দিলে নির্বাচিত দিনের রিপোর্ট দেখাবে") {
                if (days.isEmpty()) Text("এই মাসে কোনো দৈনিক হিসাব নেই।", color = Muted, fontSize = 13.sp)
                days.forEach { item ->
                    Column(Modifier.fillMaxWidth().clickable { onSelectDate(item.first) }.padding(vertical = 8.dp)) {
                        Text(item.first, color = Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("কুরআন " + numberBn(item.second["quranAyat"]?.toDoubleOrNull() ?: 0.0) + " আয়াত · হাদিস " + numberBn(item.second["hadithCount"]?.toDoubleOrNull() ?: 0.0) + " · সাহিত্য " + numberBn((item.second["literatureIslamic"]?.toDoubleOrNull() ?: 0.0) + (item.second["literatureOther"]?.toDoubleOrNull() ?: 0.0)) + " পৃষ্ঠা · পড়াশোনা " + numberBn(item.second["textbookHours"]?.toDoubleOrNull() ?: 0.0) + " ঘণ্টা", color = Muted, fontSize = 12.sp)
                    }
                    Divider(color = Border)
                }
            }
        }
        item(key = "monthly_pdf_actions") {
            PrimaryAction("পুরো মাসের PDF সংরক্ষণ", Icons.Default.PictureAsPdf, onSaveMonthlyPdf)
            SecondaryAction("পুরো মাসের PDF শেয়ার", onShareMonthlyPdf)
            SecondaryAction("পুরো মাসের রিপোর্ট প্রিন্ট", onPrintMonthly, Icons.Default.Print)
            Spacer(Modifier.height(18.dp))
        }
    }
}
@Composable
private fun ReportRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value.ifBlank { "—" }, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
    Divider(color = Border.copy(alpha = .75f))
}

@Composable
private fun PrimaryAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(9.dp))
        Text(title, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryAction(title: String, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(49.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Forest)) {
        if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(title, fontWeight = FontWeight.Bold)
    }
}

private class LocalPdfPrintAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
        if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
        val info = PrintDocumentInfo.Builder(file.name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN).build()
        callback.onLayoutFinished(info, true)
    }
    override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
        if (destination == null) { callback.onWriteFailed("PDF destination unavailable"); return }
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        if (cancellationSignal?.isCanceled == true) { callback.onWriteCancelled(); return }
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) { callback.onWriteFailed(e.message) }
    }
}

private object PdfReport {
    fun createMonthlySnapshot(context: Context, month: String, plan: Map<String, String>, days: List<Pair<String, Map<String, String>>>): File {
        val document = PdfDocument()
        val writer = Writer(document, month)
        writer.title("একনজরে মাসিক রিপোর্ট")
        writer.text("মাস: " + monthLabel(month), 12f, true, Forest.toArgb())
        writer.text("মোট রিপোর্টের দিন: " + numberBn(days.size), 10f)
        writer.text("কুরআন: " + numberBn(dailyTotal(days, "quranAyat")) + " আয়াত  |  হাদিস: " + numberBn(dailyTotal(days, "hadithCount")) + " টি", 10f)
        writer.space(8f)
        monthlySnapshotSections(days, plan).forEach { section ->
            writer.section(section.title)
            section.rows.forEach { (label, value) -> writer.row(label, value) }
            writer.space(4f)
        }
        writer.space(14f)
        writer.text("দায়িত্বশীলের পরামর্শ ও মন্তব্য: __________________________________")
        writer.text("স্বাক্ষর: __________________________    তারিখ: __________________")
        writer.finish()
        val file = File(context.cacheDir, "report-boi-summary-" + month + ".pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun createDaily(context: Context, date: String, values: Map<String, String>): File {
        val document = PdfDocument()
        val writer = Writer(document, date.take(7))
        writer.title("রিপোর্ট বই — দৈনিক রিপোর্ট")
        writer.text("রিপোর্টের তারিখ: " + date, 12f, true, Forest.toArgb())
        writer.text("ব্যবহারকারী: ব্যক্তিগত হিসাব", 10f, false, Muted.toArgb())
        writer.space(8f)
        var any = false
        dailyGroups.forEach { group ->
            val filled = group.fields.filter {
                val value = values[it.key].orEmpty()
                value.isNotBlank() && value != "false"
            }
            if (filled.isNotEmpty()) {
                any = true
                writer.section(group.title)
                filled.forEach { field ->
                    val raw = values[field.key].orEmpty()
                    writer.row(field.label, if (field.type == FieldType.CHECK) "হ্যাঁ" else raw + if (field.unit.isNotBlank()) " " + field.unit else "")
                }
                writer.space(5f)
            }
        }
        if (!any) writer.text("এই তারিখে কোনো তথ্য পূরণ করা হয়নি।")
        writer.space(18f)
        writer.text("দায়িত্বশীলের মন্তব্য: __________________________________________")
        writer.text("স্বাক্ষর: __________________________    তারিখ: __________________")
        writer.finish()
        val file = File(context.cacheDir, "report-boi-" + date + ".pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun create(context: Context, month: String, plan: Map<String, String>, days: List<Pair<String, Map<String, String>>>): File {
        val document = PdfDocument()
        val writer = Writer(document, month)
        writer.title("রিপোর্ট বই — মাসিক রিপোর্ট")
        writer.text("মাস: " + monthLabel(month), 12f, true, Forest.toArgb())
        writer.text("ব্যবহারকারী: ব্যক্তিগত হিসাব", 10f, false, Muted.toArgb())
        writer.text("রেকর্ড: " + numberBn(days.size) + " দিন  |  কুরআন: " + numberBn(days.sumOf { it.second["quranAyat"]?.toDoubleOrNull() ?: 0.0 }) + " আয়াত  |  হাদিস: " + numberBn(days.sumOf { it.second["hadithCount"]?.toDoubleOrNull() ?: 0.0 }) + " টি", 10f)
        writer.space(8f)
        writer.section("একনজরে মাসিক রিপোর্ট")
        monthlySnapshotSections(days, plan).forEach { section ->
            writer.subheading(section.title)
            section.rows.forEach { (label, value) -> writer.row(label, value) }
            writer.space(3f)
        }
        writer.space(8f)
        writer.section("মাসিক পরিকল্পনা")
        var anyPlan = false
        planGroups.forEach { group ->
            val filled = group.fields.filter { !plan[it.key].isNullOrBlank() }
            if (filled.isNotEmpty()) {
                anyPlan = true
                writer.subheading(group.title)
                filled.forEach { field ->
                    writer.row(field.label, plan[field.key].orEmpty() + if (field.unit.isNotBlank()) " " + field.unit else "")
                }
            }
        }
        if (!anyPlan) writer.text("এই মাসে কোনো পরিকল্পনা সংরক্ষণ করা হয়নি।")
        writer.space(8f)
        writer.section("মাসের মোট কাজ")
        totalFields.forEach { field ->
            val total = days.sumOf { it.second[field.key]?.toDoubleOrNull() ?: 0.0 }
            writer.row(field.label, numberBn(total) + if (field.unit.isNotBlank()) " " + field.unit else "")
        }
        writer.row("পত্রিকা পাঠ", numberBn(days.count { it.second["magazineRead"] == "true" }) + " দিন")
        writer.row("শরীরচর্চা", numberBn(days.count { it.second["exerciseDone"] == "true" }) + " দিন")
        writer.row("আত্মসমালোচনা", numberBn(days.count { it.second["selfReviewDone"] == "true" }) + " দিন")
        writer.space(8f)
        writer.section("দৈনিক হিসাব")
        if (days.isEmpty()) writer.text("এই মাসে কোনো দৈনিক হিসাব নেই।")
        days.forEach { item ->
            writer.subheading(item.first)
            dailyGroups.forEach { group ->
                group.fields.forEach { field ->
                    val value = item.second[field.key].orEmpty()
                    if (value.isNotBlank() && value != "false") {
                        writer.row(field.label, if (field.type == FieldType.CHECK) "হ্যাঁ" else value + if (field.unit.isNotBlank()) " " + field.unit else "")
                    }
                }
            }
        }
        writer.space(18f)
        writer.text("দায়িত্বশীলের মন্তব্য: __________________________________________")
        writer.text("স্বাক্ষর: __________________________    তারিখ: __________________")
        writer.finish()
        val file = File(context.cacheDir, "report-boi-" + month + ".pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private class Writer(private val document: PdfDocument, private val month: String) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 48f
        private val width = 595
        private val height = 842
        private val left = 38f
        private val right = 557f
        init { startPage() }

        private fun startPage() {
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            canvas = page!!.canvas
            canvas!!.drawColor(AColor.WHITE)
            y = 45f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Forest.toArgb(); textSize = 10f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
            canvas!!.drawText("রিপোর্ট বই  •  " + monthLabel(month), left, y, paint)
            y += 23f
        }
        private fun nextPageIfNeeded(lines: Int = 1) {
            if (y + lines * 18f > height - 42f) {
                document.finishPage(page!!)
                startPage()
            }
        }
        fun title(value: String) {
            nextPageIfNeeded(3)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Forest.toArgb(); textSize = 21f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
            canvas!!.drawText(value, left, y + 3f, p); y += 32f
        }
        fun section(value: String) {
            nextPageIfNeeded(2)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Forest.toArgb() }
            canvas!!.drawRoundRect(left, y, right, y + 25f, 5f, 5f, p)
            val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.WHITE; textSize = 12f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
            canvas!!.drawText(value, left + 9f, y + 17f, t); y += 34f
        }
        fun subheading(value: String) {
            nextPageIfNeeded(2)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Green.toArgb(); textSize = 11f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
            canvas!!.drawText(value, left, y, p); y += 17f
        }
        fun row(label: String, value: String) {
            val lines = wrap(label + ": " + value, 70)
            nextPageIfNeeded(lines.size)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ink.toArgb(); textSize = 9.5f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
            lines.forEach { canvas!!.drawText(it, left + 2f, y, p); y += 15f }
            y += 2f
            val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Border.toArgb(); strokeWidth = .5f }
            canvas!!.drawLine(left, y, right, y, divider); y += 5f
        }
        fun text(value: String, size: Float = 10f, bold: Boolean = false, color: Int = Ink.toArgb()) {
            val lines = wrap(value, 78)
            nextPageIfNeeded(lines.size)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; textSize = size; typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL) }
            lines.forEach { canvas!!.drawText(it, left, y, p); y += size + 7f }
        }
        fun space(value: Float) { y += value }
        fun finish() { document.finishPage(page!!) }
        private fun wrap(value: String, max: Int): List<String> {
            val words = value.split(Regex("\\s+"))
            val lines = mutableListOf<String>()
            var line = ""
            words.forEach { word ->
                if (line.isNotEmpty() && line.length + word.length + 1 > max) { lines.add(line); line = word }
                else line = if (line.isEmpty()) word else line + " " + word
            }
            if (line.isNotEmpty()) lines.add(line)
            return lines.ifEmpty { listOf("") }
        }
    }
}
