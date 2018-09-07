(ns raj.subscription
  (:require [raj.compose :refer [map-effect batch-effects]]))

(defn map-subscription [subscription callback]
  {:effect (map-effect (subscription :effect) callback)
   :cancel (subscription :cancel)})

(defn batch-subscriptions [subscriptions]
  (let [effects (map :effect subscriptions)
        cancels (map :cancel subscriptions)]
    {:effect (batch-effects effects)
     :cancel (batch-effects cancels)}))

(defn- -diff-sub [memo key cancel subscription]
  (cond
    (and cancel (not subscription))
    (update memo :cancel-map #(conj % cancel))

    (and (not cancel) subscription)
    (let [{:keys [effect cancel]} (subscription)]
      {:effects (conj (memo :effects) effect)
       :cancel-map (assoc (memo :cancel-map) key cancel)})

    cancel
    (update memo :cancel-map #(assoc % key cancel))

    :else memo))

(defn- -transition [cancel-map subscription-map]
  (let [cancel-keys (keys cancel-map)
        subscription-keys (keys subscription-map)
        keys (distinct (concat cancel-keys subscription-keys))
        {:keys [effects new-cancel-map]} (reduce
                                          (fn [memo key]
                                            (-diff-sub memo
                                                       key
                                                       (cancel-map key)
                                                       (subscription-map key)))
                                          {:effects []
                                           :cancel-map {}}
                                          keys)]
    {:effect (batch-effects effects)
     :cancel-map new-cancel-map}))

(defn with-subscriptions [subscriptions program]
  (let [[program-model program-effect] (program :init)
        {:keys [effect cancel-map]} (-transition {} (subscriptions program-model))
        init [{:cancel-map cancel-map
               :program-model program-model}
              (batch-effects [program-effect effect])]
        update (fn [msg model]
                 (let [update (program :update)
                       {:keys [program-model cancel-map]} model
                       {:keys [new-program-model program-effect]} (update msg program-model)
                       {:keys [effect new-cancel-map]} (-transition cancel-map (subscriptions new-program-model))]
                   [{:program-model new-program-model
                     :cancel-map new-cancel-map}
                    (batch-effects [program-effect effect])]))
        done (fn [model]
               (let [{:keys [program-model cancel-map]} model
                     {:keys [effect]} (-transition cancel-map {})
                     done (program :done)]
                 (do
                   (effect)
                   (when done (done program-model)))))]
    {:init init
     :update update
     :done done
     :view (program :view)}))
