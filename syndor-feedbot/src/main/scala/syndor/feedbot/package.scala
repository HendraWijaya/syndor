package syndor

import akka.dispatch.Future
import akka.actor.Channel
import akka.actor.UntypedChannel

/**
 * Contains utilities that are common to feedbot
 */
package object feedbot {
  // Class that adds replyWith to Akka channels
  class EnhancedChannel[-T](original: Channel[T]) {
    /**
     * Replies to a channel with the result or exception from
     * the passed-in future
     */
    def replyWith[A <: T](f: Future[A])(implicit sender: UntypedChannel) = {
      f.onComplete({ f =>
        f.value.get match {
          case Left(t) =>
            original.sendException(t)
          case Right(v) =>
            original.tryTell(v)
        }
      })
    }
  }

  // implicitly create an EnhancedChannel wrapper to add methods to the
  // channel
  implicit def enhanceChannel[T](original: Channel[T]): EnhancedChannel[T] = {
    new EnhancedChannel(original)
  }
}