package com.kiwi.finanzas.ui.views

import android.app.AlertDialog
import android.content.Context
import android.app.DatePickerDialog
import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.kiwi.finanzas.R
import com.kiwi.finanzas.db.Entrada
import com.kiwi.finanzas.db.EntradaDAO
import com.kiwi.finanzas.db.TipoDAO
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.kiwi.finanzas.db.Agrupado
import com.kiwi.finanzas.db.Tipo
import com.kiwi.finanzas.formatAsCurrency
import com.kiwi.finanzas.getPreference
import com.kiwi.finanzas.getValidatedNumber
import com.kiwi.finanzas.isLeapYear
import com.kiwi.finanzas.savePreference
import com.kiwi.finanzas.ui.theme.myBlue
import com.kiwi.finanzas.ui.theme.myGreen
import com.kiwi.finanzas.ui.theme.myRed
import com.kiwi.finanzas.ui.theme.myYellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.Month

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(daoEntradas: EntradaDAO, daoTipos: TipoDAO, context: Context, modifier: Modifier) {
    val tiposNull by daoTipos.getAll().collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val currentTime = LocalDateTime.now()
    var gastosNull: List<Entrada>? by remember { mutableStateOf(null) }
    var showDetalles by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var addNew by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableIntStateOf(getPreference(context, "tutorialHome").toInt() % 1000) }
    var entradaEdit: Entrada? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val periodo by remember { mutableStateOf(if(getPreference(context,"periodo") >= 0f) getPreference(context,"periodo") else 1f) }
    val agrupadosNull by daoEntradas.getTotales(currentTime.monthValue, currentTime.year).collectAsState(initial = null)
    val showTutorial = (tutorialStep < 2) || (((agrupadosNull ?: listOf<Entrada>()).isNotEmpty()) && tutorialStep == 2) || (((agrupadosNull ?: listOf<Entrada>()).isNotEmpty()) && tutorialStep == 3)
    LaunchedEffect(Unit) {
        scope.launch{
            gastosNull = daoEntradas.getMesHome(currentTime.monthValue, currentTime.year, 10, 0)
        }
    }
    fun addEntradas(){
        scope.launch {
            gastosNull = gastosNull?.plus(daoEntradas.getMesHome(currentTime.monthValue, currentTime.year, 10, gastosNull?.size ?: 0))
        }
    }
    fun reloadEntradas(){
        scope.launch {
            gastosNull = daoEntradas.getMesHome(currentTime.monthValue, currentTime.year, gastosNull?.size ?: 10, 0)
        }
    }
    fun addSingle(){
        scope.launch {
            val entrada = daoEntradas.getMesHome(currentTime.monthValue, currentTime.year, 1, 0)
            gastosNull = entrada.plus(gastosNull ?: listOf())
        }
    }
    val agrupadosPeriodo by daoEntradas.getTotalesPeriodo(
        ((currentTime.year-1)*372) + ((currentTime.monthValue - 1)*31),
        ((currentTime.year-1)*372) + ((currentTime.monthValue)*31),
            ).collectAsState(initial = emptyList())
    if(tiposNull != null && gastosNull != null && agrupadosNull != null) {
        val agrupados = agrupadosNull!!
        val tipos = tiposNull!!
        val gastos = gastosNull!!
        val listState = rememberLazyListState()
        var inicio = 90f
        val total1 = agrupados.filter { it.total > 0 }.sumOf { it.total }
        val total3 = agrupadosPeriodo.sumOf { it.total }
        val gastoMax = getPreference(context, "maxDia")
        val total2 = agrupadosPeriodo.sumOf { it.total }
        val totalDegree = (total2 / gastoMax) * -360f
        // Detectar si el ítem de mayor índice (último en la lista) es visible
        val endReached by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val lastItemIndex = gastos.lastIndex
                layoutInfo.visibleItemsInfo.any { it.index == lastItemIndex }
            }
        }

        // Disparar el evento
        LaunchedEffect(endReached) {
            if (endReached) {
                addEntradas()
            }
        }
        if (showTutorial){
            DialogTutorial(tutorialStep, onChange = {
                tutorialStep++
                savePreference(context, "tutorialHome", tutorialStep.toFloat())
            }, 1)
        }
        if (showDetalles) {
            DialogDetalles(onDismis = {
                showDetalles = false
            }, agrupadosComp = agrupados)
        }
        if (showEdit) {
            if (entradaEdit != null) {
                DialogEdit(
                    context, entrada = entradaEdit!!, onDismis = {
                    showEdit = false
                    entradaEdit = null
                }, onEdit = { ent ->
                    coroutineScope.launch {
                        daoEntradas.update(ent)
                        reloadEntradas()
                        showEdit = false
                        entradaEdit = null
                    }
                }, tipos = tipos,
                    onDelete = { id ->
                        coroutineScope.launch {
                            daoEntradas.delete(id)
                            reloadEntradas()
                            showEdit = false
                            entradaEdit = null
                        }
                    })
            }
        }
        if (addNew) {
            DialogEdit(
                context, onDismis = {
                    addNew = false
                }, onCreate = { ent ->
                    coroutineScope.launch {
                        daoEntradas.insert(ent)
                        addNew = false
                        addSingle()
                    }
                }, tipos = tipos, entrada = null)
        }
        Column(modifier = modifier
            .blur(if (showDetalles || showEdit || addNew) 16.dp else 0.dp)
            .background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(20.dp, 50.dp, 20.dp, 20.dp)) {
                Row(modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 20.dp)) {
                    if (agrupados.isNotEmpty()) {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .let {
                                if (tutorialStep == 2) {
                                    it
                                        .border(10.dp, MaterialTheme.colorScheme.tertiary)
                                        .padding(10.dp)
                                } else {
                                    it
                                }
                            }) {
                            Canvas(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(100.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                showDetalles = true
                                            }
                                        )
                                    }
                            ) {
                                agrupadosPeriodo.filter { it.total > 0 }.forEach {
                                    val fin = ((it.total / total1) * 360f).toFloat()
                                    drawArc(it.color(), -1f * inicio, -1f * fin, true)
                                    inicio += fin
                                }
                                //drawArc(Color.Red, -1f*inicio, -10f, true)
                            }
                            Text(
                                text = DecimalFormat("0.00€").format(total3),
                                color = if (total3 >= getPreference(context, "maxDia")) myRed else if(total3 >= (0.8f * getPreference(context, "maxDia"))) myYellow else myGreen,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp),
                                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .let {
                            if (tutorialStep == 0) {
                                it
                                    .border(10.dp, MaterialTheme.colorScheme.tertiary)
                                    .padding(10.dp)
                            } else {
                                it
                            }
                        }) {
                        Canvas(
                            modifier = Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            drawArc(Color.Gray, -90f, -360f, true)
                            drawArc(myBlue, -90f, totalDegree.toFloat(), true)
                        }
                        Text(
                            text = DecimalFormat("0.00€").format(gastoMax - total2),
                            color = if (gastoMax - total2 < 0) myRed else myGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp),
                            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                        items(gastos) { it ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp, 3.dp)
                                    .let {
                                        if (tutorialStep == 3) {
                                            it
                                                .border(10.dp, MaterialTheme.colorScheme.tertiary)
                                                .padding(10.dp)
                                        } else {
                                            it
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = tipos.first { t -> it.tipo == t.id }
                                    .color()),
                                onClick = {
                                    entradaEdit = it
                                    showEdit = true
                                }
                            ) {
                                Text(
                                    text = it.concepto,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp, 5.dp),
                                    style = TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = tipos.first { t -> t.id == it.tipo }.textColor(),
                                )
                                Row {
                                    Text(
                                        text = DecimalFormat("0.00€").format(it.cantidad),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(10.dp, 5.dp),
                                        style = TextStyle(
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (it.cantidad > 0) myRed else myGreen
                                    )
                                    Text(
                                        text = "${it.dia}-${it.mes}-${it.anno}",
                                        modifier = Modifier.padding(2.dp),
                                        color = tipos.first { t -> t.id == it.tipo }.textColor(),
                                    )
                                }
                            }
                        }
                    }
                    IconButton({
                        addNew = true
                    }, modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .let {
                                if (tutorialStep == 1) {
                                    it
                                        .border(10.dp, MaterialTheme.colorScheme.tertiary)
                                        .padding(10.dp)
                                } else {
                                    it
                                }
                            }) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(200.dp)
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
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun DialogTutorial(tutorialStep: Int, onChange: () -> Unit, tipo: Int) { //0: Main, 1: Home, 2: Historic, 3: Settings
    val indice = (10*tipo) + tutorialStep
    val titulo = when(indice){
        0 -> stringResource(R.string.saludos)
        1 -> stringResource(R.string.navegacion)
        10 -> stringResource(R.string.gasto_total)
        11 -> stringResource(R.string.anadir_gasto)
        12 -> stringResource(R.string.desglose_de_gastos)
        13 -> stringResource(R.string.entradas_de_datos)
        20 -> stringResource(R.string.historico)
        21 -> stringResource(R.string.fecha_de_consulta)
        22 -> stringResource(R.string.mostrar_ocultar_grafica)
        23 -> stringResource(R.string.exportar_a_excel)
        30 -> stringResource(R.string.gasto_mensual_tutorial)
        31 -> stringResource(R.string.tipos_de_gasto)
        else -> ""
    }
    val texto = when(indice){
        0 -> stringResource(R.string.saludos_t)
        1 -> stringResource(R.string.navegacion_t)
        10 -> stringResource(R.string.gasto_total_t)
        11 -> stringResource(R.string.anadir_gasto_t)
        12 -> stringResource(R.string.desglose_de_gasto_t)
        13 -> stringResource(R.string.entradas_t)
        20 -> stringResource(R.string.historico_t)
        21 -> stringResource(R.string.fecha_consulta_t)
        22 -> stringResource(R.string.mostrar_ocultar_Graficas_t)
        23 -> stringResource(R.string.exportar_excel_t)
        30 -> stringResource(R.string.gasto_mensual_t)
        31 -> stringResource(R.string.tipos_gastos_t)
        else -> ""
    }
    AlertDialog(confirmButton = {
        TextButton(onClick = {
            onChange()
        }) { Text(stringResource(R.string.entendido)) }
    }, text = {
        Text(texto)
    }, onDismissRequest = {
        onChange()
    }, title = {
        Text(
            titulo,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogDetalles(onDismis: () -> Unit = {}, agrupadosComp: List<Agrupado> = listOf(), titulo: String? = null){
    val agrupados = agrupadosComp.filter { it.total > 0 }
    val agrupadosGanancias = agrupadosComp.filter { it.total <= 0 }
    val total1 = agrupados.sumOf { it.total }
    var inicio = 90f
    AlertDialog(
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally){
                if(titulo != null) {
                    Text(
                        titulo, style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Canvas(
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp)
                ) {
                    agrupados.forEach {
                        val fin = ((it.total / total1) * 360f).toFloat()
                        drawArc(it.color(), -1f * inicio, -1f * fin, true)
                        inicio += fin
                    }
                    //drawArc(Color.Red, -1f*inicio, -10f, true)
                }
                Spacer(modifier = Modifier.height(20.dp))
                var total = agrupados.sumOf { a -> a.total }
                LazyColumn {
                    items(agrupados){
                        Card(modifier = Modifier.padding(5.dp),
                            colors = CardDefaults.cardColors(containerColor = it.color())) {
                            Row(modifier = Modifier.padding(5.dp)){
                                Text(text = it.nombre, modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f), color = it.textColor())
                                Text(text = String.format("%.2f€", it.total), color = it.textColor())
                                Spacer(modifier = Modifier.width(15.dp))
                                Text(text = String.format("%.0f", (it.total/total)*100)+"%", color = it.textColor())
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                LazyColumn {
                    items(agrupadosGanancias){
                        Card(modifier = Modifier.padding(5.dp),
                            colors = CardDefaults.cardColors(containerColor = it.color())) {
                            Row(modifier = Modifier.padding(5.dp)){
                                Text(text = it.nombre, modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f), color = it.textColor())
                                Text(text = String.format("+%.2f€", it.total*-1f), color = it.textColor())
                            }
                        }
                    }
                }
            }
        },
        onDismissRequest = {onDismis()}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogEdit(context: Context, onDismis: () -> Unit = {}, onDelete: (id: Int) -> Unit = {}, onEdit: (entrada: Entrada) -> Unit = {}, onCreate: (entrada: Entrada) -> Unit = {}, entrada: Entrada?, tipos: List<Tipo> = listOf()){
    var text by remember { mutableStateOf(entrada?.concepto ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var tipoDefault = stringResource(R.string.tipo)
    var amount by remember { mutableStateOf(formatAsCurrency(DecimalFormat("0.00").format(entrada?.cantidad ?: 0.0f))) }
    var tipo by remember { mutableStateOf(tipos.find { t -> t.id == entrada?.tipo}?.nombre ?: tipoDefault) }
    var tipoColor by remember { mutableStateOf(tipos.find { t -> t.id == entrada?.tipo}?.color() ?: Color.Gray) }
    var textColor by remember { mutableStateOf(tipos.find { t -> t.id == entrada?.tipo}?.textColor() ?: Color.Black) }
    var tipoId by remember { mutableIntStateOf(entrada?.tipo ?: 0) }
    val currentTime = LocalDateTime.now()
    var year by remember { mutableIntStateOf(entrada?.anno ?: currentTime.year) }
    var month by remember { mutableIntStateOf(entrada?.mes ?: currentTime.monthValue) }
    var day by remember { mutableIntStateOf(entrada?.dia ?: currentTime.dayOfMonth) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            year = selectedYear
            month = selectedMonth + 1
            day = selectedDay
        }, year, month, day
    )
    AlertDialog(
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally){
                OutlinedCard {
                    Column(modifier = Modifier.padding(10.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = text,
                            onValueChange = {
                                text = it
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.concepto),
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
                        Row(
                            modifier = Modifier.padding(0.dp, 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DropdownMenu(expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                tipos.filter { t -> t.disponible == 1 }.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.nombre) },
                                        onClick = {
                                            expanded = false
                                            tipo = it.nombre
                                            tipoId = it.id
                                            tipoColor = it.color()
                                            textColor = it.textColor()
                                        },
                                        modifier = Modifier.background(it.color())
                                    )
                                }
                            }
                            OutlinedButton(
                                modifier = Modifier.weight(1F),
                                colors = ButtonDefaults.buttonColors(containerColor = tipoColor),
                                onClick = { expanded = true }) {
                                Text(text = tipo, color = textColor)
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            val textFieldValue = remember(amount) {
                                TextFieldValue(
                                    text = amount,
                                    selection = TextRange(amount.length) // Cursor al final
                                )
                            }
                            OutlinedTextField(
                                modifier = Modifier.weight(0.6F),
                                value = textFieldValue,
                                onValueChange = { newText ->
                                    amount = formatAsCurrency(newText.text)
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.precio)
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
                        }
                        Row {
                            Spacer(modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f))
                            TextButton(onClick = {
                                datePickerDialog.show()
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                                Text("$day-${month}-$year", color = Color.White)
                            }
                        }
                    }
                }
                Row{
                    if(entrada != null) {
                        TextButton(onClick = {
                            if (tipoId != 0 && amount != "" && text != "") {
                                onDelete(entrada.id)
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = myRed)) {
                            Text(stringResource(R.string.borrar), color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(50.dp))
                    }
                    TextButton(onClick = {
                        if (tipoId != 0 && amount != "" && text != "") {
                            if(entrada != null) {
                                onEdit(
                                    Entrada(
                                        concepto = text,
                                        anno = year,
                                        mes = month,
                                        dia = day,
                                        hora = entrada.hora,
                                        min = entrada.min,
                                        cantidad = amount.toDouble(),
                                        tipo = tipoId,
                                        id = entrada.id
                                    )
                                )
                            }else{
                                onCreate(Entrada(
                                    concepto = text,
                                    anno = year,
                                    mes = month,
                                    dia = day,
                                    hora = currentTime.hour,
                                    min = currentTime.minute,
                                    cantidad = amount.toDouble(),
                                    tipo = tipoId
                                ))
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = myBlue)) {
                        Text(stringResource(R.string.ok), color = Color.White)
                    }
                }
            }
        },
        onDismissRequest = {onDismis()},
    )
}
