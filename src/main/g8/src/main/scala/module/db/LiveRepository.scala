package $package$.module.db

import scala.jdk.CollectionConverters._

import io.circe.generic.auto._, io.circe.syntax._
import io.getquill.{ H2JdbcContext, SnakeCase }

import org.flywaydb.core.Flyway

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging

import $package$.model.config.config.AppConfig
import $package$.model.database.User
import $package$.model.{ DBFailure, ExpectedFailure }
import $package$.module.logging.AppLogger

import zio.clock.Clock
import zio.{ Has, ZIO, ZLayer }

object LiveRepository extends LazyLogging {

  val live: ZLayer[Has[Config], Nothing, Has[UserRepository.Service]] = ZLayer.fromService { cfg: Config =>
    new UserRepository.Service {

      private lazy val ctx: H2JdbcContext[SnakeCase.type] = new H2JdbcContext(SnakeCase, cfg)
      import ctx._

      def hello(name: String): ZIO[Any, ExpectedFailure, String] = ZIO.succeed(User(11, "Boris", 15).asJson.toString)

      def get(id: Long): ZIO[Any, ExpectedFailure, Option[User]] =
        for {
          list <- ZIO.effect(ctx.run(query[User].filter(_.id == lift(id)))).mapError(t => DBFailure(t))
          user <- list match {
                   case Nil    => ZIO.none
                   case s :: _ => ZIO.some(s)
                 }
        } yield user

      def create(user: User): ZIO[Any, ExpectedFailure, Unit] =
        zio.IO
          .effect(ctx.run(query[User].insert(lift(user))))
          .mapError(t => DBFailure(t))
          .unit

      def delete(id: Long): ZIO[Any, ExpectedFailure, Unit] =
        zio.IO
          .effect(ctx.run(query[User].filter(_.id == lift(id)).delete))
          .mapError(t => DBFailure(t))
          .unit
    }
  }

  private def dbConfig(cfg: AppConfig): Config = {
    val map = Map(
      "dataSourceClassName" -> cfg.db.className,
      "dataSource.url"      -> cfg.db.url,
      "dataSource.user"     -> cfg.db.user,
      "dataSource.password" -> cfg.db.pass
    ).asJava

    ConfigFactory.parseMap(map)
  }

  private def dbInit(cfg: AppConfig): Int =
    Flyway
      .configure()
      .validateMigrationNaming(true)
      .dataSource(cfg.db.url, cfg.db.user, cfg.db.pass)
      .load()
      .migrate()

  def getEnv(
    cfg: AppConfig
  ): ZLayer[Any, Nothing, Has[UserRepository.Service] with Has[AppLogger.Logger.Service] with Clock] = {
    logger.info(">>>>> Running Live Repository")
    dbInit(cfg)
    ZLayer.succeed(dbConfig(cfg)) >>> LiveRepository.live ++ AppLogger.liveEnv ++ Clock.live
  }
}
