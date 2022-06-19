package scheduler

import cats.data.Chain
import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.Resource
import cats.effect.{Deferred, Fiber, FiberIO, IO, Outcome, Ref}
import cats.implicits._

import java.util.UUID

sealed trait Job

object Job {
  case class Id(value: UUID) extends AnyVal

  case class Scheduled(id: Id, task: IO[_]) extends Job {
    def start: IO[Job.Running] =
      for {
        exitCase <- Deferred[IO, ExitCase]
        fiber    <- task.void
          .guaranteeCase {
            case Outcome.Succeeded(_) => exitCase.complete(ExitCase.Succeeded).void
            case Outcome.Canceled()   => exitCase.complete(ExitCase.Canceled).void
            case Outcome.Errored(e)   => exitCase.complete(ExitCase.Errored(e)).void
          }
          .start
      } yield Job.Running(id, fiber, exitCase)
  }
  case class Running(id: Id,
                     fiber: Fiber[IO, Throwable, Unit],
                     exitCase: Deferred[IO, ExitCase]) extends Job {
    val await: IO[Completed] = exitCase.get.map(Completed(id, _))
  }
  case class Completed(id: Id, exitCase: ExitCase) extends Job

  def create[A](task: IO[A]): IO[Scheduled] = IO(Id(UUID.randomUUID())).map(Scheduled(_, task))
}

trait JobScheduler {
  def schedule(task: IO[_]): IO[Job.Id]
}

object JobScheduler {

  case class State(maxRunning: Int,
                   scheduled: Chain[Job.Scheduled] = Chain.empty,
                   running: Map[Job.Id, Job.Running] = Map.empty,
                   completed: Chain[Job.Completed] = Chain.empty) {
    def enqueue(job: Job.Scheduled): State = copy(scheduled = scheduled :+ job)
    def dequeue: (State, Option[Job.Scheduled]) =
      if (running.size >= maxRunning) this -> None
      else
        scheduled.uncons
          .map {
            case (head, tail) => copy(scheduled = tail) -> Some(head)
          }
          .getOrElse(this -> None)

    def running(job: Job.Running): State = copy(running = running + (job.id -> job))

    def onComplete(job: Job.Completed): State =
      copy(maxRunning = maxRunning + 1, running = running.removed(job.id),  completed = completed :+ job)
  }

  def resource(maxRunning: Int): IO[Resource[IO, JobScheduler]] =
    for {
      stateRef <- Ref[IO].of(JobScheduler.State(maxRunning))
      zzz      <- Zzz.apply
      scheduler = new JobScheduler {
        override def schedule(task: IO[_]): IO[Job.Id] = for {
          job <- Job.create(task)
          _   <- stateRef.update(_.enqueue(job))
          _   <- zzz.wakeUp
        } yield job.id
      }
      reactor = Reactor(stateRef)
      onStart = (_: Job.Id) => IO.unit
      onComplete = (_: Job.Id, _: ExitCase) => zzz.wakeUp
      loop = // asleep-awake-asleep
        (zzz.sleep *> reactor.whenAwake(onStart, onComplete)).foreverM
    } yield loop.background.as(scheduler)
}

trait Reactor {
  def whenAwake(onStart: Job.Id => IO[Unit],
                onComplete: (Job.Id, ExitCase) => IO[Unit] ): IO[Unit]
}

object Reactor {
  def apply(stateRef: Ref[IO, JobScheduler.State]): Reactor =
    new Reactor {

      override def whenAwake(onStart: Job.Id => IO[Unit],
                             onComplete: (Job.Id, ExitCase) => IO[Unit]): IO[Unit] = {

        def startNextJob: IO[Option[Job.Running]] =
          for {
            job     <- stateRef.modify(_.dequeue)
            running <- job.traverse(startJob)
          } yield running

        def jobCompleted(job: Job.Completed): IO[Unit] =
          stateRef.update(_.onComplete(job))
            .flatTap(_ => onComplete(job.id, job.exitCase))

        def registerOnComplete(job: Job.Running): IO[FiberIO[Unit]] =
          job.await
            .flatMap(jobCompleted)
            .start

        def startJob(scheduled: Job.Scheduled): IO[Job.Running] = for {
          running <- scheduled.start
          _ <- stateRef.update(_.running(running))
          _ <- registerOnComplete(running)
          _ <- onStart(running.id).attempt
        } yield running

        startNextJob
          .iterateUntil(_.isEmpty)
          .void
      }
    }
}

trait Zzz {
  // semantically block until wakeUp is invoked
  def sleep: IO[Unit]
  // wake up any sleepers, no effect if already awake
  def wakeUp: IO[Unit]
}

object Zzz {

  sealed trait State
  case class Asleep(block: Deferred[IO, Unit]) extends State
  case class Awake(block: Deferred[IO, Unit]) extends State

  def apply: IO[Zzz] = for {
    awaken   <- Deferred[IO, Unit]
    stateRef <- Ref[IO].of[State](Asleep(awaken))
  } yield new Zzz {
    override def sleep: IO[Unit] = stateRef.modify {
      case asleep @ Asleep(_) => asleep        -> IO.unit
      case Awake(block)       => Asleep(block) -> block.get
    }.flatten
      .uncancelable

    override def wakeUp: IO[Unit] =  stateRef.modify {
      case Asleep(block)     => Awake(block) -> awaken.complete(()).void
      case awake @ Awake(_)  => awake        -> IO.unit
    }.flatten
      .uncancelable
  }
}
