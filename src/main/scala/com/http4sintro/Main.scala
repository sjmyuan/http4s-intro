package com.http4sintro

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._

object Main extends IOApp {
  def route: HttpApp[IO] =
    HttpRoutes
      .of[IO]({
        case GET -> Root / "hello" => Ok("hello world")
      })
      .orNotFound
  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8888, "0.0.0.0")
      .withHttpApp(route)
      .resource
      .use(_ => IO.never)
}
