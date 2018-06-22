
package ca

import scala.collection.JavaConverters._
import com.sun.jna.{Library, Native, Platform, Structure}

import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SQLContext
import org.apache.spark.SparkContext
import org.apache.spark.SparkFiles


trait EntryPoints extends Library {
  def mysum(x: Int, y: Int): Int

  def mymultiply(x: Double, y: Double): Double

  def myarray(x: Array[Double], array_size: Int): Unit

  def cos(angle: Double): Double
}

object Libraries {
  def sum = Native.loadLibrary("sum", classOf[EntryPoints]).asInstanceOf[EntryPoints]
  def mul = Native.loadLibrary("mul", classOf[EntryPoints]).asInstanceOf[EntryPoints]
  def m = Native.loadLibrary("m", classOf[EntryPoints]).asInstanceOf[EntryPoints]
}

// Building loader for the two libraries
object LibraryLoader {
  lazy val loadsum = {
    System.load(SparkFiles.get("libsum.so"))
  }
  lazy val loadmul = {
    System.load(SparkFiles.get("libmul.so"))
  }
}

object HelloWorld {
  def time[R](text: String, block: => R, loops: Int = 1): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()

    var dt:Double = ((t1 - t0)/loops.toDouble).asInstanceOf[Double] / (1000.0*1000.0*1000.0)

    val unit = "s"

    println("\n" + text + "> Elapsed time:" + " " + dt + " " + unit)

    result
  }

  def test_Spark = {

    val cores = 100
    val conf = new SparkConf().setMaster("local[*]").setAppName("TSpark").
      set("spark.cores.max", s"$cores").
      set("spark.executor.memory", "200g")


    println("===== Launch a Spark pipeline that calls C functions via JNA")

    val nil: List[Double] = Nil

    val sc = new SparkContext(conf)
    val l = sc.parallelize((1 to 10)).map(x => {
      LibraryLoader.loadsum; Libraries.sum.mysum(x, 12)
    }).
      map(x => {
        LibraryLoader.loadmul; Libraries.mul.mymultiply(x.toDouble, 0.5)
      }).
      aggregate(nil)((x, y) => y :: x, (x, y) => y ::: x).toArray
    println(l.mkString(" "))

    println("===== Call a C function that modifies a Scala array")

    Libraries.mul.myarray(l, l.length)
    println(l.mkString(" "))
  }

  def main(args: Array[String]) {
    println("HelloWorld")

    println("===== Calling simple functions with numeric scalars")
    val r1 = Libraries.sum.mysum(1, 2)
    val r2 = Libraries.mul.mymultiply(1.111, 2.222)
    println("r1 = " + r1.toString + " r2 = " + r2.toString)

    println("===== Comparing overhead from Scala versus C")

    time("scala cos", {
      for (i <- 0 to 100000)
      {
        val angle = 12.0
        math.cos(angle)
      }
    }, 100000)


    time("C cos", {
      for (i <- 0 to 100000)
      {
        val angle = 12.0
        Libraries.m.cos(angle)
      }
    }, 100000)

    if (false) test_Spark

    println("===== Call a C function that modifies a large Scala array")

    val rand = scala.util.Random

    // val a = (for (i <- 1 to 10*1000*1000) yield rand.nextDouble).toArray

    val megas = 1
    val a = (for (i <- 1 to megas * 1000 * 1000) yield rand.nextDouble).toArray

    var result: Double = 0.0

    val iterations = 100
    time("Copy Large array", {
      for (i <- 0 to iterations) {
        val b = a.clone()
        val before = b.sum

        // println(s"Apply function to an array: sum $before")
        result = before
      }
      result
    }, iterations)

    println(s"Copy Large array: $result")

    time("Using large array in Scala", {
      result = 0.0
      for (i <- 0 to iterations) {
      val b = a.clone()
      val before = b.sum
      val after = b.map(_ * 2.0).sum

      //println (s"Apply function to an array: sum $before $after")
        result += after
      }
      result
    }, iterations)

    println (s"Apply function to an array using Scala map: $result")

    time("Using large array from C", {
      result = 0.0
      for (i <- 0 to iterations) {
      val b = a.clone()
      val before = b.sum
      Libraries.mul.myarray(b, b.length)
      val after = b.sum

      //println (s"Apply function to an array: sum $before $after")
        result += after
      }
      result
    }, iterations)

    println (s"Apply function to an array calling a C function: $result")

  }
}

