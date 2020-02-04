package com.http4sintro

import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import io.circe.HCursor
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.syntax._
import org.http4s.EntityDecoder
import org.http4s.circe._
import org.http4s.EntityEncoder
import cats.implicits._
import org.http4s.client.blaze.BlazeClientBuilder

object Main extends IOApp {
  object UserName extends QueryParamDecoderMatcher[String]("user_name")

  object Age extends OptionalQueryParamDecoderMatcher[Int]("age")

  object PhoneNumber extends OptionalMultiQueryParamDecoderMatcher[String]("phone")

  case class Person(name: String, age: Option[Int], phoneNumbers: List[String])

  object Person {
    implicit val personDecoder: Decoder[Person] = new Decoder[Person] {
      override def apply(c: HCursor): Decoder.Result[Person] =
        for {
          name <- c.get[String]("name")
          age <- c.get[Option[Int]]("age")
          phoneNumbers <- c.get[List[String]]("phone")
        } yield Person(name, age, phoneNumbers)
    }

    implicit val personEncoder: Encoder[Person] = new Encoder[Person] {
      override def apply(a: Person): Json = Json.obj(
        "name" -> a.name.asJson,
        "age" -> a.age.asJson,
        "phone" -> a.phoneNumbers.asJson
      )
    }

    implicit val personEntityDecoder: EntityDecoder[IO, Person] = jsonOf[IO, Person]
    implicit val personEntityEncoder: EntityEncoder[IO, Person] = jsonEncoderOf[IO, Person]

  }

  def helloWorldRoutes: HttpRoutes[IO] =
    HttpRoutes
      .of[IO]({
        case GET -> Root / "hello" / name => Ok(s"Hello, ${name}")
        case GET -> Root / "hello" :? UserName(name) +& Age(age) +& PhoneNumber(phoneNumber) =>
          phoneNumber match {
            case Invalid(e) => BadRequest("Failed to parse phone number.")
            case Valid(Nil) =>
              age.fold(Ok(s"Hello, ${name}"))(x => Ok(s"Hello, ${name}, your age is ${x}"))
            case Valid(numbers) =>
              age.fold(Ok(s"Hello, ${name}, your phone number is ${numbers.mkString(";")}"))(
                x => Ok(s"Hello, ${name}, your age is ${x}, your phone number is ${numbers.mkString(";")}")
              )
          }
        case GET -> Root / "json" / "hello" :? UserName(name) +& Age(age) +& PhoneNumber(phoneNumber) =>
          phoneNumber match {
            case Invalid(e) => BadRequest("Failed to parse phone number.")
            case Valid(numbers) =>
              Ok(Person(name, age, numbers))
          }
        case request @ POST -> Root / "json" / "hello" =>
          request.as[Person].flatMap(person => Ok(person))
        case GET -> Root / "add" / "int" / IntVar(x) / IntVar(y)    => Ok(s"Result is ${x + y}")
        case GET -> Root / "add" / "long" / LongVar(x) / LongVar(y) => Ok(s"Result is ${x + y}")
        case request @ GET -> Root / "header"                       => Ok(request.headers.toString)
      })
  override def run(args: List[String]): IO[ExitCode] = {
    val resource = for {
      client <- BlazeClientBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global).resource
      server <- BlazeServerBuilder[IO]
        .bindHttp(8888, "0.0.0.0")
        .withHttpApp((helloWorldRoutes <+> Employee.routes(client)).orNotFound)
        .resource
    } yield server
    resource.use(_ => IO.never)
  }
}
