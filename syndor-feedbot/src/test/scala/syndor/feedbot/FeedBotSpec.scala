package syndor.feedbot

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import syndor.model.Source
import syndor.EnvironmentSupport._
import akka.actor.Actor.actorOf
import syndor.model.FeedItem
import syndor.model.Feed
import com.mongodb.casbah.commons.MongoDBObject
import java.text.SimpleDateFormat

class FeedBotSpec extends FlatSpec
  with ShouldMatchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with TestFeedSupport
  with TestHttpServerSupport {
  
  private val feedBot = actorOf(new FeedBot(nrOfFeeds = 10, maxNrOfFetching = 100))
  
  override def beforeAll() {
    feedBot.start()
    super.beforeAll()
  }

  override def afterAll() {
    try {
       super.afterAll()
    } finally {
       feedBot.stop()
    }
  }
  
  behavior of "feed bot"

  it should "update feeds concurrently" in {
    Feed.collection.count should be(0)
    
    // Insert 3 feeds
    insertFeed(httpServer.resolve("/resource/reuters_2011_10_23.xml").toExternalForm()) // 10 items
    insertFeed(httpServer.resolve("/resource/tempo_fokus_2011_10_29.xml").toExternalForm()) // 15 items
    insertFeed(httpServer.resolve("/resource/techcrunch_2011_10_31.xml").toExternalForm()) // 20 items

    Feed.collection.count should be(3)
    FeedItem.collection.count should be(0)

    feedBot ! FeedBot.UpdateFeeds

    // Sleep as long as timeout to make sure it has finished and add 100 to make sure data
    // are written to mongo
    Thread.sleep(FeedBotConfig.fetcherTimeoutInMillis + 100)

    FeedItem.collection.count should be(45)

    val title = "Spain, Italy under pressure as EU frames bank deal"
    val cursor = FeedItem.find(ref = MongoDBObject("title" -> title))
    val item = cursor.next
    item.title should be(title)
    item.publishedDate should be (new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").parse("Sun, 23 Oct 2011 03:42:14 -0400").getTime)
    
    cursor.hasNext should be(false)
  }
  
  def insertFeed(url: String): Feed = {
    val feed = Feed(
      sourceId = source.id,
      title = "Test",
      url = url)

    Feed.insert(feed)

    return feed
  }
}