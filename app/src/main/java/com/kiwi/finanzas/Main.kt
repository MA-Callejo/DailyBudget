package com.kiwi.finanzas

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.kiwi.finanzas.db.DataBase
import com.kiwi.finanzas.ui.NavigationItem
import com.kiwi.finanzas.ui.theme.FinanzasTheme
import com.kiwi.finanzas.ui.views.DialogTutorial
import com.kiwi.finanzas.ui.views.Historico
import com.kiwi.finanzas.ui.views.Home
import com.kiwi.finanzas.ui.views.Settings
import java.time.LocalDateTime

class Main : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge()
        setContent {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            FinanzasTheme {
                Greeting(this)
            }
        }
    }
}

@Composable
fun Greeting(context: Context) {
    val database = DataBase.getDatabase(context)
    val daoEntradas = database.entryDao()
    val daoTipos = database.typeDao()
    val currentTime = LocalDateTime.now()
    var anno: Int? = currentTime.year
    var mes: Int? = currentTime.monthValue
    var dia: Int? = null
    val navigationItems = listOf(
        NavigationItem(
            title = stringResource(R.string.home),
            icon = Icons.Default.Home,
            route = "home"
        ),
        NavigationItem(
            title = stringResource(R.string.historic),
            icon = Icons.Default.DateRange,
            route = "historico"
        ),
        NavigationItem(
            title = stringResource(R.string.setting),
            icon = Icons.Default.Settings,
            route = "settings"
        )
    )
    val selectedNavigationIndex = rememberSaveable {
        mutableIntStateOf(0)
    }
    var tutorialStep by remember { mutableIntStateOf(getPreference(context, "tutorialMain").toInt() % 1000) }
    val showTutorial = tutorialStep < 2
    if (showTutorial){
        DialogTutorial(tutorialStep, onChange = {
            tutorialStep++
            savePreference(context, "tutorialMain", tutorialStep.toFloat())
        }, 0)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = if(tutorialStep != 1) Modifier else Modifier.border(10.dp, MaterialTheme.colorScheme.tertiary)
            ) {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedNavigationIndex.intValue == index,
                        onClick = {
                            selectedNavigationIndex.intValue = index
                        },
                        icon = {
                            Icon(imageVector = item.icon, contentDescription = item.title)
                        },
                        label = {
                            Text(
                                item.title,
                                color = if (index == selectedNavigationIndex.intValue)
                                    MaterialTheme.colorScheme.onBackground
                                else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.surface,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        if(!showTutorial) {
            when (selectedNavigationIndex.intValue) {
                1 -> {
                    Historico(
                        anno,
                        mes,
                        dia,
                        daoEntradas,
                        daoTipos,
                        context,
                        { annoNew, mesNew, diaNew ->
                            anno = annoNew
                            mes = mesNew
                            dia = diaNew
                        },
                        Modifier.padding(innerPadding)
                    )
                }

                2 -> {
                    Settings(daoTipos, context, Modifier.padding(innerPadding))
                }

                else -> {
                    Home(daoEntradas, daoTipos, context, Modifier.padding(innerPadding))
                }
            }
        }
    }
}
