package scheduler

import cats.implicits._
import cats.effect.{ExitCode, IO, IOApp}
import helper.debug._

import scala.concurrent.duration._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    def createTask(i: Int): IO[Unit] =
      IO.sleep((100 + i * 10).millisecond) *>
        IO(s"Task $i").debug.void

    for {
      resource <- JobScheduler.resource(maxRunning = 2)
      _ <- resource.use { scheduler =>
        (0 until 50)
          .map(createTask)
          .toList
          .parTraverse(scheduler.schedule(_)) *>
          IO.sleep(50000.millisecond)
      } // once the use effect completes, the (hidden) loop will be cancelled, stopping any internal notifications to the Reactor
    } yield ExitCode.Success
  }
}
