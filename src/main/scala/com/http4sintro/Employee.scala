package com.http4sintro

import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import cats.effect.IO
import org.http4s.client.Client
import org.http4s.Uri
import org.http4s.headers.Host

object Employee {
  private val dummyApi: Uri = Uri.unsafeFromString("http://dummy.restapiexample.com/api/v1")
  def routes(client: Client[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ GET -> Root / "proxy" / "employees" =>
      client.fetch(IO.pure(request.removeHeader(Host).withUri(dummyApi / "employees")))(IO(_))
    case request @ GET -> Root / "proxy" / "employee" / id =>
      client.fetch(IO.pure(request.removeHeader(Host).withUri(dummyApi / "employee" / id)))(IO(_))
    case request @ POST -> Root / "proxy" / "create" =>
      client.fetch(IO.pure(request.removeHeader(Host).withUri(dummyApi / "create")))(IO(_))
    case request @ PUT -> Root / "proxy" / "update" / id =>
      client.fetch(IO.pure(request.removeHeader(Host).withUri(dummyApi / "update" / id)))(IO(_))
    case request @ DELETE -> Root / "proxy" / "delete" / id =>
      client.fetch(IO.pure(request.removeHeader(Host).withUri(dummyApi / "delete" / id)))(IO(_))
  }
}
