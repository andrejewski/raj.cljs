(ns app.my-program
  (:require [raj.core :as raj]
            [raj.reagent :refer [reagent-component]]
            [reagent.core :as reagent]))

(def init
  [{::username ""
    ::password ""}])

(defn update [msg model context]
  (case (:type msg)
    ::set-username [(assoc model ::username (:username msg))]
    ::set-password [(assoc model ::password (:password msg))]))

(defn view [model dispatch context]
  [:div
   [:input {:type "text"
            :value (::username model)
            :on-change #(dispatch {:type ::set-username
                                   :username (-> % .-target .-value)})}]
   [:input {:type "password"
            :value (::password model)
            :on-change #(dispatch {:type ::set-password
                                   :password (-> % .-target .-value)})}]])

(defn make-program [deps]
  {:init init
   :update (raj/inject deps update)
   :view (raj/inject deps view)})

(defn ^:export run []
  (reagent/render
   (reagent-component make-program)
   (js/document.getElementById "app")))