// Copyright (C) 2017 BarD Software s.r.o
package net.sourceforge.ganttproject

/**
 * This is a smoke-test code which checks that Kotlin compiles and runs
 * in GanttProject build.
 *
 * @author dbarashev@bardsoftware.com
 */
class Greeter(val name : String) {
  fun greet() {
    println("Hello, ${name}");
  }
}

fun main(args : Array<String>) {
  Greeter(if (args.size > 0) args[0] else "Anonymous").greet()
}
