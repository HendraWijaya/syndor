package syndor.feedbot

import syndor.model.Feed
import syndor.model.Source
import syndor.model.FetchStatus
import syndor.EnvironmentSupport._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import syndor.model.FeedItem
import org.scalatest.Suite

trait TestFeedSupport extends TestBootSupport with BeforeAndAfterAll with BeforeAndAfterEach { this: Suite  =>

  protected val source = Source(name = "Reuters", url = "http://www.reuters.com")
  
  override def beforeAll() {
    Source.insert(source)
    super.beforeAll()
  }

  override def afterAll() {
    try {
      super.afterAll()
    } finally {
       Source.collection.drop()
    }
  }
  
  override def beforeEach() {
    FeedItem.collection.drop()
    Feed.collection.drop()
    super.beforeEach()
  }
}