(ns huemoe.logging
  (:require [mount.core :as mount]
            [taoensso.timbre :as logger]))

(mount/defstate logging
  :start "logging")
