import com.typesafe.sbt.packager.docker.Cmd

name := """codacy-duplication-pmdcpd"""

version := "1.0.0-SNAPSHOT"

val languageVersion = "2.11.12"

scalaVersion := languageVersion

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
)

val pmdVersion = "6.0.1"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.10" withSources(),
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4" withSources(),
  "com.codacy" %% "codacy-duplication-scala-seed" % "1.0.2",
  "net.sourceforge.pmd" % "pmd-scala" % pmdVersion withSources(),
  "net.sourceforge.pmd" % "pmd-java" % pmdVersion withSources(),
  "net.sourceforge.pmd" % "pmd-javascript" % pmdVersion withSources(),
  "net.sourceforge.pmd" % "pmd-ruby" % pmdVersion withSources(),
  "net.sourceforge.pmd" % "pmd-python" % pmdVersion withSources(),
  "net.sourceforge.pmd" % "pmd-cs" % pmdVersion withSources(),
  "org.scalameta" %% "scalameta" % "1.4.0" withSources()
)

// FIXES: package database contains object and package with same name: DBType
scalacOptions := scalacOptions.value.filterNot(_ == "-Xfatal-warnings") ++ Seq("-Yresolve-term-conflict:object")

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

mappings in Universal <++= (resourceDirectory in Compile) map { (resourceDir: File) =>
  val src = resourceDir / "docs"
  val dest = "/docs"

  for {
    path <- (src ***).get
    if !path.isDirectory
  } yield path -> path.toString.replaceFirst(src.toString, dest)
}

val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "develar/java"

val installAll = """apk update && apk add bash curl &&
                   |rm -rf /tmp/* &&
                   |rm -rf /var/cache/apk/*""".stripMargin.replaceAll(System.lineSeparator(), " ")

dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("WORKDIR", _) => List(cmd,
    Cmd("RUN", installAll)
  )

  case cmd@(Cmd("ADD", "opt /opt")) => List(cmd,
    Cmd("RUN", s"adduser -u 2004 -D $dockerUser")
  )
  case other => List(other)
}
