package com.kiwi.finanzas.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntradaDAO {
    @Query("SELECT * FROM entradas")
    fun getAll(): Flow<List<Entrada>>

    @Query("SELECT * FROM entradas order by anno asc, mes asc, dia asc, hora asc, min asc LIMIT 1")
    suspend fun getPrimeraEntrada(): List<Entrada>

    @Query("SELECT * FROM entradas where mes = :mes and anno = :anno order by dia desc, hora desc, min desc, id desc LIMIT :limit OFFSET :skip")
    suspend fun getMesHome(mes: Int, anno: Int, limit: Int, skip: Int): List<Entrada>

    @Insert
    suspend fun insert(entrada: Entrada)

    @Query("DELETE FROM entradas where id = :id")
    suspend fun delete(id: Int)

    @Update
    suspend fun update(entrada: Entrada)

    @Query("SELECT * FROM entradas where anno = :anno")
    fun getAllAnno(anno: Int): Flow<List<Entrada>>

    @Query("SELECT distinct(anno) FROM entradas order by anno desc")
    suspend fun getAnnos(): List<Int>

    @Query("SELECT distinct(mes) FROM entradas where anno = :mes order by mes desc")
    fun getMeses(mes: Int): Flow<List<Int>>
    @Query("SELECT distinct(dia) FROM entradas where anno = :anno and mes = :mes order by dia desc")
    fun getDias(mes: Int, anno: Int): Flow<List<Int>>

    @Query("SELECT * FROM entradas where mes = :mes and anno = :anno order by dia desc, hora desc, min desc")
    fun getAllMes(mes: Int, anno: Int): Flow<List<Entrada>>

    @Query("SELECT * FROM entradas where (((anno-1)*372) + ((mes-1)*31) + dia) >= :dia")
    fun getGastoPeriodo(dia: Int): Flow<List<Entrada>>

    @Query("SELECT tipos.id as tipoId, tipos.red, tipos.green, tipos.blue, tipos.nombre, sum(entradas.cantidad) as total from entradas join tipos on (entradas.tipo = tipos.id) where (((entradas.anno)*10000) + ((entradas.mes)*100) + entradas.dia) between :dia and :dia2 group by tipo order by total desc")
    fun getTotalesPeriodo(dia: Int, dia2: Int): Flow<List<Agrupado>>

    @Query("SELECT * FROM entradas where mes = :mes and anno = :anno and dia = :dia")
    suspend fun getAllDia(mes: Int, dia: Int, anno: Int): List<Entrada>

    @Query("SELECT tipos.id as tipoId, tipos.red, tipos.green, tipos.blue, tipos.nombre, sum(entradas.cantidad) as total from entradas join tipos on (entradas.tipo = tipos.id) where mes = :mes and anno = :anno group by tipo order by total desc")
    fun getTotales(mes: Int, anno: Int): Flow<List<Agrupado>>
    @Query("SELECT tipos.id as tipoId, tipos.red, tipos.green, tipos.blue, tipos.nombre, sum(entradas.cantidad) as total from entradas join tipos on (entradas.tipo = tipos.id) where mes = :mes and anno = :anno group by tipo order by total desc")
    suspend fun getTotalesMes(mes: Int, anno: Int): List<Agrupado>
    @Query("SELECT tipos.id as tipoId, tipos.red, tipos.green, tipos.blue, tipos.nombre, sum(entradas.cantidad) as total from entradas join tipos on (entradas.tipo = tipos.id) where anno = :anno group by tipo order by total desc")
    suspend fun getTotalesAnno(anno: Int): List<Agrupado>
    @Query("SELECT tipos.id as tipoId, tipos.red, tipos.green, tipos.blue, tipos.nombre, sum(entradas.cantidad) as total from entradas join tipos on (entradas.tipo = tipos.id) where mes = :mes and anno = :anno and dia = :dia group by tipo order by total desc")
    suspend fun getTotalesDia(mes: Int, anno: Int, dia: Int): List<Agrupado>

    @Query("SELECT * FROM entradas WHERE concepto LIKE :nombre order by anno desc, mes desc, dia desc, hora desc, min desc")
    fun getAllFiltro(nombre: String): Flow<List<Entrada>>

    @Query("SELECT * FROM entradas where anno = :anno and concepto LIKE :nombre order by mes desc, dia desc, hora desc, min desc")
    fun getAllAnnoFiltro(anno: Int, nombre: String): Flow<List<Entrada>>

    @Query("SELECT * FROM entradas where mes = :mes and anno = :anno and concepto LIKE :nombre order by dia desc, hora desc, min desc")
    fun getAllMesFiltro(mes: Int, anno: Int, nombre: String): Flow<List<Entrada>>

    @Query("SELECT * FROM entradas where mes = :mes and anno = :anno and dia = :dia and concepto LIKE :nombre order by dia desc, hora desc, min desc")
    fun getAllDiaFiltro(mes: Int, dia: Int, anno: Int, nombre: String): Flow<List<Entrada>>
}