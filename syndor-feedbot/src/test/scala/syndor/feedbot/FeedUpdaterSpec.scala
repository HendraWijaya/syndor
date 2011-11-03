package syndor.feedbot

import org.bson.types.ObjectId
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpec
import syndor.EnvironmentSupport._
import syndor.model.Feed
import syndor.model.FeedItem
import syndor.model.Source
import syndor.model.FetchStatus
import syndor.TestHttpServer
import akka.actor.Actor.actorOf
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import org.scalatest.BeforeAndAfterEach
import java.text.SimpleDateFormat

class FeedUpdaterSpec extends FlatSpec
  with ShouldMatchers
  with BeforeAndAfterAll
  with TestFeedSupport
  with TestHttpServerSupport {
  
  private val updater = actorOf[FeedUpdater]

  override def beforeAll() {
    updater.start()
    super.beforeAll()
  }

  override def afterAll() {
    try {
       super.afterAll()
    } finally {
       updater.stop()
    }
  }

  behavior of "feed updater"

  it should "read feed items" in {
    
    Feed.collection.count should be(0)
    
    val feed = insertFetchingFeed(httpServer.resolve("/resource/reuters_2011_10_23.xml").toExternalForm())

    Feed.collection.count should be(1)
    FeedItem.collection.count should be(0)

    (updater ? FeedUpdater.UpdateRequest(feed)).await.get

    // Sleep as long as timeout to make sure it has finished
    //Thread.sleep(FeedBotConfig.fetcherTimeoutInMillis + 10)

    FeedItem.collection.count should be(10)

    verifyFeedItems(FeedItem.find(MongoDBObject()).sort(orderBy = MongoDBObject("publishedDate" -> -1)))

    Feed.findOneByID(feed.id) map { updatedFeed =>
      updatedFeed.fetchStatus.fetching should be(false)
      updatedFeed.fetchStatus.success should be(true)
      updatedFeed.fetchStatus.message should be(None)
      updatedFeed.fetchStatus.fetchStart should be (feed.fetchStatus.fetchStart)
      updatedFeed.fetchStatus.fetchEnd should be > feed.fetchStatus.fetchEnd

      updatedFeed.fetchStatus.fetchStart should be < updatedFeed.fetchStatus.fetchEnd
    } orElse {
      fail("Unable to find updated feed with id %s".format(feed.id))
    }
    
    def verifyFeedItems(cursor: SalatMongoCursor[FeedItem]) {
      cursor.next should have(
        'title("Clues to Gaddafi's death concealed from public view"),
        'uri("http://www.reuters.com/article/2011/10/23/us-libya-gaddafi-finalhours-idUSTRE79M02W20111023?feedType=RSS&feedName=topNews"),
        'link("http://feeds.reuters.com/~r/reuters/topNews/~3/zjpIRmrOdG8/us-libya-gaddafi-finalhours-idUSTRE79M02W20111023"))
    }
  }

  it should "ignore items that are already fetched" in {
    val feed1 = insertFetchingFeed(httpServer.resolve("/resource/reuters_2011_10_23.xml").toExternalForm())
    val feed2 = insertFetchingFeed(httpServer.resolve("/resource/reuters_2011_10_23.xml").toExternalForm())
    
    Feed.collection.count should be(2)
    FeedItem.collection.count should be(0)

    (updater ? FeedUpdater.UpdateRequest(feed1)).as[FeedUpdater.UpdateResponse].get
    (updater ? FeedUpdater.UpdateRequest(feed2)).as[FeedUpdater.UpdateResponse].get
    
    // Sleep as long as timeout to make sure it has finished
    //Thread.sleep(FeedBotConfig.fetcherTimeoutInMillis + 10)
    
    FeedItem.collection.count should be(10)
  }
  
  it should "fail when updating non existent resource" in {
    val feed = insertFetchingFeed(httpServer.resolve("/resource/non_existent.xml").toExternalForm())

    Feed.collection.count should be(1)
    FeedItem.collection.count should be(0)

    //(updater ? FeedUpdater.UpdateRequest(feed)).as[FeedUpdater.UpdateResponse].get

    evaluating {
       (updater ? FeedUpdater.UpdateRequest(feed)).await.get
    } should produce[IllegalStateException] // Should get here due to 404
    
    // Sleep as long as timeout to make sure it has finished
    //Thread.sleep(FeedBotConfig.fetcherTimeoutInMillis + 10)

    FeedItem.collection.count should be(0)

    Feed.findOneByID(feed.id) map { updatedFeed =>
      updatedFeed.fetchStatus.fetching should be(false)
      updatedFeed.fetchStatus.success should be(false)
      updatedFeed.fetchStatus.message should not be (None)
      updatedFeed.fetchStatus.fetchStart should be (feed.fetchStatus.fetchStart)
      updatedFeed.fetchStatus.fetchEnd should be > feed.fetchStatus.fetchEnd

      updatedFeed.fetchStatus.fetchStart should be < updatedFeed.fetchStatus.fetchEnd
    } orElse {
      fail("Unable to find updated feed with id %s".format(feed.id))
    }
  }
  
  def insertFetchingFeed(url: String): Feed = {
    val feed = Feed(
      sourceId = source.id,
      title = "Test",
      url = url,
      fetchStatus = FetchStatus(
         fetching = true,
         fetchStart = System.currentTimeMillis))

    Feed.insert(feed)

    return feed
  } 
}