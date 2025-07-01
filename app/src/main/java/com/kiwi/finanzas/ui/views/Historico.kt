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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.kiwi.finanzas.getDiaSemana
import com.kiwi.finanzas.getPreference
import com.kiwi.finanzas.getPresupuesto
import com.kiwi.finanzas.isLeapYear
import com.kiwi.finanzas.savePreference
import com.kiwi.finanzas.textoGraficas
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
    var showCharts by remember { mutableStateOf(getPreference(context, "Charts") >= 1f) }
    var annoAct by remember { mutableStateOf(anno) }
    var mesAct by remember { mutableStateOf(mes) }
    var diaAct by remember { mutableStateOf(dia) }
    var total by remember { mutableStateOf(0f) }
    var entradas by remember { mutableStateOf<List<Entrada>?>(null) }
    var agrupados by remember { mutableStateOf<List<Pair<Int, List<Agrupado>>>?>(null) }
    var detallesConsulta by remember { mutableStateOf<Pair<Int, List<Agrupado>>?>(null) }
    var entradaEdit: Entrada? by remember { mutableStateOf(null) }
    var scope = rememberCoroutineScope()
    val presupuesto = getPresupuesto(context)
    var primeraEntrada by remember { mutableStateOf<Entrada?>(null) }
    val createFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    if(tipos != null) {
                        if (entradas != null) {
                            writer.write("Concepto\tFecha\tImporte\tTipo")
                            writer.newLine()
                            entradas!!.forEach { entry ->
                                writer.write("${entry.concepto}\t${entry.anno}-${entry.mes}-${entry.dia}\t${entry.cantidad}\t${tipos!!.find { t -> t.id == entry.tipo }?.nombre}")
                                writer.newLine()
                            }
                        } else {
                            if (agrupados != null) {
                                var cabeceras = "${if(mesAct != null) "Dia" else if(annoAct != null) "Mes" else "Año"}\t"
                                tipos!!.forEach { tp ->
                                    cabeceras += tp.nombre+"\t"
                                }
                                cabeceras += "Total"
                                writer.write(cabeceras)
                                writer.newLine()
                                agrupados!!.forEach { grupo ->
                                    var linea = "${grupo.first}\t"
                                    tipos!!.forEach { tp ->
                                        linea += "${grupo.second.firstOrNull{ gs -> gs.tipoId == tp.id }?.total ?: 0f}\t"
                                    }
                                    linea += "${grupo.second.sumOf { gs -> gs.total }}"
                                    writer.write(linea)
                                    writer.newLine()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
        primeraEntrada = null
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
                    primeraEntrada = daoEntradas.getPrimeraEntrada().firstOrNull()
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
                                  detallesConsulta = null}, agrupadosComp = detallesConsulta!!.second,
            titulo =
                if (annoAct != null)
                    "$annoAct/${
                    if (mesAct != null) 
                        "${Month.of(mesAct!!).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE,context.resources.configuration.getLocales().get(0))}/${detallesConsulta!!.first}" 
                    else 
                        Month.of(detallesConsulta!!.first).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE,context.resources.configuration.getLocales().get(0))}"
                else
                    "${detallesConsulta!!.first}")
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
            Row(modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)){
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
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    Text(if (annoAct != null) "$annoAct${if (mesAct != null) "/${Month.of(mesAct!!).getDisplayName(java.time.format.TextStyle.FULL_STANDALONE,context.resources.configuration.getLocales().get(0))}${if (diaAct != null) "/$diaAct" else ""}" else ""}" else stringResource(R.string.todos),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ))
                    Text(DecimalFormat("0.00€").format(total),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ))
                }
                if(diaAct == null) {
                    IconButton(onClick = {
                        showCharts = !showCharts
                        savePreference(context, "Charts", if(showCharts) 1f else 0f)
                    }) {
                        Icon(
                            painterResource(if (showCharts) R.drawable.bar_chart_off_24px else R.drawable.bar_chart_24px),
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                IconButton(onClick = {
                    createFileLauncher.launch("Registro${if (annoAct != null) "_$annoAct${if (mesAct != null) "_$mesAct${if (diaAct != null) "_$diaAct" else ""}" else ""}" else ""}.csv")
                }) {
                    Icon(
                        painterResource(R.drawable.archive_24px),
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
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
                    if(showCharts){
                        graficas(agrupados!!,
                            if (mesAct != null)
                                Month.of(mesAct!!).length(isLeapYear(annoAct!!)
                            ) else
                                12,
                            (if (mesAct != null) presupuesto / Month.of(
                                mesAct!!
                            ).length(
                                isLeapYear(annoAct!!)
                            ) else if (annoAct != null)
                                presupuesto
                            else
                                presupuesto * 12f).toDouble(),
                            onDetalles = { det ->
                                showDetalles = true
                                detallesConsulta = det
                            })
                    }
                    if(annoAct != null && mesAct != null && diaAct == null){
                        val newAgrupados = List(getDiaSemana(annoAct!!, mesAct!!, 1)-1){Pair(0, listOf<Agrupado>())} +
                                agrupados!!.sortedBy { ag -> ag.first } +
                                List(7-((agrupados!!.size + getDiaSemana(annoAct!!, mesAct!!, 1)-1)%7)){Pair(0, listOf<Agrupado>())}
                        LazyColumn(modifier = Modifier.fillMaxSize()){
                            item{
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Text("L")
                                    Text("M")
                                    Text("X")
                                    Text("J")
                                    Text("V")
                                    Text("S")
                                    Text("D")
                                }
                            }
                            items(newAgrupados.chunked(7)){ diasMes ->
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly){
                                    diasMes.forEachIndexed { diaSem, dm ->
                                        Box(modifier = Modifier
                                            .weight(1f)
                                            .padding(8.dp)){
                                            if(dm.first != 0){
                                                ItemView(
                                                    agrupados = dm.second,
                                                    titulo = if (mesAct != null)
                                                        context.getString(R.string.dia) + " ${dm.first}"
                                                    else if (annoAct != null)
                                                        Month.of(dm.first).getDisplayName(
                                                            java.time.format.TextStyle.SHORT_STANDALONE,
                                                            context.resources.configuration.getLocales()
                                                                .get(0)
                                                        )
                                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                    else
                                                        context.getString(R.string.year) + " ${dm.first}",
                                                    presupuesto = if (mesAct != null) presupuesto / Month.of(
                                                        mesAct!!
                                                    ).length(
                                                        isLeapYear(annoAct!!)
                                                    ) else if (annoAct != null) presupuesto else presupuesto * 12f,
                                                    onDetalles = {
                                                        if (dm.second.isNotEmpty()) {
                                                            showDetalles = true
                                                            detallesConsulta =
                                                                dm
                                                        }
                                                    },
                                                    onEnter = {
                                                        if (mesAct != null) {
                                                            changeFecha(
                                                                annoAct,
                                                                mesAct,
                                                                dm.first
                                                            )
                                                        } else {
                                                            if (annoAct != null) {
                                                                changeFecha(
                                                                    annoAct,
                                                                    dm.first,
                                                                    null
                                                                )
                                                            } else {
                                                                changeFecha(
                                                                    dm.first,
                                                                    null,
                                                                    null
                                                                )
                                                            }
                                                        }
                                                    },
                                                    recortar = if (annoAct == null && primeraEntrada != null) {
                                                        if (dm.first == primeraEntrada!!.anno) {
                                                            (primeraEntrada!!.mes - 1) * presupuesto
                                                        } else {
                                                            0f
                                                        }
                                                    } else {
                                                        0f
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (agrupados != null) {
                                items(agrupados!!.sortedBy { ag -> ag.first }
                                    .chunked(3)) { agrupadosDos ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        agrupadosDos.forEach { agrupados ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(8.dp)
                                            ) {
                                                ItemView(
                                                    agrupados = agrupados.second,
                                                    titulo = if (mesAct != null)
                                                        context.getString(R.string.dia) + " ${agrupados.first}"
                                                    else if (annoAct != null)
                                                        Month.of(agrupados.first).getDisplayName(
                                                            java.time.format.TextStyle.SHORT_STANDALONE,
                                                            context.resources.configuration.getLocales()
                                                                .get(0)
                                                        )
                                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                    else
                                                        context.getString(R.string.year) + " ${agrupados.first}",
                                                    presupuesto = if (mesAct != null) presupuesto / Month.of(
                                                        mesAct!!
                                                    ).length(
                                                        isLeapYear(annoAct!!)
                                                    ) else if (annoAct != null) presupuesto else presupuesto * 12f,
                                                    onDetalles = {
                                                        if (agrupados.second.isNotEmpty()) {
                                                            showDetalles = true
                                                            detallesConsulta =
                                                                agrupados
                                                        }
                                                    },
                                                    onEnter = {
                                                        if (mesAct != null) {
                                                            changeFecha(
                                                                annoAct,
                                                                mesAct,
                                                                agrupados.first
                                                            )
                                                        } else {
                                                            if (annoAct != null) {
                                                                changeFecha(
                                                                    annoAct,
                                                                    agrupados.first,
                                                                    null
                                                                )
                                                            } else {
                                                                changeFecha(
                                                                    agrupados.first,
                                                                    null,
                                                                    null
                                                                )
                                                            }
                                                        }
                                                    },
                                                    recortar = if (annoAct == null && primeraEntrada != null) {
                                                        if (agrupados.first == primeraEntrada!!.anno) {
                                                            (primeraEntrada!!.mes - 1) * presupuesto
                                                        } else {
                                                            0f
                                                        }
                                                    } else {
                                                        0f
                                                    }
                                                )
                                            }
                                            for (i in 0 until (3 - agrupadosDos.size)) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(8.dp)
                                                ) { /* Empty Box to fill the space */ }
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

@Composable
fun graficas(agrupados: List<Pair<Int, List<Agrupado>>>, entradasCant: Int=0, presupuesto: Double=1000.0, onDetalles: (Pair<Int, List<Agrupado>>) -> Unit){
    val textMeasurer = rememberTextMeasurer()
    val drawnRects = remember { mutableStateListOf<Pair<Rect, Int>>() }
    Box(modifier = Modifier
        .padding(20.dp)
        .fillMaxWidth()
        .height(200.dp)
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                val match = drawnRects.firstOrNull { (rect, _) ->
                    rect.contains(offset)
                }
                match?.let { (_, dato) ->
                    val detalles = agrupados.firstOrNull { it.first == dato }
                    if (detalles != null) {
                        onDetalles(detalles)
                    }
                }
            }
        }) {
        val lineasColor = MaterialTheme.colorScheme.onBackground
        val fondoColor = MaterialTheme.colorScheme.background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawnRects.clear()
            val height = this.size.height - 50f
            val width = this.size.width - 100f
            val maximo =
                max(presupuesto, if(agrupados.isNotEmpty()) agrupados.maxOf { ag -> ag.second.sumOf { ags -> ags.total } } else 0.0)
            val factorHeight = height / maximo
            val minimoId = agrupados.minOfOrNull { ag -> ag.first }
            val maximoId = agrupados.maxOfOrNull { ag -> ag.first }
            val entradas =
                if ((minimoId ?: 0) > 100) ((maximoId ?: 0) - (minimoId ?: 0)) + 1 else entradasCant
            val factorWidth = width / ((entradas * 2) + 1)
            val indices = if ((minimoId ?: 0) > 100) {
                (minimoId ?: 0)..(maximoId ?: 0) + 1
            } else {
                1..entradasCant
            }
            for (i in 1..10) {
                drawLine(
                    lineasColor.copy(alpha = 0.5f),
                    Offset(100f, height - (((maximo * i) / 10) * factorHeight).toFloat()),
                    Offset(
                        this.size.width,
                        height - (((maximo * i) / 10) * factorHeight).toFloat()
                    ),
                    strokeWidth = 2f
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = textoGraficas((maximo.toFloat() * i) / 10),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = lineasColor
                    ),
                    topLeft = Offset(
                        x = 0f,
                        y = height - (((maximo * i) / 10) * factorHeight).toFloat()
                    )
                )
            }
            drawLine(
                myGreen,
                Offset(100f, height - (presupuesto * factorHeight).toFloat()),
                Offset(this.size.width, height - (presupuesto * factorHeight).toFloat()),
                strokeWidth = 5f
            )
            drawText(
                textMeasurer = textMeasurer,
                text = textoGraficas(presupuesto.toFloat()),
                style = TextStyle(
                    fontSize = 10.sp,
                    color = myGreen,
                    background = fondoColor
                ),
                topLeft = Offset(
                    x = 0f,
                    y = height - (presupuesto * factorHeight).toFloat()
                )
            )
            indices.forEachIndexed { index, ind ->
                val entradasF = agrupados.firstOrNull { ag -> ag.first == ind }?.second
                drawnRects.add(Pair(
                    Rect(Offset(((index * 2) + 1) * factorWidth + 100f, 0f), Size(factorWidth, this.size.height)),
                    ind))
                if (entradasF != null) {
                    var altura = height
                    entradasF.forEach { en ->
                        val alturaFin = altura - (en.total * factorHeight).toFloat()
                        drawRect(
                            en.color(),
                            Offset(((index * 2) + 1) * factorWidth + 100f, alturaFin),
                            Size(factorWidth, (en.total * factorHeight).toFloat())
                        )
                        altura = alturaFin
                    }
                    drawText(
                        textMeasurer = textMeasurer,
                        text = if (ind < 100) DecimalFormat("00").format(ind) else "$ind",
                        style = TextStyle(
                            fontSize = if (entradasCant > 20) 6.sp else 12.sp,
                            color = lineasColor
                        ),
                        topLeft = Offset(
                            x = if (ind < 100) ((index * 2) + 1) * factorWidth + 100f else ((index * 2) + 1.3f) * factorWidth + 100f,
                            y = height + 5f
                        )
                    )
                }
            }
            drawLine(
                lineasColor,
                Offset(100f, height),
                Offset(this.size.width, height),
                strokeWidth = 5f
            )
            drawLine(lineasColor, Offset(100f, 0f), Offset(100f, height), strokeWidth = 5f)
        }
    }
}

@Composable
fun ItemView(titulo: String, agrupados: List<Agrupado>, presupuesto: Float, onDetalles: ()-> Unit, onEnter: () -> Unit, recortar: Float){
    val totalCoste = agrupados.sumOf { ag -> ag.total }
    var inicio = 90f
    OutlinedCard(onClick = { onEnter() }, border = BorderStroke(1.dp, if(totalCoste > presupuesto) myRed else MaterialTheme.colorScheme.onBackground)) {
        Column(modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally){
            Text(text = titulo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 5.dp),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(10.dp))
            Canvas(
                modifier = Modifier
                    .width(80.dp)
                    .height(80.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                onDetalles()
                            },
                            onTap = {
                                onEnter()
                            }
                        )
                    }
            ) {
                drawArc(Color.Gray, 0f, 360f, true)
                agrupados.filter { it2 -> it2.total > 0 }
                    .forEach {
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
            Text(text=DecimalFormat("0.00€").format(totalCoste), modifier = Modifier
                .padding(0.dp, 10.dp, 0.dp, 0.dp)
                .fillMaxWidth(), textAlign = TextAlign.Center, style = TextStyle(fontWeight = FontWeight.Bold)
            )
            Text(text=if(totalCoste > (presupuesto-recortar)) DecimalFormat("0.00€").format((presupuesto-recortar) - totalCoste) else "+${DecimalFormat("0.00€").format((presupuesto-recortar) - totalCoste)}", modifier = Modifier
                .fillMaxWidth(), textAlign = TextAlign.Center,
                color = if(totalCoste > (presupuesto-recortar)) myRed else myGreen)
        }
    }
}