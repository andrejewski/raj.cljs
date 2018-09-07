(ns raj.core)

(defn runtime [{:keys [init, update, done, view]}]
  (let [state (atom nil)
        running (atom true)
        change (fn [[newState effect] dispatch]
                 (when effect (effect dispatch))
                 (reset! state newState)
                 (view @state dispatch))
        dispatch (fn dispatch [message]
                   (when @running
                     (-> message
                         (update @state)
                         (change dispatch))))]
    (change init dispatch)
    (fn []
      (reset! running false)
      (when done (done @state)))))
