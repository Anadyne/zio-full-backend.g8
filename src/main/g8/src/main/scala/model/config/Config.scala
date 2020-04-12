package $package$.model.config

import pureconfig.ConfigSource
import pureconfig.generic.auto._

object config {

  final case class Server(serverType: String, host: String, port: Int)

  final case class DBConfig(
    className: String,
    url: String,
    user: String,
    pass: String
  )

  final case class AppConfig(
    server: Server,
    db: DBConfig
  )

  def loadConfig() = ConfigSource.default.load[AppConfig]
}
