(ns raj.compose)

(defn map-effect [effect, callback]
  (when effect
    (fn [msg] (effect (callback msg)))))

(defn batch-effects [effects]
  (fn _batch-effects [msg]
    (map (fn [effect] (effect msg)) effects)))

(defn batch-programs
  [programs, container-view]
  (let [initial-states (map #(-> % :init (nth 0)) programs)
        initial-effects (map #(-> % :init (nth 1)) programs)
        init [initial-states (batch-effects initial-effects)]
        update (fn [{:keys [index value]} model]
                 (let [program-model (nth model index)
                       program-update (:update (nth programs index))
                       [new-model new-effect] (program-update value program-model)]
                   [(assoc model index new-model) (map-effect new-effect (fn [value] {:index index :value value}))]))
        view (fn [models dispatch]
               (container-view (map-indexed
                                (fn [index program]
                                  (fn []
                                    ((:view program) (nth models index) #(dispatch {:index index :value %}))))
                                programs)))
        done (fn [models]
               (map-indexed
                (fn [index model]
                  (let [done (-> programs (nth index) :done)]
                    (when done (done model))))
                models))]
    {:init init
     :update update
     :view view
     :done done}))

(defn inject [context func]
  (fn [& args] (apply func (conj args context))))