package $package$.client

import sttp.client.asynchttpclient.zio._
import sttp.client.{ RequestT, Response }

import zio.console.{ putStrLn, Console }
import zio.{ URIO, ZEnv, ZIO }

class Client() {

  def run[A](req: RequestT[sttp.client.Identity, A, Nothing]): URIO[ZEnv, Serializable] = {

    // create a description of a program, which requires two dependencies in the environment:
    // the SttpClient, and the Console
    val sendAndPrint: ZIO[Console with SttpClient, Throwable, Response[A]] = {
      for {
        response <- SttpClient.send(req)
        _        <- putStrLn(s"Got response code: ${response.code}")
        _        <- putStrLn(response.body.toString)
      } yield response
    }

    // provide an implementation for the SttpClient dependency. other dependencies are
    // provided by Zio
    sendAndPrint.provideCustomLayer(AsyncHttpClientZioBackend.layer()).fold(err => err, resp => resp)
  }

}
