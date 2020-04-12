package $package$.module.db

import io.circe.generic.auto._, io.circe.syntax._

import com.typesafe.scalalogging.LazyLogging

import $package$.model.database.User
import $package$.model.{ ExpectedFailure }
import $package$.module.logging.AppLogger
import $package$.types.MockType

import zio.clock.Clock
import zio.{ Has, ZIO, ZLayer }

object MockRepository extends LazyLogging {

  val live: ZLayer[Has[MockType], Nothing, Has[UserRepository.Service]] = ZLayer.fromService { ref: MockType =>
    new UserRepository.Service {

      def hello(name: String): ZIO[Any, ExpectedFailure, String] = ZIO.succeed(User(13, "Boris", 34).asJson.toString)

      def get(id: Long): ZIO[Any, ExpectedFailure, Option[User]] =
        for {
          user <- ref.get.map(_.get(id))
          out <- user match {
                  case Some(s) => ZIO.some(s)
                  case None    => ZIO.none
                }
        } yield out

      def create(user: User): ZIO[Any, ExpectedFailure, Unit] = ref.update(map => map.+(user.id -> user)).unit

      def delete(id: Long): ZIO[Any, ExpectedFailure, Unit] = ref.update(map => map.-(id)).unit

    }
  }

  def getEnv(
    ref: MockType
  ): ZLayer[Any, Nothing, Has[UserRepository.Service] with Has[AppLogger.Logger.Service] with Clock] = {
    logger.info(">>>>> Running Mock Repository")
    ZLayer.succeed(ref) >>> MockRepository.live ++ AppLogger.liveEnv ++ Clock.live
  }

}
