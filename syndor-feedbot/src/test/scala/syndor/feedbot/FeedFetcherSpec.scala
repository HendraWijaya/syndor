package syndor.feedbot

import scala.collection.JavaConverters._
import akka.actor.Actor.actorOf
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import syndor._
import syndor.model._
import syndor.EnvironmentSupport._
import syndor.feedbot.FeedFetcher._
import com.sun.syndication.feed.synd.SyndEntry

class FeedFetcherSpec extends FlatSpec
  with ShouldMatchers
  with BeforeAndAfterAll
  with TestBootSupport
  with TestHttpServerSupport {

  private val fetcher = actorOf[FeedFetcher]
  private val source = Source(name = "Reuters", url = "http://www.reuters.com")

  override def beforeAll = {
    super.beforeAll
    fetcher.start()
  }

  override def afterAll = {
    super.afterAll
    fetcher.stop()
  }

  behavior of "feed fetcher"

  it should "fetch regular feedburner feed" in {
    val feed = Feed(
      sourceId = source.id,
      title = "Test Feed",
      url = httpServer.resolve("/resource/reuters_2011_10_23.xml").toExternalForm())

    val f = fetcher ? FetchFeedRequest(feed)
    f.get match {
      case FetchFeedResponse(syndFeed, etag, lastModified) =>
        syndFeed should be('defined)

        val entries = syndFeed.get.getEntries.asInstanceOf[java.util.List[SyndEntry]].asScala
        entries.size should be(10)
        entries.foreach(_ should not be (null))

        etag should be(None)
        lastModified should be(None)
      case _ =>
        throw new Exception("Wrong reply message from fetcher")
    }
  }

  it should "fetch feed with illegal XML characters" in {
    // Error on line 36: An invalid XML character (Unicode: 0x1c) was found in the CDATA section.
    val feed = Feed(
      sourceId = source.id,
      title = "Test Feed",
      url = httpServer.resolve("/resource/tempo_fokus_2011_10_29.xml").toExternalForm())

    val f = fetcher ? FetchFeedRequest(feed)
    f.get match {
      case FetchFeedResponse(syndFeed, etag, lastModified) =>
        syndFeed should be('defined)

        val entries = syndFeed.get.getEntries.asInstanceOf[java.util.List[SyndEntry]].asScala
        entries.size should be(15)
        entries.foreach(_ should not be (null))

        // This item contains illegal char
        entries(1).getDescription.getValue should be("  <p>Siapapun yang terlibat harus dibongkar.</p> ")

        etag should be(None)
        lastModified should be(None)
      case _ =>
        throw new Exception("Wrong reply message from fetcher")
    }
  }

  it should "fail when fetching non existent resource" in {
    val feed = Feed(
      sourceId = source.id,
      title = "Test Feed",
      url = httpServer.resolve("/resource/non_existent.xml").toExternalForm())

    evaluating {
      (fetcher ? FetchFeedRequest(feed)).await.get
    } should produce[IllegalStateException] // Should get here due to 404
  }

  it should "handle conditional get properly" in {
    val feed = Feed(
      sourceId = source.id,
      title = "Test Feed",
      etag = Some("abcd"),
      lastModified = Some(System.currentTimeMillis),
      url = httpServer.resolve("/resource/reuters_2011_10_23.xml").toExternalForm())

    val f = fetcher ? FetchFeedRequest(feed)
    f.get match {
      case FetchFeedResponse(syndFeed, etag, lastModified) =>
        syndFeed should be(None)
        etag should be(None)
        lastModified should be(None)
      case _ =>
        throw new Exception("Wrong reply message from fetcher")
    }
  }
}