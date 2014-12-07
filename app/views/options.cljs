(ns client.views.options
  (:require-macros [cljs.core.async.macros :refer [go-loop]] )
  (:require [strokes :refer [d3]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [cljs-time.core :as t :refer [now plus minutes hours]]
            [cljs-time.coerce :refer [to-long from-long]]
            [cljs.core.async :as async :refer [put! <! >! chan merge]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [client.request :refer [request]]))

(defn- success [res]
  (.log js/console "done!"))
(defn- fail [error]
  (.log js/console (str "error: " error)))

(defn- update [url data]
  (request {:type :post
            :url url }
           data
           :success success
           :fail fail ))

(defn- handle-change
  ([app owner chan k]
   (let [v (om/get-state owner k)
         new-val (not v)]
     (om/set-state! owner k new-val)
     (om/transact! app :filtered (fn [_] false))
     (om/transact! app :visible (fn [_] true))
     (put! chan {k new-val})))
  ([app owner chan k v]
   (do
     (om/set-state! owner k v)
     (om/transact! app :filtered (fn [_] false))
     (om/transact! app :data (fn [_] []))
     (put! chan {k v})))
  )

(defn switch
  "Button Switch Component"
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
      ;; FIXME should be passed in
      {:mode :fake})
    om/IRenderState
    (render-state [_ state]
      (let [current-mode (:mode state)
            c (:stream-chan state)
            is-active? (fn [mode] (= mode (:mode state))) ]
        (html [:div {:class "switch-container"}
               [:button {:type "button"
                         :class (str "btn btn-info col-lg-6 "
                                     (if (is-active? :live)
                                       "active"
                                       ""))
                         :ref "live"
                        :on-click #(handle-change app owner c :mode :live )
                         } "Live"]
               [:button {:type "button"
                         :class (str "btn btn-info col-lg-6 "
                                     (if (is-active? :fake)
                                       "active"
                                       ""))
                         :ref "fake"
                        :on-click #(handle-change app owner c :mode :fake )
                         } "Fake"]]
              )))))

(defn toggle [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:toggle false})
    om/IRenderState
    (render-state [_ state]
      (let []
      (html [:div {:class " col-lg-12 toggle-container"}
              [:button {:type "button"
                        :class (str "btn btn-warning col-lg-12 btn-block" (if (:toggle state)
                                                         "active"
                                                         ""))
                        :ref "run"
                        :on-click #(handle-change app owner (:toggle-chan state) :toggle )
                        } "Run"]])))))

(defn options-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:stream-chan (chan)
       :toggle-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [stream (om/get-state owner :stream-chan)
            state (om/get-state owner :toggle-chan)
            c (merge [stream state]) ]
        (go-loop
          []
          (let [m (<! c)]
            (print m)
            (cond
              (contains? m :toggle ) ( update "/switch/mode" m)
              (contains? m :stream ) ( update "/switch/stream" m))
            (recur)
            ))) )
    om/IRenderState
    (render-state [_ state]
      (let [stream (om/get-state owner :stream-chan)
            mode (om/get-state owner :toggle-chan)]
        (html [:div {:class "row option-container"}
               [:h3.col-lg-12 "Options"]
               [:div {:class "col-lg-12 option-group"}
                (om/build switch app
                          {:init-state {:stream-chan stream}})]
               [:div {:class "option-group"}
                (om/build toggle app
                          {:init-state {:toggle-chan mode}})]])))))

