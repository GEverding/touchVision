(ns client.views.pgm
  (:require-macros [cljs.core.async.macros :refer [go-loop]] )
  (:require [strokes :refer [d3]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [cljs.core.async :as async :refer [<! >! chan put! sub sliding-buffer]]
            [cljs-log.core :as log]
            [om-tools.core :refer-macros (defcomponent)]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(def ^:private pressure-colours ["#A52A2A"
                                 "#C25051"
                                 "#E09432"
                                 "#EFBD2E"
                                 "#73B845"
                                 "#009BDD"])

(strokes/bootstrap)
(defonce ^:private l (log/get-logger "pgm"))

(defn ^:private brushed [app owner]
  (let [brush (om/get-state owner [:d3-props :brush])
        rng (.extent brush)
        ch (:select-chan (om/get-shared owner))
        brush-empty? (.empty brush)
        low (nth rng 0)
        high (nth rng 1) ]
    (put! ch {:low low :high high})
    true))

(defcomponent graph [app owner opts]
    (init-state
      [_]
      (let [width (:width opts)
            height (:height opts)
            x (-> d3 .-scale (.linear) (.range [0 width]))
            y (-> d3 .-scale (.linear) (.range [height 0])) ]
        {:d3-props
         {:width width
          :height height
          :margin (:margin opts)
          :x x :y y
          :brush (-> d3 .-svg (.brush) (.x x))
          :x-axis (-> d3 .-svg (.axis) (.scale x) (.orient "bottom"))
          :y-axis (-> d3 .-svg (.axis) (.scale y) (.orient "left"))}
         :datoms []}))
  (will-mount
    [_]
    (let [ws (:ws-chan (om/get-shared owner))
          ch (chan (sliding-buffer 25))]
      (sub ws :post ch)
      (go-loop
        [m (<! ch)]
        (when m
          (do
            (log/finest l m)
            (om/update-state! owner :datoms #(conj % (:data m)))
            (recur (<! ch)))) )))
    (did-mount
      [this]
      (let [brushg (om/get-node owner "brush")
            height (om/get-state owner [:d3-props :height])
            x-axis (om/get-state owner [:d3-props :x-axis])
            brush (om/get-state owner [:d3-props :brush])]
        (-> d3
            (.select brushg)
            (.call brush)
            (.selectAll "rect")
            (.attr "y" -6)
            (.attr "height" (+ height 7)))
        (-> d3
            (.select (om/get-node owner "x-axis"))
            (.call x-axis))
        (-> brush
            (.on "brushend" (partial brushed app owner)))))
    (did-update
      [_ _ _]
      (let [x-axis (om/get-state owner [:d3-props :x-axis])]
        (-> d3
            (.select (om/get-node owner "x-axis"))
            (.call x-axis))))
    (render-state
      [_ {:keys [d3-props datoms]}]
      (let [{:keys [x y width height]} d3-props]
        (html [:svg {:height (+ height 30) :width width :ref "svg"}
               (when (< 0 (count datoms))
                 (let [domain (->> datoms
                                   ;(filter #(:visible %))
                                   (mapv (fn [{:keys [timestamp]}] timestamp)))]
                   ;; (println (clj->js domain))
                   (-> x (.domain
                           (-> d3 (.extent (clj->js domain) ))))
                   (-> y (.domain [0 1]))
                   [:g {:class "data"}
                    (->> datoms
                         ;(filter #(:visible %))
                         (map (fn [{:keys [pressure timestamp]}]
                                [:rect {:style {:fill (nth pressure-colours (int pressure)) }
                                        :height height
                                        :width 2
                                        :x (x timestamp)}]))) ]))
               [:g {:class "x axis" :transform (str "translate(0," height ")") :ref "x-axis"}]
               [:g {:class "brush" :ref "brush"}]]))))

(defcomponent pgm-view [app owner]
  (render
    [_]
    (let [margin {:top 10 :right 40 :bottom 100 :left 40}
          height (- 200 (:top margin) (:bottom margin))
          width  (- (-> js/window .-innerWidth) (:left margin) (:right margin)) ]
      (html [:div {:class "pgm-container js-graph-view"}
             (om/build graph app {:opts {:width width
                                         :height height
                                         :margin margin }})

             ]))))

