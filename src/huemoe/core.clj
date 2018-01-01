(ns huemoe.core
  (:gen-class)
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [huemoe.bot :as bot]
            [huemoe.nrepl :as nrepl]
            [mount.core :as mount]))

(defn -main [& args]
  (mount/start)
  (while true
    (Thread/sleep Long/MAX_VALUE)))
