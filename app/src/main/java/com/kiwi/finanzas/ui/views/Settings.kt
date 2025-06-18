package com.kiwi.finanzas.ui.views

import android.R
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.MotionEvent
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kiwi.finanzas.db.Entrada
import com.kiwi.finanzas.db.Tipo
import com.kiwi.finanzas.db.TipoDAO
import com.kiwi.finanzas.ui.theme.myBlue
import com.kiwi.finanzas.ui.theme.myGreen
import com.kiwi.finanzas.ui.theme.myRed
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(daoTipos: TipoDAO, context: Context, modifier: Modifier) {
    val tiposNull by daoTipos.getAll().collectAsState(initial = null)
    var text by remember { mutableStateOf(getPreference(context, "maxDia").toString()) }
    var tipoSelected: Tipo? by remember { mutableStateOf(null) }
    var selector by remember { mutableStateOf(false) }
    var completos by remember { mutableStateOf(false) }
    var expandedDay by remember { mutableStateOf(false)}
    var periodo by remember { mutableFloatStateOf(if(getPreference(context,"periodo") >= 0f) getPreference(context,"periodo") else 1f) }
    val coroutineScope = rememberCoroutineScope()
    if(tiposNull != null) {
        val tipos = tiposNull!!
        if (selector) {
            CustomDialog(
                tipo = tipoSelected, onDismis = { selector = false },
                onOk = { tipo ->
                    selector = false
                    if (tipoSelected == null) {
                        coroutineScope.launch {
                            daoTipos.insert(tipo)
                        }
                    } else {
                        coroutineScope.launch {
                            daoTipos.update(tipo)
                        }
                    }
                },
                onRestore = { id ->
                    coroutineScope.launch {
                        daoTipos.restore(id)
                        selector = false
                    }
                },
                onDelete = { id ->
                    coroutineScope.launch {
                        daoTipos.delete(id)
                        selector = false
                    }
                })
        }
        Column(modifier = modifier.blur(if (selector) 16.dp else 0.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 50.dp, 20.dp, 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 24.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        text = "Gasto mensual",
                        color = Color.White
                    )
                    OutlinedTextField(
                        modifier = Modifier.width(110.dp),
                        textStyle = TextStyle(fontSize = 24.sp),
                        value = text,
                        onValueChange = {
                            val valor = getValidatedNumber(it)
                            val valorNum = if (valor == "") 1000f else valor.toFloat()
                            text = valorNum.toString()
                            savePreference(context, "maxDia", valorNum)
                        },
                        placeholder = {
                            Text(
                                text = "Valor",
                                modifier = Modifier.width(100.dp),
                                style = TextStyle(fontSize = 24.sp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 1,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                    )
                    Text(
                        text = "€",
                        color = Color.White,
                        style = TextStyle(fontSize = 24.sp),
                        modifier = Modifier.padding(5.dp, 0.dp, 20.dp, 0.dp)
                    )
                }
                Row(
                    modifier = Modifier.padding(0.dp, 40.dp, 0.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 24.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        text = "Periodo de gestión: ",
                        color = Color.White
                    )
                    Column {
                        DropdownMenu(
                            expanded = expandedDay,
                            onDismissRequest = { expandedDay = false }) {
                            DropdownMenuItem(
                                text = { Text("Diario") },
                                onClick = {
                                    periodo = 1f
                                    savePreference(context, "periodo", periodo)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Semanal") },
                                onClick = {
                                    periodo = 2f
                                    savePreference(context, "periodo", periodo)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Quincenal") },
                                onClick = {
                                    periodo = 3f
                                    savePreference(context, "periodo", periodo)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mensual") },
                                onClick = {
                                    periodo = 4f
                                    savePreference(context, "periodo", periodo)
                                }
                            )
                        }
                        TextButton(onClick = { expandedDay = true }) {
                            Text(
                                text = when (periodo) {
                                    1f -> "Diario"
                                    2f -> "Semanal"
                                    3f -> "Quincenal"
                                    else -> "Mensual"
                                },
                                style = TextStyle(fontSize = 24.sp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Text(
                        text = "Mostrar eliminados",
                        color = Color.White,
                        modifier = Modifier.padding(5.dp, 0.dp)
                    )
                    Switch(checked = completos, onCheckedChange = {
                        completos = it
                    })
                }
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 20.dp),
                ) {
                    LazyColumn {
                        items(tipos.filter { t -> t.disponible == 1 || completos }) {
                            OutlinedCard(
                                onClick = {
                                    selector = true
                                    tipoSelected = it
                                },
                                colors = CardDefaults.outlinedCardColors(containerColor = it.color()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                            ) {
                                Text(
                                    text = it.nombre,
                                    color = it.textColor(),
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(), textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    IconButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        tipoSelected = null
                        selector = true
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_input_add),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }
    else{
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .width(64.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = label)
        Slider(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.Gray,
                activeTrackColor = Color.Gray
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CustomDialog(tipo: Tipo? = null, onDismis: () -> Unit = {}, onOk: (tipo: Tipo) -> Unit = {}, onRestore: (id: Int) -> Unit = {}, onDelete: (id: Int) -> Unit = {}) {
    var nombre by remember { mutableStateOf(tipo?.nombre ?: "") }
    var selectedColor by remember { mutableStateOf(tipo?.color() ?: Color.Gray) }
    var red by remember { mutableStateOf(selectedColor.red) }
    var green by remember { mutableStateOf(selectedColor.green) }
    var blue by remember { mutableStateOf(selectedColor.blue) }
    AlertDialog(
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedCard {
                    Column(modifier = Modifier.padding(10.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = nombre,
                            onValueChange = {
                                nombre = it
                            },
                            placeholder = {
                                Text(
                                    text = "Nombre",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 1,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        ColorPicker(onColorSelected = {
                            selectedColor = it
                            red = selectedColor.red
                            blue = selectedColor.blue
                            green = selectedColor.green
                        }, color = selectedColor)
                    }
                }
                Row {
                    if (tipo != null) {
                        if (tipo.disponible == 1) {
                            TextButton(
                                colors = ButtonDefaults.buttonColors(containerColor = myRed),
                                onClick = {
                                    onDelete(tipo.id)
                                }) {
                                Text("Borrar", color = Color.White)
                            }
                        } else {
                            TextButton(
                                colors = ButtonDefaults.buttonColors(containerColor = myGreen),
                                onClick = {
                                    onRestore(tipo.id)
                                }) {
                                Text("Restaurar", color = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(50.dp))
                    }
                    TextButton(onClick = {
                        if (nombre != "") {
                            onOk(
                                Tipo(
                                    id = tipo?.id ?: 0,
                                    nombre = nombre,
                                    blue = blue,
                                    red = red,
                                    green = green
                                )
                            )
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = myBlue)) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        },
        onDismissRequest = { onDismis() },

    )
}

data class HSVColor(val hue: Float, val saturation: Float, val value: Float)
fun Color.toHSV(): HSVColor {
    val maxColorComponent = maxOf(red, green, blue)
    val minColorComponent = minOf(red, green, blue)
    val delta = maxColorComponent - minColorComponent

    val hue = when {
        delta == 0f -> 0f
        maxColorComponent == red -> (60 * (((green - blue) / delta) + 6)) % 360
        maxColorComponent == green -> (60 * (((blue - red) / delta) + 2)) % 360
        else -> (60 * (((red - green) / delta) + 4)) % 360
    }

    val saturation = if (maxColorComponent != 0f) delta / maxColorComponent else 0f

    val value = maxColorComponent

    return HSVColor(hue, saturation, value)
}


@Composable
fun ColorPicker(
    modifier: Modifier = Modifier,
    onColorSelected: (Color) -> Unit,
    color: Color
) {
    var hue by remember { mutableStateOf(color.toHSV().hue) }
    var saturation by remember { mutableStateOf(color.toHSV().saturation) }
    var value by remember { mutableStateOf(color.toHSV().value) }
    val selectedColor = hsvToColor(hue, saturation, value)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ColorSelectionBox(
            hue = hue,
            saturation = saturation,
            value = value,
            onSaturationBrightnessChange = { newSaturation, newValue ->
                saturation = newSaturation
                value = newValue
                onColorSelected(hsvToColor(hue, saturation, value))
            },
            selectedColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
        HueSlider(
            hue = hue,
            onHueChange = { newHue -> hue = newHue }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(selectedColor, shape = CircleShape)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    val hueGradient = remember {
        (0..360).map {
            Color.hsv(it.toFloat(), 1f, 1f)
        }
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
        .onSizeChanged { canvasSize = it }
        .pointerInteropFilter { event ->
            val size = canvasSize
            if (size != IntSize.Zero) {
                when {
                    event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE -> {
                        val x = event.x.coerceIn(0f, size.width.toFloat())
                        val newHue = (x / size.width) * 360f
                        onHueChange(newHue.coerceIn(0f, 360f))
                        true
                    }

                    event.action == MotionEvent.ACTION_UP -> {
                        val x = event.x.coerceIn(0f, size.width.toFloat())
                        val newHue = (x / size.width) * 360f
                        onHueChange(newHue.coerceIn(0f, 360f))
                        true
                    }

                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        drawRect(brush = androidx.compose.ui.graphics.Brush.horizontalGradient(hueGradient))
        val indicatorX = (hue / 360f) * size.width
        drawArc(Color.Black, 0f, 360f, true, Offset((indicatorX-size.height/2f)-8f, -8f), Size(size.height+16f, size.height+16f))
        drawArc(Color.White, 0f, 360f, true, Offset((indicatorX-size.height/2f)-6f, -6f), Size(size.height+12f, size.height+12f))
        drawArc(Color.hsv(hue, 1f, 1f), 0f, 360f, true, Offset((indicatorX-size.height/2f)-4f, -4f), Size(size.height+8f, size.height+8f))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColorSelectionBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationBrightnessChange: (Float, Float) -> Unit,
    selectedColor: Color
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var saturation by remember { mutableStateOf(saturation) }
    var value by remember { mutableStateOf(value) }
    var posX = saturation * canvasSize.width
    var posY = (1f - value)*canvasSize.height
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .onSizeChanged { canvasSize = it }
        .pointerInteropFilter { event ->
            val size = canvasSize
            if (size != IntSize.Zero) {
                when {
                    event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE -> {
                        val x = event.x.coerceIn(0f, size.width.toFloat())
                        val y = event.y.coerceIn(0f, size.height.toFloat())
                        saturation = (x / size.width).coerceIn(0f, 1f)
                        value = 1f - (y / size.height).coerceIn(0f, 1f)
                        //posX = saturation * size.width
                        //posY = (1f - value)*size.height
                        onSaturationBrightnessChange(saturation, value)
                        true
                    }

                    event.action == MotionEvent.ACTION_UP -> {
                        val x = event.x.coerceIn(0f, size.width.toFloat())
                        val y = event.y.coerceIn(0f, size.height.toFloat())
                        val offset = Offset(x, y)
                        saturation = (offset.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        //posX = saturation * size.width
                        //posY = (1f - value)*size.height
                        onSaturationBrightnessChange(saturation, value)
                        true
                    }

                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        val saturationGradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
            0f to Color.White,
            1f to Color.hsv(hue, 1f, 1f)
        )
        val valueGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Color.Black
        )
        drawRect(brush = saturationGradient)
        drawRect(brush = valueGradient)
        drawArc(Color.Black, 0f, 360f, true, Offset(posX-24f, posY-24f), Size(48f, 48f))
        drawArc(Color.White, 0f, 360f, true, Offset(posX-22f, posY-22f), Size(44f, 44f))
        drawArc(selectedColor, 0f, 360f, true, Offset(posX-20f, posY-20f), Size(40f, 40f))
    }
}

fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val c = value * saturation
    val x = c * (1 - kotlin.math.abs((hue / 60f) % 2 - 1))
    val m = value - c

    val (r, g, b) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = ((r + m) * 255).roundToInt(),
        green = ((g + m) * 255).roundToInt(),
        blue = ((b + m) * 255).roundToInt()
    )
}

@Composable
@Preview
fun vista(){
    Column(modifier = Modifier.blur(if (false) 16.dp else 0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 50.dp, 20.dp, 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontSize = 24.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "Gasto mensual",
                    color = Color.White
                )
                OutlinedTextField(
                    modifier = Modifier.width(100.dp),
                    textStyle = TextStyle(fontSize = 24.sp),
                    value = "text",
                    onValueChange = {

                    },
                    placeholder = {
                        Text(
                            text = "Valor",
                            modifier = Modifier.width(100.dp),
                            style = TextStyle(fontSize = 24.sp)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                )
                Text(
                    text = "€",
                    color = Color.White,
                    style = TextStyle(fontSize = 24.sp),
                    modifier = Modifier.padding(5.dp, 0.dp, 20.dp, 0.dp)
                )
            }
            Row(modifier = Modifier.padding(0.dp, 40.dp, 0.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
                DropdownMenu(expanded = false,
                    onDismissRequest = {  }) {
                    DropdownMenuItem(
                        text = { Text("Diario") },
                        onClick = {

                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Semanal") },
                        onClick = {

                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Quincenal") },
                        onClick = {

                        }
                    )
                }
                Text(
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontSize = 24.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "Periodo de gestión:",
                    color = Color.White
                )
                TextButton(onClick = {  }) {
                    Text(
                        text = "Diario",
                        style = TextStyle(fontSize = 24.sp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}