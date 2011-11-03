package syndor.model

import org.bson.types.ObjectId

import scala.collection.JavaConverters._

import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao.SalatDAO

import com.mongodb.casbah.MongoConnection

import com.sun.syndication.feed.synd.SyndEntry
import com.sun.syndication.feed.synd.SyndCategory

case class FeedItem(
    @Key("_id") id: ObjectId = new ObjectId,
    feedId: ObjectId,
    uri: String, 
    link: String, 
    title: String, 
    description: String,
    publishedDate: Long,
    categories: List[String] = List.empty[String])
    
object FeedItem extends SalatDAO[FeedItem, ObjectId](collection = MongoConfig.collection("feed_item")) {
  def make(feed: Feed, syndEntry: SyndEntry): FeedItem = {
    return FeedItem(
      feedId = feed.id,
      uri = syndEntry.getUri,
      link = syndEntry.getLink,
      title = syndEntry.getTitle,
      description = syndEntry.getDescription.getValue,
      publishedDate = Option(syndEntry.getPublishedDate) match {
        case Some(date) => date.getTime
        case _ => System.currentTimeMillis
      },
      categories = syndEntry.getCategories.asInstanceOf[java.util.List[SyndCategory]].asScala map { _.getName } toList
    )
  }
}