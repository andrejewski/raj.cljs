(ns raj.reagent
  (:require [raj.core :as raj]
            [reagent.core :as reagent]))

(defn reagent-component [create-program]
  (reagent/create-class
   {:component-did-mount (fn [this]
                           (let [props (reagent/props this)
                                 program (create-program props)
                                 state (reagent/atom nil)
                                 on-change (fn [model dispatch]
                                             (do
                                               (assoc this ::dispatch dispatch)
                                               (swap! state model)))
                                 kill-runtime (raj/runtime (assoc program :view on-change))]
                             (merge this {::kill-runtime kill-runtime
                                          ::view (:view program)
                                          ::state state})))
    :component-will-unmount (fn [this]
                              (let [kill-runtime (::kill-runtime this)]
                                (when kill-runtime (kill-runtime))))
    :render (fn [this]
              (let [state (-> this ::state deref)
                    view (::view this)
                    dispatch (::dispatch this)]
                (when view (view state dispatch))))}))