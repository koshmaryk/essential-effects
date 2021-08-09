package io

import cats.effect.{ ExitCode, IO, IOApp }

import scala.concurrent.duration._

object TickingClock extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    tickingClock.as(ExitCode.Success)

  val tickingClock: IO[Unit] = for {
    _ <- IO(println(System.currentTimeMillis))
    _ <- IO.sleep(1.second)
    _ <- tickingClock
  } yield ()
}
