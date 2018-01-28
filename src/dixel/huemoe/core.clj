(ns dixel.huemoe.core
  (:gen-class)
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [cyrus-config.core :as cfg]
            [dixel.huemoe.bot-test :as bot]
            [dixel.huemoe.nrepl :as nrepl]
            [mount.core :as mount]
            [taoensso.timbre :as log]))

(defn -main [& args]
  (cfg/validate!)
  (log/info (cfg/show))
  (mount/start)
  (while true
    (Thread/sleep Long/MAX_VALUE)))
