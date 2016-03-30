


package ca.uwaterloo.cs.bigdata2016w.evisoup.assignment5

//import io.bespin.scala.util.Tokenizer


import org.apache.log4j._
import org.apache.hadoop.fs._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.rogach.scallop._
import scala.collection.mutable.Map
// --input TPC-H-0.1-TXT --date '1996-01-01'
// select count(*) from lineitem where l_shipdate = 'YYYY-MM-DD';

class Conf5(args: Seq[String]) extends ScallopConf(args)  {
  mainOptions = Seq(input, date)
  val input = opt[String](descr = "input path", required = true)

  val date = opt[String](descr = "given date", required = true)
}

object Q5 {
  val log = Logger.getLogger(getClass().getName())

  def main(argv: Array[String]) {
    val args = new Conf5(argv)

    log.info("Input: " + args.input())
    log.info(">>>>>>Date: " + args.date())

    val conf = new SparkConf().setAppName("Q3")
    val sc = new SparkContext(conf)
    val date = args.date()

///////////////////////////////////////////////////

    val sFile = sc.textFile(args.input()+"/customer.tbl")
    val cusSet = sFile
      .map(line => {
        val tokens = line.split('|')
            //customer -> (cust key, nation key)
            ( tokens(0).toString ,tokens(3).toString )  
      })
      .filter( p => {
        //usa =24 , canada = 3
         if( p._2.toInt == 24 || p._2.toInt == 3 ){
              true
          }else{
            false
          }
      })
      .collectAsMap

     val cusMap = sc.broadcast( cusSet)
///////////////////////////////////////////////////


    val pFile = sc.textFile(args.input()+"/nation.tbl")
    val natSet = pFile
      .map(line => {
        val tokens = line.split('|')
            //nation -> (nation key, name)
            ( tokens(0).toString ,  tokens(1).toString   )
            
      })
      .filter( p => {
        //usa =24 , canada = 3
         if( p._1.toInt == 24 || p._1.toInt == 3 ){
              true
          }else{
            false
          }
      })
      .collectAsMap

    val natMap = sc.broadcast( natSet )

///////////////////////////////////////////////////

val textFile = sc.textFile(args.input()+"/orders.tbl")
    val firstSet = textFile
      .map(line => {
        val tokens = line.split('|')
            ( tokens(0).toInt, tokens(1) )
            //(orderkey, custkey)
      })

///////////////////////////////////////////////////

    val textFileTwo = sc.textFile(args.input()+"/lineitem.tbl")
    val secSet = textFileTwo
      .map(line => {
        val tokens = line.split('|')

          (tokens(0).toInt,  tokens(10).substring(0,7) )
           //order 0,  date10

      })

      // .filter( p => {

      //    if( p._2 == date.toString() ||
      //        p._2.substring(0,7) == date.toString() ||
      //        p._2.substring(0,4) == date.toString() ){
      //         true
      //     }else{
      //       false
      //     }

      //  })

      .cogroup(firstSet)

      // .filter( p => {
      //    if( p._2._1.toList != Nil ){
      //         true
      //     }else{
      //       false
      //     }
      // })

      .flatMap( x => {
          var one = x._2._1.toList
          var two = x._2._2.toList
          for( i <- 0 to one.length-1 ) yield (x._1 , two(0), one(i).toString() )
          //(orderKey, cusKey, "1995-10" )
      })

/////////////////////////////////////////////////////////


      .filter( x => {
             if(  cusMap.value.contains( x._2.toString() ) &&
                  natMap.value.contains(cusMap.value( x._2.toString() )) ){
                  true
              }else{
                false
              }
          })

                     //natMap -> (nation key, name， "1995-10")
           //cusMap -> (cust key, nation key)
      //x -> (orderKey, cust key , "1995-10")

      .map( x=>{

      ( cusMap.value( x._2.toString() ).toInt,  natMap.value(cusMap.value( x._2.toString() )) , x._3 )
      	
      })

  

///////////////////////////

      
      .map( pair => (pair, 1))
      .reduceByKey(_ + _)
      //.map( x => (x._1._1.toInt, (x._1._2 , x._1._3 , x._2) ))
      .map( x => (x._1._3, (x._1._2 , x._2) ))
      .sortByKey()

      
      .map( line => {
        (  line._1 , line._2._1, line._2._2 )
      })
      .collect()
      .foreach(println)

  }
}