package main.scala.actor

import java.util.concurrent.TimeUnit

import akka.actor._
import com.codahale.metrics.{Clock, Counter, MetricRegistry, Timer}
import main.scala.actor.Statistic.{RecordFilterTime, RecordSourceFailure}
import main.scala.model.Table


object Statistic {

  case object SystemStart

  case object RecordSlice
  case class RecordSliceTime(time: Long)

  case object RecordSource
  case object RecordSourceFailure
  case class RecordSourceTime(time: Long)

  case class RecordFilter(originSize: Long, filterSize: Long)
  case object RecordFilterFailure
  case class RecordFilterTime(time: Long)

  case object RecordConvert
  case object RecordConvertFailure
  case class RecordConvertTime(time: Long)

  case class RecordInsert(num: Int)
  case class RecordFlush(time: Long)

  case object Print

  def props = Props(new Statistic)
}

class Statistic extends Actor with ActorLogging {

  import Statistic._

  val registry = new MetricRegistry()
  val name = MetricRegistry.name(classOf[Statistic], _: String)

  var systemStartTime = 0L
  val clock = Clock.defaultClock

  val sliceTime = new Timer()
  registry.register(name("sliceT"), sliceTime)
  val sliceCount = new Counter()
  registry.register(name("sliceC"), sliceCount)

  val sourceTime = new Timer()
  registry.register(name("sourceT"), sourceTime)
  val sourceCount = new Counter()
  registry.register(name("sourceC"), sourceCount)
  val sourceFailureCount = new Counter()
  registry.register(name("sourceFailureC"), sourceFailureCount)

  val filterTime = new Timer()
  registry.register(name("filterT"), filterTime)
  val filterCountOriginal = new Counter()
  registry.register(name("filterCountOriginal"), filterCountOriginal)
  val filterCountAfter = new Counter()
  registry.register(name("filterCountAfter"), filterCountAfter)
  val filterFailureCount = new Counter()
  registry.register(name("filterFailureCount"), filterFailureCount)

  val convertTime = new Timer()
  registry.register(name("convertT"), convertTime)
  val convertCount = new Counter()
  registry.register(name("convertC"), convertCount)
  val convertFailureCount = new Counter()
  registry.register(name("convertFailureC"), convertFailureCount)

  val flushTime = new Timer()
  registry.register(name("flushT"), flushTime)
  val sinkCount = new Counter()
  registry.register(name("sinkC"), sinkCount)

  def receive = {

    case SystemStart =>
      systemStartTime = clock.getTick

    case RecordSlice =>
      sliceCount.inc()

    case RecordSliceTime(t) =>
      sliceTime.update(t, TimeUnit.NANOSECONDS)

    case RecordSource =>
      sourceCount.inc()

    case RecordSourceFailure =>
       sourceFailureCount.inc()

    case RecordSourceTime(t) =>
      sourceTime.update(t, TimeUnit.NANOSECONDS)

    case RecordFilter(os, fs) =>
      filterCountOriginal.inc(os)
      filterCountAfter.inc(fs)

    case RecordFilterFailure =>
      filterFailureCount.inc()

    case RecordFilterTime(t) =>
      filterTime.update(t, TimeUnit.NANOSECONDS)

    case RecordConvert =>
      convertCount.inc()

    case RecordConvertFailure =>
      convertFailureCount.inc()

    case RecordConvertTime(t) =>
      convertTime.update(t, TimeUnit.NANOSECONDS)

    case RecordFlush(t) =>
      flushTime.update(t, TimeUnit.NANOSECONDS)

    case RecordInsert(num) =>
      sinkCount.inc(num)

    case Print =>
      val elapsed = toMillis(clock.getTick - systemStartTime)
      println(s">>>>>>>>>>>>>>>  elapsed so far: $elapsed ")
      printTable()
  }

  def printTable() = {
    val sliceParam = PrintParam("slice", sliceTime, sliceCount.getCount, sliceCount.getCount, 0)
    val sourceParam = PrintParam("source", sourceTime, sourceCount.getCount, sourceCount.getCount, sourceFailureCount.getCount)
    val filterParam = PrintParam("filter", filterTime, filterCountOriginal.getCount, filterCountAfter.getCount, filterFailureCount.getCount)
    val convertParam = PrintParam("convert", convertTime, convertCount.getCount, convertCount.getCount, convertFailureCount.getCount)
    val sinkParam = PrintParam("sink", flushTime, sinkCount.getCount, sinkCount.getCount, 0)

    val table = buildTable(Seq(sliceParam,sourceParam,filterParam,convertParam,sinkParam))
    table.print()
  }

  def buildTable(params: Seq[PrintParam]): Table = {
    val head = Seq("phase",
      "before",
      "after",
      "failure",
      "MeanRate",
      "OneMinuteRate",
      "FiveMinuteRate",
      "FifteenMinuteRate",
      "Max",
      "Mean",
      "Min",
      "75thPercentile",
      "95thPercentile",
      "98thPercentile",
      "99thPercentile")

    val table = Table(head)

    params.foreach (param => {
      val timer = param.timer
      val s = timer.getSnapshot
      table.addRow(Seq(param.phase,
        param.phaseStart,
        param.phaseEnd,
        param.phaseFailure,
        timer.getMeanRate,
        timer.getOneMinuteRate,
        timer.getFiveMinuteRate,
        timer.getFifteenMinuteRate,
        toMillis(s.getMax),
        toMillis(s.getMean.toLong),
        toMillis(s.getMin),
        toMillis(s.get75thPercentile().toLong),
        toMillis(s.get95thPercentile().toLong),
        toMillis(s.get98thPercentile().toLong),
        toMillis(s.get99thPercentile().toLong)
      ).map(_.toString))
    })

    table
  }

  def toMillis(nanoseconds: Long): String = {
    nanoseconds / 1000 / 1000 +" ms"
  }
}

case class PrintParam(phase: String, timer: Timer, phaseStart: Long, phaseEnd: Long, phaseFailure: Long)

