(ns client.views.pgm
  (:require-macros [cljs.core.async.macros :refer [go]] )
  (:require [strokes :refer [d3]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [cljs.core.async :as async :refer [<! >! chan put! sub]]
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


(defn ^:private brushed [app owner]
  (let [brush (om/get-state owner :brush)
        rng (.extent brush)
        c (:chan @app)
        brush-empty? (.empty brush)
        low (nth rng 0)
        high (nth rng 1) ]
    (put! c {:low low
             :high high
             :brush-empty? brush-empty?})
    true))

(defcomponent graph [app owner opts]
    (init-state
      [_]
      (let [data (:data app)
            width (:width opts)
            height (:height opts)
            x (-> d3 .-scale (.linear) (.range [0 width]))
            y (-> d3 .-scale (.linear) (.range [height 0])) ]
        {:width width
         :height height
         :margin (:margin opts)
         :x x :y y
         :brush (-> d3 .-svg (.brush) (.x x))
         :x-axis (-> d3 .-svg (.axis) (.scale x) (.orient "bottom"))
         :y-axis (-> d3 .-svg (.axis) (.scale y) (.orient "left"))}))
    (did-mount
      [this]
      (let [brushg (om/get-node owner "brush")
            height (om/get-state owner :height)
            x-axis (om/get-state owner :x-axis)
            brush (om/get-state owner :brush)]
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
    (render-state
      [_ {:keys [x y width height]}]
      (let [data (:data app)
            domain (vec (->> data
                              (filter #(:visible %))
                              (map (fn [{:keys [timestamp]}] timestamp))  )) ]
        (println (clj->js domain))
        (-> x (.domain
                (-> d3 (.extent (clj->js domain) ))))
        (-> y (.domain [0 1]))
        (.log js/console (x))
        (html [:svg {:height height
                     :width width
                     :ref "svg"}
               [:g {:class "data"}
                (->> data
                     (filter #(:visible %))
                     (map (fn [{:keys [pressure timestamp]}]
                            [:rect {:style {:fill (nth pressure-colours (int pressure)) }
                                    :height height
                                    :width 2
                                    :x (x timestamp)}]))) ]
               [:g {:class "x axis" :ref "x-axis"}]
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

