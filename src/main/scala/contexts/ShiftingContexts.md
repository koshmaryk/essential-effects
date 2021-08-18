Thread pools on the JVM should usually be divided into the following three categories:
1. CPU-bound (#CPUs)
2. Blocking IO
3. Non-blocking IO polling

Cats-Effect 2 provides two abstractions to facilitate shifting work to other pools.

`ContextShift` as a pure representation of a threadpool:
```scala
trait ContextShift[F[_]] {

  def shift: F[Unit]

  def evalOn[A](ec: ExecutionContext)(fa: F[A]): F[A]

}
```
`evalOn` allows us to shift an operation onto another pool and have the continuation be automatically shifted back.
`shift` is a uni-directional shift of thread pool so that the continuation runs on the pool that the `ContextShift` represents.
In fact, `shift` is never safe for this reason and `evalOn` is only safe if the `ContextShift` in implicit scope represents the threadpool 
that we were running on before so that we shift back to where we were executing before.

`Blocker` as an abstraction for unbounded pool for blocking operations:
```scala
trait Blocker {
  def blockOn[F[_], A](fa: F[A])(implicit cs: ContextShift[F]): F[A]
}

blocker.blockOn(IO(readFile)) >> IO(println("Shifted back to the pool that CS represents"))
```
`Blocker`relies upon `ContextShift` for its actual behaviour and is simply a marker for a threadpool 
that is suitable for blocking operations. `blockOn` behaves exactly like `ContextShift#evalOn` - 
the provided `fa` will be run on the blocker's pool and then the continuation will run on the pool that `cs` represents.

Unfortunately with these abstractions we lose the ability to reason locally about what thread pool effects are running on.

Cats-Effect 3 introduces a re-designed typeclass `Async`:
```scala
trait Async[F[_]] {
  def evalOn[A](fa: F[A], ec: ExecutionContext): F[A]

  def executionContext: F[ExecutionContext]
}

IO(println("current pool")) >> IO.blocking(println("blocking pool")) >> IO(println("current pool"))
```
The execution shifts back to the threadpool defined by `Async#executionContext`. Also, Cats-Effect 3 has a builtin `blocking` 
which will shift execution to an internal blocking threadpool and shift it back afterwards using `Async`.