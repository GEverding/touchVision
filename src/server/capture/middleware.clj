(ns server.capture.middleware
  )

(defn wrap-capture-channel
  [handler capture]
  (fn [req]
    (let [res (handler (assoc-in req [:resources :capture] capture))]
      res)))
