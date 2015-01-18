(ns server.handlers.utils.res
  (:use plumbing.core))

(defmulti res (fn [blob & args]
                (cond
                  (contains? blob :msg) :200
                  (contains? blob :err) :500
                  :else :404)))

(defmethod res :200
  ([blob] (res blob :post))
  ([blob kind]
   (letk [[msg {data {}} & more] blob]
     (let [r {:message msg}
           body (if-not (empty? data)
                  (conj r {:data data})
                  r)]
     (merge
       {:status (if (= :post kind) 201 200)
        :headers {"Content-Type" "application/json"}
        :body body}
       more
       )))))

(defmethod res :500 [blob & args]
  (letk [[err {data {}} & more] blob]
    (let [r {:error err}
          body (if-not (empty? data)
                (conj r {:data data})
                r)]
      (println body)
      (println data)
    (merge
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body body}
      more
      ))))

(defmethod res :404 [blob & args]
  {:status 404
   :headers {"Content-Type" "application/json"}
   :body {:error "Not Found"} })

