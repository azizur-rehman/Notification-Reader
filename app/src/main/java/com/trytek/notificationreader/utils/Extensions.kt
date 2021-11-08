package com.trytek.notificationreader.utils

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.trytek.notificationreader.R
import okio.IOException
import org.jetbrains.anko.AlertBuilder
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.Serializable
import java.math.BigDecimal
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*


fun Context.showConfirmDialog(
    dialogMessage: String,
    okText: String = "Ok",
    onOkClick: (() -> Unit?)?
) = Dialog(this).run{
    val alert = alert { message = dialogMessage
        positiveButton("Yes") { onOkClick?.let { it1 -> it1() } }
        negativeButton("Cancel") {  }
    }
    alert.show()
}


fun Context.showInfoDialog(
    dialogMessage: String,
    cancelable: Boolean = true,
    onOkClick: (() -> Unit?)? = null
): AlertBuilder<DialogInterface> {
    val alert = alert { message = dialogMessage
//        iconResource = R.drawable.ic_info_outline_black_24dp
//        title = "Info"
        okButton { onOkClick?.let { it1 -> it1() } }
        isCancelable = cancelable
    }
    alert.show()
    return alert
}


fun Context.showProgressDialog(shouldShowInitially: Boolean = true): ProgressDialog {
    val dialog = ProgressDialog(this)
    with(dialog){
        setCancelable(false)
        setMessage("Please wait...")
        if(shouldShowInitially)
            show()
    }

    return dialog
}

fun getAllCountries(): LinkedHashMap<String, String> {
    val countryList = LinkedHashMap<String, String>()
    val locale = Locale.getAvailableLocales()
    for (loc in locale) {
        val country = loc.displayCountry
        val countryCode = loc.country
        if (!country.isEmpty() && !countryList.containsKey(country)) {
            countryList[country] = countryCode
        }
    }
    return countryList
}







fun <T> Call<T>.onCall(onResponse: (networkException: Throwable?, response: Response<T>?) -> Unit) {
    this.enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
            onResponse.invoke(t, null)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
            onResponse.invoke(null, response)
        }

    })
}

fun Spinner.setSampleSpinnerAdapter(optionalList: List<String>? = null){
    this.adapter = this.context.sampleArrayAdapter(optionalList)
}

fun Context.sampleArrayAdapter(optionalList: List<String>?) = ArrayAdapter(
    this,
    android.R.layout.simple_list_item_1,
    optionalList ?: emptyList()
)

fun getSampleList(prefix: String = "Item - ", count: Int = 5): List<String> {
    val list:MutableList<String> = ArrayList()
    for(i in 0 until count)
        list.add("$prefix $i")

    return list
}


fun Context.openPDF(file: File){
    try {
        var intent:Intent

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(this, "${packageName}.file_provider", file)
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            this.startActivity(intent)
        } else {
            intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(file.path), "application/pdf")
            intent = Intent.createChooser(intent, "Open File")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        }
    }
    catch (e: Exception){
        e.printStackTrace()
        showInfoDialog("Failed to open invoice. \nError : ${e.message}")
    }
}


val Context.hasLocationPermission: Boolean
    get() = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

val Context.hasStoragePermission: Boolean
    get() = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED


fun <T> RecyclerView.setCustomAdapter(
    items: Collection<T>, @LayoutRes resID: Int,
    onBindViewHolder: (itemView: View, position: Int, item: T) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>{

    val adapter =  object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            val view = LayoutInflater.from(this@setCustomAdapter.context).inflate(
                resID,
                parent,
                false
            )
            return object : RecyclerView.ViewHolder(view){}
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            onBindViewHolder.invoke(holder.itemView, position, items.elementAt(position))
        }

    }
    this.adapter = adapter
    return adapter


}

fun <T> ViewPager2.setCustomAdapter(
    items: Collection<T>, @LayoutRes resID: Int,
    onBindViewHolder: (itemView: View, position: Int, item: T) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>{

    val adapter =  object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            val view = LayoutInflater.from(this@setCustomAdapter.context).inflate(
                resID,
                parent,
                false
            )
            return object : RecyclerView.ViewHolder(view){}
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            onBindViewHolder.invoke(holder.itemView, position, items.elementAt(position))
        }

    }
    this.adapter = adapter
    return adapter


}


fun <T> RecyclerView.setCustomAdapter(
    items: Collection<T>,
    view: View,
    onBindViewHolder: (itemView: View, position: Int, item: T) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder> {

    val adapter =  object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            return object : RecyclerView.ViewHolder(view){}
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            onBindViewHolder.invoke(holder.itemView, position, items.elementAt(position))
        }

    }
    this.adapter = adapter
    return adapter

}

fun Context.hideKeyboard(){
    val context = this
    val activity = context as Activity
    val windowToken = activity.window.decorView.rootView.windowToken
    val inputService = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputService.hideSoftInputFromWindow(windowToken, 0)

}

fun Fragment.addArgs(vararg pairs: Pair<String, Any?>) = this.apply {

    arguments = Bundle().apply {
        pairs.forEach { pair ->
            when (pair.second) {
                is Int -> putInt(pair.first, pair.second as Int)
                is String -> putString(pair.first, pair.second.toString())
                is Serializable -> putSerializable(pair.first, pair.second as Serializable)
                is Boolean -> putBoolean(pair.first, pair.second as Boolean)
                else -> Log.e("FragmentUtils", "addArgs: Pair not available, Please add it")
            }


        }
    }
}


fun isEditTextValid(vararg editText: TextInputEditText):Boolean{
    var isValid = true
    editText.forEach {
        val layout = it.parent.parent as TextInputLayout?

        if(it.text.isNullOrEmpty()){
            isValid = false
            layout?.error = it.context.getString(R.string.field_required)
            layout?.errorIconDrawable = null
        }

        it.doAfterTextChanged { text->
            if(text.isNullOrEmpty().not())
                layout?.error = null
        }
    }

    editText.getOrNull(0)?.requestFocus()

    return isValid
}



suspend fun <T : Any> safeAPICall(call: suspend () -> Response<T>) : T{
    val response = try {
        call.invoke()
    }
    catch (e: java.lang.Exception){
        e.printStackTrace()
        val message = if( e is ConnectException || e is SocketTimeoutException ||
            e is HttpException ||
            e.message?.contains("unexpected end of stream on Connection") == true ||
            e is SocketException || e is UnknownHostException
        )
            "Failed to Connect. Connection TimedOut" else  "Unspecified Error"
        val responseError = ResponseError(message, 500).convertToJsonString()
        Log.e("safeAPICall", "safeAPICall: error thrown = $responseError")
        Log.e("safeAPICall", "safeAPICall: actual error = ${e.message}", e)

        //json is passed as message
        throw java.io.IOException(responseError)
    }



    if(response.isSuccessful){
        return response.body()!!
    }else{
        val error = response.errorBody()?.string()

        error?.let{
            val message = JSONObject(it).optString("message", "Something went wrong")
            val responseError = ResponseError(message.takeIf { response.code() != 500 }
                ?: response.message(), response.code())
            throw IOException(responseError.convertToJsonString())
        }
        throw IOException(
            ResponseError(
                "Something went wrong. Please try again.",
                500
            ).convertToJsonString()
        )
    }
}

fun Context.getColorCode(@ColorRes id: Int) = ContextCompat.getColor(this, id)

data class ResponseError(val message: String, val errorCode: Int)


//JSON Extensions

fun Any.convertToJsonString():String{
    return try{
        Gson().toJson(this)?:""
    }
    catch (e: Exception){
        e.printStackTrace()
        Log.e("Extensions", "convertToJsonString: ${e.message}")
        "{}"
    }
}

inline fun JSONArray.forEach(action: (jsonObject: JSONObject) -> Unit){
    for (i in 0 until length()){
        action(getJSONObject(i))
    }
}


inline fun <reified T> JSONArray.toList():List<T?>{

    val list = mutableListOf<T?>()
    this.forEach {
        list.add(it.toString().toModel<T>())
    }
    return list
}


inline fun <reified T> JSONObject.toModel(): T? = this.run {
    try {
        Gson().fromJson<T>(this.toString(), T::class.java)
    }
    catch (e: java.lang.Exception){ e.printStackTrace()
        Log.e("JSONObject to model", e.message.toString() + " = $this")

        null }
}

inline fun <reified T> String.toModel(): T? = this.run {
    try {
        JSONObject(this).toModel<T>()
    }
    catch (e: java.lang.Exception){
        Log.e("String to model", e.message.toString() + " = $this")
        e.printStackTrace()
        null
    }
}


fun View.hide(){
    visibility = View.GONE
}

fun View.show(){
    visibility = View.VISIBLE
}
var View.isVisible
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if(value)
            View.VISIBLE
        else
            View.GONE
    }

fun View.hideOrShow(){
    if (visibility == View.VISIBLE) hide()
    else show()
}



fun Double.formatToCurrencyString(currencySymbol: String? = "", isPrefix: Boolean = false): String? = kotlin.run {
    val symbols = DecimalFormatSymbols()
    symbols.groupingSeparator = ','
    symbols.decimalSeparator = '.'

    val currency = currencySymbol // if(requireCurrency) " ${currencySymbol?:""}" else ""
    val formatter = if(isPrefix) "${currency}#,##0.######################" else "#,##0.###################### $currency"
    val decimalFormat = DecimalFormat(formatter, symbols)
    decimalFormat.format(this)?:"$currency 0.0"

}

fun getFormattedDateTime(timeInMillis:Long = System.currentTimeMillis(), pattern:String = " hh:mm a yyyy-MM-dd"):String{
    return SimpleDateFormat(pattern, Locale.getDefault())
        .format(Date(timeInMillis))
}


val String.getDoubleFromDollarString: String
    get() = run {
        val regex = "[$,]".toRegex()
        this.replace(regex, "").trim()
    }


fun Double.percentOf(of: Double) = kotlin.run{
    (this.div(100) * of).roundtoTwoPlace
}

inline val Double?.ignoreNullValue get() = if (this?.isNaN() == true) 0.0 else this

inline val Double.roundOneDecimal get() = String.format("%.1f", this)
inline val Double.roundTwoDecimal get() = String.format("%.2f", this)
inline val Double.roundTwoDecimalString get() = String.format("%.2f", this)
inline val Double.roundtoTwoPlace get() = (String.format("%.2f", this).toDoubleOrNull())?:0.0
inline val String?.toDoubleOrZero get() = this?.toDoubleOrNull()?:0.0

inline val String?.toBigDecimalOrZero get() = this?.toBigDecimalOrNull()?: BigDecimal(0.0)
inline val Double?.orZero get() = this ?: 0.0

inline val Any?.takeIfNotNull get() = this.takeIf { it != null }
inline val String?.takeIfNotNullOrEmpty get() = this.takeIf { it.isNullOrEmpty().not() }

fun View.onFinishButton(){
    setOnClickListener {  (context as? Activity?)?.finish() }
}


fun EditText.pasteFromClipboard(){
    val clipboard: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    kotlin.runCatching {  setText(clipboard.primaryClip?.getItemAt(0)?.text) }
}

fun TextView.copyTextViewContent(){

    val clipboard: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(text.toString(), "content"))
    context.toast(context.getString(R.string.copied))
}

fun Context.getAllInstalledApps() = run {
    val mainIntent = Intent(Intent.ACTION_MAIN, null)
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
    val pkgAppsList: List<ResolveInfo> =
        packageManager.queryIntentActivities(mainIntent, 0)

    pkgAppsList
}