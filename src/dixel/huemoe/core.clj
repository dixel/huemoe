(ns dixel.huemoe.core
  (:gen-class)
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [dixel.huemoe.bot :as bot]
            [dixel.huemoe.nrepl :as nrepl]
            [mount.core :as mount]))

(defn -main [& args]
  (mount/start)
  (while true
    (Thread/sleep Long/MAX_VALUE)))
