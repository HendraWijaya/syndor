package syndor.model

import org.bson.types.ObjectId

import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao.SalatDAO

import com.mongodb.casbah.MongoConnection

case class Source (
    @Key("_id") id: ObjectId = new ObjectId, 
    name: String, 
    url: String)
    
object Source extends SalatDAO[Source, ObjectId](collection = MongoConfig.collection("source"))