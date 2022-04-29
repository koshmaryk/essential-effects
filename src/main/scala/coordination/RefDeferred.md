## `Ref[F, A]`
An asynchronous, concurrent mutable reference.

- Purely functional mutable reference
- Concurrent, lock-free
- Always contains a value
- Built on `IO` + `AtomicReference`

Provides safe concurrent access and modification of its content, but no functionality for synchronisation, which is instead handled by `Deferred`.

For this reason, a Ref is always initialised to a value.

The default implementation is nonblocking and lightweight, consisting essentially of a purely functional wrapper over an AtomicReference.

```scala
abstract class Ref[F[_], A] {
  def get: F[A]
  def set(a: A): F[Unit]
  def modify[B](f: A => (A, B)): F[B]
  // ... and more
}
```

- Created by giving an initial value
- Every op wrapped in `IO` for purity
- `A` is an _immutable_ structure
- Polymorphic in `F`

## `Deferred[F, A]` 
A purely functional synchronization primitive which represents a single value which may not yet be available.

- Purely functional synchronisation
- Simple one-shot semantics
- Semantic blocking 

### Semantic blocking
###### The fiber is waiting, but the underlying thread is free to run many other fibers

```scala
abstract class Deferred[F[_], A] {
  def get: F[A]
  def complete(a: A): F[Boolean]
}
```

- A `Deferred` starts empty
- At some point it will become full
- It never becomes empty or changes again

### `Get`
- on a full `Deferred`: immediately returns the value
- on an empty `Deferred`: semantically blocks until a value is available
- can be interrupted if needed
### `Complete`
- on an empty `Deferred`: fills the `Deferred` and awakes the readers
- on a full `Deferred`: fails

## Key idea
- `Ref`: I want to change a value atomically
- `Deferred`: I want to wait for something to happen