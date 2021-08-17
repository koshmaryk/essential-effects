# The `Fiber` abstraction as a logical thread

A logical thread offers a synchronous interface to an asynchronous process 
and abstract async processes as synchronous sequences of discrete steps.
You can think of fibers as lightweight threads which use cooperative scheduling, unlike actual threads which use preemptive scheduling.
Unlike OS and JVM threads which are managed in the kernel space, fibers are managed in the user space. 
In other words, fibers are a made-up construct that lives on the heap because we bring it to life in our own code.

There is one important difference and one important similarity 
when it comes to these two layers of abstraction (fibers on threads vs threads on CPU).

Difference:

Threads are scheduled on the CPU preemptively (scheduler suspends tasks), 
whereas fibers are scheduled on threads cooperatively (tasks suspend themselves).

Similarity:

**Blocking on one level means de-scheduling (suspending) on the level below.** This is a very powerful insight. 
CPU doesn't know anything about blocking; it just keeps running threads in circles. 
What we call a "blocked thread" is simply, from CPU's perspective, a thread that got de-scheduled and will be run again later at some point.

In Cats-Effect, fiber is a construct with `cancel` and `join`:
```scala
trait Fiber[F[_], A] {
  def cancel: F[Unit]
  def join: F[A]
}
```

`IO.racePair`, which doesn't provide cancelation of the “losing” effect. 
Instead, you receive the “winning” value along with the Fiber of the race “loser”,
so you can decide what you want to do with it.

With racePair and fibers, we can implement myParMapN with cancelation-on-error behaviour:

```scala
def myParMapN[A, B, C](ia: IO[A], ib: IO[B])(f: (A, B) => C): IO[C] = 
  IO.racePair(ia, ib).flatMap { 
    case Left((a, fb)) => (IO.pure(a), fb.join).mapN(f) 
    case Right((fa, b)) => (fa.join, IO.pure(b)).mapN(f)
  }
```

If no errors occur, we’ll detect which finishes first, and then join the other until completion, 
and finally combine the values with our function f.

- [x] start both the `ia` and `ib` computations, so they run concurrently (**fork** them)
- [x] wait for each result
- [x] cancel the **other** effect if `ia` or `ib` fails
- [x] finally, combine the results with the f function