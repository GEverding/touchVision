(ns server.handlers.util
  (:require [clojure.java.io :as io]
            [cheshire.core :refer [parse-string]]
            [taoensso.timbre :as timbre :refer [error]]
            [schema.utils :as sutil]))

;; convert the body to a reader. Useful for testing in the repl
;; where setting the body to a string is much simpler.
(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn parse-json
  " Helper Method to parse request body
    context: is the context passed to post! put!
    key: keyword to return the parsed data under
    parse: Schema parser"
  [context key parse]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (let [body (get-in context [:request :body])
            parsed (parse body) ]
        (if-not (sutil/error? parsed)
          [false {key  parsed}]
          (do
            (error "Error parsing: " body)
            (error "Parsing error: " parsed)
            [true {:error (sutil/error-val parsed)}]
            )))
      (catch Exception e
        (.printStackTrace e)
        {:message (format "Exception: %s" (.getMessage e))}))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

