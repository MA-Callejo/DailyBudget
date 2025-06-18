package com.kiwi.finanzas.ui.views

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kiwi.finanzas.Mes
import com.kiwi.finanzas.R
import com.kiwi.finanzas.db.Agrupado
import com.kiwi.finanzas.db.Entrada
import com.kiwi.finanzas.db.EntradaDAO
import com.kiwi.finanzas.db.TipoDAO
import com.kiwi.finanzas.getPreference
import com.kiwi.finanzas.getPresupuesto
import com.kiwi.finanzas.isLeapYear
import com.kiwi.finanzas.ui.theme.myGreen
import com.kiwi.finanzas.ui.theme.myRed
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.Month
import java.util.Locale
import kotlin.math.max


@Composable
fun Historico(anno: Int?, mes: Int?, dia: Int?, daoEntradas: EntradaDAO, daoTipos: TipoDAO, context: Context, changeFechas: (anno: Int?, mes: Int?, dia: Int?) -> Unit, modifier: Modifier) {
    val tipos by daoTipos.getAll().collectAsState(initial = null)
    var showDetalles by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var annoAct by remember { mutableStateOf(anno) }
    var mesAct by remember { mutableStateOf(mes) }
    var diaAct by remember { mutableStateOf(dia) }
    var total by remember { mutableStateOf(0f) }
    var entradas by remember { mutableStateOf<List<Entrada>?>(null) }
    var agrupados by remember { mutableStateOf<List<Pair<Int, List<Agrupado>>>?>(null) }
    var detallesConsulta by remember { mutableStateOf<List<Agrupado>?>(null) }
    var entradaEdit: Entrada? by remember { mutableStateOf(null) }
    var scope = rememberCoroutineScope()
    val presupuesto = getPresupuesto(context)

    suspend fun getAnnos(): List<Pair<Int, List<Agrupado>>>{
        val annos = daoEntradas.getAnnos()
        val res = mutableListOf<Pair<Int, List<Agrupado>>>()
        annos.forEach{ ann ->
            res.add(Pair(ann, daoEntradas.getTotalesAnno(ann)))
        }
        return res
    }
    suspend fun getMeses(): List<Pair<Int, List<Agrupado>>>{
        val res = mutableListOf<Pair<Int, List<Agrupado>>>()
        for(i in 1..12){
            res.add(Pair(i, daoEntradas.getTotalesMes(i, annoAct!!)))
        }
        return res
    }
    suspend fun getDias(): List<Pair<Int, List<Agrupado>>>{
        val res = mutableListOf<Pair<Int, List<Agrupado>>>()
        for(i in 1..Month.of(mesAct!!).length(isLeapYear(annoAct!!))){
            res.add(Pair(i, daoEntradas.getTotalesDia(mesAct!!, annoAct!!, i)))
        }
        return res
    }

    suspend fun obtenerDatos(){
        agrupados = null
        entradas = null
        if(diaAct != null){
            entradas = daoEntradas.getAllDia(mesAct!!, diaAct!!, annoAct!!)
            total = entradas!!.sumOf { it1 -> it1.cantidad }.toFloat()
        }else{
            agrupados = if(mesAct != null){
                getDias()
            }else{
                if(annoAct != null){
                    getMeses()
                }else{
                    getAnnos()
                }
            }
            total = agrupados!!.sumOf { it2 -> it2.second.sumOf { it3 -> it3.total } }.toFloat()
        }
    }
    LaunchedEffect(Unit) {
        scope.launch { obtenerDatos() }
    }

    fun changeFecha(anno: Int?, mes: Int?, dia: Int?){
        changeFechas(anno, mes, dia)
        annoAct = anno
        mesAct = mes
        diaAct = dia
        scope.launch { obtenerDatos() }
    }


    BackHandler(enabled = true, onBack = {
        if (mesAct == null) {
            changeFecha(null, null, null)
        }else {
            if (diaAct == null) {
                changeFecha(annoAct, null, null)
            } else {
                changeFecha(annoAct, mesAct, null)
            }
        }
    })
    if(showDetalles && tipos != null && detallesConsulta != null){
        DialogDetalles(onDismis = {showDetalles = false
                                  detallesConsulta = null}, agrupadosComp = detallesConsulta!!)
    }
    if(showEdit && tipos != null) {
        if (entradaEdit != null) {
            DialogEdit(
                context, entrada = entradaEdit!!, onDismis = {
                    showEdit = false
                    entradaEdit = null
                }, onEdit = { ent ->
                    scope.launch {
                        daoEntradas.update(ent)
                        showEdit = false
                        entradaEdit = null
                        obtenerDatos()
                    }
                }, tipos = tipos!!,
                onDelete = { id ->
                    scope.launch {
                        daoEntradas.delete(id)
                        showEdit = false
                        entradaEdit = null
                        obtenerDatos()
                    }
                })
        }
    }
    if(tipos != null && (entradas != null || agrupados != null)){
        Scaffold(modifier = modifier.blur(if (showDetalles || showEdit) 16.dp else 0.dp), topBar = {
            Row(){
                if(annoAct != null) {
                    IconButton(onClick = {
                        if (mesAct == null) {
                            changeFecha(null, null, null)
                        }else {
                            if (diaAct == null) {
                                changeFecha(annoAct, null, null)
                            } else {
                                changeFecha(annoAct, mesAct, null)
                            }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "",
                            tint = Color.White
                        )
                    }
                }
                Column() {
                    Text(if (annoAct != null) "$annoAct${if (mesAct != null) "/$mesAct${if (diaAct != null) "/$diaAct" else ""}" else ""}" else "TODOS")
                    Text("$total€")
                }
            }
        }) { paddingValues ->
            Column(modifier = Modifier

                .padding(paddingValues)) {

                if(entradas != null) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if(entradas != null) {
                            items(entradas!!) { it ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp, 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = tipos!!.first { t -> it.tipo == t.id }
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
                                        color = tipos!!.first { t -> t.id == it.tipo }.textColor(),
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
                                            color = tipos!!.first { t -> t.id == it.tipo }
                                                .textColor(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if(agrupados != null){
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if(agrupados != null) {
                            items(agrupados!!) { agrupados ->
                                val totalCoste = agrupados.second.sumOf { it2 -> it2.total }
                                var inicio = 90f
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp, 3.dp),
                                    onClick = {
                                        if (mesAct != null) {
                                            changeFecha(annoAct, mesAct, agrupados.first)
                                        } else {
                                            if (annoAct != null) {
                                                changeFecha(annoAct, agrupados.first, null)
                                            } else {
                                                changeFecha(agrupados.first, null, null)
                                            }
                                        }
                                    }
                                ) {
                                    Row {
                                        Column() {
                                            Text(
                                                text = if (mesAct != null)
                                                    "Dia ${agrupados.first}"
                                                else if (annoAct != null)
                                                    Month.of(agrupados.first).name
                                                else
                                                    "Year ${agrupados.first}",
                                                modifier = Modifier.padding(2.dp)
                                            )
                                            Row() {
                                                val presupuestoTot =
                                                    if (mesAct != null) presupuesto / Month.of(
                                                        mesAct!!
                                                    ).length(
                                                        isLeapYear(annoAct!!)
                                                    ) else if (annoAct != null) presupuesto else presupuesto * 12f
                                                Text(
                                                    text = DecimalFormat("0.00€").format(totalCoste),
                                                    modifier = Modifier
                                                        .padding(10.dp, 5.dp),
                                                    style = TextStyle(
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Text(
                                                    text = DecimalFormat("0.00€").format(
                                                        presupuestoTot - totalCoste
                                                    ),
                                                    modifier = Modifier
                                                        .padding(10.dp, 5.dp),
                                                    style = TextStyle(
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = if (totalCoste > presupuestoTot) myRed else myGreen
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(8.dp)
                                        ) {
                                            Canvas(
                                                modifier = Modifier
                                                    .width(100.dp)
                                                    .height(100.dp)
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onTap = {
                                                                showDetalles = true
                                                                detallesConsulta = agrupados.second
                                                            }
                                                        )
                                                    }

                                            ) {
                                                drawArc(Color.Gray, 0f, 360f, true)
                                                agrupados.second.filter { it2 -> it2.total > 0 }.forEach {
                                                    val fin =
                                                        ((it.total / totalCoste) * 360f).toFloat()
                                                    drawArc(
                                                        it.color(),
                                                        -1f * inicio,
                                                        -1f * fin,
                                                        true
                                                    )
                                                    inicio += fin
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }else{
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

/*
@Composable
fun HistoricoOld(navController: NavController, anno: Int?, mes: Int?, dia: Int?, daoEntradas: EntradaDAO, daoTipos: TipoDAO, context: Context, changeFechas: (anno: Int, mes: Int, dia: Int) -> Unit) {
    val tipos by daoTipos.getAll().collectAsState(initial = emptyList())
    var showEdit by remember { mutableStateOf(false) }
    var detallesShow by remember { mutableStateOf(false) }
    val currentTime = LocalDateTime.now()
    var entradaEdit: Entrada? by remember { mutableStateOf(null) }
    var diaExpanded by remember { mutableStateOf(false) }
    var mesExpanded by remember { mutableStateOf(false) }
    var annoExpanded by remember { mutableStateOf(false) }
    var agrupados by remember { mutableStateOf(true) }
    var expandedTipos by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Todos") }
    var tipoId: Int? by remember { mutableStateOf(null) }
    var tipoColor by remember { mutableStateOf(Color.Gray) }
    var textColor by remember { mutableStateOf(Color.White) }
    val annos by daoEntradas.getAnnos().collectAsState(initial = emptyList())
    val meses by daoEntradas.getMeses(anno ?: 0).collectAsState(initial = emptyList())
    val dias by daoEntradas.getDias(anno = anno ?: 0, mes = mes ?: 0).collectAsState(initial = emptyList())
    val entradas by if(anno != null){
        if(mes != null){
            if(dia != null){
                daoEntradas.getAllDiaFiltro(mes!!, dia!!, anno!!, "%$text%")
                    .collectAsState(initial = emptyList())
            }else {
                daoEntradas.getAllMesFiltro(mes!!, anno!!, "%$text%")
                    .collectAsState(initial = emptyList())
            }
        }else {
            daoEntradas.getAllAnnoFiltro(anno!!, "%$text%").collectAsState(initial = emptyList())
        }
    }else{
        daoEntradas.getAllFiltro("%$text%").collectAsState(initial = emptyList())
    }
    val grouped = if(anno != null){
        if(mes != null){
            if(dias.size > 0) {
                (1..dias.max()).toList()
            }else{
                listOf()
            }
        }else {
            if(meses.size > 0) {
                (meses.min()..meses.max()).toList()
            }else{
                listOf()
            }
        }
    }else{
        if(annos.size > 0){
            (annos.min()..annos.max()).toList()
        }else{
            listOf()
        }
    }

    val createFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    writer.write("Concepto,Fecha,Importe,Tipo")
                    writer.newLine()
                    entradas.forEach { entry ->
                        writer.write("${entry.concepto},${entry.anno}-${entry.mes}-${entry.dia},${entry.cantidad},${tipos.find { t -> t.id == entry.tipo }?.nombre}")
                        writer.newLine()
                    }
                }
            }
        }
    }

    if(showEdit){
        if(entradaEdit != null) {
            DialogEdit(context, entrada = entradaEdit!!, onDismis = {
                showEdit = false
                entradaEdit = null
            }, onEdit = {ent ->
                coroutineScope.launch {
                    daoEntradas.update(ent)
                    showEdit = false
                    entradaEdit = null
                }
            }, tipos = tipos,
                onDelete = {id ->
                    coroutineScope.launch {
                        daoEntradas.delete(id)
                        showEdit = false
                        entradaEdit = null
                    }
                })
        }
    }
    Column(modifier = Modifier.blur(if (showEdit || detallesShow) 16.dp else 0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 50.dp, 20.dp, 20.dp)
        ) {

            Row (modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)){
                Column (modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()){
                    OutlinedButton(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                        onClick = {
                            navController.navigate("historico/ / / ")
                        }) {
                        Text(text = (if(anno!=null) anno.toString() else "Todos"))
                    }
                }
                Column (modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()){
                    OutlinedButton(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                        onClick = {
                            navController.navigate("historico/"+anno.toString()+"/ / ")
                        }) {
                        Text(text = (if(mes!=null) Mes.obtenerPorIndice(mes!! - 1, anno!!).nombre else "Todos"))
                    }
                }
                Column (modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()){
                    OutlinedButton(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                        onClick = {
                            navController.navigate("historico/"+anno.toString()+"/"+mes.toString()+"/ ")
                        }) {
                        Text(text = (if(dia!=null) dia.toString() else "Todos"))
                    }
                }
            }
            if(!(agrupados)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    DropdownMenu(expanded = expandedTipos,
                        onDismissRequest = { expandedTipos = false }) {
                        DropdownMenuItem(
                            text = { Text("Todos", color = Color.White) },
                            onClick = {
                                expandedTipos = false
                                tipo = "Todos"
                                tipoId = null
                                tipoColor = Color.Gray
                            },
                            modifier = Modifier.background(Color.Gray)
                        )
                        tipos.forEach {
                            DropdownMenuItem(
                                text = { Text(it.nombre, color = it.textColor()) },
                                onClick = {
                                    expandedTipos = false
                                    tipo = it.nombre
                                    tipoId = it.id
                                    tipoColor = it.color()
                                },
                                modifier = Modifier.background(it.color())
                            )
                        }
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.buttonColors(containerColor = tipoColor),
                        onClick = { expandedTipos = true }) {
                        Text(text = tipo, color = textColor)
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = text,
                        onValueChange = {
                            text = it
                        },
                        placeholder = {
                            Text(
                                text = "Buscador",
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 1,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                if(dia == null){
                    items(grouped.chunked(2)){ rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (item in rowItems) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp)
                                ) {
                                    ItemView(if(anno == null) item else anno, if(mes == null) item else mes, item, if(anno == null) 1 else if(mes==null) 2 else 3,
                                        onDetalles = {detallesShow = it},
                                        daoEntradas = daoEntradas, getPreference(context,"maxDia"),
                                        onEnter = {
                                            if(dia == null){
                                                navController.navigate("historico/"+anno.toString()+"/"+mes.toString()+"/"+it.toString())
                                            }
                                            if(mes == null){
                                                navController.navigate("historico/"+anno.toString()+"/"+it.toString()+"/ ")
                                            }
                                            if(anno == null){
                                                navController.navigate("historico/"+it.toString()+"/ / ")
                                            }
                                        })
                                }
                            }
                            if (rowItems.size == 1) {
                                Box(modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)) { /* Empty Box to fill the space */ }
                            }
                        }
                    }
                }else {
                    items(if (tipoId != null) entradas.filter { it.tipo == tipoId } else entradas) { it ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp, 3.dp),
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
                                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
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
            }
            Row {
                IconButton(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(10.dp), onClick = { createFileLauncher.launch("registros.csv") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.csv),
                        contentDescription = "",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ItemView(anno: Int?, mes: Int?, dia: Int?, tipo: Int = 0, onDetalles: (estado: Boolean)-> Unit, daoEntradas: EntradaDAO, maxDia: Float, onEnter: (valor: Int) -> Unit){
    val currentTime = LocalDateTime.now()
    val agrupados by when (tipo) {
        1 -> daoEntradas.getTotalesAnno(anno!!).collectAsState(initial = emptyList())
        2 -> daoEntradas.getTotales(mes=mes!!, anno=anno!!).collectAsState(initial = emptyList())
        else -> daoEntradas.getTotalesDia(mes=mes!!, anno=anno!!, dia=dia!!).collectAsState(initial = emptyList())
    }
    var detallesShow by remember { mutableStateOf(false) }
    var total = agrupados.sumOf { it.total }
    var totalGasto = agrupados.filter{it.total > 0}.sumOf { it.total }
    var resultado = total
    var maxTotal = max(totalGasto, total)
    var inicio = 90f
    if(detallesShow){
        DialogDetalles(onDismis = {detallesShow = false
                                  onDetalles(false)}, agrupadosComp = agrupados)
    }
    OutlinedCard(onClick = {onEnter(if(tipo == 1) anno!! else if (tipo == 2) mes!! else dia!!)}, border = BorderStroke(2.dp, if(resultado >= 0) myGreen else myRed)) {
        Column(modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally){
            Canvas(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                detallesShow = true
                                onDetalles(true)
                            }
                        )
                    }

            ) {
                drawArc(Color.Gray, 0f, 360f, true)
                agrupados.filter { it.total > 0 }.forEach {
                    val fin = ((it.total / maxTotal) * 360f).toFloat()
                    Log.d("ARC", "Dia ${dia} Tipo ${it.nombre}: ${it.total} / $maxTotal -> $inicio + $fin")
                    drawArc(it.color(), -1f * inicio, -1f * fin, true)
                    inicio += fin
                }
            }
            Text(text=if(tipo == 1) anno.toString() else if (tipo == 2) Mes.obtenerPorIndice(mes!! - 1, anno!!).nombre else dia.toString(), modifier = Modifier
                .padding(0.dp, 10.dp, 0.dp, 0.dp)
                .fillMaxWidth(), textAlign = TextAlign.Center, style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Log.d("GASTO", "$dia : $resultado")
            Text(text=if(resultado >= 0) String.format("%.2f€", resultado) else String.format("%.2f€", resultado*-1f), modifier = Modifier
                .fillMaxWidth(), textAlign = TextAlign.Center,
                color = if(resultado >= 0) myRed else myGreen)
        }
    }
}*/