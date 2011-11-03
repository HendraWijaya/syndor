Syndor is an example back-end application that reads and updates feeds asynchronously. The data are stored in MongoDB. It uses the following technologies:

  - [scala]
  - [akka]
  - [salat]
  - [casbah]
  - [async-http-client] 
  - [rome]
  - [scalatest]

The implementation is using akka futures to get asynchronous flow in all layers. The project consists of two sub projects:

  - syndor-common
  - syndor-feedbot

The main class is in syndor-feedbot. You need sbt 0.11.x to run the bot. You also need to have MongoDB installed. To run the feedbot, make sure to run MongoDB first and type:

    $ sbt
    $ > project syndor-feedbot
    $ > run

To configure MongoDB database name, edit the file:

    /syndor-common/src/main/scala/syndor/model/MongoConfig.scala

Below are the default names used in each environment:

    def dev() {
      db = "syndor_dev"
    }

    def test() {
      db = "syndor_test"
    }

    def prod() {
      db = "syndor"
    }

  [scala]: http://www.scala-lang.org/
  [akka]: http://www.akka.io
  [salat]: http://github.com/novus/salat
  [casbah]: http://github.com/mongodb/casbah
  [async-http-client]: http://github.com/sonatype/async-http-client
  [rome]: https://rometools.jira.com/wiki/display/ROME/Home
  [scalatest]: http://www.scalatest.org/

