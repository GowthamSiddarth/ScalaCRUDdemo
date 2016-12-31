package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Success, Failure}

import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.api.collections.bson.BSONCollection

import reactivemongo.bson.BSONDocument

class Application extends Controller {

  val mongoUri: String = "mongodb://localhost:27017/"
  val driver: MongoDriver = new MongoDriver()
  val uri = Future.fromTry(MongoConnection.parseURI(mongoUri))
  val futureCon = uri.map(driver.connection(_))

  def dbFromConnection(connection: MongoConnection): Future[BSONCollection] =
    connection.database("somedatabase").map(_.collection("somecollection"))

  def createStud = Action.async(parse.json) { request =>
    val requestBody = request.body
    val rollnum = (requestBody \ "RollNum").as[String]
    val name = (requestBody \ "Name").as[String]
    val age = (requestBody \ "Age").as[Int]

    val doc: BSONDocument = BSONDocument("RollNum" -> rollnum, "Name" -> name, "Age" -> age)


    futureCon onComplete {
      case Success(con) =>
        val futureCol = dbFromConnection(con)
        futureCol onComplete {
          case Success(col) =>
            val writeRes = col.insert(doc)
            writeRes onComplete {
              case Success(res) =>
                println(res)
                Ok("Success")
              case Failure(exception) =>
                exception.printStackTrace()
            }
          case Failure(exception) => exception.printStackTrace()
        }
      case Failure(exception) => exception.printStackTrace()
    }

    Future {
      BadRequest("Failure")
    }
  }

  def removeStud = Action.async(parse.json) { request =>
    val field: String = (request.body \ "RollNum").as[String]
    val selector = BSONDocument("RollNum" -> field)

    futureCon onComplete {
      case Success(con) =>
        val col = dbFromConnection(con)
        col onComplete {
          case Success(res) =>
            val futureRemove = res.remove(selector)
            futureRemove onComplete {
              case Success(res) =>
                Ok("Document Removed")
              case Failure(exception) =>
                exception.printStackTrace
            }
          case Failure(exception) => exception.printStackTrace
        }
      case Failure(exception) => exception.printStackTrace
    }

    Future {
      BadRequest("Failure")
    }
  }

  def updateStud = Action.async(parse.json) { request =>
    val requestBody = request.body
    val oldRollNum = (((requestBody \ "old").as[JsValue]) \ "RollNum").as[String]
    val newJsVal = (requestBody \ "new").as[JsValue]
    val newRollNum = (newJsVal \ "RollNum").as[String]
    val newName = (newJsVal \ "Name").as[String]
    val newAge = (newJsVal \ "Age").as[Int]
    val selector = BSONDocument("RollNum" -> oldRollNum)

    val modifier: BSONDocument = BSONDocument(
      "$set" -> BSONDocument(
        "RollNum" -> newRollNum,
        "Name" -> newName,
        "Age" -> newAge))

    futureCon onComplete {
      case Success(con) =>
        val futureCol = dbFromConnection(con)
        futureCol onComplete {
          case Success(col) =>
            val writeRes = col.update(selector, modifier)
            writeRes onComplete {
              case Success(res) =>
                println(res)
                Ok("Success")
              case Failure(exception) =>
                exception.printStackTrace()
            }
          case Failure(exception) => exception.printStackTrace()
        }
      case Failure(exception) => exception.printStackTrace()
    }

    Future {
      Ok("Success")
    }
  }

  def findStud = Action.async(parse.json) { request =>
    val rollNum = (request.body \ "RollNum").as[String]

    futureCon onComplete {
      case Success(con) =>
        val futureCol = dbFromConnection(con)
        futureCol onComplete {
          case Success(col) =>
            val query = BSONDocument("RollNum" -> BSONDocument("$eq" -> rollNum))
            val writeRes = col.find(query).one[BSONDocument]
            writeRes onComplete {
              case Success(doc) =>
                /*
                val roll = doc.get.getAs[String]("RollNum")
                val name = doc.get.getAs[String]("Name")
                val age = doc.get.getAs[Int]("Age")
                */
                println(doc.get.toMap.toString)
              case Failure(exception) =>
                exception.printStackTrace
            }
          case Failure(exception) => exception.printStackTrace()
        }
      case Failure(exception) => exception.printStackTrace()
    }

    Future {
      BadRequest("Success")
    }
  }
}
