# Raj.cljs

> [Raj](https://github.com/andrejewski/raj) is the Elm Architecture for JavaScript.<br>
  Raj.cljs is the Raj architecture for ClojureScript.

This project ports the Raj framework and some libraries to ClojureScript:

- [`raj`](https://github.com/andrejewski/raj) => `raj.core`
- [`raj-compose`](https://github.com/andrejewski/raj-compose) => `raj.compose`
- [`raj-react`](https://github.com/andrejewski/raj-react) => `raj.reagent`
- [`raj-spa`](https://github.com/andrejewski/raj-spa) => `raj.switch`
- [`raj-subscription`](https://github.com/andrejewski/raj-subscription) => `raj.subscription`

Just as Raj sought to make the Elm architecture idiomatic for JavaScript developers,
Raj.cljs should be made for ClojureScript developers.

**This is not production ready. I am not an experienced Clojure/Script developer.
  If this is interesting to you, please help me make this better.**

## Motivation

I am happy with Raj as a JavaScript framework.
With Raj, I only use a strict subset of JavaScript and much of its quirks can be distracting.
Looking to improve my development of applications, it may be the biggest gains lie in a change of language.

This project is an experiment to see how well Raj fits Clojure.
I have always been a fan of Clojure and the philosophy behind its design.
I think Clojure is worth exploring for Raj applications because:

- Immutability is first-class.
  There is so much anxiety for application and libraries writers in mutable languages.
  For 99% of use cases, I don't want worry about data shifting underneath me.

  - I will often avoid Maps and Sets in JavaScript because they have an imperative, mutable API.
    I will find ways to maintain invariants myself using arrays and object wrappers.
    In Clojure, boom! Immutable maps `{:key "value"}` and sets `#{1 2 3}` with common functions around them.

  - Clojure/Script strikes a good balance of having immutability by default and reasonable
      escape hatches into an underlying, mutable ecosystem (Java, JavaScript).
    This meshes well with how Raj forces side-effects/OOP to the edges.

- The language is geared toward data.
  Raj programs are data-driven and Clojure has a rich library of functions to work with a small set of powerful primitives.

- The king in ClojureScript right now is [Re-frame](https://github.com/Day8/re-frame).
  I enjoyed my time with Re-frame and it was a big influence on my first stabs at what would become Raj.
  Of course, Elm's simplicity blew Re-frame away.
  I think there's a lot of potential in Raj for ClojureScript developers
   as being a simpler, more scalable, and less complicated choice.

## Notes

- I did a fair bit of reading about `core.async` for the Raj.cljs side-effect story.
  With Raj.js, I choose to ignore completely Promises/Streams/Iterators precisely because it's not a problem
    that my UI or business logic need.
  With Raj.cljs, I think CSP is *cool* but again not something we need.
  I am open to convincing, but I believe having side-effect functions passing messages is far better and
    can encapsulate all styles of Promises/Streams/Iterators/Channels.
  Channels, in my opinion, are too open-ended in that most contracts should not expose both put and take
    operations to any particular function.
  Functions, you can only call those; simple.

- `raj-compose/assembleProgram` was replaced with `raj.compose/inject`.
  The reasoning here is that it's more idiomatic to define program parts as top-level functions.
  This is something that I may bring back to `raj-compose` as it's more granular and does not have
    special keys like `dataOptions`.

- `(defn update [msg model])` conflicts with Clojure's `update`.
  Maybe would should name this function differently (`change`?).
  I kept it the same for now.

## TODO

- [ ] Publish to Clojars
- [ ] Support more or less asynchronous lazy-loading forms in `raj.switch` 
- [ ] Write tests 
