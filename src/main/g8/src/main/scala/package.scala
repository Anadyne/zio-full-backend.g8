package $package$

import $package$.model.database.User
import $package$.module.db.UserRepository
import $package$.module.logging.AppLogger.Logger

import zio.clock.Clock
import zio.{ Has, RIO, Ref }

object types {

  type AppEnvironment = Clock with UserRepository with Logger
  type AppTask[+A]    = RIO[AppEnvironment, A]

  type UserRepository = Has[UserRepository.Service]

  type MockType = Ref[Map[Long, User]]

}
