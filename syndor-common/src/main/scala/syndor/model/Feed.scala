package syndor.model

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConverters._

import org.bson.types.ObjectId

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao.SalatDAO

import com.mongodb.casbah.Imports._

import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.feed.synd.SyndEntry

case class Feed(
    @Key("_id") id: ObjectId = new ObjectId, 
    sourceId: ObjectId,
    url: String, 
    title: String,
    description: String = "",
    etag: Option[String] = None, 
    lastModified: Option[Long] = None,
    fetchStatus: FetchStatus = FetchStatus())
    
object Feed extends SalatDAO[Feed, ObjectId](collection = MongoConfig.collection("feed")) {
 
  def grabForFetching(size: Int): Seq[Feed] = {
    // Seems like this is the simplest way now and the most stable
    // Although it might be slow.
    // TODO: Look for faster way
    for{ 
      i <- 1 to size
      feed <- grabForFetching
    } yield {
      feed
    }
  }
  
  def grabForFetching: Option[Feed] = {
    return collection.findAndModify(
      query = MongoDBObject("fetchStatus.fetching" -> false),
      fields = MongoDBObject(),
      sort = MongoDBObject(),
      remove = false,
      update = $set("fetchStatus.fetching" -> true, "fetchStatus.fetchStart" -> System.currentTimeMillis),
      returnNew = true,
      upsert = false) map {
      grater[Feed].asObject(_)
    }
  }
  
  def finishFetchingModified(feed: Feed, syndFeed: SyndFeed, etag: Option[String] = None, lastModified: Option[Long] = None) {
    processSyndEntries(feed, syndFeed.getEntries.asInstanceOf[java.util.List[SyndEntry]].asScala)
    
    Feed.save(feed.copy(
      title = syndFeed.getTitle,
      description = syndFeed.getDescription,
      etag = etag orElse feed.etag, // etag might be missing
      lastModified = lastModified orElse feed.lastModified, // lastModified might be missing
      fetchStatus = feed.fetchStatus.copy(success = true, fetching = false, fetchEnd = System.currentTimeMillis)
    ))
  }
  
  def finishFetchingNotModified(feed: Feed) {
    Feed.save(feed.copy(
      fetchStatus = feed.fetchStatus.copy(success = true, fetching = false, fetchEnd = System.currentTimeMillis)
    ))
  }
  
  def finishFetchingWithFailure(feed: Feed, message: String) {
    Feed.save(feed.copy(
      fetchStatus = feed.fetchStatus.copy(success = false, fetching = false, fetchEnd = System.currentTimeMillis, message = Option(message))
    ))
  }
  
  private def processSyndEntries(feed: Feed, entries: Seq[SyndEntry]) {
    entries.foreach { entry =>
      FeedItem.findOne(MongoDBObject("link" -> entry.getLink)) orElse {
        FeedItem.insert(FeedItem.make(feed, entry))
      }
    }
  }
  
  def reset() {
    // Resetting fetching status
    update(MongoDBObject("fetchStatus.fetching" -> true), $set("fetchStatus.fetching" -> false))
  }
}