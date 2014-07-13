(ns client.views.pgm
  (:require-macros [cljs.core.async.macros :refer [go]] )
  (:require [strokes :refer [d3]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [cljs-time.core :as t :refer [now plus minutes hours]]
            [cljs-time.coerce :refer [to-long from-long]]
            [cljs.core.async :as async :refer [<! >! chan]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(strokes/bootstrap)

;; (defn Band
;;   "Create Gradient Band"
;;   [app owner]
;;   (reify
;;     om/IRender
;;     (render [_]
;;       (let [xScale (:x opts)
;;             offset (xScale app) ]
;;         (dom/rect #js {:color "red"
;;                        :height (:height opts)
;;                        :width 3
;;                        :x offset
;;                        }) ))))

;; (defn GradientSeries [app owner opts]
;;   (reify
;;     om/IRender
;;     (render [_]
;;       (let [width (:width opts)
;;             height (:height opts)
;;             height2 (:height2 opts)
;;             x (-> d3 .-time (.scale) (.range [0 width]))
;;             ;; x2 = d3.time.scale().range([0, width]),
;;             x2 (-> d3 .-time (.scale) (.range [0 width]))
;;             ;; y = d3.scale.linear().range([height, 0]),
;;             y (-> d3 .-scale (.linear) (.range [height 0]))
;;             ;; y2 = d3.scale.linear().range([height2, 0]);
;;             y2 (-> d3 .-scale (.linear) (.range [height2 0]))
;;             ;; xAxis = d3.svg.axis().scale(x).orient("bottom"),
;;             x-axis (-> d3 .-svg (.axis) (.scale x) (.orient "bottom"))
;;             ;; xAxis2 = d3.svg.axis().scale(x2).orient("bottom"),
;;             x-axis2 (-> d3 .-svg (.axis) (.scale x2) (.orient "bottom"))
;;             ;; yAxis = d3.svg.axis().scale(y).orient("left");
;;             y-axis (-> d3 .-svg (.axis) (.scale y) (.orient "left"))
;;             ]
;;         (-> x (.domain (-> d3
;;                            (.extent (:bands app)))))
;;         (-> y (.domain [0 1]))
;;         (apply dom/div nil
;;                (om/build-all Band (:bands app) {:opts (assoc opts :x x)})) ))))

(defn graph [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
      (let [data (:data app)
            width (:width opts)
            height (:height opts)
            x (-> d3 .-time (.scale) (.range [0 width]))
            y (-> d3 .-scale (.linear) (.range [height 0])) ]
        {:width width
         :height height
         :margin (:margin opts)
         :x x :y y
         :brush (-> d3 .-svg (.brush) (.x x))
         :x-axis (-> d3 .-svg (.axis) (.scale x) (.orient "bottom"))
         :y-axis (-> d3 .-svg (.axis) (.scale y) (.orient "left"))}))
    om/IDidMount
    (did-mount [this]
      (let [brushg (om/get-node owner "brush")
            height (om/get-state owner :height)
            brush (om/get-state owner :brush)]
        (.log js/console brushg)
        (.log js/console brush)
        (-> d3
            (.select brushg)
            (.call brush)
            (.selectAll "rect")
            (.attr "y" -6)
            (.attr "height" (+ height 7)))))
    om/IRenderState
    (render-state [_ {:keys [x y data width height]}]
      (let [data (:data app)]
        (-> x (.domain
                (-> d3
                    (.extent
                      (map
                        (fn [{:keys [timestamp]}] timestamp)
                        data)))))
        (-> y (.domain [0 1]))
        (html [:svg {:height height
                     :width width
                     :ref "svg"}
               [:g {:class "data"}
                (for [d data]
                  [:rect {:color "red"
                          :height height
                          :width 2
                          :x (x (:timestamp d))}])]
               [:g {:class "brush" :ref "brush"}]]) ) )))


(defn pgm-view [app owner]
  (reify
    om/IRender
    (render [_]
      (let [margin {:top 10 :right 40 :bottom 100 :left 40}
            margin2 {:top 430 :right 10 :bottom 20 :left 40}
            height (- 200 (:top margin) (:bottom margin))
            width  (- (-> js/window .-innerWidth) (:left margin) (:right margin))
            height2 (- 500 (:top margin2) (:bottom margin2))
            ]
        ;; (om/transact! app :bands (fn [_] data))
        (html [:div {:class "pgm-container js-graph-view"}
               (om/build graph app {:opts {:width width :height height
                                           :margin margin :margin2 margin2
                                           :height2 height2}})

               ])))))

