package main.scala.taobao

import main.scala.core.DefaultSinkProvider
import main.scala.model.TaoBaoCSVEntity
import spray.json._
import main.scala.model.TaoBaoCSVJsonProtocol._

/**
  * Created by Administrator on 2016-07-18.
  */
class TaoBaoSinkProvider(args: TaoBaoBackfillerArgs) extends DefaultSinkProvider(args) {
  type T = TaoBaoCSVEntity

  override def toJSONOutput(entities: Seq[TaoBaoCSVEntity]): String =
    entities.toJson.prettyPrint


}
