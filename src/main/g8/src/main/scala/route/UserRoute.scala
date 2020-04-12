package $package$.route

import io.circe.generic.auto._

import org.http4s._
import org.http4s.dsl.Http4sDsl

import com.typesafe.scalalogging.LazyLogging

import $package$.implicits.Throwable._
import $package$.model.database.User
import $package$.model.response.{ BadRequestResponse, ErrorResponse, InternalServerErrorResponse, NotFoundResponse }
import $package$.model.{ DBFailure, ExpectedFailure, NotFoundFailure }
import $package$.module.db.UserRepository
import $package$.module.logging.AppLogger.Logger
import $package$.types.UserRepository
import cats.syntax.semigroupk._
import sttp.tapir.DecodeResult.Error
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerDefaults.StatusCodes
import sttp.tapir.server.http4s._
import sttp.tapir.server.{ DecodeFailureContext, DecodeFailureHandling, ServerDefaults }

import zio.interop.catz._
import zio.{ RIO, ZIO }

object Endpoints {
  private val userEndpoint = endpoint.in("user")

  val helloEndpoint: Endpoint[String, ErrorResponse, String, Nothing] = endpoint.get
    .in("hello")
    .in(query[String]("name"))
    .errorOut(
      oneOf(
        statusMapping(StatusCodes.error, jsonBody[NotFoundResponse]),
        statusMapping(StatusCodes.error, jsonBody[InternalServerErrorResponse])
      )
    )
    .out(stringBody)

  val getUserEndpoint: Endpoint[Long, ErrorResponse, User, Nothing] = userEndpoint.get
    .in(path[Long]("user id"))
    .errorOut(
      oneOf(
        statusMapping(StatusCodes.error, jsonBody[InternalServerErrorResponse]),
        statusMapping(StatusCodes.error, jsonBody[NotFoundResponse])
      )
    )
    .out(jsonBody[User])

  val createUserEndpoint: Endpoint[User, ErrorResponse, Unit, Nothing] = userEndpoint.post
    .in(jsonBody[User])
    .errorOut(
      oneOf[ErrorResponse](
        statusMapping(StatusCodes.error, jsonBody[InternalServerErrorResponse])
      )
    )
    .out(statusCode(StatusCodes.success))

  val deleteUserEndpoint: Endpoint[Long, ErrorResponse, Unit, Nothing] = userEndpoint.delete
    .in(path[Long]("user id"))
    .errorOut(
      oneOf(
        statusMapping(StatusCodes.error, jsonBody[InternalServerErrorResponse]),
        statusMapping(StatusCodes.error, jsonBody[NotFoundResponse])
      )
    )
    .out(emptyOutput)
}

class UserRoute[R <: UserRepository with Logger] extends Http4sDsl[RIO[R, *]] with LazyLogging {
  import Endpoints._

  private implicit val customServerOptions: Http4sServerOptions[RIO[R, *]] = Http4sServerOptions
    .default[RIO[R, *]]
    .copy(
      decodeFailureHandler = (ctx: DecodeFailureContext) => {
        ctx.failure match {
          case Error(_, error) =>
            DecodeFailureHandling.response(jsonBody[BadRequestResponse])(BadRequestResponse(error.toString))
          case _ => ServerDefaults.decodeFailureHandler(ctx)
        }
      }
    )

  private val helloRoute: HttpRoutes[RIO[R, *]] = helloEndpoint.toRoutes { name =>
    logger.debug(s"Hello called for user $name")
    handleError(UserRepository.hello(name))
  }

  private val newRoute: HttpRoutes[RIO[R, *]] = createUserEndpoint.toRoutes { id =>
    logger.debug(s"New User called for user $id")
    handleError(UserRepository.create(id))
  }

  private val getRoute: HttpRoutes[RIO[R, *]] = getUserEndpoint.toRoutes { id =>
    logger.debug(s"Get User called for user $id")
    handleError(getUser(id))
  }

  private val delRoute: HttpRoutes[RIO[R, *]] = deleteUserEndpoint.toRoutes { id =>
    logger.debug(s"Del User called for user $id")
    val result = for {
      _ <- Logger.debug(s"id: $id")
      _ <- UserRepository.delete(id)
    } yield ()

    handleError(result)
  }

  val allRoutes: HttpRoutes[RIO[R, *]] = { helloRoute <+> newRoute <+> getRoute <+> delRoute }

  val getEndpoints = List(getUserEndpoint, createUserEndpoint, deleteUserEndpoint)

  private def getUser(userId: Long): ZIO[R, ExpectedFailure, User] =
    for {
      _    <- Logger.debug(s"id: $userId")
      user <- UserRepository.get(userId)
      usr <- user match {
              case None    => ZIO.fail(NotFoundFailure(s"Can not find a user by $userId"))
              case Some(s) => ZIO.succeed(s)
            }
    } yield usr

  private def handleError[A](result: ZIO[R, ExpectedFailure, A]): ZIO[R, Throwable, Either[ErrorResponse, A]] =
    result
      .fold(
        {
          case DBFailure(t)             => Left(InternalServerErrorResponse("Database BOOM !!!", t.getMessage, t.getStacktrace))
          case NotFoundFailure(message) => Left(NotFoundResponse(message))
        },
        Right(_)
      )
      .foldCause(
        c => Left(InternalServerErrorResponse("Unexpected errors", "", c.squash.getStacktrace)),
        identity
      )

}
