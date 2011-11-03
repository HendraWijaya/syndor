package syndor.feedbot

import syndor.EnvironmentSupport._
import com.mongodb.casbah.Imports._
import akka.actor.Actor
import akka.actor.Actor.actorOf
import akka.actor.Supervisor
import akka.config.Supervision._

object Main extends App {
  Boot.load(Dev)

  val feedBot = actorOf(new FeedBot(nrOfFeeds = 40, maxNrOfFetching = 100))
  
  // Wrap the feed bot with a supervisor for auto restart when 
  // something goes wrong
  Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Throwable]), 10, 100),
      Supervise(
        feedBot,
        Permanent) ::
        Nil))

  feedBot ! FeedBot.ScheduleUpdateFeeds(100)
  
  Util.addShutdownHook {
    // Here add anything that requires graceful shutdown, such as stopping the actor
    // feedBot.stop()
  }
  
  /*
   * A utility to provide graceful shutdown hook
   */
  object Util {
    def addShutdownHook(body: => Unit) {
      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run { body }
      })
    }
  }
}