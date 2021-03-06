package com.jason.taobao.core

import com.jason.core.DefaultSinkProvider
import com.jason.taobao.model.TaoBaoEntity
import com.jason.taobao.model.TaoBaoEntityJsonProtocol._
import com.jason.utils.ElementReflectUtil
import spray.json._

class TaoBaoSinkProvider(args: TaoBaoBackfillerArgs) extends DefaultSinkProvider(args) {
  type EntityTpe = TaoBaoEntity

  override def toJSONOutput(entities: Seq[TaoBaoEntity]): Seq[String] =
    entities map {_.toJson.prettyPrint}

  override def toXMLOutput(entities: Seq[TaoBaoEntity]): Seq[String] = ElementReflectUtil.toXML(entities)

  override def toCSVOutput(entities: Seq[TaoBaoEntity]): Seq[String] = ElementReflectUtil.toCSV(entities)

}
