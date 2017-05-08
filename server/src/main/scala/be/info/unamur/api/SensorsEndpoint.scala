package be.info.unamur.api

import java.sql.Timestamp

import be.info.unamur.persistence.entities.Sensor
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{AsyncResult, FutureSupport, ScalatraServlet}
import scala.concurrent.{ExecutionContext, Future}
import scalikejdbc._
import org.json4s.JsonDSL._

/** Api endpoint to retrieve sensors information.
  *
  * @author Noé Picard
  */
class SensorsEndpoint extends ScalatraServlet with JacksonJsonSupport with FutureSupport {
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  override protected implicit def executor: ExecutionContext = ExecutionContext.global


  before() {
    contentType = formats("json")
  }

  post("/") {
    new AsyncResult() {
      override val is = Future {
        (params.get(SensorsEndpoint.NameParamIdentifier),
          params.getAs[Double](SensorsEndpoint.ValueParamIdentifier),
          params.getAs[Double](SensorsEndpoint.GrossValueParamIdentifier)) match {
          case (Some(name), Some(value), Some(grossValue)) => Sensor.create(name, value, grossValue, new Timestamp(System.currentTimeMillis()))
          case (_, _, _) => halt(400, "error" -> "Missing parameter(s)")
        }
      }
    }
  }

  get("/:name") {
    new AsyncResult() {
      override val is = Future {
        Sensor.findLastByName(params(SensorsEndpoint.NameParamIdentifier)) match {
          case Some(s) => s
          case None => halt(400, "error" -> "Sensor not found")
        }
      }
    }
  }

  get("/all/:name") {
    new AsyncResult() {
      override val is = Future {
        params get (SensorsEndpoint.TimeParamIdentifier) match {
          case Some(time) => getSensorsData(params(SensorsEndpoint.NameParamIdentifier), time) match {
            case Nil => halt(400, "error" -> "No data for this sensor or this time")
            case s => params get (SensorsEndpoint.CountParamIdentifier) match {
              case Some("true") => "size" -> s.length
              case _ => s
            }
          }
          case _ => halt(400, "error" -> "Bad time : please use : 'hour' or 'day' or 'month'")

        }
      }
    }
  }

  get("/all/evolution/:name") {
    new AsyncResult() {
      override val is = Future {
        params get (SensorsEndpoint.NameParamIdentifier) match {
          case Some(n) => params get (SensorsEndpoint.PeriodParamIdentifier) match {
            case Some(p) => params get (SensorsEndpoint.TimeParamIdentifier) match {
              case Some(t) => t match {
                case "hour" => ("evolutionValues" -> getValuesArray(n, p.toInt, SensorsEndpoint.HourInMillis), "periods" -> getPeriodsArray(p.toInt, SensorsEndpoint.HourInMillis, t))
                case "day" => ("evolutionValues" -> getValuesArray(n, p.toInt, SensorsEndpoint.DayInMillis), "periods" -> getPeriodsArray(p.toInt, SensorsEndpoint.DayInMillis, t))
                case "month" => ("evolutionValues" -> getValuesArray(n, p.toInt, SensorsEndpoint.MonthInMillis), "periods" -> getPeriodsArray(p.toInt, SensorsEndpoint.MonthInMillis, t))
              }
              case _ => halt(400, "error" -> "Time parameter missing")
            }
            case _ => halt(400, "error" -> "Period parameter missing")
          }
          case _ => halt(400, "error" -> "Sensor name missing")
        }
      }
    }
  }


  def getSensorsData(sensor: String, time: String): List[Sensor] = {
    val currentTime = System.currentTimeMillis()
    val currentTimeHour = new Timestamp(currentTime - SensorsEndpoint.HourInMillis)
    val currentTimeDay = new Timestamp(currentTime - SensorsEndpoint.DayInMillis)
    val currentTimeMonth = new Timestamp(currentTime - SensorsEndpoint.MonthInMillis)
    time match {
      case "hour" => Sensor.findAllBy(sqls"name = ${sensor} and created_at > ${currentTimeHour}")
      case "day" => Sensor.findAllBy(sqls"name = ${sensor} and created_at > ${currentTimeDay}")
      case "month" => Sensor.findAllBy(sqls"name = ${sensor} and created_at > ${currentTimeMonth}")
      case "all" => Sensor.findAllBy(sqls"name = ${sensor}")

    }
  }


  def getValuesArray(sensor: String, period: Integer, interval: Long): Array[Integer] = {
    var upperTime = System.currentTimeMillis()
    var valuesList: Array[Integer] = new Array[Integer](period)
    for (p <- 0 to period - 1) {
      valuesList(p) = Sensor.findAllBy(sqls"name = ${sensor} and created_at > ${new Timestamp(upperTime - interval)} and created_at < ${new Timestamp(upperTime)}").size
      upperTime = upperTime - interval
    }
    valuesList.reverse
  }

  def getPeriodsArray(period: Integer, interval: Long, time: String): Array[String] = {
    var upperTime = System.currentTimeMillis()
    var periodsList: Array[String] = new Array[String](period)
    for (p <- 0 to period - 1) {
      time match {
        case "hour" => periodsList(p) = new Timestamp(upperTime).getHours.toString.concat("h")
        case "day" => periodsList(p) = convertIntToDay(new Timestamp(upperTime))
        case "month" => periodsList(p) = convertIntToMonth(new Timestamp(upperTime))
      }
      upperTime = upperTime - interval
    }
    periodsList.reverse
  }

  def convertIntToMonth(time: Timestamp): String = {
    time.getMonth match {
      case 0 => "January ".concat("20").concat(time.getYear().toString.substring(1))
      case 1 => "February ".concat("20").concat(time.getYear().toString.substring(1))
      case 2 => "March ".concat("20").concat(time.getYear().toString.substring(1))
      case 3 => "April ".concat("20").concat(time.getYear().toString.substring(1))
      case 4 => "May ".concat("20").concat(time.getYear().toString.substring(1))
      case 5 => "June ".concat("20").concat(time.getYear().toString.substring(1))
      case 6 => "July ".concat("20").concat(time.getYear().toString.substring(1))
      case 7 => "August ".concat("20").concat(time.getYear().toString.substring(1))
      case 8 => "September ".concat("20").concat(time.getYear().toString.substring(1))
      case 9 => "October ".concat("20").concat(time.getYear().toString.substring(1))
      case 10 => "November ".concat("20").concat(time.getYear().toString.substring(1))
      case 11 => "December ".concat("20").concat(time.getYear().toString.substring(1))
    }
  }

  def convertIntToDay(time: Timestamp): String = {
    time.getMonth match {
      case 0 => time.getDay.toString.concat(" January ").concat("20").concat(time.getYear().toString.substring(1))
      case 1 => time.getDay.toString.concat(" February ").concat("20").concat(time.getYear().toString.substring(1))
      case 2 => time.getDay.toString.concat(" March ").concat("20").concat(time.getYear().toString.substring(1))
      case 3 => time.getDay.toString.concat(" April ").concat("20").concat(time.getYear().toString.substring(1))
      case 4 => time.getDay.toString.concat(" May ").concat("20").concat(time.getYear().toString.substring(1))
      case 5 => time.getDay.toString.concat(" June ").concat("20").concat(time.getYear().toString.substring(1))
      case 6 => time.getDay.toString.concat(" July ").concat("20").concat(time.getYear().toString.substring(1))
      case 7 => time.getDay.toString.concat(" August ").concat("20").concat(time.getYear().toString.substring(1))
      case 8 => time.getDay.toString.concat(" September ").concat("20").concat(time.getYear().toString.substring(1))
      case 9 => time.getDay.toString.concat(" October ").concat("20").concat(time.getYear().toString.substring(1))
      case 10 => time.getDay.toString.concat(" November ").concat("20").concat(time.getYear().toString.substring(1))
      case 11 => time.getDay.toString.concat(" December ").concat("20").concat(time.getYear().toString.substring(1))
    }
  }

}

object SensorsEndpoint {
  val NameParamIdentifier = "name"
  val CountParamIdentifier = "count"
  val ValueParamIdentifier = "value"
  val TimeParamIdentifier = "time"
  val PeriodParamIdentifier = "periods"
  val GrossValueParamIdentifier = "gross_value"
  val HourInMillis = 3600000
  val DayInMillis = 86400000
  val MonthInMillis = 2678400000L

}
