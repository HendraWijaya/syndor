import sbt._
import Keys._

object BuildSettings {
    import Dependencies._
    import Resolvers._

    val buildOrganization = "syndor"
    val buildVersion = "1.0-SNAPSHOT"
    val buildScalaVersion = "2.9.1"

    val globalSettings = Seq(
        organization := buildOrganization,
        version := buildVersion,
        scalaVersion := buildScalaVersion,
        scalacOptions += "-deprecation",
        fork in Test := true,
        parallelExecution in Test := false,
        libraryDependencies ++= Seq(slf4jApi, slf4jSimpleTest, scalatest),
        resolvers := Seq(scalaToolsRepo, akkaRepo, sonatypeRepo, novusSnaps))

    val projectSettings = Defaults.defaultSettings ++ globalSettings
    
}

object Resolvers {
    val sonatypeRepo = "Sonatype Release" at "http://oss.sonatype.org/content/repositories/releases"
    val scalaToolsRepo = "Scala Tools" at "http://scala-tools.org/repo-snapshots/"
    val akkaRepo = "Akka" at "http://akka.io/repository/"
    val novusSnaps = "repo.novus snaps" at "http://repo.novus.com/snapshots/"
}

object Dependencies {
    val scalatest = "org.scalatest" %% "scalatest" % "1.6.1" % "test"

    val slf4jApi = "org.slf4j" % "slf4j-api" % "1.6.2" % "runtime"
    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.2"
    val slf4jSimpleTest = slf4jSimple % "test"
    
    val logbackClassic = "ch.qos.logback" % "logback-classic" % "0.9.29" % "runtime"

    val akkaActor = "se.scalablesolutions.akka" % "akka-actor" % "1.2"
    val akkaSlf4j = "se.scalablesolutions.akka" % "akka-slf4j" % "1.2"

    val rome = "rome" % "rome" % "1.0"
    val romeModules = "org.rometools" % "rome-modules" % "1.0"
    
    val scalaIo = "com.github.scala-incubator.io" %% "scala-io-core" % "0.2.0"
    
    val asyncHttp = "com.ning" % "async-http-client" % "1.6.5"

    val casbah = "com.mongodb.casbah" %% "casbah-core" % "2.1.5-1"
    val salat = "com.novus" %% "salat-core" % "0.0.8-SNAPSHOT"
    
    val jettyVersion = "7.4.0.v20110414"
    val jettyServer = "org.eclipse.jetty" % "jetty-server" % jettyVersion
    val jettyServlet = "org.eclipse.jetty" % "jetty-servlet" % jettyVersion
    val jettyServerTest = jettyServer % "test"
    val jettyServletTest = jettyServlet % "test"
}

object SyndorBuild extends Build {
    import BuildSettings._
    import Dependencies._
    import Resolvers._

    override lazy val settings = super.settings ++ globalSettings
                            
    lazy val root = Project("syndor",
                            file("."),
                            settings = projectSettings,
                            aggregate = Seq(common, feedbot))
                            
    lazy val feedbot = Project("syndor-feedbot",
                              file("syndor-feedbot"),
                              settings = projectSettings ++
                              Seq(libraryDependencies ++= Seq(
                                akkaActor, akkaSlf4j, 
                                asyncHttp, scalaIo, slf4jSimple)),
                              dependencies = Seq(common % "compile->compile;test->test"))
                              
    lazy val common = Project("syndor-common",
                           file("syndor-common"),
                           settings = projectSettings ++
                           Seq(libraryDependencies ++= Seq(salat, casbah, rome, romeModules, jettyServletTest, jettyServerTest)))
}


