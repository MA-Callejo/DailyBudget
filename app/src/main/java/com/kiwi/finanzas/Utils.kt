package com.kiwi.finanzas

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.collectAsState
import java.text.DecimalFormat

fun getValidatedNumber(text: String): String {
    val textSinComa = text.replace(",", ".")
    val filteredChars = textSinComa.filterIndexed { index, c ->
        c in "0123456789" ||                      // Take all digits
                (c == '.' && textSinComa.indexOf('.') == index) ||
                (c == '-' && index == 0)// Take only the first decimal
    }
    // Now we need to remove extra digits from the input
    return if(filteredChars.contains('.')) {
        val beforeDecimal = filteredChars.substringBefore('.')
        val afterDecimal = filteredChars.substringAfter('.')
        beforeDecimal + "." + afterDecimal.take(2)    // If decimal is present, take first 3 digits before decimal and first 2 digits after decimal
    } else {
        filteredChars                     // If there is no decimal, just take the first 3 digits
    }
}

fun formatAsCurrency(digits: String): String {
    val padded = digits.filter { it.isDigit() }.padStart(3, '0') // Asegura al menos 3 dígitos
    val euros = padded.dropLast(2).toInt()
    val cents = padded.takeLast(2).toInt()
    return String.format("%d.%02d", euros, cents)
}

fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0))
}

fun savePreference(context: Context, key: String, value: Float) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = sharedPreferences.edit()
    editor.putFloat(key, value)
    editor.apply()
}

fun getPreference(context: Context, key: String): Float {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    return sharedPreferences.getFloat(key, 1000f)
}

fun textoGraficas(valor: Float): String{
    var valorCalculo = valor
    var letra = ""
    if(valorCalculo >= 1000f){
        letra = "K"
        valorCalculo /= 1000f
    }
    if(valorCalculo >= 1000f){
        letra = "M"
        valorCalculo /= 1000f
    }
    val resultado = ((valorCalculo*10).toInt()/10f)
    return DecimalFormat("0.0").format(resultado)+letra
}

fun getPresupuesto(context: Context): Float {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val maximo = sharedPreferences.getFloat("maxDia", 1000f)
    val periodo = sharedPreferences.getFloat("periodo", 0f)
    return when(periodo){
        1f -> {
            maximo * 31
        }
        2f -> {
            maximo * 4
        }
        3f -> {
            maximo * 2
        }
        else -> {
            maximo
        }
    }
}