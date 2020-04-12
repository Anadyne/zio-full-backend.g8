package $package$.client

import sttp.client.asynchttpclient.zio._
import sttp.client.{ RequestT, Response }

import zio.console.{ putStrLn, Console }
import zio.{ URIO, ZEnv, ZIO }

class Client() {

  def run[A](req: RequestT[sttp.client.Identity, A, Nothing]): URIO[ZEnv, Serializable] = {

    val sendAndPrint: ZIO[Console with SttpClient, Throwable, Response[A]] = {
      for {
        response <- SttpClient.send(req)
        _        <- putStrLn(response.body.toString)
      } yield response
    }

    sendAndPrint.provideCustomLayer(AsyncHttpClientZioBackend.layer()).fold(err => err, resp => resp)
  }
}

