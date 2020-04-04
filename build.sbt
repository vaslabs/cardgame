name := "cardgame"

version := "0.1"

scalaVersion in ThisBuild := "2.13.1"


lazy val `card-game` = (project in file("."))
  .settings(noPublishSettings)
  .settings(compilerSettings)
  .aggregate(endpoints, service, protocol)

lazy val endpoints =
  (project in file("endpoints"))
      .settings(
        libraryDependencies ++= Dependencies.Modules.endpoints
      ).settings(noPublishSettings).settings(compilerSettings)


lazy val protocol =
  (project in file("protocol"))
    .settings(libraryDependencies ++= Dependencies.Modules.protocol)
    .settings(noPublishSettings)
    .settings(compilerSettings)

lazy val service = (project in file("service"))
  .settings(libraryDependencies ++= Dependencies.Modules.service)
  .enablePlugins(DockerPlugin, JavaAppPackaging, AshScriptPlugin)
  .settings(dockerSettings)
  .settings(versioningSettings)
  .settings(compilerSettings)
  .enablePlugins(KubeDeploymentPlugin, KubeServicePlugin, KubeIngressPlugin)
  .settings(deploymentSettings)
  .settings(ingressSettings)
  .dependsOn(endpoints)

lazy val repo = sys.env.get("CI_REGISTRY")
lazy val dockerSettings = Seq(
  (packageName in Docker) := "cardgame-generator",
  dockerBaseImage := "openjdk:8u191-jre-alpine3.8",
  dockerRepository := repo,
  dockerUsername := Some("eng"),
  dockerExposedPorts := List(8080)
)

lazy val noPublishSettings = Seq(
  publish / skip := true
)

lazy val versioningSettings = Seq(
  dynverSeparator in ThisBuild := "-",
  dynverVTagPrefix in ThisBuild := false
)

import kubeyml.deployment.plugin.Keys._
import kubeyml.deployment.{Resource, Cpu, Memory}
import kubeyml.ingress.plugin.Keys._
import kubeyml.ingress.{Host, HttpRule, ServiceMapping, Path => IngressPath}
import kubeyml.service.plugin.Keys
import kubeyml.deployment.api._
import kubeyml.ingress.api._

lazy val deploymentSettings = Seq(
  namespace in kube := "scala-demo",
  application in kube := "scala-demo-service",
  resourceLimits in kube := Resource(Cpu.fromCores(2), Memory(4096))
)

lazy val domain = sys.env.getOrElse("SCALA_DEMO_HOST", "localhost")

val ingressSettings = Seq(
  ingressRules in kube := List(
    HttpRule(
      Host(domain),
      List(
        IngressPath(ServiceMapping((Keys.service in kube).value.name, 8080), "/")
      )
    )
  ),
  (ingressAnnotations in kube) := Map(
    Annotate.nginxIngress(), // this adds kubernetes.io/ingress.class: nginx
    Annotate.nginxRewriteTarget("/"), //this adds nginx.ingres
  )
)

lazy val compilerSettings = Seq(
  scalacOptions in ThisProject ++= Seq(
    "-deprecation",
    "-feature",
    "-language:postfixOps",              //Allow postfix operator notation, such as `1 to 10 toList'
    "-language:implicitConversions",
    "-language:higherKinds",
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-macros:after",               // Warn unused macros after compilation
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
    "-Ywarn-unused:params",              // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",            // Warn if a private member is unused.
    "-Ywarn-value-discard",               // Warn when non-Unit expression results are unused.
    "-Ywarn-unused:imports",
    "-Xfatal-warnings"
  )
)