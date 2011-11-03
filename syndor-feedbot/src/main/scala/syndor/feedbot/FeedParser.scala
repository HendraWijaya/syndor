package syndor.feedbot

import java.io.StringReader
import com.sun.syndication.feed.synd.SyndEntry
import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.io.SyndFeedInput
import com.sun.syndication.io.XmlReader
import akka.actor.Actor.actorOf
import akka.actor.Actor
import akka.config.Supervision.Permanent
import syndor.model.{ FeedItem, Feed }
import java.io.InputStream
import org.xml.sax.InputSource
import java.io.FilterInputStream
import java.io.FilterReader
import java.io.Reader

import syndor.xml.XmlFilterReader

object FeedParser {
  sealed trait Request
  case class ParseFeedRequest(contentType: Option[String], stream: InputStream) extends Request

  sealed trait Response
  case class ParseFeedResponse(syndFeed: SyndFeed) extends Response

  private def buildSyndFeed(contentType: Option[String], stream: InputStream): SyndFeed = {

    var xmlReader = contentType match {
      case Some(x) => new XmlReader(stream, x, true)
      case None => new XmlReader(stream, true)
    }

    val filteredStream = new XmlFilterReader(xmlReader)

    val feedInput = new SyndFeedInput()
    feedInput.setXmlHealerOn(true)
    feedInput.build(filteredStream)
  }

  private def parseFeed(contentType: Option[String], stream: InputStream): ParseFeedResponse = {
    ParseFeedResponse(buildSyndFeed(contentType, stream))
  }
}

class FeedParser
  extends Actor
  with CpuBoundActorPool {

  import FeedParser._
  
  self.lifeCycle = Permanent

  override def instance = actorOf(new Worker)

  override def receive = _route

  private class Worker extends Actor {

    override def receive = {
      case request: Request => request match {
        case ParseFeedRequest(contentType, stream) =>
          self.tryReply(FeedParser.parseFeed(contentType, stream))
      }
    }
  }
}