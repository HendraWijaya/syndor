package syndor.feedbot

import akka.actor._
import akka.routing._

/**
 * An actor pool suitable for CPU-bound computation
 */
trait CpuBoundActorPool
    extends DefaultActorPool
    with SmallestMailboxSelector
    with BoundedCapacityStrategy
    with MailboxPressureCapacitor
    with Filter
    with BasicRampup
    with BasicBackoff {
    self: Actor =>

    // Selector: selectionCount is how many pool members to send each message to
    override def selectionCount = 1
    // Selector: partialFill controls whether to pick less than selectionCount or
    // send the same message to duplicate delegates, when the pool is smaller
    // than selectionCount. Does not matter if lowerBound >= selectionCount.
    override def partialFill = false
    // BoundedCapacitor: create between lowerBound and upperBound delegates in the pool
    override val lowerBound = 1
    override lazy val upperBound = Runtime.getRuntime().availableProcessors() * 2
    // MailboxPressureCapacitor: pressure is number of delegates with >pressureThreshold messages queued
    override val pressureThreshold = 1
    // BasicRampup: rampupRate is percentage increase in capacity when all delegates are busy
    override def rampupRate = 0.2
    // BasicBackoff: backoffThreshold is the percentage-busy to drop below before
    // we reduce actor count
    override def backoffThreshold = 0.7
    // BasicBackoff: backoffRate is the amount to back off when we are below backoffThreshold.
    // this one is intended to be less than 1.0-backoffThreshold so we keep some slack.
    override def backoffRate = 0.20
}
