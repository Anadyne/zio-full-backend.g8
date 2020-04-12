package $package$

import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{ CORS, Logger }

import $package$.model.config.config.loadConfig
import $package$.model.database.User
import $package$.module.db._
import $package$.module.db.{ LiveRepository, MockRepository }
import $package$.route.UserRoute
import cats.effect.{ ExitCode }
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import types._

import zio.console.putStrLn
import zio.interop.catz._
import zio.{ Ref, ZIO }

object Server extends CatsApp {

  private val userRoute = new UserRoute[AppEnvironment]
  private val yaml      = userRoute.getEndpoints.toOpenAPI("User", "1.0").toYaml
  private val httpApp =
    Router("/" -> userRoute.allRoutes, "/docs" -> new SwaggerHttp4s(yaml).routes[AppTask]).orNotFound
  private val finalHttpApp = Logger.httpApp[AppTask](true, true)(httpApp)

  override def run(args: List[String]) = {
    val res = for {

      ref <- Ref.make(Map.empty[Long, User])
      cfg <- ZIO.fromEither(loadConfig())

      // Choose Env type - Live or Mock
      env = if (cfg.server.serverType == "mock") MockRepository.getEnv(ref) else LiveRepository.getEnv(cfg)

      server <- ZIO
                 .runtime[AppEnvironment]
                 .flatMap(implicit rts =>
                   BlazeServerBuilder[AppTask]
                     .bindHttp(cfg.server.port, cfg.server.host)
                     .withHttpApp(CORS(finalHttpApp))
                     .serve
                     .compile[AppTask, AppTask, ExitCode]
                     .drain
                 )
                 .provideCustomLayer(env)
                 .orDie
    } yield server

    res.foldM(err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))
  }
}
