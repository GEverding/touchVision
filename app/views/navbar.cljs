(ns client.views.navbar
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn trigger-drawer [e]
  (do
    (.preventDefault e)
    (-> (sel1 :body)
        (dommy/toggle-class! "menu-active"))))

(defn navbar-html []
  (html [:nav {:class "navbar navbar-inverse navbar-default" :role "navigation" }
         [:div {:class "container-fluid"}
          [:div {:class "navbar-header"}
           [ :button {:type "button"
                      :class "navbar-toggle"
                      :data-toggle "collapse"
                      :data-target "#bs-example-navbar-collapse-1" }
            [:span {:class "sr-only"} "Toggle Navigation" ]
            [:span {:class "icon-bar"}]
            [:span {:class "icon-bar"}]
            [:span {:class "icon-bar"}]]
           [:a {:class "navbar-brand" :href="/"} "touchVision"]]
         [:div {:class "collapse navbar-collapse js-navbar-controls" :id "navbar-top" } ]]]))

(defn navbar-controls [app owner]
  (reify
    om/IRender
    (render [_] (navbar-html))))

