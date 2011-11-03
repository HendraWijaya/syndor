package syndor.feedbot

import java.util.Date

import syndor.model.Feed
import com.ning.http.util.DateUtil
import com.sun.syndication.feed.synd.SyndFeed

import FeedFetcher.FetchFeedRequest
import FeedFetcher.Request
import UrlFetcher.FetchUrlRequest
import UrlFetcher.FetchUrlResponse
import akka.actor.Actor.actorOf
import akka.actor.actorRef2Scala
import akka.actor.scala2ActorRef
import akka.actor.Actor
import akka.actor.ActorRef
import akka.config.Supervision.OneForOneStrategy
import akka.config.Supervision.Permanent
import akka.dispatch.DefaultCompletableFuture
import akka.dispatch.Future
import akka.event.EventHandler
import akka.util.duration.intToDurationInt

object FeedFetcher {
  sealed trait Request
  case class FetchFeedRequest(feed: Feed) extends Request

  sealed trait Response
  case class FetchFeedResponse(syndFeed: Option[SyndFeed], etag: Option[String], lastModified: Option[Long]) extends Response

  import UrlFetcher._
  
  private def fetchUrl(url: String, headers: Map[String, String], urlFetcher: ActorRef): Future[FetchUrlResponse] = {
    // Ok we need a larger timeout here because it's making an HTTP request
    implicit val timeout = Actor.Timeout(FeedBotConfig.fetcherTimeoutInMillis millis)
    (urlFetcher ? FetchUrlRequest(url, headers)).asInstanceOf[Future[FetchUrlResponse]]
  }

  private def fetchFeed(feed: Feed, urlFetcher: ActorRef, feedParser: ActorRef): Future[FetchFeedResponse] = {

    val headers = collection.mutable.Map.empty[String, String]

    // Ask for conditional get if it's been modified
    feed.etag.foreach(headers += "If-None-Match" -> _)
    feed.lastModified.foreach { timeInMillis =>
      headers += "If-Modified-Since" -> DateUtil.formatDate(new Date(timeInMillis))
    }

    val response = new DefaultCompletableFuture[FetchFeedResponse]
    
    fetchUrl(feed.url, headers.toMap, urlFetcher) map {

      case FetchUrlResponse(status, headers, stream) if status == 200 =>
        val parsed = feedParser ? FeedParser.ParseFeedRequest(headers.get("Content-Type"), stream)
        parsed map {
          case FeedParser.ParseFeedResponse(syndFeed) =>
            response.completeWithResult(FetchFeedResponse(Some(syndFeed), headers.get("Etag"), parseLastModified(headers.get("Last-Modified"))))
        } onException {
          case e =>
            EventHandler.error(this, "Exception parsing '" + feed.url + "': " + e.getClass.getSimpleName + ": " + e.getMessage)
            response.completeWithException(e)
        }

      case FetchUrlResponse(status, headers, stream) if status == 304 =>
        //EventHandler.info(this, "Not modified for %s".format(feed.url))
        response.completeWithResult(FetchFeedResponse(None, None, None))

      case FetchUrlResponse(status, _, _) =>
        response.completeWithException(new IllegalStateException("Failed to fetch, illegal status: " + status))

      case whatever =>
        response.completeWithException(throw new IllegalStateException("Unexpected reply from url fetcher: " + whatever))

    } onException {

      case e =>
        EventHandler.error(this, "Exception fetching '" + feed.url + "': " + e.getClass.getSimpleName + ": " + e.getMessage)
        response.completeWithException(e)

    } onTimeout { _ =>
        EventHandler.warning(this, "UrlFetcher timed out in %s ms while fetching %s".format(FeedBotConfig.fetcherTimeoutInMillis, feed.url))
    }
    
    response
  }
  
  private def parseLastModified(lastModified: Option[String]): Option[Long] = {
    lastModified map {
      DateUtil.parseDate(_).getTime()
    }
  }
}

class FeedFetcher extends Actor {
  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 10, 100)
  
  import FeedFetcher._

  private val urlFetcher = actorOf[UrlFetcher]
  private val feedParser = actorOf[FeedParser]
  
  override def preStart() = {
    self.startLink(urlFetcher)
    self.startLink(feedParser)
  }

  override def postStop() = {
    self.unlink(urlFetcher)
    self.unlink(feedParser)
    
    urlFetcher.stop
    feedParser.stop
  }
  
  override def preRestart(err: Throwable) = {
     postStop()
  }
  
  override def receive = {
    case request: Request => request match {
      case FetchFeedRequest(feed) =>
        self.channel.replyWith(FeedFetcher.fetchFeed(feed, urlFetcher, feedParser))
    }
  }
}

