package $package$.module.logging

import zio.console.Console
import zio.{ Has, UIO, ZIO, ZLayer }

object AppLogger {
  type Logger = Has[Logger.Service]

  object Logger {
    trait Service {
      def error(msg: => String): UIO[Unit]
      def warn(msg: => String): UIO[Unit]
      def info(msg: => String): UIO[Unit]
      def debug(msg: => String): UIO[Unit]
      def trace(msg: => String): UIO[Unit]
    }
    val any: ZLayer[Logger, Nothing, Logger] = ZLayer.requires[Logger]

    val live = ZLayer.fromFunction { console: Console =>
      new Service {

        def error(msg: => String): UIO[Unit] = console.get.putStr(msg)
        def warn(msg: => String): UIO[Unit]  = console.get.putStr(msg)
        def info(msg: => String): UIO[Unit]  = console.get.putStr(msg)
        def debug(msg: => String): UIO[Unit] = console.get.putStr(msg)
        def trace(msg: => String): UIO[Unit] = console.get.putStr(msg)

      }
    }
    def debug(msg: => String): ZIO[Logger, Nothing, Unit] = ZIO.accessM(_.get.debug(msg))
  }

  val liveEnv = Console.live >>> Logger.live

}
