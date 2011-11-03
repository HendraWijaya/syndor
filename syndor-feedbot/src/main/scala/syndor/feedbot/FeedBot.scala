package syndor.feedbot

import java.util.concurrent.atomic.AtomicInteger

import syndor.model.Feed

import akka.actor.Actor._
import akka.actor.Actor
import akka.actor.Scheduler
import akka.config.Supervision.OneForOneStrategy
import akka.config.Supervision.Permanent
import akka.event.EventHandler
import akka.util.duration._

object FeedBot {
  sealed trait Request
  object UpdateFeeds extends Request
  case class ScheduleUpdateFeeds(fetchPeriodInMs: Int) extends Request
}

class FeedBot(nrOfFeeds: Int, maxNrOfFetching: Int) extends Actor {
  import FeedBot._

  self.lifeCycle = Permanent
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 10, 100)

  private val nrOfFetching = new AtomicInteger(0)
  private val nrOfFetched = new AtomicInteger(0)

  private val feedUpdater = actorOf[FeedUpdater]

  override def preStart {
    nrOfFetching.set(0)
    nrOfFetched.set(0)

    /*
     * Reset the status of all feeds: resetting fetching status to false.
     * Useful when the process had crashed previously
     */
    Feed.reset()

    self.startLink(feedUpdater)
  }

  /*
   * This is not called when it crashes
   */
  override def postStop() = {
    EventHandler.info(this, "Stopping FeedBot...")
    self.unlink(feedUpdater)
    feedUpdater.stop
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
      case UpdateFeeds =>
        if (nrOfFetching.get < maxNrOfFetching) {
          updateFeeds()
        }
      case ScheduleUpdateFeeds(period) =>
        Scheduler.schedule(self, UpdateFeeds, 0, period, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
  }

  private def updateFeeds() {
    Feed.grabForFetching(nrOfFeeds).foreach { feed =>
      updateFeed(feed)
    }
  }

  private def updateFeed(feed: Feed) {

    nrOfFetching.incrementAndGet()

    (feedUpdater ? FeedUpdater.UpdateRequest(feed))(timeout = FeedBotConfig.fetcherTimeoutInMillis millis) onResult {
      case FeedUpdater.UpdateResponse(feed: Feed) =>
        nrOfFetching.decrementAndGet()
    } onException {
      case e =>
        nrOfFetching.decrementAndGet()
        EventHandler.warning(this, "Updating feed %s results in exception %s".format(feed.url, e))
    } onTimeout { _ =>
      nrOfFetching.decrementAndGet()
      EventHandler.warning(this, "Timeout while updating %s".format(feed.url))
    }

  }
}