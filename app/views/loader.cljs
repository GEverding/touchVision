(ns client.views.loader
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.match :as m]
            [cljs.core.async :as async :refer [<! pub put! chan sub sliding-buffer]]
            [cljs-log.core :as log]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [client.views.helper :refer (clear-screen)]
            [client.request :refer (r)]
            [sablono.core :as html :refer-macros [html]]))

(def ^:private l (log/get-logger "loader"))

(defn load-recording [app owner]
  (let [ws-pub-chan  (om/get-shared owner [:ws-chan])
        select (om/get-node owner "select")
        selected-index (aget select "selectedIndex")

        record-id (.-value (aget (.-options select) selected-index))]
    (if record-id
      (let [cb (r {:type :get :url (str "/recording/" record-id)})]
        (go
          (let [res (<! cb)]
            (if (= (:status res) 200)
             (let [out (:chan ws-pub-chan)
                    datoms (get-in res [:body :data])]
                (println datoms)
                (println out)
                (log/fine l "got datoms")
                (clear-screen owner)
                (put! out {:type :post :data datoms}))
              (js/alert "Not Valid Record"))))))))

(defn fetch-recordings [owner]

  (let [ch (r {:type :get
               :url "/recordings"})]
    (log/fine l "Fetcing new Recordings")
    (go (let [res (<! ch)]
          (if (= (:status res) 200)
            (do
              (om/set-state! owner :recordings (get-in res [:body :data]))))))))


(defcomponent select-row [data owner]
  (render
   [_]
   (html [:option
          {:value (:recording_id data)}
          (str (:recording_id data) "-" (.format (js/moment (:created_on data)) "l LTS"))])))

(defcomponent loader-view [app owner]
  (init-state
   [_]
   {:recordings []})
  (will-mount
   [_]
    (js/setInterval fetch-recordings 1000 owner))
  (render-state
   [_ state]
   (do
     ;; (log/fine l state)
     (html [:div {:class "loading-container col-md-12 col-sm-6"}
            [:h3 "Load Recording"]
            [:div.option-group
             [:div.col-sm-12
              [:select {:ref "select"}
               (om/build-all select-row (:recordings state))]]
             [:div.col-sm-12
              [:button {:type "button"
                        :class "btn btn-success btn-block"
                        :on-click (fn [_] (load-recording app owner))
                        } "Load"]]
             ]]))))
