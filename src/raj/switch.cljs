(ns raj.switch
  (:require [raj.compose :refer [map-effect batch-effects inject]]))

(defn- program-msg [data]
  {:type ::program-msg
   :data data})

(defn- router-msg [data]
  {:type ::router-msg
   :data data})

(defn- load-msg [data]
  {:type ::load-msg
   :data data})

(defn- is-promise?  [value]
  (-> value .-then fn?))

(defn- load-program [promise]
  (fn [dispatch]
    (-> promise
        (.then (fn [program] (dispatch {:type ::value :data program})))
        (.catch (fn [error] (dispatch {:type ::error :data error}))))))

(defn- -init [{:keys [initial-program router]}]
  (let [[initial-model initial-effect] (initial-program :init)
        router-subscription (router :subscribe)
        {:keys [router-effect cancel]} (router-subscription)]
    [{::route-cancel cancel
      ::route-emitter nil
      ::transitioning false
      ::current-program initial-program
      ::program-key nil
      ::program-model initial-model}
     (batch-effects [(map-effect initial-effect program-msg)
                     (map-effect router-effect router-msg)])]))

(defn- transition [model new-program context]
  (let [[new-program-model new-program-effect] (new-program :init)
        new-model (merge model {::current-program new-program
                                ::program-model new-program-model})
        old-done (-> model ::current-program :done)
        old-effect (when old-done #(old-done (model ::program-model)))
        new-effect (map-effect new-program-effect program-msg)]
    [new-model
     (batch-effects [old-effect new-effect])]))

(defn- keyed [key make-program]
  {:type ::keyed
   :key key
   :make-program make-program})

(defn- is-keyed? [map]
  (= (map :type) ::keyed))

(defn- create-emitter [initial-value]
  (let [last-value (atom initial-value)
        listeners (atom [])]
    {:emit (fn [value]
             (reset! last-value value)
             (doseq [listener @listeners]
               (listener value)))
     :subscribe (fn [listener]
                  (swap! listeners conj listener)
                  (do (listener @last-value)))}))

(defn- get-route-changes [route model context]
  (let [result ((context :get-route-changes) route {:keyed keyed})]
    (if (is-keyed? result)
      (let [{:keys [key make-program]} result
            {program-key ::program-key} model
            continuation (and program-key (= program-key key))]
        (if continuation
          (let [emit (-> model ::route-emitter :emit)
                effect (fn [] (emit route))]
            [model (::current-program model) effect]))
        (let [route-emitter (create-emitter route)
              program (make-program {:subscribe (route-emitter :subscribe)})
              new-model (merge model {::program-key key
                                      ::route-emitter route-emitter})]
          [new-model program]))
      [(merge model {::program-key nil
                     ::route-emitter nil})
       result])))

(defn- -update [msg model context]
  (case (msg :type)
    ::route-msg
    (let [route (msg :data)
          [new-model program effect] (get-route-changes route model context)
          change (if (is-promise? program)
                   [(assoc new-model ::transitioning true)
                    (map-effect (load-program program) load-msg)]
                   (transition model program context))]
      (if effect
        (let [[new-model new-effect] change]
          [new-model (batch-effects [effect new-effect])])
        change))

    ::load-msg
    (let [new-model (assoc model ::transitioning false)
          {type :type value :data} (msg :data)]
      (case type
        ::value
        (transition new-model value context)

        ::error
        (let [get-error-program (context :get-error-program)]
          (if get-error-program
            (let [error-program (get-error-program {:error value})]
              (transition new-model error-program context))
            (let [error-handler (context :error-handler)]
              (do
                (error-handler value)
                [new-model]))))))

    ::program-msg
    (let [program-update (-> model ::current-program :update)
          program-model (model ::program-model)
          [new-program-model effect] (program-update (msg :data) program-model)
          new-model (assoc model ::program-model new-program-model)]
      [new-model
       (map-effect effect program-msg)])))

(defn- -view [model dispatch context]
  (let [view-model (-> model ::program-model)
        view-dispatch #(-> % program-msg dispatch)
        view-fn (-> model ::current-program :view)
        sub-view (view-fn view-model view-dispatch)
        {container-view ::container-view} context]
    (if container-view
      (container-view sub-view {:transitioning (::transitioning model)})
      sub-view)))

(defn- -done [model context]
  (let [sub-done (-> model ::current-program :done)
        router-done (-> model ::router-cancel)]
    (do
      (when sub-done (-> model ::program-model sub-done))
      (when router-done (router-done)))))

(defn switch [context]
  {:init (-init context)
   :update (inject context -update)
   :view (inject context -view)
   :done (inject context -done)})