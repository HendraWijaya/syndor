package syndor.feedbot

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._
import com.mongodb.casbah.Imports._
import com.sun.syndication.feed.synd.SyndEntry
import com.sun.syndication.feed.synd.SyndFeed
import akka.actor.Actor._
import akka.actor.Actor
import akka.actor.Scheduler
import akka.config.Supervision.Permanent
import akka.util.duration._
import syndor.model.FeedItem
import syndor.model.Feed
import akka.event.EventHandler
import akka.config.Supervision.OneForOneStrategy
import akka.dispatch.DefaultCompletableFuture
import akka.dispatch.Future

object FeedUpdater {
  sealed trait Request
  case class UpdateRequest(feed: Feed) extends Request
  
  sealed trait Response
  case class UpdateResponse(feed: Feed) extends Response
}

class FeedUpdater extends Actor {
  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 10, 100)

  import FeedUpdater._

  private val feedFetcher = actorOf[FeedFetcher]

  override def preStart {
    self.startLink(feedFetcher)
  }

  /*
   * This is not called when it crashes
   */
  override def postStop() = {
    self.unlink(feedFetcher)
    feedFetcher.stop
  }

  /*
   * This will be called upon crashing on old actor
   * before restarting a new one
   */
  override def preRestart(err: Throwable) = {
    postStop()
  }

  override def receive = {
    case request: Request => request match {
      case UpdateRequest(feed: Feed) =>
         self.channel.replyWith(updateFeed(feed))
    }
  }
  
  private def updateFeed(feed: Feed): Future[UpdateResponse] = {
    implicit val timeout = Actor.Timeout(FeedBotConfig.fetcherTimeoutInMillis millis)

    val fetchFeedResponse = feedFetcher ? FeedFetcher.FetchFeedRequest(feed)

    val response = new DefaultCompletableFuture[UpdateResponse]
    
    fetchFeedResponse onResult {
      case FeedFetcher.FetchFeedResponse(Some(syndFeed), etag, lastModified) =>
        onFeedModified(feed, syndFeed, etag, lastModified)
        response.completeWithResult(UpdateResponse(feed))
      case FeedFetcher.FetchFeedResponse(None, _, _) =>
        onFeedNotModified(feed)
        response.completeWithResult(UpdateResponse(feed))
      case whatever =>
        val message = "Fetch feed results in invalid response".format(whatever)
        EventHandler.warning(this, message)
        onFeedFetchedFailure(feed, message)
        response.completeWithException(new IllegalStateException("Unexpected reply from feed fetcher: " + whatever))
    } onException {
      case e =>
        val message = "Fetching feed %s results in exception %s".format(feed.url, e)
        EventHandler.error(this, message)
        onFeedFetchedFailure(feed, message)
        response.completeWithException(e)
    } onTimeout { _ =>
      val message = "Timeout while fetching %s".format(feed.url)
      EventHandler.warning(this, message)
      onFeedFetchedFailure(feed, message)
    }
    
    response
  }

  private def onFeedModified(feed: Feed, syndFeed: SyndFeed, etag: Option[String] = None, lastModified: Option[Long] = None) {
    Feed.finishFetchingModified(feed, syndFeed, etag, lastModified)
  }

  private def onFeedNotModified(feed: Feed) {
    Feed.finishFetchingNotModified(feed)
  }

  private def onFeedFetchedFailure(feed: Feed, message: String) {
    Feed.finishFetchingWithFailure(feed, message)
  }
}