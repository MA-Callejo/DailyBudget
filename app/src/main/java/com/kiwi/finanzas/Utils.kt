package com.kiwi.finanzas

import android.content.Context
import android.content.SharedPreferences

fun getValidatedNumber(text: String): String {
    // Start by filtering out unwanted characters like commas and multiple decimals
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