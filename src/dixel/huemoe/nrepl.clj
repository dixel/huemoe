(ns dixel.huemoe.nrepl
  (:require [clojure.tools.nrepl.server :as nrepl]
            [cyrus-config.core :as cfg]
            [mount.core :as mount]))

(cfg/def nrepl-port "NREPL port" {:spec int?
                                  :required false
                                  :default 52001})

(mount/defstate nrepl
  :start (nrepl/start-server :port nrepl-port)
  :stop (nrepl/stop-server nrepl))
