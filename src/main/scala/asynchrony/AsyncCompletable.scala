package asynchrony

import cats.effect._
import java.util.concurrent.CompletableFuture
import scala.jdk.FunctionConverters._
import helper.debug._

object AsyncCompletable extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    effect.debug.as(ExitCode.Success)

  val effect: IO[String] = fromCF(IO(cf()))

  def fromCF[A](cfa: IO[CompletableFuture[A]]): IO[A] =
    cfa.flatMap { fa =>
      IO.async_ { cb =>
        val handler: (A, Throwable) => Unit = {
          case (a , null) => cb(Right(a))
          case (null , t) => cb(Left(t))
        }

        fa.handle(handler.asJavaBiFunction)

        ()
      }
    }

  def cf(): CompletableFuture[String] =
    CompletableFuture.supplyAsync(() => "woo!")
}
