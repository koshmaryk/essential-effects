# The Effect Pattern Checklist
1. Does the type of the program tell us

   a. what **kind of effects** the program will perform; and

   b. what **type of value** it will produce?

2. When externally-visible side effects are required, is the effect description **separate** from the execution?

## Is `Option[A]` an effect?

1. Does the type of the program tell us
   
    a. what **kind of effects** the program will perform; and 
    ###### The Option type represents optionality. Optionality means a value may (or may not) exist.

    b. what **type of value** it will produce?
    ###### A value of type A, if one exists.
2. When externally-visible side effects are required, is the effect description **separate** from the execution?

    ###### No externally-visible side effects are required.

##### Therefore, Option is an effect.

## Is `Future[A]` an effect?

1. Does the type of the program tell us

   a. what **kind of effects** the program will perform; and
    ###### A Future represents an asynchronous computation.

   b. what **type of value** it will produce?
    ###### A value of type A, if the asynchronous computation is  successful.

2. When externally-visible side effects are required, is the effect description **separate** from the execution?

    ###### Externally-visible side effects are required: the body of a Future can do anything, including side effects.

    ###### But those side effects are not executed after the description of composed operations; the execution is scheduled immediately upon construction.

##### Therefore, Future does not separate effect description from execution: it is **unsafe**.
