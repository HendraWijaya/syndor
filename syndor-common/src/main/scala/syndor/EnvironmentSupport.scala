package syndor

object EnvironmentSupport {
  sealed trait Environment
  case object Dev extends Environment
  case object Test extends Environment
  case object Prod extends Environment
}

trait EnvironmentSupport {
  import EnvironmentSupport._
  
  def load(env: Environment) {
    env match {
      case Dev =>
        dev()
      case Test =>
        test()
      case Prod =>
        prod()
    }
  }

  def dev()
  
  def test()
  
  def prod()
}