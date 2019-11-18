package com.bardsoftware.constable

import java.io.File

fun main() {
    println("Enter path to sql file with database schema to load")
    val path = readLine()!!
    if (!File(path).exists()) {
        println("File does not exist")
        return
    }
    val h2Manager = H2Manager()
    h2Manager.execute(path)
}