package $package$.module.db

import $package$.model.ExpectedFailure
import $package$.model.database.User
import $package$.types.UserRepository

import zio.{ ZIO }

// Generic Repository
object UserRepository {
  trait Service {
    def hello(name: String): ZIO[Any, ExpectedFailure, String]
    def get(id: Long): ZIO[Any, ExpectedFailure, Option[User]]
    def create(user: User): ZIO[Any, ExpectedFailure, Unit]
    def delete(id: Long): ZIO[Any, ExpectedFailure, Unit]
  }

  // Accessor Methods
  def hello(name: String): ZIO[UserRepository, ExpectedFailure, String] =
    ZIO.accessM(_.get.hello(name))

  def get(id: Long): ZIO[UserRepository, ExpectedFailure, Option[User]] =
    ZIO.accessM(_.get.get(id))

  def create(user: User): ZIO[UserRepository, ExpectedFailure, Unit] =
    ZIO.accessM(_.get.create(user))

  def delete(id: Long): ZIO[UserRepository, ExpectedFailure, Unit] =
    ZIO.accessM(_.get.delete(id))
}
