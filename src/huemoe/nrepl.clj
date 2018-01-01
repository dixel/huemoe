(ns huemoe.nrepl
  (:require [mount.core :as mount]
            [huemoe.config :as conf]
            [clojure.tools.nrepl.server :as nrepl]))

(mount/defstate nrepl
  :start (nrepl/start-server :port (conf/config :nrepl-port))
  :stop (nrepl/stop-server nrepl))
