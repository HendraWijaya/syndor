package syndor.model

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.MongoCollection
import syndor.EnvironmentSupport

object MongoConfig extends EnvironmentSupport {
  var db: String = _
  
  def collection(name: String): MongoCollection = {
    MongoConnection()(db)(name)
  }
  
  def dev() {
    db = "syndor_dev"
  }
  
  def test() {
    db = "syndor_test"
  }
  
  def prod() {
    db = "syndor"
  }
}
