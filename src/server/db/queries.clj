(ns server.db.queries
  (:require [yesql.core :refer [defqueries]]))

(defqueries "sql/queries.sql")
