package syndor.feedbot

import org.scalatest.Suite

import syndor.EnvironmentSupport.Test

trait TestBootSupport { this: Suite =>
  Boot.load(Test)
}