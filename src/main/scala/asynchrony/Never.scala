/*package asynchrony

import cats.effect._
import helper.debug._

object Never extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    never
      .guarantee(IO("i guess never is now").debug.void)
      .as(ExitCode.Success)

  val never: IO[Nothing] =
    IO.async(_ => ())
}*/
